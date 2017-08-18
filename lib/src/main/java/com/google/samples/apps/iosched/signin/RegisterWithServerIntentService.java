/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.signin;

import android.app.IntentService;
import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.samples.apps.iosched.fcm.ServerUtilities;

/**
 * Registers the users account ID and device ID pair with the server.
 */
public class RegisterWithServerIntentService extends IntentService {
    public static final String ACTION_REGISTER = "com.google.samples.apps.iosched." +
            "signin.action." + "register";

    public static final String ACTION_UNREGISTER = "com.google.samples.apps.iosched." +
            "signin.action." + "unregister";

    public static final String EXTRA_ACCOUNT_ID = "com.google.samples.apps.iosched.signin.extra." +
            "account_id";

    public RegisterWithServerIntentService() {
        super("RegisterWithServerIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            final String accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID);
            if (ACTION_REGISTER.equals(action)) {
                ServerUtilities.register(this, FirebaseInstanceId.getInstance().getToken(),
                        accountId);
            } else if (ACTION_UNREGISTER.equals(action)) {
                ServerUtilities.unregister(this, FirebaseInstanceId.getInstance().getToken());
            }
        }
    }
}