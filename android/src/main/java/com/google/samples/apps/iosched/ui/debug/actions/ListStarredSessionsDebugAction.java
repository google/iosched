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

import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;
import com.google.samples.apps.iosched.ui.debug.DebugAction;
import com.google.api.client.util.Charsets;

import java.util.Set;

/**
 * Simple DebugAction that lists starred sessions of current user.
 *
 */
public class ListStarredSessionsDebugAction implements DebugAction {

    @Override
    public void run(Context context, final Callback callback) {
        new AsyncTask<Context, Void, Set<String>>() {
            @Override
            protected Set<String> doInBackground(Context... contexts) {
                return UserDataHelper.getLocalStarredSessionIDs(contexts[0]);
            }

            @Override
            protected void onPostExecute(Set<String> sessions) {
                callback.done(true, "Found sessions: " + new String(
                        UserDataHelper.toByteArray(sessions), Charsets.UTF_8));
            }
        }.execute(context);
    }

    @Override
    public String getLabel() {
        return "show local starred sessions";
    }

}
