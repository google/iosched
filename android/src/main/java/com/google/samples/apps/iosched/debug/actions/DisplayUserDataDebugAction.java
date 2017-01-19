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
package com.google.samples.apps.iosched.debug.actions;

import com.google.common.base.Charsets;
import com.google.samples.apps.iosched.debug.DebugAction;
import com.google.samples.apps.iosched.sync.userdata.util.UserData;
import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Simple DebugAction that displays the local user data of a current user.
 */
public class DisplayUserDataDebugAction implements DebugAction {

    @Override
    public void run(Context context, final Callback callback) {
        new AsyncTask<Context, Void, UserData>() {
            @Override
            protected UserData doInBackground(Context... contexts) {
                return UserDataHelper.getLocalUserData(contexts[0]);
            }

            @Override
            protected void onPostExecute(UserData userData) {
                callback.done(true, "Found User Data: " + new String(
                        UserDataHelper.toByteArray(userData), Charsets.UTF_8));
            }
        }.execute(context);
    }

    @Override
    public String getLabel() {
        return "show local user data";
    }

}
