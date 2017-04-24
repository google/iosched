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
package com.google.samples.apps.iosched.server.schedule.server;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.samples.apps.iosched.rpc.sync.Sync;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.VendorAPIEntityFetcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles notification to the GCM server that the data has changed.
 */
public class GCMPing {

  static Logger LOG = Logger.getLogger(VendorAPIEntityFetcher.class.getName());
  public static final String SCOPE_EMAIL = "https://www.googleapis.com/auth/userinfo.email";

  /**
   * Notify user clients that session data has changed.
   */
  public void notifySessionSync() {
    try {
      SyncServiceManager.INSTANCE.sync.sendSessionDataSync();
    } catch (IOException e) {
      LOG.severe("Unable to either get Sync service or send session data sync.");
    } catch (NullPointerException e) {
      LOG.severe("Unable to get Sync instance.");
    }
  }

  /**
   * Notify user's clients that user data has changed so they can sync.
   *
   * @param userId ID of user whose clients should sync.
   */
  public void notifyUserSync(String userId) {
    try {
      SyncServiceManager.INSTANCE.sync.sendUserSync(userId);
    } catch (IOException e) {
      LOG.severe("Unable to either get sync service or send user sync.");
    } catch (NullPointerException e) {
      LOG.severe("Unable to get Sync instance.");
    }
  }
}
