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

package com.google.samples.apps.iosched.model;

import android.database.Cursor;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.samples.apps.iosched.provider.ScheduleContract;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class TagMetadataTest {

    private static final String FAKE_TAG_ID = "FAKE TAG ID";

    private static final String FAKE_TAG_NAME = "FAKE TAG NAME";

    private static final String FAKE_TAG_CATEGORY = "FAKE TAG CATEGORY";

    private static final String FAKE_TAG_UNUSED_CATEGORY = "FAKE TAG UNUSED CATEGORY";

    private static final int FAKE_TAG_ORDER_IN_CATEGORY = 1;

    private static final String FAKE_TAG_ABSTRACT = "FAKE TAG ABSTRACT";

    private static final int FAKE_TAG_COLOR = 255;

    private static final int TAG_ID_COLUMN_INDEX = 1;

    private static final int TAG_NAME_COLUMN_INDEX = 2;

    private static final int TAG_CATEGORY_COLUMN_INDEX = 3;

    private static final int TAG_ORDER_IN_CATEGORY_COLUMN_INDEX = 4;

    private static final int TAG_ABSTRACT_COLUMN_INDEX = 5;

    private static final int TAG_COLOR_COLUMN_INDEX = 6;

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Mock
    private Cursor mMockCursor;

    private TagMetadata mTagMetadata;


    @Test
    public void constructor_WithNullCursor_ThrowsNPE() {
        // Expected
        mThrown.expect(NullPointerException.class);

        // When TagMetadata initialised with null cursor
        mTagMetadata = new TagMetadata(null);

        // Then NPE is thrown
    }

    @Test
    public void constructor_WithEmptyCursor_Returns() {
        // Given an empty mock cursor
        when(mMockCursor.getCount()).thenReturn(0);

        // When TagMetadata initialised with mock cursor
        mTagMetadata = new TagMetadata(mMockCursor);

        // Then returns
    }

    @Test
    public void constructor_WithOneTag_TagCorrectlyLoaded() {
        // Given a mock cursor with a fake tag
        initMockCursorWithOneTag(mMockCursor);

        // When TagMetadata initialised with mock cursor
        mTagMetadata = new TagMetadata(mMockCursor);


        // Then the tag metadata has the correct data
        assertThat(mTagMetadata.getTag(FAKE_TAG_ID).getName(), is(FAKE_TAG_NAME));
        assertThat(mTagMetadata.getTag(FAKE_TAG_ID).getAbstract(), is(FAKE_TAG_ABSTRACT));
        assertThat(mTagMetadata.getTag(FAKE_TAG_ID).getCategory(), is(FAKE_TAG_CATEGORY));
        assertThat(mTagMetadata.getTag(FAKE_TAG_ID).getColor(), is(FAKE_TAG_COLOR));
        assertThat(mTagMetadata.getTag(FAKE_TAG_ID).getOrderInCategory(),
                is(FAKE_TAG_ORDER_IN_CATEGORY));
        assertThat(mTagMetadata.getTagsInCategory(FAKE_TAG_CATEGORY).size(), is(1));
        assertThat(mTagMetadata.getTagsInCategory(FAKE_TAG_UNUSED_CATEGORY), nullValue());
    }

    @Test
    public void getTagByTagName_ReturnsCorrectTag() {
        // Given a TagMetadata initialised with  mock cursor
        initMockCursorWithOneTag(mMockCursor);
        mTagMetadata = new TagMetadata(mMockCursor);

        // When tag is searched by name
        TagMetadata.Tag tag = mTagMetadata.getTag(FAKE_TAG_NAME);

        // Then tag is returned with correct data
        assertNotNull(tag);
        assertThat(tag.getAbstract(), is(FAKE_TAG_ABSTRACT));
        assertThat(tag.getCategory(), is(FAKE_TAG_CATEGORY));
        assertThat(tag.getId(), is(FAKE_TAG_ID));
        assertThat(tag.getName(), is(FAKE_TAG_NAME));
        assertThat(tag.getColor(), is(FAKE_TAG_COLOR));
        assertThat(tag.getOrderInCategory(),
                is(FAKE_TAG_ORDER_IN_CATEGORY));
    }

    @Test
    public void constructor_WithTwoTags_TagsCorrectlyLoaded() {
        // Given a mock cursor with two fake tags
        initMockCursorWithTwoTags(mMockCursor);

        // When TagMetadata initialised with mock cursor
        mTagMetadata = new TagMetadata(mMockCursor);


        // Then the tag metadata has two tags for the fake category
        // Note: we cannot check the tag data because we used a random tag id, to get two distinct
        // tags.
        assertThat(mTagMetadata.getTagsInCategory(FAKE_TAG_CATEGORY).size(), is(2));
        assertThat(mTagMetadata.getTagsInCategory(FAKE_TAG_UNUSED_CATEGORY), nullValue());
    }

    public static void initMockCursorWithOneTag(Cursor cursor) {
        // Return a count of 1
        when(cursor.getCount()).thenReturn(1);
        when(cursor.moveToPosition(0)).thenReturn(true);

        // Return fake tag details
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_ID))
                .thenReturn(TAG_ID_COLUMN_INDEX);
        when(cursor.getString(TAG_ID_COLUMN_INDEX)).thenReturn(FAKE_TAG_ID);
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_NAME))
                .thenReturn(TAG_NAME_COLUMN_INDEX);
        when(cursor.getString(TAG_NAME_COLUMN_INDEX)).thenReturn(FAKE_TAG_NAME);
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_CATEGORY))
                .thenReturn(TAG_CATEGORY_COLUMN_INDEX);
        when(cursor.getString(TAG_CATEGORY_COLUMN_INDEX)).thenReturn(FAKE_TAG_CATEGORY);
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_ORDER_IN_CATEGORY))
                .thenReturn(TAG_ORDER_IN_CATEGORY_COLUMN_INDEX);
        when(cursor.getInt(TAG_ORDER_IN_CATEGORY_COLUMN_INDEX))
                .thenReturn(FAKE_TAG_ORDER_IN_CATEGORY);
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_ABSTRACT))
                .thenReturn(TAG_ABSTRACT_COLUMN_INDEX);
        when(cursor.getString(TAG_ABSTRACT_COLUMN_INDEX)).thenReturn(FAKE_TAG_ABSTRACT);
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_COLOR))
                .thenReturn(TAG_COLOR_COLUMN_INDEX);
        when(cursor.getInt(TAG_COLOR_COLUMN_INDEX)).thenReturn(FAKE_TAG_COLOR);
    }

    private void initMockCursorWithTwoTags(Cursor cursor) {
        // Return a count of 2
        when(cursor.getCount()).thenReturn(2);
        when(cursor.moveToPosition(0)).thenReturn(true);
        when(cursor.moveToPosition(1)).thenReturn(true);

        // Return fake tag details, using random tag id so we have two distinct tags
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_ID))
                .thenReturn(TAG_ID_COLUMN_INDEX);
        when(cursor.getString(TAG_ID_COLUMN_INDEX)).thenReturn(FAKE_TAG_ID + " "
                + System.currentTimeMillis());
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_NAME))
                .thenReturn(TAG_NAME_COLUMN_INDEX);
        when(cursor.getString(TAG_NAME_COLUMN_INDEX)).thenReturn(FAKE_TAG_NAME);
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_CATEGORY))
                .thenReturn(TAG_CATEGORY_COLUMN_INDEX);
        when(cursor.getString(TAG_CATEGORY_COLUMN_INDEX)).thenReturn(FAKE_TAG_CATEGORY);
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_ORDER_IN_CATEGORY))
                .thenReturn(TAG_ORDER_IN_CATEGORY_COLUMN_INDEX);
        when(cursor.getInt(TAG_ORDER_IN_CATEGORY_COLUMN_INDEX))
                .thenReturn(FAKE_TAG_ORDER_IN_CATEGORY);
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_ABSTRACT))
                .thenReturn(TAG_ABSTRACT_COLUMN_INDEX);
        when(cursor.getString(TAG_ABSTRACT_COLUMN_INDEX)).thenReturn(FAKE_TAG_ABSTRACT);
        when(cursor.getColumnIndex(ScheduleContract.Tags.TAG_COLOR))
                .thenReturn(TAG_COLOR_COLUMN_INDEX);
        when(cursor.getInt(TAG_COLOR_COLUMN_INDEX)).thenReturn(FAKE_TAG_COLOR);
    }
}
