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

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that pushes a GCM message to all registered devices. This servlet creates multicast
 * entities consisting of 1000 devices each, and creates tasks to send individual GCM messages to
 * each device in the multicast.
 *
 * This servlet must be authenticated against with an administrator's Google account, ideally a
 * shared role account.
 */
public class SendMessageToAllServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Queue queue = QueueFactory.getQueue("MulticastMessagesQueue");
        String announcement = req.getParameter("announcement");
        if (announcement == null) {
            announcement = "";
        }

        List<String> devices = Datastore.getDevices();

        // must split in chunks of 1000 devices (GCM limit)
        int total = devices.size();
        List<String> partialDevices = new ArrayList<String>(total);
        int counter = 0;
        for (String device : devices) {
            counter++;
            partialDevices.add(device);
            int partialSize = partialDevices.size();
            if (partialSize == Datastore.MULTICAST_SIZE || counter == total) {
                String multicastKey =
                        Datastore.createMulticast(partialDevices, announcement);
                logger.fine("Queuing " + partialSize + " devices on multicast " +
                        multicastKey);
                TaskOptions taskOptions = TaskOptions.Builder
                        .withUrl("/send")
                        .param("multicastKey", multicastKey)
                        .method(Method.POST);
                queue.add(taskOptions);
                partialDevices.clear();
            }
        }
        logger.fine("Queued message to " + counter + " devices");
        resp.sendRedirect("/success.html");
    }
}
