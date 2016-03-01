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

import android.content.Context;
import android.content.SharedPreferences;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.samples.apps.iosched.sync.userdata.UserAction;
import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MergeHelperTest {
    /**
     * Creates a {@link UserAction} for a viewed video.
     *
     * @param requiresSync Indicates whether the action requires a data sync or not.
     * @return THe {@link UserAction} for a viewed video.
     */
    private static UserAction createViewedVideoAction(boolean requiresSync) {
        UserAction action = new UserAction();
        action.type = UserAction.TYPE.VIEW_VIDEO;
        action.videoId = Constants.LOCAL_VIDEO_ID;
        action.requiresSync = requiresSync;
        return action;
    }

    @Mock
    private Context mMockContext;

    @Mock
    private SharedPreferences mMockSharedPreferences;

    MergeHelper mHelper;

    @Before
    public void setUp() {
        mHelper = new MergeHelper(new UserDataHelper.UserData(),
                new UserDataHelper.UserData(), new UserDataHelper.UserData());
        withLocalGCMKey();
    }

    @Test
    public void mergeGCMKeys_whenRemoteDoesNotHaveKey() {
        mHelper.mergeGCMKeys();
        assertThatMergedGCMKeyIs(Constants.LOCAL_GCM_KEY);
    }

    @Test
    public void mergeGCMKeys_whenRemoteHasKey() {
        withRemoteGCMKey();
        mHelper.mergeGCMKeys();
        assertThatMergedGCMKeyIs(Constants.REMOTE_GCM_KEY);
    }

    @Test
    public void mergeDirtyActions_localViewedVideosOnly() {
        mHelper.mergeUnsyncedActions(withViewedVideoLocalActions());
        assertThatMergedUserDataHas(Constants.LOCAL_VIDEO_ID);
    }

    @Test
    public void mergeDirtyActions_localViewedVideosOnly_withoutRequiresSync() {
        mHelper.mergeUnsyncedActions(withViewedVideoLocalActionsNoSync());
        assertThatMergedUserDataHasNoVideoIds();
    }

    @Test
    public void mergeDirtyActions_remoteViewedVideosOnly() {
        withRemoteViewedVideo();
        mHelper.mergeUnsyncedActions(withViewedVideoLocalActions());
        assertThatMergedUserDataHas(Constants.REMOTE_VIDEO_ID);
    }

    @Test
    public void mergeDirtyActions_localAndRemoteViewedVideos() {
        withRemoteViewedVideo();
        mHelper.mergeUnsyncedActions(withViewedVideoLocalActions());
        assertThatMergedUserDataHas(Constants.LOCAL_VIDEO_ID);
        assertThatMergedUserDataHas(Constants.REMOTE_VIDEO_ID);
    }

    /**
     * Adds a GCM key to the local user data.
     */
    private void withLocalGCMKey() {
        mHelper.mLocalUserData.setGcmKey(Constants.LOCAL_GCM_KEY);
    }

    /**
     * Adds a GCM key to the remote user data.
     */
    private void withRemoteGCMKey() {
        mHelper.mRemoteUserData.setGcmKey(Constants.REMOTE_GCM_KEY);
    }

    /**
     * Creates and returns a {@link UserAction} list which contains a single viewed video action
     * that requires sync.
     */
    private List<UserAction> withViewedVideoLocalActions() {
        return new ArrayList<UserAction>() {{
            add(createViewedVideoAction(true));
        }};
    }

    /**
     * Creates and returns a {@link UserAction} list which contains a single viewed video action
     * that *does not require* sync.
     */
    private List<UserAction> withViewedVideoLocalActionsNoSync() {
        return new ArrayList<UserAction>() {{
            add(createViewedVideoAction(false));
        }};
    }

    /**
     * Adds a remote viewed video to the remote user data.
     */
    private void withRemoteViewedVideo() {
        mHelper.mRemoteUserData.getViewedVideoIds().add(Constants.REMOTE_VIDEO_ID);
    }

    /**
     * Asserts that {@code gcmKey} is stored in merged user data.
     */
    private void assertThatMergedGCMKeyIs(String gcmKey) {
        assertThat(gcmKey, is(equalTo(mHelper.mMergedUserData.getGcmKey())));
    }

    /**
     * Asserts that {@code videoId} is stored in merged user data.
     */
    private void assertThatMergedUserDataHas(String videoId) {
        assertThat(mHelper.mMergedUserData.getViewedVideoIds(), hasItem(videoId));
    }

    /**
     * Asserts that no video ID was stored in merged user data.
     */
    private void assertThatMergedUserDataHasNoVideoIds() {
        assertThat(mHelper.mMergedUserData.getViewedVideoIds(), is(Collections.<String>emptySet()));
    }
}
