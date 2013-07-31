/*
 * Copyright 2013 Google Inc.
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

import com.google.android.apps.iosched.gcm.server.db.ApiKeyInitializer;
import com.google.android.apps.iosched.gcm.server.db.MessageStore;
import com.google.android.apps.iosched.gcm.server.db.DeviceStore;
import com.google.android.apps.iosched.gcm.server.db.models.Device;
import com.google.android.apps.iosched.gcm.server.db.models.MulticastMessage;
import com.google.android.gcm.server.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import javax.servlet.ServletConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Utility class for sending individual messages to devices.
 *
 * This class is responsible for communication with the GCM server for purposes of sending
 * messages.
 *
 * @return true if success, false if
 */
public class MessageSender {
    private String mApiKey;
    private Sender mGcmService;

    private static final int TTL = (int) TimeUnit.MINUTES.toSeconds(300);
    protected final Logger mLogger = Logger.getLogger(getClass().getName());
    /** Maximum devices in a multicast message */
    private static final int MAX_DEVICES = 1000;

    public MessageSender(ServletConfig config) {
        mApiKey = (String) config.getServletContext().getAttribute(
                ApiKeyInitializer.ATTRIBUTE_ACCESS_KEY);
        mGcmService = new Sender(mApiKey);
    }

    public void multicastSend(List<Device> devices, String action,
                              String squelch, String extraData) {
        Queue queue = QueueFactory.getQueue("MulticastMessagesQueue");

        // Split messages into batches for multicast
        // GCM limits maximum devices per multicast request. AppEngine also limits the size of
        // lists stored in the datastore.
        int total = devices.size();
        List<String> partialDevices = new ArrayList<String>(total);
        int counter = 0;
        for (Device device : devices) {
            counter ++;
            // If squelch is set, device is the originator of this message,
            // and has asked us to squelch itself. This prevents message echos.
            if (!device.getGcmId().equals(squelch)) {
                // Device not squelched.
                partialDevices.add(device.getGcmId());
            }
            int partialSize = partialDevices.size();
            if (partialSize == MAX_DEVICES || counter == total) {
                // Send multicast message
                Long multicastKey = MessageStore.createMulticast(partialDevices,
                        action,
                        extraData);
                mLogger.fine("Queuing " + partialSize + " devices on multicast " + multicastKey);
                TaskOptions taskOptions = TaskOptions.Builder
                        .withUrl("/queue/send")
                        .param("multicastKey", Long.toString(multicastKey))
                        .method(TaskOptions.Method.POST);
                queue.add(taskOptions);
                partialDevices.clear();
            }
        }
        mLogger.fine("Queued message to " + total + " devices");
    }

    boolean sendMessage(Long multicastId) {
        MulticastMessage msg = MessageStore.getMulticast(multicastId);
        List<String> devices = msg.getDestinations();
        String action = msg.getAction();
        Message.Builder builder = new Message.Builder().delayWhileIdle(true);
        if (action == null || action.length() == 0) {
            throw new IllegalArgumentException("Message action cannot be empty.");
        }
        builder.collapseKey(action)
                    .addData("action", action)
                    .addData("extraData", msg.getExtraData())
                    .timeToLive(TTL);
        Message message = builder.build();
        MulticastResult multicastResult = null;
        try {
            // TODO(trevorjohns): We occasionally see null messages. (Maybe due to squelch?)
            // We should these from entering the send queue in the first place. In the meantime,
            // here's a hack to prevent this.
            if (devices != null) {
                multicastResult = mGcmService.sendNoRetry(message, devices);
                mLogger.info("Result: " + multicastResult);
            } else {
                mLogger.info("Null device list detected. Aborting.");
                return true;
            }
        } catch (IOException e) {
            mLogger.log(Level.SEVERE, "Exception posting " + message, e);
            return true;
        }
        boolean allDone = true;
        // check if any registration id must be updated
        if (multicastResult.getCanonicalIds() != 0) {
            List<Result> results = multicastResult.getResults();
            for (int i = 0; i < results.size(); i++) {
                String canonicalRegId = results.get(i).getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    String regId = devices.get(i);
                    DeviceStore.updateRegistration(regId, canonicalRegId);
                }
            }
        }
        if (multicastResult.getFailure() != 0) {
            // there were failures, check if any could be retried
            List<Result> results = multicastResult.getResults();
            List<String> retriableRegIds = new ArrayList<String>();
            for (int i = 0; i < results.size(); i++) {
                String error = results.get(i).getErrorCodeName();
                if (error != null) {
                    String regId = devices.get(i);
                    mLogger.warning("Got error (" + error + ") for regId " + regId);
                    if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                        // application has been removed from device - unregister it
                        DeviceStore.unregister(regId);
                    }
                    if (error.equals(Constants.ERROR_UNAVAILABLE)) {
                        retriableRegIds.add(regId);
                    }
                }
            }
            if (!retriableRegIds.isEmpty()) {
                // update task
                MessageStore.updateMulticast(multicastId, retriableRegIds);
                allDone = false;
                return false;
            }
        }
        if (allDone) {
            return true;
        } else {
            return false;
        }
    }

}
