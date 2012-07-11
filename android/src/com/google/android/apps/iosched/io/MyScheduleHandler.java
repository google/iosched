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

package com.google.android.apps.iosched.io;

import com.google.android.apps.iosched.io.model.MyScheduleItem;
import com.google.android.apps.iosched.io.model.MyScheduleResponse;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.util.Lists;
import com.google.gson.Gson;

import android.content.ContentProviderOperation;
import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.android.apps.iosched.util.LogUtils.LOGI;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Handler that parses "my schedule" JSON data into a list of content provider operations.
 */
public class MyScheduleHandler extends JSONHandler {

    private static final String TAG = makeLogTag(MyScheduleHandler.class);

    public MyScheduleHandler(Context context) {
        super(context);
    }

    public ArrayList<ContentProviderOperation> parse(String json)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        MyScheduleResponse response = new Gson().fromJson(json, MyScheduleResponse.class);
        if (response.error == null) {
            LOGI(TAG, "Updating user's schedule");
            if (response.schedule_list != null) {
                // Un-star all sessions first
                batch.add(ContentProviderOperation
                        .newUpdate(ScheduleContract.addCallerIsSyncAdapterParameter(
                                Sessions.CONTENT_URI))
                        .withValue(Sessions.SESSION_STARRED, 0)
                        .build());

                // Star only those sessions in the "my schedule" response
                for (MyScheduleItem item : response.schedule_list) {
                    batch.add(ContentProviderOperation
                            .newUpdate(ScheduleContract.addCallerIsSyncAdapterParameter(
                                    Sessions.buildSessionUri(item.session_id)))
                            .withValue(Sessions.SESSION_STARRED, 1)
                            .build());
                }
            }
        }

        return batch;
    }
}
