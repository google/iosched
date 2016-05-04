/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.gcm;

import android.app.IntentService;
import android.content.Intent;

import com.google.samples.apps.iosched.messaging.MessagingRegistrationWithGCM;
import com.google.samples.apps.iosched.util.AccountUtils;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Requests that the server remove the specified Instance ID token (gcmId) from being paired with
 * the user with the specified gcmKey. Then update shared preferences to indicate that
 * the application InstanceID token is not registered with the server.
 */
public class GCMUnregisterIntentService extends IntentService {

    private static final String TAG = makeLogTag(GCMUnregisterIntentService.class);

    public GCMUnregisterIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String accountName = intent
                .getStringExtra(MessagingRegistrationWithGCM.ACTIVE_ACCOUNT_NAME);
        // Get the correct GCM key for the user. GCM key is a somewhat non-standard
        // approach we use in this app. For more about this, check GCM.TXT.
        final String gcmKey = accountName != null ?
                AccountUtils.getGcmKey(this, accountName) : null;

        // Unregister on server.
        ServerUtilities.unregister(ServerUtilities.getGcmRegId(this), gcmKey);

        // Set device unregistered on server.
        ServerUtilities.setRegisteredOnServer(this, false, ServerUtilities.getGcmRegId(this), null);
    }
}
