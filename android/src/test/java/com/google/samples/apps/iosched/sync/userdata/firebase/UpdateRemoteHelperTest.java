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

package com.google.samples.apps.iosched.sync.userdata.firebase;

import android.test.suitebuilder.annotation.SmallTest;

import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;
import com.google.samples.apps.iosched.util.FirebaseUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.hasItem;


@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class UpdateRemoteHelperTest {

    UpdateRemoteHelper mHelper;

    @Before
    public void setUp() {
        mHelper = new UpdateRemoteHelper(new UserDataHelper.UserData(),
                new HashMap<String, Object>());
    }

    @Test
    public void updatePendingFirebaseUpdatesMap_storesGcmKey() {
        withGCMKey();
        mHelper.updatePendingFirebaseUpdatesMap();
        assertMapHasValue(Constants.REMOTE_GCM_KEY);
    }

    @Test
    public void updatePendingFirebaseUpdatesMap_storesViewedVideos() {
        withViewedVideos();
        mHelper.updatePendingFirebaseUpdatesMap();
        assertMapHasKey(FirebaseUtils.getViewedVideoChildPath(Constants.LOCAL_VIDEO_ID));
        assertMapHasKey(FirebaseUtils.getViewedVideoChildPath(Constants.REMOTE_VIDEO_ID));
    }

    /**
     * Adds a GCM Key to the merged user data.
     */
    private void withGCMKey() {
        mHelper.mMergedUserData.setGcmKey(Constants.REMOTE_GCM_KEY);
    }

    /**
     * Adds viewed video IDs to merged user data.
     */
    private void withViewedVideos() {
        mHelper.mMergedUserData.getViewedVideoIds().add(Constants.REMOTE_VIDEO_ID);
        mHelper.mMergedUserData.getViewedVideoIds().add(Constants.LOCAL_VIDEO_ID);
    }

    /**
     * Asserts that the pending Firebase updates map keys contain {@code key}.
     */
    private void assertMapHasKey(String key) {
        assertThat(mHelper.mPendingFirebaseUpdatesMap.keySet(), hasItem(key));
    }

    /**
     * Asserts that the pending Firebase updates map value contain {@code value}.
     */
    private void assertMapHasValue(String value) {
        assertThat(mHelper.mPendingFirebaseUpdatesMap.values(), hasItem(value));
    }
}
