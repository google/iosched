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

package com.google.samples.apps.iosched.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.samples.apps.iosched.provider.ScheduleContract;

import java.util.ArrayList;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * Created by lwray on 5/8/14.
 */
public class FeedbackSyncHelper {
    private static final String TAG = makeLogTag(FeedbackSyncHelper.class);


    Context mContext;
    EventFeedbackApi mEventFeedbackApi;

    FeedbackSyncHelper(Context context) {
        mContext = context;
        mEventFeedbackApi = new EventFeedbackApi(context);

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
        List<String> updatedSessions = new ArrayList<String>();

        while (c.moveToNext()) {
            String sessionId = c.getString(c.getColumnIndex(ScheduleContract.Feedback.SESSION_ID));

            List<String> questions = new ArrayList<String>();
            questions.add(c.getString(c.getColumnIndex(ScheduleContract.Feedback.SESSION_RATING)));
            questions.add(c.getString(c.getColumnIndex(ScheduleContract.Feedback.ANSWER_RELEVANCE)));
            questions.add(c.getString(c.getColumnIndex(ScheduleContract.Feedback.ANSWER_CONTENT)));
            questions.add(c.getString(c.getColumnIndex(ScheduleContract.Feedback.ANSWER_SPEAKER)));
            questions.add(c.getString(c.getColumnIndex(ScheduleContract.Feedback.COMMENTS)));

            if (mEventFeedbackApi.sendSessionToServer(sessionId, questions)) {
                LOGI(TAG, "Successfully updated session " + sessionId);
                updatedSessions.add(sessionId);
            } else {
                LOGE(TAG, "Couldn't update session " + sessionId);
            }
        }

        c.close();

        // Flip the "synced" flag to true for any successfully updated sessions, but leave them
        // in the database to prevent duplicate feedback
        ContentValues contentValues = new ContentValues();
        contentValues.put(ScheduleContract.Feedback.SYNCED, 1);
        for (String sessionId : updatedSessions) {
            cr.update(ScheduleContract.Feedback.buildFeedbackUri(sessionId), contentValues, null, null);
        }

    }
}
