/*
 * Copyright 2012 Google Inc.
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
package com.google.android.apps.iosched.gcm.server.device;

import com.google.android.apps.iosched.gcm.server.BaseServlet;
import com.google.android.apps.iosched.gcm.server.db.MessageStore;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that adds a new message and notifies all registered devices.
 *
 * <p>This class should not be called directly. Instead, it's used as a helper
 * for the SendMessage task queue.
 */
@SuppressWarnings("serial")
public class MulticastQueueWorker extends BaseServlet {

    private MessageSender mSender;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mSender = new MessageSender(config);
    }

    /**
     * Processes the request to add a new message.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        Long multicastId = new Long(req.getParameter("multicastKey"));
        boolean success = mSender.sendMessage(multicastId);
        if (success) {
            taskDone(resp, multicastId);
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
    private void taskDone(HttpServletResponse resp, Long multicastId) {
        MessageStore.deleteMulticast(multicastId);
        resp.setStatus(200);
    }

}
