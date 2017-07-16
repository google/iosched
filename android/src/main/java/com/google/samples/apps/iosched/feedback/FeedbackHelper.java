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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackData;
import com.google.samples.apps.iosched.provider.ScheduleContract;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A helper class for Session Feedback.
 */
public class FeedbackHelper {

    private static final String TAG = makeLogTag(FeedbackHelper.class);

    private Context mContext;

    public FeedbackHelper(Context context) {
        mContext = context;
    }

    /**
     * Saves the session feedback using the appropriate content provider, and dismisses the
     * feedback notification.
     */
    public void saveSessionFeedback(SessionFeedbackData data) {
        if (null == data.comments) {
            data.comments = "";
        }

        String answers = data.sessionId + ", " + data.sessionRating + ", " + data.sessionRelevantAnswer
                + ", " + data.contentAnswer + ", " + data.speakerAnswer + ", " + data.comments;
        LOGD(TAG, answers);

        ContentValues values = new ContentValues();
        values.put(ScheduleContract.Feedback.SESSION_ID, data.sessionId);
        values.put(ScheduleContract.Feedback.UPDATED, System.currentTimeMillis());
        values.put(ScheduleContract.Feedback.SESSION_RATING, data.sessionRating);
        values.put(ScheduleContract.Feedback.ANSWER_RELEVANCE, data.sessionRelevantAnswer);
        values.put(ScheduleContract.Feedback.ANSWER_CONTENT, data.contentAnswer);
        values.put(ScheduleContract.Feedback.ANSWER_SPEAKER, data.speakerAnswer);
        values.put(ScheduleContract.Feedback.COMMENTS, data.comments);

        Uri uri = mContext.getContentResolver()
                .insert(ScheduleContract.Feedback.buildFeedbackUri(data.sessionId), values);
        LOGD(TAG, null == uri ? "No feedback was saved" : uri.toString());
    }

    /**
     * Whether a feedback notification has already been fired for a particular {@code sessionId}.
     *
     * @return true if feedback notification has been fired.
     */
    public boolean isFeedbackNotificationFiredForSession(String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        final String key = createKeyForNotification(sessionId);
        return sp.getBoolean(key, false);
    }

    /**
     * Sets feedback notification as fired for a particular {@code sessionId}.
     */
    public void setFeedbackNotificationAsFiredForSession(String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        final String key = createKeyForNotification(sessionId);
        sp.edit().putBoolean(key, true).apply();
    }

    private String createKeyForNotification(String sessionId){
        return String.format("feedback_notification_fired_%s", sessionId);
    }

}
