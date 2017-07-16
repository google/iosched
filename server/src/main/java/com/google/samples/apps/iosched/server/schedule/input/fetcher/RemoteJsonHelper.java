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
package com.google.samples.apps.iosched.server.schedule.input.fetcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.samples.apps.iosched.server.schedule.Config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map.Entry;

/**
 */
public class RemoteJsonHelper {

  public static JsonObject mergeJsonFiles(JsonObject target, String... filenames) throws IOException {
    if (target == null) {
      target = new JsonObject();
    }
    for (String filename: filenames) {
      String url = Config.CLOUD_STORAGE_BASE_URL+filename;
      JsonObject obj = fetchJsonFromPublicURL(url);
      if (obj == null) {
        throw new FileNotFoundException(url);
      } else {
        for (Entry<String, JsonElement> entry: obj.entrySet()) {
          if (entry.getValue().isJsonArray()) {
            // tries to merge an array with the existing one, if it's the case:
            JsonArray existing = target.getAsJsonArray(entry.getKey());
            if (existing == null) {
               existing = new JsonArray();
               target.add(entry.getKey(), existing);
            }
            existing.addAll(entry.getValue().getAsJsonArray());
          } else {
            target.add(entry.getKey(), entry.getValue());
          }
        }
      }
    }
    return target;
  }

  public static JsonObject fetchJsonFromPublicURL(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setReadTimeout(1000 * 30); // 30 seconds
    int response = connection.getResponseCode();
    if (response < 200 || response >= 300) {
      throw new IllegalArgumentException("Unexpected HTTP response ["+response+"] at URL: "+urlStr);
    }

    InputStream stream = connection.getInputStream();
    JsonReader reader = new JsonReader(new InputStreamReader(stream, Charset.forName("UTF-8")));
    return (JsonObject) new JsonParser().parse(reader);
  }

}
