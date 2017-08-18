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

package com.google.samples.apps.iosched.feedback;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.samples.apps.iosched.provider.ScheduleContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * Provides unidirectional sync from the feedback data provided by the user to the server feedback
 * API.
 */
public class FeedbackSyncHelper {
    private static final String TAG = makeLogTag(FeedbackSyncHelper.class);

    /**
     * Translates between fields in ScheduleContract.Feedback and numerical values sent to the
     * server.
     */
    private static final HashMap<String, Integer> FIELDS_TO_VALUES_MAP =
            new HashMap<String, Integer>() {{
        put(ScheduleContract.Feedback.SESSION_RATING, 10);
        put(ScheduleContract.Feedback.ANSWER_RELEVANCE, 20);
        put(ScheduleContract.Feedback.ANSWER_CONTENT, 30);
        put(ScheduleContract.Feedback.ANSWER_SPEAKER, 40);
    }};

    private Context mContext;
    private FeedbackApiHelper mFeedbackApiHelper;

    public FeedbackSyncHelper(Context context, FeedbackApiHelper feedbackApi) {
        mContext = context;
        mFeedbackApiHelper = feedbackApi;
    }

    public void sync() {
        final ContentResolver cr = mContext.getContentResolver();
        final Uri newFeedbackUri = ScheduleContract.Feedback.CONTENT_URI;
        Cursor c = cr.query(newFeedbackUri,
                null,
                ScheduleContract.Feedback.SYNCED + " = 0",
                null,
                null);

        LOGD(TAG, "Number of unsynced feedbacks: " + c.getCount());
        HashMap<String, String> questions = new HashMap<>();

        HashMap<Integer, Integer> result = new HashMap<>();

        List<String> updatedSessions = new ArrayList<>();

        try {
            while (c.moveToNext()) {
                String localSessionId = c.getString(c.getColumnIndex(ScheduleContract.Feedback.SESSION_ID));
                String remoteSessionId = localSessionId;

                String field;
                String data;

                field = ScheduleContract.Feedback.SESSION_RATING;
                data = c.getString(c.getColumnIndex(field));
                result.put(FIELDS_TO_VALUES_MAP.get(field), Integer.valueOf(data));

                field = ScheduleContract.Feedback.ANSWER_RELEVANCE;
                data = c.getString(c.getColumnIndex(field));
                result.put(FIELDS_TO_VALUES_MAP.get(field), Integer.valueOf(data));

                field = ScheduleContract.Feedback.ANSWER_CONTENT;
                data = c.getString(c.getColumnIndex(field));
                result.put(FIELDS_TO_VALUES_MAP.get(field), Integer.valueOf(data));

                field = ScheduleContract.Feedback.ANSWER_SPEAKER;
                data = c.getString(c.getColumnIndex(field));
                result.put(FIELDS_TO_VALUES_MAP.get(field), Integer.valueOf(data));

                if (mFeedbackApiHelper.sendSessionToServer(remoteSessionId, result)) {
                    LOGI(TAG, "Successfully updated session " + remoteSessionId);
                    updatedSessions.add(localSessionId);
                } else {
                    LOGE(TAG, "Couldn't update session " + remoteSessionId);
                }
            }
        } catch (Exception e){
            LOGE(TAG, "Couldn't read from cursor " + e);
        } finally {
            c.close();
        }

        // Flip the "synced" flag to true for any successfully updated sessions, but leave them
        // in the database to prevent duplicate feedback
        ContentValues contentValues = new ContentValues();
        contentValues.put(ScheduleContract.Feedback.SYNCED, 1);
        for (String sessionId : updatedSessions) {
            cr.update(ScheduleContract.Feedback.buildFeedbackUri(sessionId), contentValues, null, null);
        }
    }
}
