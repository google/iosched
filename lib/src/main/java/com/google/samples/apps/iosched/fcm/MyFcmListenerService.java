/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.samples.apps.iosched.fcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.samples.apps.iosched.fcm.command.AnnouncementCommand;
import com.google.samples.apps.iosched.fcm.command.FeedCommand;
import com.google.samples.apps.iosched.fcm.command.NotificationCommand;
import com.google.samples.apps.iosched.fcm.command.SyncCommand;
import com.google.samples.apps.iosched.fcm.command.SyncUserCommand;
import com.google.samples.apps.iosched.fcm.command.TestCommand;
import com.google.samples.apps.iosched.util.LogUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;

/**
 * Receive downstream FCM messages, examine the payload and determine what action
 * if any to take. Only known commands are executed.
 */
public class MyFcmListenerService extends FirebaseMessagingService {

    private static final String TAG = LogUtils.makeLogTag("MsgListenerService");
    private static final String ACTION = "action";
    private static final String EXTRA_DATA = "extraData";

    private static final Map<String, FcmCommand> MESSAGE_RECEIVERS;

    static {
        // Known messages and their FCM message receivers
        Map<String, FcmCommand> receivers = new HashMap<>();
        receivers.put("test", new TestCommand());
        receivers.put("announcement", new AnnouncementCommand());
        receivers.put("sync_schedule", new SyncCommand());
        receivers.put("sync_user", new SyncUserCommand());
        receivers.put("notification", new NotificationCommand());
        receivers.put("feed_update", new FeedCommand());
        MESSAGE_RECEIVERS = Collections.unmodifiableMap(receivers);
    }

    /**
     * Handle data in FCM message payload. The action field within the data determines the
     * type of Command that is initiated and executed.
     *
     * @param message Contains the message sender and a map of actions and associated extra data.
     */
    @Override
    public void onMessageReceived(RemoteMessage message) {
        String from = message.getFrom();

        // A map containing the action and extra data associated with that action.
        Map<String, String> data = message.getData();

        // Handle received FCM data messages.
        String action = data.get(ACTION);
        String extraData = data.get(EXTRA_DATA);
        LOGD(TAG, "Got FCM message, " + ACTION + "=" + action +
                ", " + EXTRA_DATA + "=" + extraData);
        if (action == null) {
            LOGE(TAG, "Message received without command action");
            return;
        }
        //noinspection DefaultLocale
        action = action.toLowerCase();
        FcmCommand command = MESSAGE_RECEIVERS.get(action);
        if (command == null) {
            LOGE(TAG, "Unknown command received: " + action);
        } else {
            command.execute(this, action, extraData);
        }
    }
}
