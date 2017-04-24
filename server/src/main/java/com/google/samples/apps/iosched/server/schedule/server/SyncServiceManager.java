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
import com.google.samples.apps.iosched.rpc.sync.Sync;
import java.io.IOException;
import java.util.Arrays;

public enum SyncServiceManager {
  INSTANCE;

  static final String[] SCOPES = {"https://www.googleapis.com/auth/userinfo.email"};

  Sync sync;

  SyncServiceManager() {
    this.sync = getSyncService();
  }

  /**
   * Get Sync service.
   *
   * @return Sync service.
   * @throws IOException
   */
  private Sync getSyncService() {
    HttpTransport httpTransport = new UrlFetchTransport();
    JacksonFactory jacksonFactory = new JacksonFactory();
    final GoogleCredential credential;
    try {
      credential = GoogleCredential.getApplicationDefault()
          .createScoped(Arrays.asList(SCOPES));
      Sync sync = new Sync.Builder(httpTransport, jacksonFactory,
          new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
              credential.initialize(request);
            }
          }).setApplicationName("IOSched Backend").build();
      return sync;
    } catch (IOException e) {
      return null;
    }
  }

}
