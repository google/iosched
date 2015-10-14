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

package com.google.samples.apps.iosched.videolibrary;

import com.google.samples.apps.iosched.InvalidEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.test.suitebuilder.annotation.SmallTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class VideoLibraryModelTest {

    /** Cursor position in the {@link #FAKE_VIDEO_CURSOR_DATA} table. */
    private int mCursorIndex = -1;

    /** Fake data to be used for the mocked cursor. */
    private static final Object[][] FAKE_VIDEO_CURSOR_DATA = {
            new String[]{"ID1", "ID2", "ID3"},
            new Integer[]{2012, 2013, 2012},
            new String[]{"Android", "APIs", "Android"},
            new String[]{"Title 1", "Title 2", "Title 3"},
            new String[]{"Desc 1", "Desc 2", "Desc 3"},
            new String[]{"VID1", "VID2", "VID3"},
            new String[]{"Speaker Name 1", "Speaker Name 2", "Speaker Name 3"},
            new String[]{"http://url.com/1", "http://url.com/2", "http://url.com/3"}};

    /** Indexes of the "column" for each video attribute in the {@link #FAKE_VIDEO_CURSOR_DATA}. */
    private static final int VIDEO_ID_COLUMN_INDEX = 0;
    private static final int VIDEO_YEAR_COLUMN_INDEX = 1;
    private static final int VIDEO_TOPIC_COLUMN_INDEX = 2;
    private static final int VIDEO_TITLE_COLUMN_INDEX = 3;
    private static final int VIDEO_DESC_COLUMN_INDEX = 4;
    private static final int VIDEO_VID_COLUMN_INDEX = 5;
    private static final int VIDEO_SPEAKER_COLUMN_INDEX = 6;
    private static final int VIDEO_THUMBNAIL_URL_COLUMN_INDEX = 7;

    @Mock
    private Context mMockContext;

    @Mock
    private Cursor mMockCursor;

    @Mock
    private Bundle mMockBundle;

    @Mock
    private CursorLoader mMockCursorLoader;

    @Spy
    private VideoLibraryModel mSpyModel = new VideoLibraryModel(mMockContext, null);

    @Captor
    private ArgumentCaptor<Integer> mGetCursorValueCaptor;

    private VideoLibraryModel mVideoLibraryModel;

    @Before
    public void initMocks() {
        // Initialize Mocks and Spies.
        initWithStubbedCursor();
        initWithStubbedCursorLoaderAndSpyModel();

        // Create an instance of the model.
        mVideoLibraryModel = new VideoLibraryModel(mMockContext, null);
    }

    @Test
    public void readDataFromCursor_VideosQuery_VideosLoaded() {
        // Given a mock cursor with data ran with a valid query.
        boolean success = mVideoLibraryModel.readDataFromCursor(
                mMockCursor, VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS);

        // Then the model is updated and the request succeeds.
        assertThat(success, is(true));

        // Check that all Videos have been filled properly.
        assertThat(mVideoLibraryModel.getVideos().size(), is(FAKE_VIDEO_CURSOR_DATA[0].length));
        for (int i = 0; i < FAKE_VIDEO_CURSOR_DATA[0].length; i++) {
            VideoLibraryModel.Video video = mVideoLibraryModel.getVideos().get(i);
            assertThat(video, is(equalsVideoDataInCursor(FAKE_VIDEO_CURSOR_DATA, i)));
        }
    }

    @Test
    public void readDataFromCursor_FiltersQuery_FiltersLoaded() {
        // Given a mock cursor with data ran with a valid query
        boolean success = mVideoLibraryModel.readDataFromCursor(
                mMockCursor, VideoLibraryModel.VideoLibraryQueryEnum.FILTERS);

        // Then the model is updated and the request succeeds
        assertThat(success, is(true));
        // Then the model is updated and the request succeeds
        assertThat(mVideoLibraryModel.getTopics(),
                hasItems((String[]) FAKE_VIDEO_CURSOR_DATA[VIDEO_TOPIC_COLUMN_INDEX]));
        assertThat(mVideoLibraryModel.getYears(),
                hasItems((Integer[])FAKE_VIDEO_CURSOR_DATA[VIDEO_YEAR_COLUMN_INDEX]));
    }

    @Test
    public void readDataFromCursor_UnsupportedQuery_Unsuccessful() {
        // When ran with an invalid query
        boolean result = mVideoLibraryModel.readDataFromCursor(mMockCursor, InvalidEnum.INVALID);

        // Then the request model update fails
        assertThat(result, is(false));
    }

    @Test
    public void readDataFromCursor_EmptyCursor_EmptyResult() {
        // Given an empty mock cursor.
        when(mMockCursor.moveToFirst()).thenReturn(false);

        // When ran with a valid query.
        boolean result = mVideoLibraryModel.readDataFromCursor(mMockCursor,
                VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS);

        // Then the request model update succeeds.
        assertThat(result, is(true));
        // And the list of videos is empty.
        assertThat(mVideoLibraryModel.getVideos().size(),  is(equalTo(0)));
    }

    @Test
    public void createCursorLoader_VideosQuery_Success() {
        // Given a mock cursor loader set up for a video query
        int videosLoaderId = VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS.getId();

        // When ran with the video query
        CursorLoader createdCursorLoader =
                (CursorLoader) mSpyModel.createCursorLoader(videosLoaderId, Uri.EMPTY, null);

        // Then the returned cursor loader is the same as the mock one
        assertThat(createdCursorLoader, sameInstance(mMockCursorLoader));
    }

    @Test
    public void createCursorLoader_FilteredVideosQuery_Success() {
        // Given a mock cursor loader set up for a video query
        int videosLoaderId = VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS.getId();

        when(mMockBundle.containsKey(VideoLibraryModel.KEY_TOPIC)).thenReturn(true);
        when(mMockBundle.containsKey(VideoLibraryModel.KEY_YEAR)).thenReturn(true);
        when(mMockBundle.getString(VideoLibraryModel.KEY_TOPIC)).thenReturn("Android");
        when(mMockBundle.getInt(VideoLibraryModel.KEY_YEAR)).thenReturn(2012);

        // When ran with the video query
        CursorLoader createdCursorLoader =
                (CursorLoader) mSpyModel.createCursorLoader(videosLoaderId, Uri.EMPTY, mMockBundle);

        // Then the returned cursor loader is the same as the mock one
        assertThat(createdCursorLoader, sameInstance(mMockCursorLoader));
    }

    @Test
    public void createCursorLoader_FiltersQuery_Success() {
        // Given a mock cursor loader set up for a video query
        int videosLoaderId = VideoLibraryModel.VideoLibraryQueryEnum.FILTERS.getId();

        // When ran with the video query
        CursorLoader createdCursorLoader =
                (CursorLoader) mSpyModel.createCursorLoader(videosLoaderId, Uri.EMPTY, null);

        // Then the returned cursor loader is the same as the mock one
        assertThat(createdCursorLoader, sameInstance(mMockCursorLoader));
    }

    @Test
    public void createCursorLoader_UnknownLoaderId_CreatesNullCursorLoader() {
        // When ran with an invalid query
        Loader<Cursor> cursorLoader = mVideoLibraryModel
                .createCursorLoader(InvalidEnum.INVALID.getId(), null, null);

        // Then the returned cursor is null
        assertThat(cursorLoader, nullValue());
    }

    private void initWithStubbedCursorLoaderAndSpyModel() {
        doReturn(mMockCursorLoader).when(mSpyModel).getCursorLoaderInstance(
                any(Context.class), any(Uri.class), any(String[].class), any(String.class),
                any(String[].class), any(String.class));
    }

    /**
     *  Initializes the {@code mMockCursor} so that it returns data from the
     *  {@link #FAKE_VIDEO_CURSOR_DATA} table.
     */
    private void initWithStubbedCursor() {
        // Mock movements of the cursor position. Cursor position in the array is tracked using
        // mCursorIndex.
        when(mMockCursor.moveToFirst()).then(
                new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable {
                        mCursorIndex = 0;
                        return FAKE_VIDEO_CURSOR_DATA[0].length > 0;
                    }
                });
        when(mMockCursor.moveToNext()).then(
                new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable {
                        if (mCursorIndex < FAKE_VIDEO_CURSOR_DATA[0].length - 1) {
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
                return FAKE_VIDEO_CURSOR_DATA[mGetCursorValueCaptor.getValue()][mCursorIndex];
            }
        };

        // Map the fake data table column index.
        when(mMockCursor.getColumnIndex(ScheduleContract.Videos.VIDEO_ID))
                .thenReturn(VIDEO_ID_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Videos.VIDEO_YEAR))
                .thenReturn(VIDEO_YEAR_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Videos.VIDEO_TOPIC))
                .thenReturn(VIDEO_TOPIC_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Videos.VIDEO_TITLE))
                .thenReturn(VIDEO_TITLE_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Videos.VIDEO_DESC))
                .thenReturn(VIDEO_DESC_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Videos.VIDEO_VID))
                .thenReturn(VIDEO_VID_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Videos.VIDEO_SPEAKERS))
                .thenReturn(VIDEO_SPEAKER_COLUMN_INDEX);
        when(mMockCursor.getColumnIndex(ScheduleContract.Videos.VIDEO_THUMBNAIL_URL))
                .thenReturn(VIDEO_THUMBNAIL_URL_COLUMN_INDEX);

        // Returning values from the fake data table.
        when(mMockCursor.getString(mGetCursorValueCaptor.capture())).then(getCursorValue);
        when(mMockCursor.getInt(mGetCursorValueCaptor.capture())).then(getCursorValue);
    }

    /**
     * Checks that the given {@code VideoLibraryModel.Video} is equal to the video data in the given
     * cursor table at the given {@code index}.
     */
    private Matcher<VideoLibraryModel.Video> equalsVideoDataInCursor(final Object[][] cursorTable,
            final int index) {
        return new BaseMatcher<VideoLibraryModel.Video>() {
            @Override
            public boolean matches(final Object item) {
                final VideoLibraryModel.Video video = (VideoLibraryModel.Video) item;
                return video.getId().equals(cursorTable[VIDEO_ID_COLUMN_INDEX][index])
                        && video.getYear() == (Integer) cursorTable[VIDEO_YEAR_COLUMN_INDEX][index]
                        && video.getTopic().equals(cursorTable[VIDEO_TOPIC_COLUMN_INDEX][index])
                        && video.getTitle().equals(cursorTable[VIDEO_TITLE_COLUMN_INDEX][index])
                        && video.getDesc().equals(cursorTable[VIDEO_DESC_COLUMN_INDEX][index])
                        && video.getVid().equals(cursorTable[VIDEO_VID_COLUMN_INDEX][index])
                        && video.getSpeakers().equals(
                            cursorTable[VIDEO_SPEAKER_COLUMN_INDEX][index])
                        && video.getThumbnailUrl().equals(
                            cursorTable[VIDEO_THUMBNAIL_URL_COLUMN_INDEX][index]);
            }
            @Override
            public void describeTo(final Description description) {
                description.appendText("The Video does not match the data in table ")
                        .appendValue(cursorTable).appendText(" at index ").appendValue(index);
            }
        };
    }
}
