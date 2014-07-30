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

package com.google.samples.apps.iosched.io;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContract.Announcements;
import com.google.samples.apps.iosched.provider.ScheduleContract.SyncColumns;
import com.google.samples.apps.iosched.util.Lists;
import com.google.samples.apps.iosched.util.NetUtils;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.CommonGoogleClientRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Activity;
import com.google.api.services.plus.model.ActivityFeed;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class AnnouncementsFetcher {
    private static final String TAG = makeLogTag(AnnouncementsFetcher.class);
    private Context mContext;

    public AnnouncementsFetcher(Context context) {
        mContext = context;
    }

    public ArrayList<ContentProviderOperation> fetchAndParse() throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        // Set up the HTTP transport and JSON factory
        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new AndroidJsonFactory();

        // Set up the main Google+ class
        Plus plus = new Plus.Builder(httpTransport, jsonFactory, null)
                .setApplicationName(NetUtils.getUserAgent(mContext))
                .setGoogleClientRequestInitializer(
                        new CommonGoogleClientRequestInitializer(Config.API_KEY))
                .build();

        ActivityFeed activities;
        try {
            activities = plus.activities().list(Config.ANNOUNCEMENTS_PLUS_ID, "public")
                    .setMaxResults(100l)
                    .execute();
            if (activities == null || activities.getItems() == null) {
                throw new IOException("Activities list was null.");
            }

        } catch (IOException e) {
            LOGE(TAG, "Error fetching announcements", e);
            return batch;
        }

        LOGI(TAG, "Updating announcements data");

        // Clear out existing announcements
        batch.add(ContentProviderOperation
                .newDelete(ScheduleContract.addCallerIsSyncAdapterParameter(
                        Announcements.CONTENT_URI))
                .build());

        StringBuilder sb = new StringBuilder();
        for (Activity activity : activities.getItems()) {
            // Filter out anything not including the conference hashtag.
            sb.setLength(0);
            appendIfNotEmpty(sb, activity.getAnnotation());
            if (activity.getObject() != null) {
                appendIfNotEmpty(sb, activity.getObject().getContent());
            }

            if (!sb.toString().contains(Config.CONFERENCE_HASHTAG)) {
                continue;
            }

            // Insert announcement info
            batch.add(ContentProviderOperation
                    .newInsert(ScheduleContract
                            .addCallerIsSyncAdapterParameter(Announcements.CONTENT_URI))
                    .withValue(SyncColumns.UPDATED, System.currentTimeMillis())
                    .withValue(Announcements.ANNOUNCEMENT_ID, activity.getId())
                    .withValue(Announcements.ANNOUNCEMENT_DATE, activity.getUpdated().getValue())
                    .withValue(Announcements.ANNOUNCEMENT_TITLE, activity.getTitle())
                    .withValue(Announcements.ANNOUNCEMENT_ACTIVITY_JSON, activity.toPrettyString())
                    .withValue(Announcements.ANNOUNCEMENT_URL, activity.getUrl())
                    .build());
        }

        return batch;
    }

    private static void appendIfNotEmpty(StringBuilder sb, String s) {
        if (!TextUtils.isEmpty(s)) {
            sb.append(s);
        }
    }
}
