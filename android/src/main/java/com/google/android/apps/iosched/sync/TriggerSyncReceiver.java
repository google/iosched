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


package com.google.android.apps.iosched.sync;

import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.AccountUtils;
import com.google.android.gms.auth.GoogleAuthUtil;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * A simple {@link BroadcastReceiver} that triggers a sync. This is used by the GCM code to trigger
 * jittered syncs using {@link android.app.AlarmManager}.
 */
public class TriggerSyncReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String accountName = AccountUtils.getChosenAccountName(context);
        if (TextUtils.isEmpty(accountName)) {
            return;
        }

        ContentResolver.requestSync(
                AccountUtils.getChosenAccount(context),
                ScheduleContract.CONTENT_AUTHORITY, new Bundle());
    }
}
