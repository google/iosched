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
package com.google.samples.apps.iosched.server.gcm;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.samples.apps.iosched.server.gcm.db.DeviceStore;
import com.google.samples.apps.iosched.server.gcm.db.models.Device;
import com.google.samples.apps.iosched.server.gcm.device.MessageSender;
import com.google.samples.apps.iosched.server.userdata.Ids;
import java.util.List;
import javax.servlet.ServletContext;

/**
 * Endpoint to send FCM messages to ping clients to sync.
 */
@Api(
    name = "ping",
    title = "IOSched Sync",
    description = "Ping IOSched clients to sync.",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "iosched.apps.samples.google.com",
        ownerName = "google.com",
        packagePath = "rpc"
    ),
    clientIds = {Ids.WEB_CLIENT_ID, Ids.ANDROID_CLIENT_ID,
        Ids.IOS_CLIENT_ID_DEV_IO2017,
        Ids.IOS_CLIENT_ID_DOGFOOD_IO2017,
        Ids.IOS_CLIENT_ID_GWEB_IO2017,
        Ids.IOS_CLIENT_ID_DEV_GWEB_IO2017,
        Ids.IOS_CLIENT_ID_DOGFOOD_GWEB_IO2017,
        com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID
    },
    audiences = {Ids.ANDROID_AUDIENCE}
)
public class FcmSendEndpoint {

  public static final String ACTION_SYNC_USER = "sync_user";
  public static final String ACTION_SYNC_SCHEDULE = "sync_schedule";
  public static final String INVALID_CREDENTIALS_MSG = "Invalid credentials";

  /**
   * Clients can initiate a sync on all of a user's devices. This will usually be called
   * when a client pushes a user data update to the server and wants other clients to
   * sync that change.
   *
   * @param context Servlet context (injected by Endpoints)
   * @param user User requesting the sync (injected by Endpoints)
   */
  @ApiMethod(name = "sendSelfSync", path = "self")
  public void sendSelfSync(ServletContext context, User user) throws UnauthorizedException {
    if (user == null) {
      throw new UnauthorizedException(INVALID_CREDENTIALS_MSG);
    }
    MessageSender sender = new MessageSender(context);
    String userId = user.getId();
    List<Device> devices = DeviceStore.findDevicesByUserId(userId);
    sender.multicastSend(devices, ACTION_SYNC_USER, null);
  }

  /**
   * Ping a user's devices to sync user data. This is likely called when the server makes
   * a change to user data and wants the corresponding user's clients to sync.
   *
   * @param context Servlet context (injected by Endpoints)
   * @param user User making the request (injected by Endpoints)
   * @param userId ID of the user whose devices should sync.
   * @return SendUserSyncResult which contains the number of devices pinged.
   */
  @ApiMethod(name = "sendUserSync", path = "users/{userId}",
      clientIds = {Ids.SERVICE_ACCOUNT_ANONOMYOUS_CLIENT_ID})
  public SendUserSyncResult sendUserSync(ServletContext context, User user, @Named("userId") String userId)
      throws UnauthorizedException, NotFoundException {
    validateServiceAccount(user);
    MessageSender sender = new MessageSender(context);
    List<Device> devices = DeviceStore.findDevicesByUserId(userId);
    if (devices.isEmpty()) {
      throw new NotFoundException("No devices for user found");
    }
    sender.multicastSend(devices, ACTION_SYNC_USER, null);
    return new SendUserSyncResult(devices.size());
  }

  /**
   * Ping all users' devices to sync user data.
   *
   * @param context Servlet context (injected by Endpoints)
   * @param user User making the request (injected by Endpoints)
   */
  @ApiMethod(name = "sendUserDataSync", path = "users",
      clientIds = {com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID})
  public void sendUserDataSync(ServletContext context, User user)
      throws UnauthorizedException {
    if (user == null) {
      throw new UnauthorizedException(INVALID_CREDENTIALS_MSG);
    }
    MessageSender sender = new MessageSender(context);
    List<Device> devices = DeviceStore.getAllDevices();
    sender.multicastSend(devices, ACTION_SYNC_USER, null);
  }

  /**
   * Ping all users' devices to sync session data.
   *
   * @param context Servlet context (injected by Endpoints)
   * @param user User making the request (injected by Endpoints)
   */
  @ApiMethod(name = "sendSessionDataSync", path = "sessions",
      clientIds = {Ids.SERVICE_ACCOUNT_ANONOMYOUS_CLIENT_ID})
  public void sendSessionDataSync(ServletContext context, User user)
      throws UnauthorizedException {
    validateServiceAccount(user);
    MessageSender sender = new MessageSender(context);
    List<Device> devices = DeviceStore.getAllDevices();
    sender.multicastSend(devices, ACTION_SYNC_SCHEDULE, null);
  }

  /**
   * Ping all users' devices to update UI indicating that the feed has been updated.
   *
   * @param context Servlet context (injected by Endpoints)
   * @param user User making the request (injected by Endpoints)
   */
  @ApiMethod(name = "sendFeedPing", path = "feed",
      clientIds = {Ids.SERVICE_ACCOUNT_CLIENT_ID})
  public void sendFeedPing(ServletContext context, User user)
      throws UnauthorizedException {
    validateServiceAccount(user);
    MessageSender sender = new MessageSender(context);
    List<Device> devices = DeviceStore.getAllDevices();
    sender.multicastSend(devices, "feed_update", null);
  }

  private void validateServiceAccount(User user) throws UnauthorizedException {
    if (user == null || !user.getEmail().equals(Ids.SERVICE_ACCOUNT_EMAIL)) {
      throw new UnauthorizedException(INVALID_CREDENTIALS_MSG);
    }
  }

}
