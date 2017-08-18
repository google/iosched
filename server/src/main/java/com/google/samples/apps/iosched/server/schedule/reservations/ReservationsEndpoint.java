/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.server.schedule.reservations;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

/** Endpoint for reservation management. */
@Api(
    name = "reservations",
    title = "IOSched reservation management",
    description = "Manage reservations in IOSched",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "iosched.apps.samples.google.com",
        ownerName = "google.com",
        packagePath = "rpc"
    ),
    clientIds = {com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID}
)
public class ReservationsEndpoint {

  /**
   * Reset reservations in datastore to match those in RTDB. Reservations in RTDB are used
   * as the source of truth, corresponding reservations in datastore are updated to match
   * those in RTDB. Reservations in RTDB that do not exist in datastore are added to datastore.
   * Reservations that exist in datastore and do not exist in RTDB are updated in datastore
   * with status DELETED.
   *
   * Use of this endpoint should be followed by a user data sync.
   *
   * @param user User making request (injected by Endpoints)
   */
  @ApiMethod(name = "reset", path = "reset")
  public void reset(User user)
      throws UnauthorizedException {
    if (user == null) {
      throw new UnauthorizedException("Invalid credentials");
    }

    // Add Sync Reservations worker to queue.
    Queue queue = QueueFactory.getQueue("SyncReservationsQueue");

    TaskOptions taskOptions = TaskOptions.Builder
        .withUrl("/queue/syncres")
        .method(Method.GET);
    queue.add(taskOptions);
  }

}
