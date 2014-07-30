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
package com.google.iosched.server.servlet;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.ShortBlob;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.iosched.server.UpdateRunLogger;
import com.google.iosched.server.cloudstorage.CloudFileManager;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A basic servlet that serves log from previous Updater runs as a JSON object.
 *
 */
public class LogDataServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {

    resp.setContentType("application/json");

    UpdateRunLogger logger = new UpdateRunLogger();
    JsonObject response = new JsonObject();

    int limitElements = 10;
    if (req.getParameter("limit")!=null) {
      limitElements = Integer.parseInt(req.getParameter("limit"));
    }
    List<Entity> lastRunsEntities = logger.getMostRecentRuns(limitElements);
    JsonArray lastRuns = new JsonArray();
    for (Entity run: lastRunsEntities) {
      JsonObject obj= new JsonObject();
      JsonObject timings = new JsonObject();
      TreeMap<String, Object> sortedMap = new TreeMap<String, Object>(run.getProperties());
      for (Entry<String, Object> property: sortedMap.entrySet()) {
        Object value = property.getValue();
        String key = property.getKey();
        if (key.startsWith("time_")) {
          timings.add(key.substring("time_".length()), new JsonPrimitive((Number) value));
        } else {
          JsonPrimitive converted = null;
          if (value instanceof ShortBlob) {
            converted = new JsonPrimitive(bytesToHex(((ShortBlob) value).getBytes()));
          } else if (value instanceof String) {
            converted = new JsonPrimitive((String) value);
          } else if (value instanceof Number) {
            converted = new JsonPrimitive((Number) value);
          } else if (value instanceof Boolean) {
            converted = new JsonPrimitive((Boolean) value);
          } else if (value instanceof Character) {
            converted = new JsonPrimitive((Character) value);
          } else if (value instanceof Date) {
            converted = new JsonPrimitive(DateFormat.getDateTimeInstance().format((Date) value));
          }
          if (converted != null) {
            obj.add(key, converted);
          }
        }
      }
      obj.add("timings", timings);
      lastRuns.add(obj);
    }
    response.add("lastruns", lastRuns);
    CloudFileManager cloudManager = new CloudFileManager();
    response.add("bucket", new JsonPrimitive(cloudManager.getBucketName()));
    response.add("productionManifest", new JsonPrimitive(cloudManager.getProductionManifestURL()));
    response.add("stagingManifest", new JsonPrimitive(cloudManager.getStagingManifestURL()));

    new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create().toJson(response, resp.getWriter());
  }

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }
}
