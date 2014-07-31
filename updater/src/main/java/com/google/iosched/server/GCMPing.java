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

import com.google.appengine.api.utils.SystemProperty;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles notification to the GCM server that the data has changed.
 */
public class GCMPing {

  static Logger LOG = Logger.getLogger(GCMPing.class.getName());

  public void notifyGCMServer(String urlStr, String key) {
    if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Development) {
      // In the development server, don't notify GCM
      LOG.warning("Should notify the GCM server that new data is available, pinging URL "+urlStr);
    } else {
      try {
        URL url = new URL(urlStr);
        if (LOG.isLoggable(Level.INFO)) {
          LOG.info("Pinging GCM at URL: "+url);
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(1000 * 30); // 30 seconds
        connection.setRequestProperty("Authorization", "key="+key);
        connection.connect();
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
          LOG.severe("Unexpected response code from GCM server: "+statusCode+". "+connection.getResponseMessage());
        }

      } catch (Exception ex) {
        LOG.log(Level.SEVERE, "Unexpected error when pinging GCM server", ex);
      }
    }
  }
}
