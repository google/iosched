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
package com.google.iosched.server.input.fetcher;

import com.google.appengine.api.utils.SystemProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.iosched.Config;
import com.google.iosched.input.fetcher.EntityFetcher;
import com.google.iosched.input.fetcher.RemoteJsonHelper;
import com.google.iosched.server.cloudstorage.CloudFileManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * EntityFetcher that fetches entities from a set of files stored in CloudStorage.
 */
public class CloudStorageRemoteFilesEntityFetcher implements EntityFetcher {

  private static final Logger LOGGER = Logger.getLogger(CloudStorageRemoteFilesEntityFetcher.class.getName());

  private CloudFileManager fileManager;
  private String[] filenames;
  private JsonObject object;

  public CloudStorageRemoteFilesEntityFetcher(String... filenames) {
    this.fileManager = new CloudFileManager();
    this.filenames = filenames;
  }

  @Override
  public JsonElement fetch(Enum<?> entityType, Map<String, String> params)
      throws IOException {
    // On the first call, read all the files
    if (object == null) {
      object = new JsonObject();
      for (String filename: filenames) {
        JsonObject obj = fileManager.readFileAsJsonObject(filename);
        if (obj == null &&
            SystemProperty.environment.value() == SystemProperty.Environment.Value.Development) {
          // In the development server, cloud storage files cannot be directly accessed.
          obj = RemoteJsonHelper.fetchJsonFromPublicURL(Config.CLOUD_STORAGE_BASE_URL+filename);
        }
        if (obj == null) {
          LOGGER.warning("Could not find file "+filename);
        } else {
          for (Entry<String, JsonElement> entry: obj.entrySet()) {
            object.add(entry.getKey(), entry.getValue());
          }
        }
      }
    }
    return object.get(entityType.name());
  }

  @Override
  public String toString() {
    return "CloudStorageEntityFetcher(filenames="+Arrays.toString(filenames)+")";
  }
}
