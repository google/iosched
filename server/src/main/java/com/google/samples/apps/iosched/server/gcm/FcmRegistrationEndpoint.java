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

package com.google.samples.apps.iosched.server.gcm;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.samples.apps.iosched.server.gcm.db.DeviceStore;
import com.google.samples.apps.iosched.server.gcm.db.models.Device;
import com.google.samples.apps.iosched.server.userdata.Ids;
import com.googlecode.objectify.NotFoundException;

/**
 * Endpoint for registering and un-registering devices with FCM tokens.
 */
@Api(
    name = "fcm",
    title = "IOSched FCM Registration",
    description = "Send and remove client FCM tokens",
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
        com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID},
    audiences = {Ids.ANDROID_AUDIENCE}
)
public class FcmRegistrationEndpoint {

  private static final String PARAMETER_DEVICE_ID = "device_id";

  /**
   * Register a user's device. Each client instance has a token that identifies it, there is usually
   * one instance per device so in most cases the token represents a device. The token is stored
   * against the user's ID to allow messages to be sent to all the user's devices.
   *
   * @param user Current user (injected by Endpoints)
   * @param deviceId FCM token representing the device.
   * @throws BadRequestException Thrown when there is no device ID in the request.
   */
  @ApiMethod(path = "register", httpMethod = HttpMethod.POST)
  public void register(User user, @Named(PARAMETER_DEVICE_ID) String deviceId)
      throws BadRequestException {

    // Check to see if deviceId.
    if (Strings.isNullOrEmpty(deviceId)) {
      // Drop request.
      throw new BadRequestException("Invalid request: Request must contain " +
          PARAMETER_DEVICE_ID);
    }

    // Check that user making requests is non null.
    if (user != null && !Strings.isNullOrEmpty(user.getId())) {
      DeviceStore.register(deviceId, user.getId());
    } else {
      // If user is null still register device so it can still get session update ping.
      DeviceStore.register(deviceId, null);
    }
  }

  /**
   * Remove a registration of a user's device. When a user signs out of a client they should
   * unregister. This will prevent messages from being sent to the wrong user if multiple users
   * are using the same device.
   *
   * @param deviceId FCM token representing the device.
   * @return Result containing a message about the un-registration.
   * @throws BadRequestException Thrown when there is no device ID in the request.
   */
  @ApiMethod(path = "unregister", httpMethod = HttpMethod.POST)
  public void unregister(User user, @Named(PARAMETER_DEVICE_ID) String deviceId)
      throws BadRequestException, UnauthorizedException,
      com.google.api.server.spi.response.NotFoundException, ForbiddenException {

    // Check to see if deviceId.
    if (Strings.isNullOrEmpty(deviceId)) {
      // Drop request.
      throw new BadRequestException("Invalid request: Request must contain " +
          PARAMETER_DEVICE_ID);
    }

    // Check that user making requests is non null.
    if (user == null) {
      throw new UnauthorizedException("Invalid credentials");
    }

    try {
      Device device = ofy().load().type(Device.class).id(deviceId).safe();
      // Check that the user trying to unregister the token is the same one that registered it.
      if (!device.getUserId().equals(user.getId())) {
        throw new ForbiddenException("Not authorized to unregister token");
      }

      DeviceStore.unregister(deviceId);
    } catch (NotFoundException e) {
      throw new com.google.api.server.spi.response.NotFoundException("Device ID: " + deviceId +
          " not found");
    }
  }

}
