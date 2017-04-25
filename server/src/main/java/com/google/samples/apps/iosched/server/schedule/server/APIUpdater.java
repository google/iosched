/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.server.schedule.server;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.ShortBlob;
import com.google.appengine.api.mail.MailService.Message;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.EntityFetcher;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.RemoteFilesEntityFetcherFactory;
import com.google.samples.apps.iosched.server.schedule.model.DataCheck;
import com.google.samples.apps.iosched.server.schedule.model.DataCheck.CheckFailure;
import com.google.samples.apps.iosched.server.schedule.model.DataCheck.CheckResult;
import com.google.samples.apps.iosched.server.schedule.model.DataExtractor;
import com.google.samples.apps.iosched.server.schedule.model.JsonDataSource;
import com.google.samples.apps.iosched.server.schedule.model.JsonDataSources;
import com.google.samples.apps.iosched.server.schedule.server.cloudstorage.CloudFileManager;
import com.google.samples.apps.iosched.server.schedule.server.input.ExtraInput;
import com.google.samples.apps.iosched.server.schedule.server.input.VendorStaticInput;
import com.google.samples.apps.iosched.server.schedule.server.input.fetcher.CloudStorageRemoteFilesEntityFetcher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Basic class that starts the API Updater flow.
 */
public class APIUpdater {

  public static final int ADMIN_MESSAGE_SIZE_LIMIT = 10000;

  public void run(boolean force, boolean obfuscate, OutputStream optionalOutput) throws IOException {

    RemoteFilesEntityFetcherFactory.setBuilder(new RemoteFilesEntityFetcherFactory.FetcherBuilder() {
        String[] filenames;

        @Override
        public RemoteFilesEntityFetcherFactory.FetcherBuilder setSourceFiles(String... filenames) {
            this.filenames = filenames;
            return this;
        }

        @Override
        public EntityFetcher build() {
            return new CloudStorageRemoteFilesEntityFetcher(filenames);
        }
    });

    UpdateRunLogger logger = new UpdateRunLogger();
    CloudFileManager fileManager = new CloudFileManager();
    
    logger.startTimer();
    JsonDataSources sources = new ExtraInput().fetchAllDataSources();
    logger.stopTimer("fetchExtraAPI");

    logger.startTimer();
    sources.putAll(new VendorStaticInput().fetchAllDataSources());
    logger.stopTimer("fetchVendorStaticAPI");

    logger.startTimer();
    JsonObject newData = new DataExtractor(obfuscate).extractFromDataSources(sources);
    logger.stopTimer("extractOurData");

    logger.startTimer();
    byte[] newHash = CloudFileManager.calulateHash(newData);
    logger.stopTimer("calculateHash");

    // compare current Vendor API log with the one from previous run:
    logger.startTimer();
    if (!force && isUpToDate(newHash, logger)) {
      logger.logNoopRun();
      return;
    }
    logger.stopTimer("compareHash");

    logger.startTimer();
    ManifestData dataProduction = extractManifestData(fileManager.readProductionManifest(), null);
    //ManifestData dataStaging = extractManifestData(fileManager.readStagingManifest(), dataProduction);

    logger.stopTimer("readManifest");

    JsonWriter optionalOutputWriter = null;

    logger.startTimer();
    // Upload a new version of the sessions file
    if (optionalOutput != null) {
      // send data to the outputstream
      Writer writer = Channels.newWriter(Channels.newChannel(optionalOutput), "UTF-8");
      optionalOutputWriter = new JsonWriter(writer);
      optionalOutputWriter.setIndent("  ");
      new Gson().toJson(newData, optionalOutputWriter);
      optionalOutputWriter.flush();
    } else {
      // save data to the CloudStorage
      fileManager.createOrUpdate(dataProduction.sessionsFilename, newData, false);
    }
    logger.stopTimer("uploadNewSessionsFile");

    // Check data consistency
    logger.startTimer();
    DataCheck checker = new DataCheck(fileManager);
    CheckResult result = checker.check(sources, newData, dataProduction);
    if (!result.failures.isEmpty()) {
      reportDataCheckFailures(result, optionalOutput);
    }
    logger.stopTimer("runDataCheck");

    if (optionalOutput == null) {
      // Only update manifest and log if saving to persistent storage

      logger.startTimer();

      // Create new manifests
      JsonObject newProductionManifest = new JsonObject();
      newProductionManifest.add("format", new JsonPrimitive(Config.MANIFEST_FORMAT_VERSION));
      newProductionManifest.add("data_files", dataProduction.dataFiles);

      JsonObject newStagingManifest = new JsonObject();
      newStagingManifest.add("format", new JsonPrimitive(Config.MANIFEST_FORMAT_VERSION));
     // newStagingManifest.add("data_files", dataStaging.dataFiles);

      // save manifests to the CloudStorage
      fileManager.createOrUpdateProductionManifest(newProductionManifest);
      fileManager.createOrUpdateStagingManifest(newStagingManifest);

      try {
        // notify production GCM server:
        new GCMPing().notifyGCMServer(Config.GCM_URL, Config.GCM_API_KEY);
      } catch (Throwable t) {
        Logger.getLogger(APIUpdater.class.getName()).log(Level.SEVERE, "Error while pinging GCM server", t);
      }

      logger.stopTimer("uploadManifest");

      logger.logUpdateRun(dataProduction.majorVersion, dataProduction.minorVersion,
          dataProduction.sessionsFilename, newHash, newData, force);
    }

  }

  private void reportDataCheckFailures(CheckResult result, OutputStream optionalOutput) throws IOException {
    StringBuilder errorMessage = new StringBuilder();
    errorMessage.append(
        "\nHey,\n\n"
        + "(this message is autogenerated)\n"
        + "\n** UPDATE: ignore the part of the message below that says the updater is halted. Halting the updating process is not implemented yet. **\n\n"
        + "The IOSched 2014 data updater is halted because of inconsistent data.\n"
        + "Please, check the messages below and fix the sources. "
        /*+ "If you are ok with the data "
        + "even in an inconsistent state, you or other app admin will need to force an update by "
        + "clicking on the \"Force update\" button at https://iosched-updater-dev.appspot.com/admin/\n\n"*/
        + "\n\n" + result.failures.size() + " data non-compliances:\n");
    for (CheckFailure f: result.failures) {
      errorMessage.append(f).append("\n\n");
    }

    // Log error message to syslog, so that it's available even if the log is truncated.
    Logger syslog = Logger.getLogger(APIUpdater.class.getName());
    syslog.log(Level.SEVERE, errorMessage.toString());

    // Send email with error message to project admins.
    if (SystemProperty.environment.value() != SystemProperty.Environment.Value.Development ||
        optionalOutput == null) {
      // send email if user is not running in dev or interactive mode (show=true)
      Message message = new Message();
      message.setSender(Config.EMAIL_FROM);
      message.setSubject("[iosched-data-error] Updater - Inconsistent data");

      String errorMessageStr = errorMessage.toString();
      if (errorMessageStr.length() > ADMIN_MESSAGE_SIZE_LIMIT) {
        int truncatedChars = errorMessage.length() - ADMIN_MESSAGE_SIZE_LIMIT;
        errorMessageStr = errorMessageStr.substring(0, ADMIN_MESSAGE_SIZE_LIMIT);
        errorMessageStr +=
                "\n\n--- MESSAGE TRUNCATED, " + truncatedChars + " CHARS REMAINING (CHECK LOG) ---";
      }
      message.setTextBody(errorMessageStr);
      // TODO(arthurthompson): Reimplement mailing, it currently fails due to invalid sender.
      //MailServiceFactory.getMailService().sendToAdmins(message);
    } else {
      // dump errors to optionalOutput
      optionalOutput.write(errorMessage.toString().getBytes());
    }
  }


  private ManifestData extractManifestData(JsonObject currentManifest, ManifestData copyFrom) {
    ManifestData data = new ManifestData();
    data.majorVersion = copyFrom == null ? Config.MANIFEST_VERSION : copyFrom.majorVersion;
    data.minorVersion = copyFrom == null ? 0 : copyFrom.minorVersion;
    data.dataFiles = new JsonArray();

    if (currentManifest != null) {
      try {
        JsonArray files = currentManifest.get("data_files").getAsJsonArray();
        for (JsonElement file: files) {
          String filename = file.getAsString();
          Matcher matcher = Config.SESSIONS_PATTERN.matcher(filename);
          if (matcher.matches()) {
            // versions numbers are only extracted from the existing session filename if copyFrom is null.
            if (copyFrom == null) {
              data.majorVersion = Integer.parseInt(matcher.group(1));
              data.minorVersion = Integer.parseInt(matcher.group(2));
            }
          } else {
            data.dataFiles.add(file);
          }
        }
      } catch (NullPointerException ex) {
        Logger.getLogger(getClass().getName()).warning("Ignoring existing manifest, as it seems "
            + "to be badly formatted.");
      } catch (NumberFormatException ex) {
        Logger.getLogger(getClass().getName()).warning("Ignoring existing manifest, as it seems "
            + "to be badly formatted.");
      }
    }

    if (copyFrom == null) {
      // Increment the minor version
      data.minorVersion++;
      data.sessionsFilename = MessageFormat.format(Config.SESSIONS_FORMAT, data.majorVersion, data.minorVersion);
    } else {
      data.sessionsFilename = copyFrom.sessionsFilename;
    }

    data.dataFiles.add(new JsonPrimitive( data.sessionsFilename ));
    return data;
  }


  private boolean isUpToDate(byte[] newHash, UpdateRunLogger logger) {
    Entity lastUpdate = logger.getLastRun();
    byte[] currentHash = null;
    if (lastUpdate != null) {
      ShortBlob hash = (ShortBlob) lastUpdate.getProperty("hash");
      if (hash != null) {
        currentHash = hash.getBytes();
      }
    }

    return Arrays.equals(currentHash, newHash);
  }

}
