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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.samples.apps.iosched.server.schedule.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EntityFetcher that fetches entities from the schedule CMS's HTTP server.
 *
 * This will need to be modified to use a custom backend for your event.
 */
@SuppressWarnings("unused")
public class VendorAPIEntityFetcher implements EntityFetcher {
  static Logger LOG = Logger.getLogger(VendorAPIEntityFetcher.class.getName());

  // TODO: Hook up to your backend data source
  public static final String BASE_URL = "UNDEFINED";

  @Override
  public JsonElement fetch(Enum<?> entityType, Map<String, String> params)
      throws IOException {
    StringBuilder urlStr = new StringBuilder(BASE_URL);
    urlStr.append(entityType.name());
    if (params != null && !params.isEmpty()) {
      urlStr.append("?");
      for (Map.Entry<String, String> param: params.entrySet()) {
        urlStr.append(param.getKey()).append("=").append(param.getValue())
            .append("&");

      }
      urlStr.deleteCharAt(urlStr.length()-1);
    }

    URL url = new URL(urlStr.toString());
    if (LOG.isLoggable(Level.INFO)) {
      LOG.info("URL requested: "+url);
    }

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setReadTimeout(1000 * 30); // 30 seconds
    connection.setRequestProperty("code", Config.CMS_API_CODE);
    connection.setRequestProperty("apikey", Config.CMS_API_KEY);

    InputStream stream = connection.getInputStream();
    JsonReader reader = new JsonReader(new InputStreamReader(stream, Charset.forName("UTF-8")));
    return new JsonParser().parse(reader);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "HttpEntityFetcher(baseURL="+BASE_URL+")";
  }
}
