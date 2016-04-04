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

package com.google.samples.apps.iosched.session;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.model.TagMetadataTest;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.testutils.SettingsMockContext;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SettingsUtils.class, TimeUtils.class})
@SmallTest
public class SessionDetailModelTest {

    private static final String FAKE_ID = "FAKE ID";

    private static final String FAKE_TITLE = "FAKE TITLE";

    private static final String FAKE_ROOM_ID = "FAKE ROOM ID";

    private static final String FAKE_HASHTAG = "FAKE HASHTAG";

    private static final String FAKE_SPEAKER_NAME = "FAKE SPEAKER NAME";

    private static final String FAKE_SPEAKER_IMAGE_URL = "FAKE SPEAKER IMAGE URL";

    private static final String FAKE_SPEAKER_COMPANY = "FAKE SPEAKER COMPANY";

    private static final String FAKE_SPEAKER_URL = "FAKE SPEAKER URL";

    private static final String FAKE_SPEAKER_ABSTRACT = "FAKE SPEAKER ABSTRACT";

    private static final int FAKE_COLOR = 200;

    private static final long FAKE_CURRENT_TIME_OFFSET = 0l;

    private static final int SESSION_TITLE_COLUMN_INDEX = 1;

    private static final int SESSION_COLOR_COLUMN_INDEX = 2;

    private static final int SESSION_START_COLUMN_INDEX = 3;

    private static final int SESSION_END_COLUMN_INDEX = 4;

    private static final int SESSION_IN_MY_SCHEDULE_COLUMN_INDEX = 5;

    private static final int SESSION_ROOM_ID_COLUMN_INDEX = 6;

    private static final int SESSION_HASHTAG_COLUMN_INDEX = 7;

    private static final int SPEAKER_NAME_COLUMN_INDEX = 8;

    private static final int SPEAKER_IMAGE_URL_COLUMN_INDEX = 9;

    private static final int SPEAKER_COMPANY_COLUMN_INDEX = 10;

    private static final int SPEAKER_URL_COLUMN_INDEX = 11;

    private static final int SPEAKER_PLUSONE_URL_COLUMN_INDEX = 12;

    private static final int SPEAKER_TWITTER_URL_COLUMN_INDEX = 13;

    private static final int SPEAKER_ABSTRACT_COLUMN_INDEX = 14;

    private static final long ONE_MINUTE = 1 * 60 * 1000l;

    private static final long ONE_HOUR = 1 * 60 * 60 * 1000l;

    @Mock
    private Context mMockContext;

    @Mock
    private SharedPreferences mMockSharedPreferences;

    @Mock
    private Uri mMockUri;

    @Mock
    private SessionsHelper mMockSessionsHelper;

    @Mock
    private Cursor mMockCursor;

    @Mock
    private Cursor mMockEmptyCursor;

    @Mock
    private CursorLoader mMockCursorLoader;

    @Mock
    private LoaderManager mMockLoaderManager;

    @Mock
    private Model.UserActionCallback mMockUserActionCallback;

    private SessionDetailModel mSessionDetailModel;

    @Before
    public void setUp() {
        // Init mocks
        initMockCursors();
        initMockContextWithFakeCurrentTime();

        LogUtils.LOGGING_ENABLED = false;

        // Create an instance of the model.
        mSessionDetailModel = new SessionDetailModel(mMockUri, mMockContext,
                mMockSessionsHelper, mMockLoaderManager);
    }

    @Test
    public void readDataFromCursor_SessionQuery_SessionLoaded() {
        // Given a mock cursor with a fake session title
        initMockCursorWithTitle(mMockCursor);
        SessionDetailModel spyModel = setSpyModelForSessionLoading();

        // When ran with session query
        boolean success = spyModel.readDataFromCursor(
                mMockCursor, SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

        // Then the model has correct session title and true is returned
        assertThat(spyModel.getSessionTitle(), is(FAKE_TITLE));
        assertThat(success, is(true));
    }

    @Test
    public void readDataFromCursor_TagMetadataQuery_TagMetadataLoaded() {
        // Given a mock cursor with a fake tag
        initMockCursorWithOneTag(mMockCursor);

        // When ran with tag query
        boolean success = mSessionDetailModel.readDataFromCursor(
                mMockCursor, SessionDetailModel.SessionDetailQueryEnum.TAG_METADATA);

        // Then the tag metadata object has been created, and true is returned
        assertThat(mSessionDetailModel.getTagMetadata(), not(nullValue()));
        assertThat(success, is(true));
    }

    @Test
    public void readDataFromCursor_TagMetadataQueryWithNullCursor_ReturnsFalse() {
        // When ran with tag query and null cursor
        boolean success = mSessionDetailModel.readDataFromCursor(
                null, SessionDetailModel.SessionDetailQueryEnum.TAG_METADATA);

        // Then false is returned
        assertThat(success, is(false));
    }

    @Test
    public void readDataFromCursor_SpeakersQueryWithOneSpeaker_SpeakerLoaded() {
        // Given a mock cursor with a fake speaker
        initMockCursorWithOneSpeaker(mMockCursor);

        // When ran with speakers query
        boolean success = mSessionDetailModel.readDataFromCursor(
                mMockCursor, SessionDetailModel.SessionDetailQueryEnum.SPEAKERS);

        // Then the model has correct speakers size, and data, and true is returned
        assertThat(mSessionDetailModel.getSpeakers().size(), is(1));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getName(), is(FAKE_SPEAKER_NAME));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getImageUrl(),
                is(FAKE_SPEAKER_IMAGE_URL));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getCompany(), is(FAKE_SPEAKER_COMPANY));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getUrl(), is(FAKE_SPEAKER_URL));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getAbstract(),
                is(FAKE_SPEAKER_ABSTRACT));
        assertThat(success, is(true));
    }

    @Test
    public void readDataFromCursor_SpeakersQueryWithTwoSpeakers_SpeakersLoaded() {
        // Given a mock cursor with 2 fake speakers
        initMockCursorWithTwoSpeakers(mMockCursor);

        // When ran with speakers query
        boolean success = mSessionDetailModel.readDataFromCursor(
                mMockCursor, SessionDetailModel.SessionDetailQueryEnum.SPEAKERS);

        // Then the model has correct speakers size, and data, and true is returned
        assertThat(mSessionDetailModel.getSpeakers().size(), is(2));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getName(), is(FAKE_SPEAKER_NAME));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getImageUrl(),
                is(FAKE_SPEAKER_IMAGE_URL));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getCompany(), is(FAKE_SPEAKER_COMPANY));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getUrl(), is(FAKE_SPEAKER_URL));
        assertThat(mSessionDetailModel.getSpeakers().get(0).getAbstract(),
                is(FAKE_SPEAKER_ABSTRACT));
        assertThat(mSessionDetailModel.getSpeakers().get(1).getName(), is(FAKE_SPEAKER_NAME));
        assertThat(mSessionDetailModel.getSpeakers().get(1).getImageUrl(),
                is(FAKE_SPEAKER_IMAGE_URL));
        assertThat(mSessionDetailModel.getSpeakers().get(1).getCompany(), is(FAKE_SPEAKER_COMPANY));
        assertThat(mSessionDetailModel.getSpeakers().get(1).getUrl(), is(FAKE_SPEAKER_URL));
        assertThat(mSessionDetailModel.getSpeakers().get(1).getAbstract(),
                is(FAKE_SPEAKER_ABSTRACT));
        assertThat(success, is(true));
    }

    @Test
    public void readDataFromCursor_FeedbackQueryWithFeedbackAvailable_SessionHasFeedback() {
        // Given a mock cursor with data
        when(mMockCursor.getCount()).thenReturn(1);

        // When ran with feedback query
        boolean success = mSessionDetailModel.readDataFromCursor(
                mMockCursor, SessionDetailModel.SessionDetailQueryEnum.FEEDBACK);

        // Then the session has feedback and true is returned
        assertThat(mSessionDetailModel.hasFeedback(), is(true));
        assertThat(success, is(true));
    }

    @Test
    public void readDataFromCursor_FeedbackQueryWithNoFeedbackAvailable_SessionHasNoFeedback() {
        // Given a mock cursor with no data
        when(mMockCursor.getCount()).thenReturn(0);

        // When ran with feedback query
        boolean success = mSessionDetailModel.readDataFromCursor(
                mMockCursor, SessionDetailModel.SessionDetailQueryEnum.FEEDBACK);

        // Then the session has no feedback and true is returned
        assertThat(mSessionDetailModel.hasFeedback(), is(false));
        assertThat(success, is(true));
    }

    @Test
    public void readDataFromCursor_ValidQueryWithNullCursor_ReturnsFalse() {
        // When ran with session query and null cursor
        boolean success = mSessionDetailModel.readDataFromCursor(
                null, SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

        // Then false is returned
        assertThat(success, is(false));
    }

    @Test
    public void readDataFromCursor_ValidQueryWithEmptyCursor_ReturnsFalse() {
        // When ran with session query and empty cursor
        boolean success = mSessionDetailModel.readDataFromCursor(
                mMockEmptyCursor, SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

        // Then false is returned
        assertThat(success, is(false));
    }

    @Test
    public void readDataFromCursor_InvalidQuery_ReturnsFalse() {
        // When ran with invalid query and non empty cursor
        boolean success = mSessionDetailModel.readDataFromCursor(
                mMockCursor, null);

        // Then false is returned
        assertThat(success, is(false));
    }


    @Test
    public void createCursorLoader_SessionQuery_ReturnsCursorLoader() {
        // Given a mock uri and mock cursor loader
        SessionDetailModel spyModel = spy(
                new SessionDetailModel(mMockUri, mMockContext, mMockSessionsHelper,
                        mMockLoaderManager));
        doReturn(FAKE_ID).when(spyModel).getSessionId(mMockUri);
        doReturn(mMockCursorLoader).when(spyModel).getCursorLoaderInstance(
                any(Context.class), any(Uri.class), any(String[].class), any(String.class),
                any(String[].class), any(String.class));

        // When ran with mock uri and session query loader id
        CursorLoader createdCursorLoader =
                (CursorLoader) spyModel.createCursorLoader(
                        SessionDetailModel.SessionDetailQueryEnum.SESSIONS, null);

        // Then the returned cursor loader is the mock cursor loader
        assertThat(createdCursorLoader, sameInstance(mMockCursorLoader));
    }

    @Test
    public void createCursorLoader_TagMetadaQuery_ReturnsCursorLoader() {
        // Given a mock uri and mock cursor loader
        SessionDetailModel spyModel = spy(
                new SessionDetailModel(mMockUri, mMockContext, mMockSessionsHelper,
                        mMockLoaderManager));
        doReturn(mMockCursorLoader).when(spyModel).getTagMetadataLoader();

        // When ran with mock uri and tag metadata query loader id
        CursorLoader createdCursorLoader =
                (CursorLoader) spyModel.createCursorLoader(
                        SessionDetailModel.SessionDetailQueryEnum.TAG_METADATA, null);

        // Then the returned cursor loader is the mock cursor loader
        assertThat(createdCursorLoader, sameInstance(mMockCursorLoader));
    }

    @Test
    public void createCursorLoader_SpeakersQuery_ReturnsCursor() {
        // Given a mock uri and mock cursor loader
        SessionDetailModel spyModel = spy(
                new SessionDetailModel(mMockUri, mMockContext, mMockSessionsHelper,
                        mMockLoaderManager));
        doReturn(mMockUri).when(spyModel).getSpeakersDirUri(any(String.class));
        doReturn(mMockCursorLoader).when(spyModel).getCursorLoaderInstance(
                any(Context.class), any(Uri.class), any(String[].class), any(String.class),
                any(String[].class), any(String.class));

        // When ran with mock uri and speakers query loader id
        CursorLoader createdCursorLoader =
                (CursorLoader) spyModel.createCursorLoader(
                        SessionDetailModel.SessionDetailQueryEnum.SPEAKERS, null);

        // Then the returned cursor loader is the mock cursor loader
        assertThat(createdCursorLoader, sameInstance(mMockCursorLoader));
    }

    @Test
    public void createCursorLoader_FeedbackQuery_ReturnsCursor() {
        // Given a mock uri and mock cursor loader
        SessionDetailModel spyModel = spy(
                new SessionDetailModel(mMockUri, mMockContext, mMockSessionsHelper,
                        mMockLoaderManager));
        doReturn(mMockUri).when(spyModel).getFeedbackUri(any(String.class));
        doReturn(mMockCursorLoader).when(spyModel).getCursorLoaderInstance(
                any(Context.class), any(Uri.class), any(String[].class), any(String.class),
                any(String[].class), any(String.class));

        // When ran with mock uri and feedback query loader id
        CursorLoader createdCursorLoader =
                (CursorLoader) spyModel.createCursorLoader(
                        SessionDetailModel.SessionDetailQueryEnum.FEEDBACK, null);

        // Then the returned cursor loader is the mock cursor loader
        assertThat(createdCursorLoader, sameInstance(mMockCursorLoader));
    }

    @Test
    public void createCursorLoader_NullQuery_ReturnsNullCursor() {
        // When ran with mock uri and null query loader id
        CursorLoader createdCursorLoader =
                (CursorLoader) mSessionDetailModel.createCursorLoader(null, null);

        // Then the returned cursor loader is null
        assertThat(createdCursorLoader, nullValue());
    }

    @Test
    public void deliverUserAction_StarSession_Success() {
        // Given a loaded session not in user schedule
        initMockCursorWithSessionNotInSchedule(mMockCursor);
        SessionDetailModel spyModel = setSpyModelForSessionLoading();
        spyModel.readDataFromCursor(mMockCursor,
                SessionDetailModel.SessionDetailQueryEnum.SESSIONS);
        doNothing().when(spyModel).sendAnalyticsEvent(
                any(String.class), any(String.class), any(String.class));

        // When ran with star user action
        spyModel.deliverUserAction(SessionDetailModel.SessionDetailUserActionEnum.STAR, null,
                mMockUserActionCallback);

        // Then verify the callback was called successfully
        verify(mMockUserActionCallback).onModelUpdated(spyModel,
                SessionDetailModel.SessionDetailUserActionEnum.STAR);
        // And the session is in user schedule and set session starred is called with true
        verify(mMockSessionsHelper)
                .setSessionStarred(eq(mMockUri), eq(true), anyString());
        assertThat(spyModel.isInSchedule(), is(true));
    }

    @Test
    public void deliverUserAction_UnstarSession_Success() {
        // Given a loaded session in user schedule
        initMockCursorWithSessionInSchedule(mMockCursor);
        SessionDetailModel spyModel = setSpyModelForSessionLoading();
        spyModel.readDataFromCursor(mMockCursor,
                SessionDetailModel.SessionDetailQueryEnum.SESSIONS);
        doNothing().when(spyModel).sendAnalyticsEvent(
                any(String.class), any(String.class), any(String.class));

        // When ran with unstar user action
        spyModel.deliverUserAction(SessionDetailModel.SessionDetailUserActionEnum.UNSTAR, null,
                mMockUserActionCallback);

        // Then verify the callback was called successfully
        verify(mMockUserActionCallback).onModelUpdated(spyModel,
                SessionDetailModel.SessionDetailUserActionEnum.UNSTAR);
        // And the session is not in user schedule and set session starred is called with false
        verify(mMockSessionsHelper)
                .setSessionStarred(eq(mMockUri), eq(false), anyString());
        assertThat(spyModel.isInSchedule(), is(false));
    }

    @Test
    public void deliverUserAction_ShowMap_Success() {
        // Given a loaded session with a fake room id
        initMockCursorWithRoomId(mMockCursor);
        SessionDetailModel spyModel = setSpyModelForSessionLoading();
        spyModel.readDataFromCursor(mMockCursor,
                SessionDetailModel.SessionDetailQueryEnum.SESSIONS);
        doNothing().when(spyModel).sendAnalyticsEvent(
                any(String.class), any(String.class), any(String.class));

        // When ran with show map user action
        spyModel.deliverUserAction(SessionDetailModel.SessionDetailUserActionEnum.SHOW_MAP, null,
                mMockUserActionCallback);

        // Then verify the callback was called successfully
        verify(mMockUserActionCallback).onModelUpdated(spyModel,
                SessionDetailModel.SessionDetailUserActionEnum.SHOW_MAP);
    }

    @Test
    public void deliverUserAction_ShowShare_Success() {
        // Given a loaded session with a fake title
        initMockCursorWithTitle(mMockCursor);
        SessionDetailModel spyModel = setSpyModelForSessionLoading();
        spyModel.readDataFromCursor(mMockCursor,
                SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

        // When ran with show share user action
        spyModel.deliverUserAction(SessionDetailModel.SessionDetailUserActionEnum.SHOW_SHARE, null,
                mMockUserActionCallback);

        // Then verify the callback was called successfully
        verify(mMockUserActionCallback).onModelUpdated(spyModel,
                SessionDetailModel.SessionDetailUserActionEnum.SHOW_SHARE);
    }

    @Test
    public void isSessionOngoing_OngoingSession_ReturnsTrue() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in progress
            initMockCursorWithOngoingSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is ongoing
            assertThat(spyModel.isSessionOngoing(), is(true));
        }
    }

    @Test
    public void isSessionOngoing_SessionNotStarted_ReturnsFalse() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in future
            initMockCursorWithNotStartedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is not ongoing
            assertThat(spyModel.isSessionOngoing(), is(false));
        }
    }

    @Test
    public void isSessionOngoing_SessionEnded_ReturnsFalse() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session that has ended
            initMockCursorWithEndedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is not ongoing
            assertThat(spyModel.isSessionOngoing(), is(false));
        }
    }

    @Test
    public void hasSessionStarted_OnGoingSession_ReturnsTrue() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in progress
            initMockCursorWithOngoingSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session has started
            assertThat(spyModel.hasSessionStarted(), is(true));
        }
    }

    @Test
    public void hasSessionStarted_SessionNotStarted_ReturnsFalse() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in future
            initMockCursorWithNotStartedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session has not started
            assertThat(spyModel.hasSessionStarted(), is(false));
        }
    }

    @Test
    public void hasSessionStarted_SessionEnded_ReturnsTrue() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session that has ended
            initMockCursorWithEndedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session has started
            assertThat(spyModel.hasSessionStarted(), is(true));
        }
    }

    @Test
    public void hasSessionEnded_SessionNotStarted_ReturnsFalse() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in progress
            initMockCursorWithNotStartedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session has not ended
            assertThat(spyModel.hasSessionEnded(), is(false));
        }
    }

    @Test
    public void hasSessionEnded_OngoingSession_ReturnsFalse() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in progress
            initMockCursorWithOngoingSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session has not ended
            assertThat(spyModel.hasSessionEnded(), is(false));
        }
    }

    @Test
    public void hasSessionEnded_SessionEnded_ReturnsTrue() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session that has ended
            initMockCursorWithEndedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session has ended
            assertThat(spyModel.hasSessionEnded(), is(true));
        }
    }

    @Test
    public void minutesSinceSessionStarted_SessionNotStarted_ReturnsCorrectMinutes() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session not started
            initMockCursorWithNotStartedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is ongoing
            assertThat(spyModel.minutesSinceSessionStarted(), is(0l));
        }
    }

    @Test
    public void minutesSinceSessionStarted_SessionStarted_ReturnsCorrectMinutes() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in progress that started 1 hour ago
            initMockCursorWithOngoingSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is ongoing
            assertThat(spyModel.minutesSinceSessionStarted(), is(60l));
        }
    }

    @Test
    public void minutesUntilSessionStarts_SessionNotStarted_ReturnsCorrectMinutes() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session starting in 1 hour
            initMockCursorWithNotStartedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is ongoing
            assertThat(spyModel.minutesUntilSessionStarts(), is(60l));
        }
    }

    @Test
    public void minutesUntilSessionStarts_SessionStarted_ReturnsCorrectMinutes() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in progress
            initMockCursorWithOngoingSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is ongoing
            assertThat(spyModel.minutesUntilSessionStarts(), is(0l));
        }
    }

    @Test
    public void isSessionReadyForFeedback_SessionNotStarted_ReturnsFalse() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session not started
            initMockCursorWithNotStartedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is ongoing
            assertThat(spyModel.isSessionReadyForFeedback(), is(false));
        }
    }

    @Test
    public void isSessionReadyForFeedback_OnGoingSessionJustStarted_ReturnsFalse() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in progress that started 1 minute ago and lasts
            // 2 hours
            initMockCursorWithJustStartedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is ongoing
            assertThat(spyModel.isSessionReadyForFeedback(), is(false));
        }
    }

    @Test
    public void isSessionReadyForFeedback_OnGoingSessionNearEnd_ReturnsTrue() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session in progress that will end within feedback
            // allowed time
            initMockCursorWithSessionEndingSoon(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is ongoing
            assertThat(spyModel.isSessionReadyForFeedback(), is(true));
        }
    }

    @Test
    public void isSessionReadyForFeedback_SessionEnded_ReturnsTrue() {
        // Only possible to mock current time in debug build
        if (BuildConfig.DEBUG) {
            // Given a mock cursor for a session that has ended
            initMockCursorWithEndedSession(mMockCursor);
            SessionDetailModel spyModel = setSpyModelForSessionLoading();

            // When session is loaded
            spyModel.readDataFromCursor(mMockCursor,
                    SessionDetailModel.SessionDetailQueryEnum.SESSIONS);

            // Then session is ongoing
            assertThat(spyModel.isSessionReadyForFeedback(), is(true));
        }
    }

    private SessionDetailModel setSpyModelForSessionLoading() {
        SessionDetailModel spyModel = spy(
                new SessionDetailModel(mMockUri, mMockContext, mMockSessionsHelper,
                        mMockLoaderManager));
        doNothing().when(spyModel).formatSubtitle();
        return spyModel;
    }

    private void initMockCursors() {
        // Set non empty cursor.
        when(mMockCursor.moveToFirst()).thenReturn(true);

        // Set session color to avoid call to mock context resources.
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_COLOR))
                .thenReturn(SESSION_COLOR_COLUMN_INDEX);
        when(mMockCursor.getInt(SESSION_COLOR_COLUMN_INDEX)).thenReturn(FAKE_COLOR);

        // Set empty cursor.
        when(mMockEmptyCursor.moveToFirst()).thenReturn(false);
    }

    private void initMockContextWithFakeCurrentTime() {
        SettingsMockContext.initMockContextForCurrentTime(FAKE_CURRENT_TIME_OFFSET, mMockContext);
    }

    private void initMockCursorWithTitle(Cursor cursor) {
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_TITLE))
                .thenReturn(SESSION_TITLE_COLUMN_INDEX);
        when(cursor.getString(SESSION_TITLE_COLUMN_INDEX)).thenReturn(FAKE_TITLE);
    }

    private void initMockCursorWithSessionNotInSchedule(Cursor cursor) {
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE))
                .thenReturn(SESSION_IN_MY_SCHEDULE_COLUMN_INDEX);
        when(cursor.getInt(SESSION_IN_MY_SCHEDULE_COLUMN_INDEX)).thenReturn(0);
    }

    private void initMockCursorWithSessionInSchedule(Cursor cursor) {
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE))
                .thenReturn(SESSION_IN_MY_SCHEDULE_COLUMN_INDEX);
        when(cursor.getInt(SESSION_IN_MY_SCHEDULE_COLUMN_INDEX)).thenReturn(1);
    }

    private void initMockCursorWithRoomId(Cursor cursor) {
        when(cursor.getColumnIndex(ScheduleContract.Sessions.ROOM_ID))
                .thenReturn(SESSION_ROOM_ID_COLUMN_INDEX);
        when(cursor.getString(SESSION_ROOM_ID_COLUMN_INDEX)).thenReturn(FAKE_ROOM_ID);
    }

    private void initMockCursorWithHashTag(Cursor cursor) {
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_HASHTAG))
                .thenReturn(SESSION_HASHTAG_COLUMN_INDEX);
        when(cursor.getString(SESSION_HASHTAG_COLUMN_INDEX)).thenReturn(FAKE_HASHTAG);
    }

    private void initMockCursorWithNotStartedSession(Cursor cursor) {
        // Return a fake start time in future
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_START))
                .thenReturn(SESSION_START_COLUMN_INDEX);
        when(cursor.getLong(SESSION_START_COLUMN_INDEX))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET + ONE_HOUR);

        // Return a fake end time in future
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_END))
                .thenReturn(SESSION_END_COLUMN_INDEX);
        when(cursor.getLong(SESSION_END_COLUMN_INDEX))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET + 2 * ONE_HOUR);
    }

    private void initMockCursorWithJustStartedSession(Cursor cursor) {
        // Return a fake start time 1 minute ago
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_START))
                .thenReturn(SESSION_START_COLUMN_INDEX);
        when(cursor.getLong(SESSION_START_COLUMN_INDEX))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET - ONE_MINUTE);

        // Return a fake end time in future
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_END))
                .thenReturn(SESSION_END_COLUMN_INDEX);
        when(cursor.getLong(SESSION_END_COLUMN_INDEX))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET + 2 * ONE_HOUR);
    }

    private void initMockCursorWithSessionEndingSoon(Cursor cursor) {
        // Return a fake start time in past
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_START))
                .thenReturn(SESSION_START_COLUMN_INDEX);
        when(cursor.getLong(SESSION_START_COLUMN_INDEX))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET - ONE_HOUR);

        // Return a fake end time within feedback before session allowed time
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_END))
                .thenReturn(SESSION_END_COLUMN_INDEX);
        when(cursor.getLong(SESSION_END_COLUMN_INDEX)).thenReturn(FAKE_CURRENT_TIME_OFFSET
                + SessionDetailConstants.FEEDBACK_MILLIS_BEFORE_SESSION_END_MS - ONE_MINUTE);
    }

    private void initMockCursorWithOngoingSession(Cursor cursor) {
        // Return a fake start time in past
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_START))
                .thenReturn(SESSION_START_COLUMN_INDEX);
        when(cursor.getLong(SESSION_START_COLUMN_INDEX))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET - ONE_HOUR);

        // Return a fake end time in future
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_END))
                .thenReturn(SESSION_END_COLUMN_INDEX);
        when(cursor.getLong(SESSION_END_COLUMN_INDEX))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET + ONE_HOUR);
    }

    private void initMockCursorWithEndedSession(Cursor cursor) {
        // Return a fake start time in past
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_START))
                .thenReturn(SESSION_START_COLUMN_INDEX);
        when(cursor.getLong(SESSION_START_COLUMN_INDEX))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET - 2 * ONE_HOUR);

        // Return a fake end time in past
        when(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_END))
                .thenReturn(SESSION_END_COLUMN_INDEX);
        when(cursor.getLong(SESSION_END_COLUMN_INDEX))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET - ONE_HOUR);
    }

    private void initMockCursorWithOneSpeaker(Cursor cursor) {
        // Return a count of 1
        when(cursor.getCount()).thenReturn(1);
        when(cursor.moveToPosition(0)).thenReturn(true);

        // Return fake speaker details
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_NAME))
                .thenReturn(SPEAKER_NAME_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_NAME_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_NAME);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_IMAGE_URL))
                .thenReturn(SPEAKER_IMAGE_URL_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_IMAGE_URL_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_IMAGE_URL);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_COMPANY))
                .thenReturn(SPEAKER_COMPANY_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_COMPANY_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_COMPANY);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_URL))
                .thenReturn(SPEAKER_URL_COLUMN_INDEX);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_PLUSONE_URL))
                .thenReturn(SPEAKER_PLUSONE_URL_COLUMN_INDEX);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_TWITTER_URL))
                .thenReturn(SPEAKER_TWITTER_URL_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_URL_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_URL);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_ABSTRACT))
                .thenReturn(SPEAKER_ABSTRACT_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_ABSTRACT_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_ABSTRACT);
    }

    private void initMockCursorWithTwoSpeakers(Cursor cursor) {
        // Return a count of 2
        when(cursor.getCount()).thenReturn(2);
        when(cursor.moveToPosition(0)).thenReturn(true);
        when(cursor.moveToPosition(1)).thenReturn(true);

        // Return fake speaker details
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_NAME))
                .thenReturn(SPEAKER_NAME_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_NAME_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_NAME);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_IMAGE_URL))
                .thenReturn(SPEAKER_IMAGE_URL_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_IMAGE_URL_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_IMAGE_URL);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_COMPANY))
                .thenReturn(SPEAKER_COMPANY_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_COMPANY_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_COMPANY);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_URL))
                .thenReturn(SPEAKER_URL_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_URL_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_URL);
        when(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_ABSTRACT))
                .thenReturn(SPEAKER_ABSTRACT_COLUMN_INDEX);
        when(cursor.getString(SPEAKER_ABSTRACT_COLUMN_INDEX)).thenReturn(FAKE_SPEAKER_ABSTRACT);
    }

    private void initMockCursorWithOneTag(Cursor cursor) {
        TagMetadataTest.initMockCursorWithOneTag(cursor);
    }
}
