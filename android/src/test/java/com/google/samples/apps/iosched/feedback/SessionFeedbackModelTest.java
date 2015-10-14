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

import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.samples.apps.iosched.InvalidEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class SessionFeedbackModelTest {

    private static final String FAKE_TITLE = "FAKE TITLE";

    private static final int SESSION_TITLE_COLUMN_INDEX = 1;

    @Mock
    private FeedbackHelper mMockFeedbackHelper;

    @Mock
    private Uri mMockUri;

    @Mock
    private Context mMockContext;

    @Mock
    private Cursor mMockCursor;

    @Mock
    private Bundle mMockBundle;


    private SessionFeedbackModel mSessionFeedbackModel;

    @Before
    public void setUp() {
        // Mocks
        initWithStubbedCursor(mMockCursor);

        // Create an instance of the model.
        mSessionFeedbackModel = new SessionFeedbackModel(mMockUri, mMockContext,
                mMockFeedbackHelper);
    }

    @Test
    public void requestModelUpdate_SubmitFeedback_Success() {
        // Given a mock bundle with feedback data and a mock FeedbackHelper
        SessionFeedbackModel spyModel = spy(
                new SessionFeedbackModel(mMockUri, mMockContext, mMockFeedbackHelper));

        doReturn(FAKE_TITLE).when(spyModel).getSessionId(mMockUri);
        doNothing().when(spyModel)
                .sendAnalyticsEvent(anyString(), anyString(), anyString());

        final String comments = "My comment";
        int rating = 1;
        int sessionRelevantAnswer = 2;
        int contentAnswer = 3;
        int speakerAnswer = 2;
        when(mMockBundle.getInt(SessionFeedbackModel.DATA_RATING_INT)).thenReturn(rating);
        when(mMockBundle.getInt(SessionFeedbackModel.DATA_SESSION_RELEVANT_ANSWER_INT)).thenReturn(sessionRelevantAnswer);
        when(mMockBundle.getInt(SessionFeedbackModel.DATA_CONTENT_ANSWER_INT)).thenReturn(contentAnswer);
        when(mMockBundle.getInt(SessionFeedbackModel.DATA_SPEAKER_ANSWER_INT)).thenReturn(speakerAnswer);
        when(mMockBundle.getString(SessionFeedbackModel.DATA_COMMENT_STRING)).thenReturn(comments);

        // When ran with the submit user action and the mock bundle
        spyModel.requestModelUpdate(
                SessionFeedbackModel.SessionFeedbackUserActionEnum.SUBMIT, mMockBundle);

        // Then the saveSessionFeedback method in the FeedbackHelper is called with the correct
        // feedback data parameters
        ArgumentCaptor<SessionFeedbackData> saveSessionCaptor =
                ArgumentCaptor.forClass(SessionFeedbackData.class);
        verify(mMockFeedbackHelper).saveSessionFeedback(saveSessionCaptor.capture());
        assertThat(saveSessionCaptor.getValue().sessionId, is(spyModel.getSessionId(mMockUri)));
        assertThat(saveSessionCaptor.getValue().sessionRating, is(rating));
        assertThat(saveSessionCaptor.getValue().sessionRelevantAnswer, is(sessionRelevantAnswer));
        assertThat(saveSessionCaptor.getValue().contentAnswer, is(contentAnswer));
        assertThat(saveSessionCaptor.getValue().speakerAnswer, is(speakerAnswer));
        assertThat(saveSessionCaptor.getValue().comments, is(comments));
    }

    @Test
    public void requestModelUpdate_UserStartedUnknownAction_Unsuccessful() {
        // When ran with an invalid user action
        boolean success = mSessionFeedbackModel.requestModelUpdate(InvalidEnum.INVALID, null);

        // Then the request model update fails
        assertThat(success, is(false));
    }

    @Test
    public void readDataFromCursor_SessionQuery_SessionLoaded() {
        // Given a mock cursor with data
        when(mMockCursor.moveToFirst()).thenReturn(true);

        // When ran with a valid query
        boolean success = mSessionFeedbackModel.readDataFromCursor(
                mMockCursor, SessionFeedbackModel.SessionFeedbackQueryEnum.SESSION);

        // Then the model is updated and the request succeeds
        assertThat(mSessionFeedbackModel.getSessionTitle(), is(FAKE_TITLE));
        assertThat(success, is(true));
    }

    @Test
    public void readDataFromCursor_UnsupportedQuery_Unsuccessful() {
        // When ran with an invalid query
        boolean result = mSessionFeedbackModel.readDataFromCursor(mMockCursor, InvalidEnum.INVALID);

        // Then the request model update fails
        assertThat(result, is(false));
    }

    @Test
    public void readDataFromCursor_EmptyCursor_Unsuccessful() {
        // Given an empty mock cursor
        when(mMockCursor.moveToFirst()).thenReturn(false);

        // When ran with a valid query
        boolean result = mSessionFeedbackModel.readDataFromCursor(mMockCursor,
                SessionFeedbackModel.SessionFeedbackQueryEnum.SESSION);

        // Then the request model update fails
        assertThat(result, is(false));
    }

    @Test
    public void createCursorLoader_SessionQuery_Success() {
        // Given a mock cursor loader set up for a session query
        int sessionsLoaderId = SessionFeedbackModel.SessionFeedbackQueryEnum.SESSION.getId();

        CursorLoader mockCursorLoaderSession = mock(CursorLoader.class);

        SessionFeedbackModel spyModel = spy(
                new SessionFeedbackModel(mMockUri, mMockContext, mMockFeedbackHelper));

        doReturn(mockCursorLoaderSession).when(spyModel).getCursorLoaderInstance(
                any(Context.class), any(Uri.class), any(String[].class), any(String.class),
                any(String[].class), any(String.class));

        // When ran with the session query
        CursorLoader createdCursorLoader1 =
                (CursorLoader) spyModel.createCursorLoader(sessionsLoaderId, mMockUri, null);

        // Then the returned cursor loader is the same as the mock one
        assertThat(createdCursorLoader1, sameInstance(mockCursorLoaderSession));
    }

    @Test
    public void createCursorLoader_UnknownLoaderId_CreatesNullCursorLoader() {
        // When ran with an invalid query
        Loader<Cursor> cursorLoader = mSessionFeedbackModel
                .createCursorLoader(InvalidEnum.INVALID.getId(), mMockUri, null);

        // Then the returned cursor is null
        assertThat(cursorLoader, nullValue());
    }

    private void initWithStubbedCursor(Cursor cursor) {
        // Return a fake title
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_TITLE))
                .thenReturn(SESSION_TITLE_COLUMN_INDEX);
        when(cursor.getString(SESSION_TITLE_COLUMN_INDEX))
                .thenReturn(FAKE_TITLE);
    }
}
