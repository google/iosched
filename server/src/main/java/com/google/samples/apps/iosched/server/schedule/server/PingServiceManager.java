/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.server.schedule.server;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.samples.apps.iosched.rpc.ping.Ping;
import java.io.IOException;
import java.util.Arrays;

public enum PingServiceManager {
  INSTANCE;

  final String[] SCOPES = {"https://www.googleapis.com/auth/userinfo.email"};

  Ping ping;

  PingServiceManager() {
    this.ping = getSyncService();
  }

  /**
   * Get Sync service.
   *
   * @return Sync service.
   * @throws IOException
   */
  private Ping getSyncService() {
    HttpTransport httpTransport = new UrlFetchTransport();
    JacksonFactory jacksonFactory = new JacksonFactory();
    final GoogleCredential credential;
    try {
      credential = GoogleCredential.getApplicationDefault()
          .createScoped(Arrays.asList(SCOPES));
      Ping ping = new Ping.Builder(httpTransport, jacksonFactory,
          new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
              credential.initialize(request);
            }
          }).setApplicationName("IOSched Backend").build();
      return ping;
    } catch (IOException e) {
      return null;
    }
  }

}
