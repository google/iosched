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
package com.google.samples.apps.iosched.server.schedule.server.cloudstorage;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.tools.cloudstorage.GcsFileMetadata;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.RemoteJsonHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * Handle all interaction with GoogleCloudStorage.
 *
 */
public class CloudFileManager {

  private static final String DEFAULT_CHARSET_NAME = "UTF-8";

  private final GcsService gcsService = GcsServiceFactory.createGcsService(
      RetryParams.getDefaultInstance());

  private final String defaultBucket;
  private final GcsFilename productionManifestFile;
  private final GcsFilename stagingManifestFile;

  public CloudFileManager() {
    defaultBucket = Config.CLOUD_STORAGE_BUCKET;
    productionManifestFile = new GcsFilename(defaultBucket, Config.MANIFEST_NAME);
    stagingManifestFile = new GcsFilename(defaultBucket, Config.MANIFEST_NAME_STAGING);
  }

  static public byte[] calulateHash(JsonElement contents) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new InternalError("MD5 MessageDigest is not available");
    }
    OutputStream byteSink = new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        // ignore, since this is only used to calculate MD5
      }
    };
    DigestOutputStream dis = new DigestOutputStream(byteSink, md);
    new Gson().toJson(contents, new OutputStreamWriter(dis, Charset.forName(DEFAULT_CHARSET_NAME)));
    return dis.getMessageDigest().digest();
  }

  /**
   * Create or update a file in a GCC bucket, using the default ACL for the bucket.
   *
   * @param filename Name of file to create
   * @param contents File contents
   * @param shortCache If true, sets cache expiry to 0 sec. Otherwise, cache expiry is set to 6,000 sec.
   * @throws IOException
   */
  public void createOrUpdate(String filename, JsonElement contents, boolean shortCache)
      throws IOException {
    GcsFilename file = new GcsFilename(defaultBucket, filename);
    GcsFileOptions options = new GcsFileOptions.Builder()
      .mimeType("application/json")
      .cacheControl("public, max-age="+(shortCache?0:6000))
      .build();
    GcsOutputChannel writeChannel = null;
    try {
      writeChannel = gcsService.createOrReplace(file, options);
      Writer writer = Channels.newWriter(writeChannel, DEFAULT_CHARSET_NAME);
      new Gson().toJson(contents, writer);
      writer.flush();
    } finally {
      if (writeChannel != null) {
        writeChannel.close();
      }
    }
  }

  public String getBucketName() {
    return defaultBucket;
  }

  public String getProductionManifestURL() {
    return productionManifestFile.getBucketName() + "/" + productionManifestFile.getObjectName();
  }

  public String getStagingManifestURL() {
    return stagingManifestFile.getBucketName() + "/" + stagingManifestFile.getObjectName();
  }

  public JsonObject readProductionManifest() throws IOException {
    return readFileAsJsonObject(productionManifestFile);
  }

  public JsonObject readStagingManifest() throws IOException {
    return readFileAsJsonObject(stagingManifestFile);
  }

  public void createOrUpdateProductionManifest(JsonObject contents) throws IOException {
    createOrUpdate(Config.MANIFEST_NAME, contents, true);
  }

  public void createOrUpdateStagingManifest(JsonObject contents) throws IOException {
    createOrUpdate(Config.MANIFEST_NAME_STAGING, contents, true);
  }

  public JsonObject readFileAsJsonObject(String filename) throws IOException {
    return readFileAsJsonObject(new GcsFilename(defaultBucket, filename));
  }

  public JsonObject readFileAsJsonObject(GcsFilename file) throws IOException {
    GcsFileMetadata metadata = gcsService.getMetadata(file);
    if (metadata == null) {
      if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Development) {
        // In the development server, try to fetch files on cloud storage via HTTP
        Logger.getAnonymousLogger().info("fetching "+file.getObjectName()+" at "+Config.CLOUD_STORAGE_BASE_URL+file.getObjectName());
        return RemoteJsonHelper.fetchJsonFromPublicURL(Config.CLOUD_STORAGE_BASE_URL+file.getObjectName());
      }
      return null;
    }
    GcsInputChannel readChannel = null;
    try {
      readChannel = gcsService.openReadChannel(file, 0);
      JsonElement element = new JsonParser().parse(Channels.newReader(readChannel,
          DEFAULT_CHARSET_NAME));
      return element.getAsJsonObject();
    } finally {
      if (readChannel != null) {
        readChannel.close();
      }
    }
  }
}
