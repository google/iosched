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

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.google.samples.apps.iosched.Config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for writing/reading from {@link Parcel} for {@link TagFilterHolder}. These tests require
 * the Android framework as they rely on {@link Parcel}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TagFilterHolderTest {

    private final static String TAG_ID_1 = "TAG_ID_1";

    private final static String TAG_ID_2 = "TAG_ID_2";

    private final static String TAG_ID_3 = "TAG_ID_3";

    private TagFilterHolder mTagFilterHolder;

    private Parcel mParcel;

    private TagFilterHolder mReadTagFilterHolder;

    @Before
    public void setUp() {
        mTagFilterHolder = new TagFilterHolder();
    }

    @Test
    public void writeToParcel_NonEmptyValues_readFromParcel() {
        // Given a tag filter holder with 3 tag ids and 2 categories, and with livestream
        mTagFilterHolder.add(TAG_ID_1, Config.Tags.CATEGORY_THEME);
        mTagFilterHolder.add(TAG_ID_2, Config.Tags.CATEGORY_THEME);
        mTagFilterHolder.add(TAG_ID_3, Config.Tags.CATEGORY_TRACK);
        mTagFilterHolder.setShowLiveStreamedSessions(true);

        // When writing it to parcel
        writeTagFilterHolderToParcel();

        // Then the recreated tag filter holder from parcel has the same 3 tag ids and 2 categories
        // and with livestream
        createTagFilterHolderFromParcel();
        assertThat(mReadTagFilterHolder.getSelectedFilters().size(), is(3));
        assertTrue(mReadTagFilterHolder.getSelectedFilters().contains(TAG_ID_1));
        assertTrue(mReadTagFilterHolder.getSelectedFilters().contains(TAG_ID_2));
        assertTrue(mReadTagFilterHolder.getSelectedFilters().contains(TAG_ID_3));
        assertThat(mReadTagFilterHolder.getCategoryCount(), is(2));
        assertThat(mReadTagFilterHolder.getCountByCategory(Config.Tags.CATEGORY_THEME), is(2));
        assertThat(mReadTagFilterHolder.getCountByCategory(Config.Tags.CATEGORY_TRACK), is(1));
        assertTrue(mReadTagFilterHolder.isShowLiveStreamedSessions());
    }

    @Test
    public void writeToParcel_EmptyValues_readFromParcel() {
        // Given an empty TagFilterHolder

        // When writing it to parcel
        writeTagFilterHolderToParcel();

        // Then the recreated tag filter holder from parcel is empty
        createTagFilterHolderFromParcel();
        assertThat(mReadTagFilterHolder.getSelectedFilters().size(), is(0));
        assertThat(mReadTagFilterHolder.getCategoryCount(),
                is(1));// If there is no category data, getCategoryCount returns 1
    }

    @Test
    public void add_invalidCategory_DoesNotCrash() {
        // Given an empty TagFilterHolder

        // When adding a tag with an invalid category
        mTagFilterHolder.add("tag", "invalid category");

        // Then the tag filter holder is still empty and has not crashed
        assertThat(mTagFilterHolder.getSelectedFilters().size(), is(0));
        assertThat(mTagFilterHolder.getCategoryCount(),
                is(1));// If there is no category data, getCategoryCount returns 1
    }

    private void writeTagFilterHolderToParcel() {
        mParcel = Parcel.obtain();
        mTagFilterHolder.writeToParcel(mParcel, 0);
    }

    private void createTagFilterHolderFromParcel() {
        // Set parcel for reading
        mParcel.setDataPosition(0);
        mReadTagFilterHolder = (TagFilterHolder) TagFilterHolder.CREATOR.createFromParcel(mParcel);
    }
}
