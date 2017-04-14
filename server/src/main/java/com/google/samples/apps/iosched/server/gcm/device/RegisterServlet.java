/*
 * Copyright 2014 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.server.gcm.device;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.samples.apps.iosched.server.gcm.AuthHelper;
import com.google.samples.apps.iosched.server.gcm.BaseServlet;
import com.google.samples.apps.iosched.server.gcm.AuthHelper.AuthInfo;
import com.google.samples.apps.iosched.server.gcm.db.DeviceStore;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that registers a device, whose registration id is identified by
 * {@link #PARAMETER_DEVICE_ID}.
 *
 * <p>
 * The client app should call this servlet everytime it receives a
 * {@code com.google.android.c2dm.intent.REGISTRATION C2DM} intent without an
 * error or {@code unregistered} extra.
 */
@SuppressWarnings("serial")
public class RegisterServlet extends BaseServlet {

  private static final String PARAMETER_DEVICE_ID = "device_id";
  private static final String PARAMETER_USER_ID = "user_id";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {

    // Let's see what this user is authorized to do
    AuthInfo authInfo = AuthHelper.processAuthorization(req);

    if (authInfo == null || !authInfo.permRegister) {
      send(resp, 403, "Not authorized");
      return;
    }
    String deviceId = getParameter(req, PARAMETER_DEVICE_ID);
    String userId = getParameter(req, PARAMETER_USER_ID);

    // Check to see if deviceId and userId exists.
    if (Strings.isNullOrEmpty(deviceId) || Strings.isNullOrEmpty(userId)) {
      // Drop request.
      send(resp, 400, "Invalid request: Request must contain both " +
          PARAMETER_DEVICE_ID + " and " + PARAMETER_USER_ID);
      return;
    }

    DeviceStore.register(deviceId, userId);
    setSuccess(resp);
  }

}
