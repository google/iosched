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

import com.google.samples.apps.iosched.server.schedule.input.fetcher.VendorAPIEntityFetcher;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Handles notification to the GCM server that the data has changed.
 */
public class GCMPing {

  static Logger LOG = Logger.getLogger(VendorAPIEntityFetcher.class.getName());

  /**
   * Notify user clients that session data has changed.
   */
  public void notifySessionSync() {
    try {
      PingServiceManager.INSTANCE.ping.sendSessionDataSync().execute();
    } catch (IOException e) {
      LOG.severe("Unable to either get Sync service or send session data ping.");
    } catch (NullPointerException e) {
      LOG.severe("Unable to get Sync instance.");
    }
  }

  /**
   * Notify user's clients that user data has changed so they can ping.
   *
   * @param userId ID of user whose clients should ping.
   */
  public void notifyUserSync(String userId) {
    try {
      PingServiceManager.INSTANCE.ping.sendUserSync(userId).execute();
    } catch (IOException e) {
      LOG.severe("Unable to either get ping service or send user ping.");
    } catch (NullPointerException e) {
      LOG.severe("Unable to get Sync instance.");
    }
  }
}
