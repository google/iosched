/*
 * Copyright 2014 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.ui.debug.actions;

import android.content.ContentResolver;
import android.accounts.Account;
import android.content.Context;
import android.os.AsyncTask;
import android.content.SyncResult;
import android.os.Bundle;

import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.ui.debug.DebugAction;
import com.google.samples.apps.iosched.util.AccountUtils;

/**
 * A DebugAction that runs an immediate full sync.
 */
public class ForceSyncNowAction implements DebugAction {
    @Override
    public void run(final Context context, final Callback callback) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... contexts) {
                Account account = AccountUtils.getActiveAccount(context);
                if (account == null) {
                    callback.done(false, "Cannot sync if there is no active account.");
                } else {
                    new SyncHelper(contexts[0]).performSync(new SyncResult(),
                      AccountUtils.getActiveAccount(context), bundle);
                }
              return null;
            }
        }.execute(context);
    }

    @Override
    public String getLabel() {
        return "Force data sync now";
    }

}
