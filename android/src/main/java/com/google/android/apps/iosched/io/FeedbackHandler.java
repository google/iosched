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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;

import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.Lists;

import com.google.api.services.googledevelopers.Googledevelopers;
import com.google.api.services.googledevelopers.model.FeedbackResponse;
import com.google.api.services.googledevelopers.model.ModifyFeedbackRequest;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.android.apps.iosched.util.LogUtils.*;

public class FeedbackHandler {

    private static final String TAG = makeLogTag(FeedbackHandler.class);

    private Context mContext;

    public FeedbackHandler(Context context) {
        mContext = context;
    }

    public ArrayList<ContentProviderOperation> uploadNew(Googledevelopers conferenceApi) {
        // Collect rows of feedback
        Cursor feedbackCursor = mContext.getContentResolver().query(
                ScheduleContract.Feedback.CONTENT_URI,
                null, null, null, null);

        int sessionIdIndex = feedbackCursor.getColumnIndex(ScheduleContract.Feedback.SESSION_ID);
        int ratingIndex = feedbackCursor.getColumnIndex(ScheduleContract.Feedback.SESSION_RATING);
        int relIndex = feedbackCursor.getColumnIndex(ScheduleContract.Feedback.ANSWER_RELEVANCE);
        int contentIndex = feedbackCursor.getColumnIndex(ScheduleContract.Feedback.ANSWER_CONTENT);
        int speakerIndex = feedbackCursor.getColumnIndex(ScheduleContract.Feedback.ANSWER_SPEAKER);
        int willUseIndex = feedbackCursor.getColumnIndex(ScheduleContract.Feedback.ANSWER_WILLUSE);
        int commentsIndex = feedbackCursor.getColumnIndex(ScheduleContract.Feedback.COMMENTS);

        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        while (feedbackCursor.moveToNext()) {
            String sessionId = feedbackCursor.getString(sessionIdIndex);
            LOGI(TAG, "Uploading feedback for: " + sessionId);
            int rating = feedbackCursor.getInt(ratingIndex);
            int answerRelevance = feedbackCursor.getInt(relIndex);
            int answerContent = feedbackCursor.getInt(contentIndex);
            int answerSpeaker = feedbackCursor.getInt(speakerIndex);
            int answerWillUseRaw = feedbackCursor.getInt(willUseIndex);
            boolean answerWillUse = (answerWillUseRaw != 0);
            String comments = feedbackCursor.getString(commentsIndex);

            ModifyFeedbackRequest feedbackRequest = new ModifyFeedbackRequest();

            feedbackRequest.setOverallScore(rating);

            feedbackRequest.setRelevancyScore(answerRelevance);
            feedbackRequest.setContentScore(answerContent);
            feedbackRequest.setSpeakerScore(answerSpeaker);

            // In this case, -1 means the user didn't answer the question.
            if (answerWillUseRaw != -1) {
                feedbackRequest.setWillUse(answerWillUse);
            }

            // Only post something If the comments field isn't empty
            if (comments != null && comments.length() > 0) {
                feedbackRequest.setAdditionalFeedback(comments);
            }

            feedbackRequest.setSessionId(sessionId);
            feedbackRequest.setEventId(Config.EVENT_ID);

            try {
                Googledevelopers.Events.Sessions.Feedback feedback = conferenceApi.events().sessions()
                        .feedback(Config.EVENT_ID, sessionId, feedbackRequest);
                FeedbackResponse response = feedback.execute();

                if (response != null) {
                    LOGI(TAG, "Successfully sent feedback for: " + sessionId + ", comment: " + comments);
                } else {
                    LOGE(TAG, "Sending logs failed");
                }
            } catch (IOException ioe) {
                LOGE(TAG, "Sending logs failed and caused IOE", ioe);
                return batch;
            }
        }
        feedbackCursor.close();

        // Clear out feedback forms we've just uploaded
        batch.add(ContentProviderOperation.newDelete(
                ScheduleContract.addCallerIsSyncAdapterParameter(
                        ScheduleContract.Feedback.CONTENT_URI))
                .build());
        return batch;
    }
}
