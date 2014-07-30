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
package com.google.iosched.server;

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
import com.google.iosched.Config;
import com.google.iosched.input.fetcher.EntityFetcher;
import com.google.iosched.input.fetcher.RemoteFilesEntityFetcherFactory;
import com.google.iosched.input.fetcher.RemoteFilesEntityFetcherFactory.FetcherBuilder;
import com.google.iosched.model.DataCheck;
import com.google.iosched.model.DataCheck.CheckFailure;
import com.google.iosched.model.DataCheck.CheckResult;
import com.google.iosched.model.DataExtractor;
import com.google.iosched.model.JsonDataSources;
import com.google.iosched.server.cloudstorage.CloudFileManager;
import com.google.iosched.server.input.ExtraInput;
import com.google.iosched.server.input.VendorStaticInput;
import com.google.iosched.server.input.fetcher.CloudStorageRemoteFilesEntityFetcher;

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

  public void run(boolean force, OutputStream optionalOutput) throws IOException {

    RemoteFilesEntityFetcherFactory.setBuilder(new FetcherBuilder() {
      String[] filenames;
      @Override
      public FetcherBuilder setSourceFiles(String... filenames) {
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
    JsonObject newData = new DataExtractor().extractFromDataSources(sources);
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
    ManifestData dataStaging = extractManifestData(fileManager.readStagingManifest(), dataProduction);

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
      newStagingManifest.add("data_files", dataStaging.dataFiles);

      // save manifests to the CloudStorage
      fileManager.createOrUpdateProductionManifest(newProductionManifest);
      fileManager.createOrUpdateStagingManifest(newStagingManifest);

      try {
        // notify production GCM server:
        new GCMPing().notifyGCMServer(Config.GCM_PROD, Config.GCM_API_KEY_PROD);
      } catch (Throwable t) {
        Logger.getLogger(APIUpdater.class.getName()).log(Level.SEVERE, "error while pinging production GCM server", t);
      }

      try {
        // notify staging GCM server:
        new GCMPing().notifyGCMServer(Config.GCM_DEV, Config.GCM_API_KEY_DEV);
      } catch (Throwable t) {
        Logger.getLogger(APIUpdater.class.getName()).log(Level.WARNING, "error while pinging staging GCM server", t);
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
        + "The iosched data updater ran but found some inconsistent data.\n"
        + "Please, check the messages below and fix the sources. "
        + "\n\n" + result.failures.size() + " data non-compliances:\n");
    for (CheckFailure f: result.failures) {
      errorMessage.append(f).append("\n\n");
    }

    if (SystemProperty.environment.value() != SystemProperty.Environment.Value.Development ||
        optionalOutput == null) {
      // send email if user is not running in dev or interactive mode (show=true)
      Message message = new Message();
      message.setSender(Config.EMAIL_TO_SEND_UPDATE_ERRORS);
      message.setSubject("[iosched-data-error] Updater - Inconsistent data");

      message.setTextBody(errorMessage.toString());
      MailServiceFactory.getMailService().sendToAdmins(message);
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
