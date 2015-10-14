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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.gms.wearable.DataMap;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.service.SessionAlarmService;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
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
        dismissFeedbackNotification(data.sessionId);
    }

    /**
     * Invokes the {@link FeedbackWearableListenerService} to dismiss the notification on both the device
     * and wear.
     */
    private void dismissFeedbackNotification(String sessionId) {
        Intent dismissalIntent = new Intent(mContext, FeedbackWearableListenerService.class);
        dismissalIntent.setAction(SessionAlarmService.ACTION_NOTIFICATION_DISMISSAL);
        dismissalIntent.putExtra(SessionAlarmService.KEY_SESSION_ID, sessionId);
        mContext.startService(dismissalIntent);
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

    /**
     * Returns the path for the session feedback with the given session id, as used when
     * communication with the wear app.
     */
    public static String getFeedbackDataPathForWear(String sessionId) {
        return SessionAlarmService.PATH_FEEDBACK + "/" + sessionId;
    }

    /**
     * Converts the feedback data from a {@link com.google.android.gms.wearable.DataMap} to a
     * {@link SessionFeedbackModel.SessionFeedbackData};
     *
     * The {@code data} is a JSON string. The format of a typical response is:
     * <pre>[{"s":"sessionId-1234"},{"q":1,"a":2},{"q":0,"a":1},{"q":3,"a":1},{"q":2,"a":1}]</pre>
     *
     * @return SessionFeedbackData object, or null if JSON Parsing error
     */
    public static SessionFeedbackData convertDataMapToFeedbackData(DataMap data) {
        try {
            if (data == null){
                LOGE(TAG, "Failed to parse session data, DataMap is null");
                return null;
            }
            String jsonString = data.getString("response");
            if (TextUtils.isEmpty(jsonString)) {
                LOGE(TAG, "Failed to parse session data, empty json string");
                return null;
            }

            LOGD(TAG, "jsonString is: " + jsonString);

            JSONArray jsonArray = new JSONArray(jsonString);
            if (jsonArray.length() > 0) {
                JSONObject sessionObj = (JSONObject) jsonArray.get(0);
                String sessionId = sessionObj.getString("s");
                int[] answers = new int[4];
                for (int i = 0; i < answers.length; i++) {
                    answers[i] = -1;
                }
                for (int i = 1; i < jsonArray.length(); i++) {
                    JSONObject answerObj = (JSONObject) jsonArray.get(i);
                    int question = answerObj.getInt("q");
                    int answer = answerObj.getInt("a") + 1;
                    answers[question] = answer;
                }
                return new SessionFeedbackData(sessionId, answers[0], answers[1], answers[2],
                        answers[3], null);
            }

        } catch (JSONException e) {
            LOGE(TAG, "Failed to parse the json received from the wear", e);
        }
        return null;
    }

}
