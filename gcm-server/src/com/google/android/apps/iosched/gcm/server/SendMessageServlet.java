/*
 * Copyright 2012 Google Inc.
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
package com.google.android.apps.iosched.gcm.server;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.appengine.api.datastore.Entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet, called from an App Engine task, that sends the given message (sync or announcement)
 * to all devices in a given multicast group (up to 1000).
 */
public class SendMessageServlet extends BaseServlet {

    private static final int ANNOUNCEMENT_TTL = (int) TimeUnit.MINUTES.toSeconds(300);

    private Sender sender;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        sender = newSender(config);
    }

    /**
     * Creates the {@link Sender} based on the servlet settings.
     */
    protected Sender newSender(ServletConfig config) {
        String key = (String) config.getServletContext()
                .getAttribute(ApiKeyInitializer.ATTRIBUTE_ACCESS_KEY);
        return new Sender(key);
    }

    /**
     * Processes the request to add a new message.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String multicastKey = req.getParameter("multicastKey");
        Entity multicast = Datastore.getMulticast(multicastKey);
        @SuppressWarnings("unchecked")
        List<String> devices = (List<String>)
                multicast.getProperty(Datastore.MULTICAST_REG_IDS_PROPERTY);
        String announcement = (String)
                multicast.getProperty(Datastore.MULTICAST_ANNOUNCEMENT_PROPERTY);

        // Build the GCM message
        Message.Builder builder = new Message.Builder()
                .delayWhileIdle(true);
        if (announcement != null && announcement.trim().length() > 0) {
            builder
                    .collapseKey("announcement")
                    .addData("announcement", announcement)
                    .timeToLive(ANNOUNCEMENT_TTL);
        } else {
            builder
                    .collapseKey("sync");
        }
        Message message = builder.build();

        // Send the GCM message.
        MulticastResult multicastResult;
        try {
            multicastResult = sender.sendNoRetry(message, devices);
            logger.info("Result: " + multicastResult);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception posting " + message, e);
            taskDone(resp, multicastKey);
            return;
        }

        // check if any registration ids must be updated
        if (multicastResult.getCanonicalIds() != 0) {
            List<Result> results = multicastResult.getResults();
            for (int i = 0; i < results.size(); i++) {
                String canonicalRegId = results.get(i).getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    String regId = devices.get(i);
                    Datastore.updateRegistration(regId, canonicalRegId);
                }
            }
        }

        boolean allDone = true;
        if (multicastResult.getFailure() != 0) {
            // there were failures, check if any could be retried
            List<Result> results = multicastResult.getResults();
            List<String> retryableRegIds = new ArrayList<String>();
            for (int i = 0; i < results.size(); i++) {
                String error = results.get(i).getErrorCodeName();
                if (error != null) {
                    String regId = devices.get(i);
                    logger.warning("Got error (" + error + ") for regId " + regId);
                    if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                        // application has been removed from device - unregister it
                        Datastore.unregister(regId);
                    } else if (error.equals(Constants.ERROR_UNAVAILABLE)) {
                        retryableRegIds.add(regId);
                    }
                }
            }

            if (!retryableRegIds.isEmpty()) {
                // update task
                Datastore.updateMulticast(multicastKey, retryableRegIds);
                allDone = false;
                retryTask(resp);
            }
        }

        if (allDone) {
            taskDone(resp, multicastKey);
        } else {
            retryTask(resp);
        }
    }

    /**
     * Indicates to App Engine that this task should be retried.
     */
    private void retryTask(HttpServletResponse resp) {
        resp.setStatus(500);
    }

    /**
     * Indicates to App Engine that this task is done.
     */
    private void taskDone(HttpServletResponse resp, String multicastKey) {
        Datastore.deleteMulticast(multicastKey);
        resp.setStatus(200);
    }
}
