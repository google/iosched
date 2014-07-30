/**
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.iosched.gcm.server.api;

import com.google.android.apps.iosched.gcm.server.AuthHelper;
import com.google.android.apps.iosched.gcm.server.AuthHelper.AuthInfo;
import com.google.android.apps.iosched.gcm.server.BaseServlet;
import com.google.android.apps.iosched.gcm.server.db.DeviceStore;
import com.google.android.apps.iosched.gcm.server.db.models.Device;
import com.google.android.apps.iosched.gcm.server.device.MessageSender;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Send message to all registered devices */
@SuppressWarnings("serial")
public class SendMessageServlet extends BaseServlet {
    private static final Logger LOG = Logger.getLogger(SendMessageServlet.class.getName());
    private static final String SELF = "self";


    /** Actions that can be executed by non admins. */
    private static final HashSet<String> UNPRIVILEGED_ACTIONS = new HashSet<String>(
        Arrays.asList(new String[]{"sync_schedule", "test", "sync_user"}));

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      // Allow CORS requests from any domain. The response below allows
      // preflight requests to be correctly responded.
      resp.addHeader("Access-Control-Allow-Origin", "*");
      resp.addHeader("Access-Control-Allow-Headers", "authorization");
      resp.setStatus(200);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // Extract URL components
        String result = req.getPathInfo();
        if (result == null) {
            send(resp, 400, "Bad request (check request format)");
            return;
        }
        String[] components = result.split("/");
        if (components.length != 3) {
            send(resp, 400, "Bad request (check request format)");
            return;
        }
        String target = components[1];
        String action = components[2];

        // Let's see what this user is authorized to do
        AuthInfo authInfo = AuthHelper.processAuthorization(req);

        // If no auth info or non-admin trying to run non-whitelisted actions, no access.
        if (authInfo == null || action == null ||
            (!UNPRIVILEGED_ACTIONS.contains(action) && !authInfo.permAdmin)) {
          send(resp, 403, "Not authorized");
          return;
        }

        // Extract extraData
        String payload = readBody(req);

        // Request decoding complete. Log request parameters
        LOG.info("Authorized User: " + authInfo.clientName +
                "\nTarget: " + target +
                "\nAction: " + action +
                "\nExtra Data: " + payload);

        MessageSender sender = new MessageSender(getServletConfig());

        // what's the audience of the message?
        if ("global".equals(target)) {
            // Only admins can spam the world
            if (!authInfo.permAdmin) {
              LOG.info("Attempt to send global message, but no admin perm.");
              send(resp, 403, "Not authorized");
              return;
            }

            List<Device> allDevices = DeviceStore.getAllDevices();
            if (allDevices == null || allDevices.isEmpty()) {
                send(resp, 404, "No devices registered");
            } else {
                int resultCount = allDevices.size();
                LOG.info("Selected " + resultCount + " devices");
                sender.multicastSend(allDevices, action, payload);
                send(resp, 200, "Message queued: " + resultCount + " devices");
            }
        } else {
            // Send message to one device
            // If target is SELF, the GCM Group ID will be the auth key in header
            if (SELF.equals(target)) {
              // do we have permission to send message to self?
              if (!authInfo.permSendSelfMessage) {
                LOG.info("Attempt to send self message, but no self message perm.");
                send(resp, 403, "Not authorized");
                return;
              }
              // the target is the auth key (it represents the group)
              target = authInfo.authKey;
            } else {
              // sending message to a specific user. Only admin can do it.
              if (!authInfo.permAdmin) {
                LOG.info("Attempt to send message to specific target, but no admin perm.");
                send(resp, 403, "Not authorized");
                return;
              }
            }
            List<Device> userDevices = DeviceStore.findDevicesByGcmGroupId(target);
            if (userDevices == null || userDevices.isEmpty()) {
                send(resp, 404, "User not found");
            } else {
                int resultCount = userDevices.size();
                LOG.info("Selected " + resultCount + " devices");
                sender.multicastSend(userDevices, action, payload);
                send(resp, 200, "Message queued: " + resultCount + " devices");
            }
        }
    }

    private String readBody(HttpServletRequest req) throws IOException {
        ServletInputStream inputStream = req.getInputStream();
        java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}


