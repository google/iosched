/**
 * Copyright 2012 Google
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

import com.google.android.apps.iosched.gcm.server.BaseServlet;
import com.google.android.apps.iosched.gcm.server.db.DeviceStore;
import com.google.android.apps.iosched.gcm.server.db.models.Device;
import com.google.android.apps.iosched.gcm.server.device.MessageSender;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Send message to all registered devices */
@SuppressWarnings("serial")
public class SendMessageServlet extends BaseServlet {
    private static final Logger LOG = Logger.getLogger(SendMessageServlet.class.getName());

    /** Authentication key for incoming message requests */
    private static final String[][] AUTHORIZED_KEYS = {
            {"YOUR_API_KEYS_HERE", "developers.google.com"},
            {"YOUR_API_KEYS_HERE", "googleapis.com/googledevelopers"},
            {"YOUR_API_KEYS_HERE", "Device Key"}
    };

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // Authenticate request
        String authKey = null;
        String authHeader = req.getHeader("Authorization");
        String squelch = null;
        if (authHeader != null) {
            // Use 'Authorization: key=...' header
            String splitHeader[] = authHeader.split("=");
            if ("key".equals(splitHeader[0])) {
                authKey = splitHeader[1];
            }
        }
        if (authKey == null) {
            // Valid auth key not found. Check for 'key' query parameter.
            // Note: This is included for WebHooks support, but will consume message body!
            authKey = req.getParameter("key");
            squelch = req.getParameter("squelch");
        }
        String authorizedUser = null;
        for (String[] candidateKey : AUTHORIZED_KEYS) {
            if (candidateKey[0].equals(authKey)) {
                authorizedUser = candidateKey[1];
                break;
            }
        }
        if (authorizedUser == null) {
            send(resp, 403, "Not authorized");
            return;
        }

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

        // Extract extraData
        String payload = readBody(req);

        // Override: add jitter for server update requests
//        if ("sync_schedule".equals(action)) {
//            payload = "{ \"sync_jitter\": 21600000 }";
//        }

        // Request decoding complete. Log request parameters
        LOG.info("Authorized User: " + authorizedUser +
                "\nTarget: " + target +
                "\nAction: " + action +
                "\nSquelch: " + (squelch != null ? squelch : "null") +
                "\nExtra Data: " + payload);

        // Send broadcast message
        MessageSender sender = new MessageSender(getServletConfig());
        if ("global".equals(target)) {
            List<Device> allDevices = DeviceStore.getAllDevices();
            if (allDevices == null || allDevices.isEmpty()) {
                send(resp, 404, "No devices registered");
            } else {
                int resultCount = allDevices.size();
                LOG.info("Selected " + resultCount + " devices");
                sender.multicastSend(allDevices, action, squelch, payload);
                send(resp, 200, "Message queued: " + resultCount + " devices");
            }
        } else {
            // Send message to one device
            List<Device> userDevices = DeviceStore.findDevicesByGPlusId(target);
            if (userDevices == null || userDevices.isEmpty()) {
                send(resp, 404, "User not found");
            } else {
                int resultCount = userDevices.size();
                LOG.info("Selected " + resultCount + " devices");
                sender.multicastSend(userDevices, action, squelch, payload);
                send(resp, 200, "Message queued: " + resultCount + " devices");
            }
        }
    }

    private String readBody(HttpServletRequest req) throws IOException {
        ServletInputStream inputStream = req.getInputStream();
        java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static void send(HttpServletResponse resp, int status, String body)
            throws IOException {
        if (status >= 400) {
            LOG.warning(body);
        } else {
            LOG.info(body);
        }

        // Prevent frame hijacking
        resp.addHeader("X-FRAME-OPTIONS", "DENY");

        // Write response data
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.print(body);
        resp.setStatus(status);
    }

}


