/**
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
package com.google.samples.apps.iosched.gcm;

import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.samples.apps.iosched.gcm.command.AnnouncementCommand;
import com.google.samples.apps.iosched.gcm.command.NotificationCommand;
import com.google.samples.apps.iosched.gcm.command.SyncCommand;
import com.google.samples.apps.iosched.gcm.command.SyncUserCommand;
import com.google.samples.apps.iosched.gcm.command.TestCommand;
import com.google.samples.apps.iosched.util.LogUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;

/**
 * Receive downstream GCM messages, examine the payload and determine what action
 * if any to take. Only known commands are executed.
 */
public class GCMMessageListenerService extends GcmListenerService {

    private static final String TAG = LogUtils.makeLogTag("MsgListenerService");
    private static final String ACTION = "action";
    private static final String EXTRA_DATA = "extraData";

    private static final Map<String, GCMCommand> MESSAGE_RECEIVERS;

    static {
        // Known messages and their GCM message receivers
        Map<String, GCMCommand> receivers = new HashMap<String, GCMCommand>();
        receivers.put("test", new TestCommand());
        receivers.put("announcement", new AnnouncementCommand());
        receivers.put("sync_schedule", new SyncCommand());
        receivers.put("sync_user", new SyncUserCommand());
        receivers.put("notification", new NotificationCommand());
        MESSAGE_RECEIVERS = Collections.unmodifiableMap(receivers);
    }

    /**
     * Handle data in GCM message payload. The action field within the data determines the
     * type of Command that is initiated and executed.
     *
     * @param from The message sender. This will either be your Google Developer Project number
     *             or the name of a topic if topic messaging was used.
     * @param data A Bundle containing the action and extra data associated with that action.
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        // Handle received GCM data messages.
        String action = data.getString(ACTION);
        String extraData = data.getString(EXTRA_DATA);
        LOGD(TAG, "Got GCM message, " + ACTION + "=" + action +
                ", " + EXTRA_DATA + "=" + extraData);
        if (action == null) {
            LOGE(TAG, "Message received without command action");
            return;
        }
        action = action.toLowerCase();
        GCMCommand command = MESSAGE_RECEIVERS.get(action);
        if (command == null) {
            LOGE(TAG, "Unknown command received: " + action);
        } else {
            command.execute(this, action, extraData);
        }
    }
}
