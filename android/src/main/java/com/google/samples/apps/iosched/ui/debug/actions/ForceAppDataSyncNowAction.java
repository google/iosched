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

import android.content.Context;
import android.os.AsyncTask;

import com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper;
import com.google.samples.apps.iosched.sync.userdata.UserDataSyncHelperFactory;
import com.google.samples.apps.iosched.ui.debug.DebugAction;
import com.google.samples.apps.iosched.util.AccountUtils;

/**
 * Run an AppData sync immediately.
 */
public class ForceAppDataSyncNowAction implements DebugAction {

    @Override
    public void run(final Context context, final Callback callback) {
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... params) {
                final AbstractUserDataSyncHelper syncer = UserDataSyncHelperFactory.buildSyncHelper(
                        context, AccountUtils.getActiveAccountName(context));
                syncer.sync();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                callback.done(true, "Sync done");
            }
        }.execute(context);

    }

    @Override
    public String getLabel() {
        return "run DriveApp sync";
    }
}
