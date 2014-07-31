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
package com.google.iosched.input.fetcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * EntityFetcher that fetches entities from a set of files stored in CloudStorage.
 */
public class HTTPRemoteFilesEntityFetcher implements EntityFetcher {

  private String[] filenames;
  private JsonObject object;

  public HTTPRemoteFilesEntityFetcher(String... filenames) {
    this.filenames = filenames;
  }

  @Override
  public JsonElement fetch(Enum<?> entityType, Map<String, String> params)
      throws IOException {
    // On the first call, read all the files
    if (object == null) {
      object = RemoteJsonHelper.mergeJsonFiles(null, filenames);
    }
    return object.get(entityType.name());
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "HTTPCloudStorageEntityFetcher(filenames="+Arrays.toString(filenames)+")";
  }
}
