/*
 * Copyright 2015 Google Inc. All rights reserved.
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


import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.apps.iosched.archframework.ModelWithLoaderManager;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.archframework.UserActionEnum;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackQueryEnum;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackUserActionEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.AnalyticsHelper;

public class SessionFeedbackModel extends
        ModelWithLoaderManager<SessionFeedbackQueryEnum, SessionFeedbackUserActionEnum> {

    protected final static String DATA_RATING_INT = "DATA_RATING_INT";

    protected final static String DATA_SESSION_RELEVANT_ANSWER_INT =
            "DATA_SESSION_RELEVANT_ANSWER_INT";

    protected final static String DATA_CONTENT_ANSWER_INT = "DATA_CONTENT_ANSWER_INT";

    protected final static String DATA_SPEAKER_ANSWER_INT = "DATA_SPEAKER_ANSWER_INT";

    protected final static String DATA_COMMENT_STRING = "DATA_COMMENT_STRING";

    private final Context mContext;

    private FeedbackHelper mFeedbackHelper;

    private Uri mSessionUri;

    private String mTitleString;

    private String mSpeakersString;

    public SessionFeedbackModel(LoaderManager loaderManager, Uri sessionUri, Context context,
            FeedbackHelper feedbackHelper) {
        super(SessionFeedbackQueryEnum.values(), SessionFeedbackUserActionEnum.values(),
                loaderManager);
        mContext = context;
        mSessionUri = sessionUri;
        mFeedbackHelper = feedbackHelper;
    }

    public String getSessionTitle() {
        return mTitleString;
    }

    public String getSessionSpeakers() {
        return mSpeakersString;
    }

    @Override
    public void cleanUp() {
        // Nothing to clean up
    }

    @Override
    public void processUserAction(final SessionFeedbackUserActionEnum action,
            @Nullable final Bundle args, final UserActionCallback callback) {
        switch (action) {
            case SUBMIT:
                mFeedbackHelper
                        .saveSessionFeedback(new SessionFeedbackData(getSessionId(mSessionUri),
                                args.getInt(DATA_RATING_INT),
                                args.getInt(DATA_SESSION_RELEVANT_ANSWER_INT),
                                args.getInt(DATA_CONTENT_ANSWER_INT),
                                args.getInt(DATA_SPEAKER_ANSWER_INT),
                                args.getString(DATA_COMMENT_STRING)));

                // ANALYTICS EVENT: Send session feedback
                // Contains: Session title.  Feedback is NOT included.
                sendAnalyticsEvent("Session", "Feedback", mTitleString);
                callback.onModelUpdated(this, action);
                break;
            default:
                callback.onError(action);
                break;
        }
    }

    @Override
    public Loader<Cursor> createCursorLoader(final SessionFeedbackQueryEnum query,
            final Bundle args) {
        CursorLoader loader = null;
        switch (query) {
            case SESSION:
                loader = getCursorLoaderInstance(mContext, mSessionUri,
                        SessionFeedbackQueryEnum.SESSION.getProjection(), null, null, null);
                break;
        }
        return loader;
    }

    @Override
    public boolean readDataFromCursor(final Cursor cursor,
            final SessionFeedbackQueryEnum query) {
        if (!cursor.moveToFirst()) {
            return false;
        }

        switch (query) {
            case SESSION:
                mTitleString = cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_TITLE));

                mSpeakersString = cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_SPEAKER_NAMES));
                return true;
            default:
                return false;

        }
    }

    @VisibleForTesting
    public CursorLoader getCursorLoaderInstance(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        return new CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder);
    }

    @VisibleForTesting
    public String getSessionId(Uri uri) {
        return ScheduleContract.Sessions.getSessionId(uri);
    }

    @VisibleForTesting
    public void sendAnalyticsEvent(String category, String action, String label) {
        AnalyticsHelper.sendEvent(category, action, label);
    }

    public enum SessionFeedbackQueryEnum implements QueryEnum {
        SESSION(0, new String[]{
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_SPEAKER_NAMES});

        private int id;

        private String[] projection;

        SessionFeedbackQueryEnum(int id, String[] projection) {
            this.id = id;
            this.projection = projection;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String[] getProjection() {
            return projection;
        }

    }

    public enum SessionFeedbackUserActionEnum implements UserActionEnum {
        SUBMIT(1);

        private int id;

        SessionFeedbackUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

    }

    public static class SessionFeedbackData {

        public String sessionId;

        public int sessionRating;

        public int sessionRelevantAnswer;

        public int contentAnswer;

        public int speakerAnswer;

        public String comments;

        public SessionFeedbackData(String sessionId, int sessionRating, int sessionRelevantAnswer,
                int contentAnswer, int speakerAnswer, String comments) {
            this.sessionId = sessionId;
            this.sessionRating = sessionRating;
            this.sessionRelevantAnswer = sessionRelevantAnswer;
            this.contentAnswer = contentAnswer;
            this.speakerAnswer = speakerAnswer;
            this.comments = comments;
        }

        @Override
        public String toString() {
            return "SessionId: " + sessionId +
                    " SessionRating: " + sessionRating +
                    " SessionRelevantAnswer: " + sessionRelevantAnswer +
                    " ContentAnswer: " + contentAnswer +
                    " SpeakerAnswer: " + speakerAnswer +
                    " Comments: " + comments;
        }
    }

}