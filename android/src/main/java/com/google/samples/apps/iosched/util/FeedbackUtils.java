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

package com.google.samples.apps.iosched.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.service.FeedbackListenerService;
import com.google.samples.apps.iosched.service.SessionAlarmService;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A utility class for Session Feedback. No state is preserved and All methods are static.
 */
public class FeedbackUtils {

    private static final String TAG = makeLogTag(FeedbackUtils.class);

    /**
     * Saves the session feedback using the appropriate content provider.
     */
    public static void saveSessionFeedback(Context context, String sessionId, int rating,
            int q1Answer, int q2Answer, int q3Answer, String comments) {
        if (null == comments) {
            comments = "";
        }

        String answers = sessionId + ", "
                + rating + ", "
                + q1Answer + ", "
                + q2Answer + ", "
                + q3Answer + ", "
                + comments;
        LOGD(TAG, answers);

        ContentValues values = new ContentValues();
        values.put(ScheduleContract.Feedback.SESSION_ID, sessionId);
        values.put(ScheduleContract.Feedback.UPDATED, System.currentTimeMillis());
        values.put(ScheduleContract.Feedback.SESSION_RATING, rating);
        values.put(ScheduleContract.Feedback.ANSWER_RELEVANCE, q1Answer);
        values.put(ScheduleContract.Feedback.ANSWER_CONTENT, q2Answer);
        values.put(ScheduleContract.Feedback.ANSWER_SPEAKER, q3Answer);
        values.put(ScheduleContract.Feedback.COMMENTS, comments);

        Uri uri = context.getContentResolver()
                .insert(ScheduleContract.Feedback.buildFeedbackUri(sessionId), values);
        LOGD(TAG, null == uri ? "No feedback was saved" : uri.toString());
        dismissFeedbackNotification(context, sessionId);
    }

    /**
     * Invokes the action {@link SessionAlarmService#ACTION_NOTIFICATION_DISMISSAL} which should
     * result in removal of the notification associated with the given session, which, in turn,
     * should result in removal of the corresponding notifications on both ends.
     */
    public static void dismissFeedbackNotification(Context context, String sessionId) {
        Intent dismissalIntent = new Intent(context, FeedbackListenerService.class);
        dismissalIntent.setAction(SessionAlarmService.ACTION_NOTIFICATION_DISMISSAL);
        dismissalIntent.putExtra(SessionAlarmService.KEY_SESSION_ID, sessionId);
        context.startService(dismissalIntent);
    }

    /**
     * Returns the appropriate path for a session feedback with the given session id.
     */
    public static String getFeedbackPath(String sessionId) {
        return SessionAlarmService.PATH_FEEDBACK + "/" + sessionId;
    }

}
