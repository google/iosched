/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.explore;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.data.ItemGroup;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.testutils.SettingsMockContext;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Iterator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SettingsUtils.class, TimeUtils.class})
@SmallTest
public class ExploreIOModelTest {

    private static final int SESSION_THEME_LIMIT = 2;

    private static final String FAKE_TAG_ID_1 = "ID1";

    private static final String FAKE_TAG_ID_2 = "ID2";

    private static final String FAKE_TAG_NAME_1 = "NAME1";

    private static final String FAKE_TAG_NAME_2 = "NAME2";

    /**
     * Fake tags data to be used for the mocked cursor.
     */
    private static final Object[][] FAKE_TAGS_CURSOR_DATA = {
            new String[]{FAKE_TAG_ID_1, FAKE_TAG_ID_2},
            new String[]{FAKE_TAG_NAME_1, FAKE_TAG_NAME_2}};

    /**
     * Indexes of the "column" for each video attribute in the {@link #FAKE_TAGS_CURSOR_DATA}.
     */
    private static final int ID_COLUMN_INDEX = 0;
    private static final int NAME_COLUMN_INDEX = 1;

    private static final String FAKE_SESSION_TITLE_1 = "TITLE1";

    private static final String FAKE_SESSION_TITLE_2_KEYNOTE = "Keynote";

    private static final String FAKE_SESSION_TITLE_3 = "TITLE3";

    private static final String FAKE_SESSION_TITLE_4 = "TITLE4";

    private static final String FAKE_SESSION_ABSTRACT = "ABSTRACT";

    private static final String FAKE_SESSION_ID_1 = "ID1";

    private static final String FAKE_SESSION_ID_2_KEYNOTE = "ID2";

    private static final String FAKE_SESSION_ID_3 = "ID3";

    private static final String FAKE_SESSION_ID_4 = "ID4";

    private static final String FAKE_SESSION_URL = "URL";

    private static final String FAKE_SESSION_MAIN_TAG1 = FAKE_TAG_NAME_1;

    private static final String FAKE_SESSION_MAIN_TAG2_KEYNOTE = Config.Tags.SPECIAL_KEYNOTE;

    private static final String FAKE_SESSION_MAIN_TAG3 = FAKE_TAG_NAME_2;

    private static final String FAKE_SESSION_MAIN_TAG4 = "";

    private static final long FAKE_SESSION_START1 =
            Config.CONFERENCE_START_MILLIS;

    private static final long FAKE_SESSION_START2 =
            Config.CONFERENCE_START_MILLIS + 2 * TimeUtils.HOUR;

    private static final long FAKE_SESSION_START3 =
            Config.CONFERENCE_START_MILLIS + 3 * TimeUtils.HOUR;

    private static final long FAKE_SESSION_START4 =
            Config.CONFERENCE_START_MILLIS + 4 * TimeUtils.HOUR;

    private static final long FAKE_SESSION_END1 = FAKE_SESSION_START1 + 1l * TimeUtils.HOUR;

    private static final long FAKE_SESSION_END2 = FAKE_SESSION_START2 + 1l * TimeUtils.HOUR;

    private static final long FAKE_SESSION_END3 = FAKE_SESSION_START3 + 1l * TimeUtils.HOUR;

    private static final long FAKE_SESSION_END4 = FAKE_SESSION_START4 + 1l * TimeUtils.HOUR;

    private static final String FAKE_SESSION_LIVESTREAM = "fejsopfe56";

    private static final String FAKE_SESSION_TAGS_1 = "THEME_TAG1";

    private static final String FAKE_SESSION_TAGS_2 = "THEME_TAG1";

    private static final String FAKE_SESSION_TAGS_3 = "THEME_TAG3";

    private static final String FAKE_SESSION_TAGS_4 = "TOPIC_TAG4";
    /**
     * Fake sessions data to be used for the mocked cursor.
     */
    private static final Object[][] FAKE_SESSIONS_CURSOR_DATA = {
            new String[]{FAKE_SESSION_TITLE_1, FAKE_SESSION_TITLE_2_KEYNOTE, FAKE_SESSION_TITLE_3,
                    FAKE_SESSION_TITLE_4},
            new String[]{FAKE_SESSION_ABSTRACT, FAKE_SESSION_ABSTRACT, FAKE_SESSION_ABSTRACT,
                    FAKE_SESSION_ABSTRACT},
            new String[]{FAKE_SESSION_ID_1, FAKE_SESSION_ID_2_KEYNOTE, FAKE_SESSION_ID_3,
                    FAKE_SESSION_ID_4},
            new String[]{FAKE_SESSION_URL, FAKE_SESSION_URL, FAKE_SESSION_URL, FAKE_SESSION_URL},
            new String[]{FAKE_SESSION_MAIN_TAG1, FAKE_SESSION_MAIN_TAG2_KEYNOTE,
                    FAKE_SESSION_MAIN_TAG3, FAKE_SESSION_MAIN_TAG4},
            new Long[]{FAKE_SESSION_START1, FAKE_SESSION_START2, FAKE_SESSION_START3,
                    FAKE_SESSION_START4},
            new Long[]{FAKE_SESSION_END1, FAKE_SESSION_END2, FAKE_SESSION_END3, FAKE_SESSION_END4},
            new String[]{FAKE_SESSION_LIVESTREAM, null, null, null},
            new String[]{FAKE_SESSION_URL, FAKE_SESSION_URL, FAKE_SESSION_URL, FAKE_SESSION_URL},
            new String[]{FAKE_SESSION_TAGS_1, FAKE_SESSION_TAGS_2, FAKE_SESSION_TAGS_3,
                    FAKE_SESSION_TAGS_4},
            new Long[]{1l, 1l, 0l, 1l}
    };

    /**
     * Indexes of the "column" for each video attribute in the {@link #FAKE_SESSIONS_CURSOR_DATA}.
     */
    private static final int TITLE_COLUMN_INDEX = 0;
    private static final int ABSTRACT_COLUMN_INDEX = 1;
    private static final int SESSION_ID_COLUMN_INDEX = 2;
    private static final int PHOTO_URL_COLUMN_INDEX = 3;
    private static final int MAIN_TAG_COLUMN_INDEX = 4;
    private static final int START_COLUMN_INDEX = 5;
    private static final int END_COLUMN_INDEX = 6;
    private static final int LIVESTREAM_ID_COLUMN_INDEX = 7;
    private static final int YOUTUBE_URL_COLUMN_INDEX = 8;
    private static final int TAGS_COLUMN_INDEX = 9;
    private static final int MY_SCHEDULE_COLUMN_INDEX = 10;

    /**
     * Cursor position in the {@link #FAKE_TAGS_CURSOR_DATA} or {@link #FAKE_SESSIONS_CURSOR_DATA}
     * array.
     */
    private int mCursorIndex = -1;

    @Captor
    private ArgumentCaptor<Integer> mGetCursorValueCaptor;

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
    private Resources mMockResources;

    private ExploreIOModel mExploreIOModel;

    @Before
    public void setUp() {
        // Disable logging
        LogUtils.LOGGING_ENABLED = false;

        // Init mocks
        SettingsMockContext
                .initMockContextForAttendingVenueSetting(true, mMockContext);
        SettingsMockContext
                .initMockContextForCurrentTime(Config.CONFERENCE_START_MILLIS + TimeUtils.MINUTE,
                        mMockContext);
        initMockThemesLimit(mMockContext, mMockResources);

        // Create an instance of the model.
        mExploreIOModel = new ExploreIOModel(mMockContext, mMockUri, mMockLoaderManager);
    }

    @Test
    public void readDataFromCursor_SessionsQuery_SessionsLoaded() {
        // Given a mock cursor with fake sessions data
        initMockCursorWithSessionsData(mMockCursor);

        // When ran with session query
        boolean success = mExploreIOModel.readDataFromCursor(
                mMockCursor, ExploreIOModel.ExploreIOQueryEnum.SESSIONS);

        // Then the model has correct session data
        assertThat(success, is(true));
        assertThat(mExploreIOModel.getKeynoteData().getSessionName(),
                is(FAKE_SESSION_TITLE_2_KEYNOTE));
        assertThat(mExploreIOModel.getLiveStreamData().getSessions().get(0).getSessionName(),
                is(FAKE_SESSION_TITLE_1));
        assertThat(mExploreIOModel.getThemes().size(), is(2));
        assertThat(mExploreIOModel.getTracks().size(), is(1));
        Iterator<ItemGroup> themeGroupIterator = mExploreIOModel.getThemes().iterator();
        ItemGroup themeGroup1 = themeGroupIterator.next();
        ItemGroup themeGroup2 = themeGroupIterator.next();
        if (themeGroup1.getTitle().equals(FAKE_SESSION_TAGS_1)) {
            assertThat(themeGroup1.getSessions().size(), is(2));
            assertThat(themeGroup1.getSessions().get(0).getSessionName(), is(FAKE_SESSION_TITLE_1));
            assertThat(themeGroup1.getSessions().get(1).getSessionName(),
                    is(FAKE_SESSION_TITLE_2_KEYNOTE));
            assertThat(themeGroup2.getSessions().size(), is(1));
            assertThat(themeGroup2.getSessions().get(0).getSessionName(), is(FAKE_SESSION_TITLE_3));
        } else {
            assertThat(themeGroup1.getSessions().size(), is(1));
            assertThat(themeGroup1.getSessions().get(0).getSessionName(), is(FAKE_SESSION_TITLE_3));
            assertThat(themeGroup2.getSessions().size(), is(2));
            assertThat(themeGroup2.getSessions().get(0).getSessionName(), is(FAKE_SESSION_TITLE_1));
            assertThat(themeGroup2.getSessions().get(1).getSessionName(),
                    is(FAKE_SESSION_TITLE_2_KEYNOTE));
        }
        ItemGroup topicGroup = mExploreIOModel.getTracks().iterator().next();
        assertThat(topicGroup.getSessions().size(), is(1));
        assertThat(topicGroup.getSessions().get(0).getSessionName(), is(FAKE_SESSION_TITLE_4));
    }

    @Test
    public void readDataFromCursor_TagsQuery_TagsLoaded() {
        // Given a mock cursor with fake tags data
        initMockCursorWithTagsData(mMockCursor);

        // When ran with tags query
        boolean success = mExploreIOModel.readDataFromCursor(
                mMockCursor, ExploreIOModel.ExploreIOQueryEnum.TAGS);

        // Then the model has correct tags data
        assertThat(success, is(true));
        assertThat(mExploreIOModel.getTagTitles().size(), is(2));
        assertThat(mExploreIOModel.getTagTitles().get(FAKE_TAG_ID_1), is(FAKE_TAG_NAME_1));
        assertThat(mExploreIOModel.getTagTitles().get(FAKE_TAG_ID_2), is(FAKE_TAG_NAME_2));
    }

    private void initMockCursorWithSessionsData(Cursor cursor) {

        // Mock movements of the cursor position. Cursor position in the array is tracked using
        // mCursorIndex.
        when(mMockCursor.moveToFirst()).then(
                new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable {
                        mCursorIndex = 0;
                        return FAKE_SESSIONS_CURSOR_DATA[0].length > 0;
                    }
                });
        when(mMockCursor.moveToNext()).then(
                new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable {
                        if (mCursorIndex < FAKE_SESSIONS_CURSOR_DATA[0].length - 1) {
                            mCursorIndex++;
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

        // Mock reading data from the cursor using the FAKE_VIDEO_CURSOR_DATA table values.
        Answer<?> getCursorValue = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return FAKE_SESSIONS_CURSOR_DATA[mGetCursorValueCaptor.getValue()][mCursorIndex];
            }
        };

        // Map the fake data table column index.
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_TITLE))
                .thenReturn(TITLE_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_ABSTRACT))
                .thenReturn(ABSTRACT_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_ID))
                .thenReturn(SESSION_ID_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_PHOTO_URL))
                .thenReturn(PHOTO_URL_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_MAIN_TAG))
                .thenReturn(MAIN_TAG_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_START))
                .thenReturn(START_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_END))
                .thenReturn(END_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_LIVESTREAM_ID))
                .thenReturn(LIVESTREAM_ID_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_YOUTUBE_URL))
                .thenReturn(YOUTUBE_URL_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_TAGS))
                .thenReturn(TAGS_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE))
                .thenReturn(MY_SCHEDULE_COLUMN_INDEX);

        // Returning values from the fake data table.
        when(mMockCursor.getString(mGetCursorValueCaptor.capture())).then(getCursorValue);
        when(mMockCursor.getLong(mGetCursorValueCaptor.capture())).then(getCursorValue);


    }

    private void initMockCursorWithTagsData(Cursor cursor) {

        // Mock movements of the cursor position. Cursor position in the array is tracked using
        // mCursorIndex.
        when(mMockCursor.moveToFirst()).then(
                new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable {
                        mCursorIndex = 0;
                        return FAKE_TAGS_CURSOR_DATA[0].length > 0;
                    }
                });
        when(mMockCursor.moveToNext()).then(
                new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable {
                        if (mCursorIndex < FAKE_TAGS_CURSOR_DATA[0].length - 1) {
                            mCursorIndex++;
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

        // Mock reading data from the cursor using the FAKE_VIDEO_CURSOR_DATA table values.
        Answer<?> getCursorValue = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return FAKE_TAGS_CURSOR_DATA[mGetCursorValueCaptor.getValue()][mCursorIndex];
            }
        };

        // Map the fake data table column index.
        when(mMockCursor.getColumnIndex(ScheduleContract.Tags.TAG_ID))
                .thenReturn(ID_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Tags.TAG_NAME))
                .thenReturn(NAME_COLUMN_INDEX);

        // Returning values from the fake data table.
        when(mMockCursor.getString(mGetCursorValueCaptor.capture())).then(getCursorValue);
        when(mMockCursor.getInt(mGetCursorValueCaptor.capture())).then(getCursorValue);
    }

    private void initMockThemesLimit(Context context, Resources resources) {
        when(context.getResources()).thenReturn(resources);
        when(resources.getInteger(R.integer
                .explore_topic_theme_onsite_max_item_count)).thenReturn(SESSION_THEME_LIMIT);
    }
}
