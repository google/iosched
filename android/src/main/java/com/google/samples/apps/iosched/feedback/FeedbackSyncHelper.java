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

import com.google.samples.apps.iosched.BuildConfig;
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

    private static final HashMap<String, String> QUESTION_KEYS = new HashMap<String, String>();
    static {
        QUESTION_KEYS.put(ScheduleContract.Feedback.SESSION_RATING, "Q10");
        QUESTION_KEYS.put(ScheduleContract.Feedback.ANSWER_RELEVANCE, "Q20");
        QUESTION_KEYS.put(ScheduleContract.Feedback.ANSWER_CONTENT, "Q30");
        QUESTION_KEYS.put(ScheduleContract.Feedback.ANSWER_SPEAKER, "Q40");
        QUESTION_KEYS.put(ScheduleContract.Feedback.COMMENTS, "Q50");
    }

    private static final HashMap<String, String> RATING_ANSWERS = new HashMap<String, String>();
    static {
        RATING_ANSWERS.put("1", "aece21ff-2cbe-e411-b87f-00155d5066d7");
        RATING_ANSWERS.put("2", "afce21ff-2cbe-e411-b87f-00155d5066d7");
        RATING_ANSWERS.put("3", "b0ce21ff-2cbe-e411-b87f-00155d5066d7");
        RATING_ANSWERS.put("4", "b1ce21ff-2cbe-e411-b87f-00155d5066d7");
        RATING_ANSWERS.put("5", "b2ce21ff-2cbe-e411-b87f-00155d5066d7");
    }

    private static final HashMap<String, String> RELEVANCE_ANSWERS = new HashMap<String, String>();
    static {
        RELEVANCE_ANSWERS.put("1", "9bce21ff-2cbe-e411-b87f-00155d5066d7");
        RELEVANCE_ANSWERS.put("2", "9cce21ff-2cbe-e411-b87f-00155d5066d7");
        RELEVANCE_ANSWERS.put("3", "9dce21ff-2cbe-e411-b87f-00155d5066d7");
        RELEVANCE_ANSWERS.put("4", "9ece21ff-2cbe-e411-b87f-00155d5066d7");
        RELEVANCE_ANSWERS.put("5", "9fce21ff-2cbe-e411-b87f-00155d5066d7");
    }

    private static final HashMap<String, String> CONTENT_ANSWERS = new HashMap<String, String>();
    static {
        CONTENT_ANSWERS.put("1", "a1ce21ff-2cbe-e411-b87f-00155d5066d7");
        CONTENT_ANSWERS.put("2", "a2ce21ff-2cbe-e411-b87f-00155d5066d7");
        CONTENT_ANSWERS.put("3", "a3ce21ff-2cbe-e411-b87f-00155d5066d7");
        CONTENT_ANSWERS.put("4", "a4ce21ff-2cbe-e411-b87f-00155d5066d7");
        CONTENT_ANSWERS.put("5", "a5ce21ff-2cbe-e411-b87f-00155d5066d7");
    }

    private static final HashMap<String, String> SPEAKER_ANSWERS = new HashMap<String, String>();
    static {
        SPEAKER_ANSWERS.put("1", "a8ce21ff-2cbe-e411-b87f-00155d5066d7");
        SPEAKER_ANSWERS.put("2", "a9ce21ff-2cbe-e411-b87f-00155d5066d7");
        SPEAKER_ANSWERS.put("3", "aace21ff-2cbe-e411-b87f-00155d5066d7");
        SPEAKER_ANSWERS.put("4", "abce21ff-2cbe-e411-b87f-00155d5066d7");
        SPEAKER_ANSWERS.put("5", "acce21ff-2cbe-e411-b87f-00155d5066d7");
    }

    Context mContext;
    FeedbackApiHelper mFeedbackApiHelper;

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
        HashMap<String, String> questions = new HashMap<String, String>();
        List<String> updatedSessions = new ArrayList<String>();

        try {
            while (c.moveToNext()) {
                String localSessionId = c.getString(c.getColumnIndex(ScheduleContract.Feedback.SESSION_ID));
                String remoteSessionId = localSessionId;
                // EventPoint uses a different Session ID for the keynote than our backend
                if ("__keynote__".equals(remoteSessionId)) {
                    remoteSessionId = BuildConfig.KEYNOTE_SESSION_ID;
                }

                String data;

                data = c.getString(c.getColumnIndex(ScheduleContract.Feedback.SESSION_RATING));
                questions.put(
                        QUESTION_KEYS.get(ScheduleContract.Feedback.SESSION_RATING),
                        RATING_ANSWERS.get(data));

                data = c.getString(c.getColumnIndex(ScheduleContract.Feedback.ANSWER_RELEVANCE));
                questions.put(
                        QUESTION_KEYS.get(ScheduleContract.Feedback.ANSWER_RELEVANCE),
                        RELEVANCE_ANSWERS.get(data));

                data = c.getString(c.getColumnIndex(ScheduleContract.Feedback.ANSWER_CONTENT));
                questions.put(
                        QUESTION_KEYS.get(ScheduleContract.Feedback.ANSWER_CONTENT),
                        CONTENT_ANSWERS.get(data));

                data = c.getString(c.getColumnIndex(ScheduleContract.Feedback.ANSWER_SPEAKER));
                questions.put(
                        QUESTION_KEYS.get(ScheduleContract.Feedback.ANSWER_SPEAKER),
                        SPEAKER_ANSWERS.get(data));

                data = c.getString(c.getColumnIndex(ScheduleContract.Feedback.COMMENTS));
                questions.put(
                        QUESTION_KEYS.get(ScheduleContract.Feedback.COMMENTS),
                        data);

                if (mFeedbackApiHelper.sendSessionToServer(remoteSessionId, questions)) {
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
