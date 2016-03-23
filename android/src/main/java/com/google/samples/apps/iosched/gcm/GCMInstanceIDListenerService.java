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

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.google.samples.apps.iosched.util.AccountUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGV;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * In the event that the current InstanceID token is invalidated we request the token be unpaired
 * with the user. Then {@see com.google.samples.apps.iosched.gcm.GCMRegistrationIntentService} is
 * started to generate a new InstanceID token.
 */
public class GCMInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = makeLogTag("GCMIIDListenerService");

    @Override
    public void onTokenRefresh() {
        LOGV(TAG, "Set registered to false");
        ServerUtilities.setRegisteredOnServer(this, false, ServerUtilities.getGcmRegId(this), null);

        // Get the correct GCM key for the user. GCM key is a somewhat non-standard
        // approach we use in this app. For more about this, check GCM.md.
        final String gcmKey = AccountUtils.hasActiveAccount(this) ?
                AccountUtils.getGcmKey(this, AccountUtils.getActiveAccountName(this)) : null;

        // Unregister on server.
        ServerUtilities.unregister(ServerUtilities.getGcmRegId(this), gcmKey);

        // Register for a new InstanceID token. This token is sent to the server to be paired with
        // the current user's GCM key.
        Intent intent = new Intent(this, GCMRegistrationIntentService.class);
        startService(intent);
    }
}
