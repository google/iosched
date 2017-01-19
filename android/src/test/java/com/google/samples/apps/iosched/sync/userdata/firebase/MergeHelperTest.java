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
import com.google.samples.apps.iosched.sync.userdata.util.UserData;
import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;
import com.google.samples.apps.iosched.util.FirebaseUtils;

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

    // TODO: expand tests to cover all sync scenarios. See b/28324707.
    public static final String UID = "uid";

    public static final String LOCAL_GCM_KEY = "LOCAL GCM KEY";

    public static final String REMOTE_GCM_KEY = "REMOTE GCM KEY";

    public static final String LOCAL_VIDEO_ID = "LOCAL VIDEO ID";

    public static final String SESSION_ONE_ID = "SESSION ONE ID";

    public static final String SESSION_TWO_ID = "SESSION TWO ID";

    public static final String REMOTE_VIDEO_ID = "REMOTE VIDEO ID";

    public static final Long CURRENT_TIMESTAMP = 1457928631L;

    public static final Long TIMESTAMP = CURRENT_TIMESTAMP;

    public static final Long STALE_TIMESTAMP = CURRENT_TIMESTAMP - (60 * 60 * 1000);

    public static final String LOCAL_SESSION_ID = "LOCAL SESSION ID";

    public static final String REMOTE_SESSION_ID = "REMOTE SESSION ID";

    /**
     * Creates a {@link UserAction} for a viewed video.
     *
     * @param videoId      The ID of the viewed video.
     * @param requiresSync Indicates whether the action requires a data sync or not.
     * @return The {@link UserAction} for a viewed video.
     */
    private static UserAction createViewedVideoAction(String videoId, boolean requiresSync) {
        UserAction action = new UserAction();
        action.type = UserAction.TYPE.VIEW_VIDEO;
        action.videoId = videoId;
        action.requiresSync = requiresSync;
        return action;
    }

    /**
     * Create a {@link UserAction} for a starred session.
     *
     * @param sessionId    The ID of the starred session.
     * @param requiresSync Indicates whether the action requires a data sync or not.
     * @return The {@link UserAction} for a starred session.
     */
    private static UserAction createAddStarAction(String sessionId, Long timestamp,
            boolean requiresSync) {
        UserAction action = new UserAction();
        action.type = UserAction.TYPE.ADD_STAR;
        action.sessionId = sessionId;
        action.requiresSync = requiresSync;
        action.timestamp = timestamp;
        return action;
    }

    /**
     * Create a {@link UserAction} for a starred session.
     *
     * @param sessionId    The ID of the starred session.
     * @param requiresSync Indicates whether the action requires a data sync or not.
     * @return The {@link UserAction} for a starred session.
     */
    private static UserAction createRemoveStarAction(String sessionId, Long timestamp,
            boolean requiresSync) {
        UserAction action = new UserAction();
        action.type = UserAction.TYPE.REMOVE_STAR;
        action.sessionId = sessionId;
        action.requiresSync = requiresSync;
        action.timestamp = timestamp;
        return action;
    }

    /**
     * Creates a {@link UserAction} for a session for which feedback was submitted.
     *
     * @param sessionId The ID of the session for which feedback was submitted.
     * @param requiresSync  Indicates whether the action requires a data sync or not.
     * @return The {@link UserAction} for a feedback submitted session.
     */
    private static UserAction createFeedbackSubmittedSessionAction(String sessionId,
            boolean requiresSync) {
        UserAction action = new UserAction();
        action.type = UserAction.TYPE.SUBMIT_FEEDBACK;
        action.sessionId = sessionId;
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
        mHelper = new MergeHelper(new UserData(),
                new UserData(), new UserData(), UID);
        withLocalGCMKey();
    }

    @Test
    public void mergeGCMKeys_whenRemoteDoesNotHaveKey() {
        mHelper.mergeGCMKeys();
        assertThatMergedGCMKeyIs(LOCAL_GCM_KEY);
    }

    @Test
    public void mergeGCMKeys_whenRemoteHasKey() {
        withRemoteGCMKey();
        mHelper.mergeGCMKeys();
        assertThatMergedGCMKeyIs(REMOTE_GCM_KEY);
    }

    @Test
    public void mergeUnsyncedActions_localSessionOnly_requiresSync() {
        mHelper.mergeUnsyncedActions(withLocalStarredSessionActions(LOCAL_SESSION_ID));
        assertThatMergedStarSessionsHas(LOCAL_SESSION_ID);
    }

    @Test
    public void mergeUnsyncedActions_localSessionOnly_noSync() {
        mHelper.mergeUnsyncedActions(withLocalStarredSessionActionsNoSync());
        assertThatMergedStarSessionsHasNoSessions();
    }

    @Test
    public void mergeUnsyncedActions_remoteSessionOnly() {
        withRemoteStarredSession(REMOTE_SESSION_ID);
        mHelper.mergeUnsyncedActions(withNoLocalUserActions());
        assertThatMergedStarSessionsHas(REMOTE_SESSION_ID);
    }

    @Test
    public void mergeUnsyncedActions_localAndRemoteSessions_differentSessionIDs() {
        withRemoteStarredSession(REMOTE_SESSION_ID);
        mHelper.mergeUnsyncedActions(withLocalStarredSessionActions(LOCAL_SESSION_ID));
        assertThatMergedStarSessionsHas(LOCAL_SESSION_ID);
        assertThatMergedStarSessionsHas(REMOTE_SESSION_ID);
    }

    @Test
    public void mergeUnsyncedActions_localRemoveStar_remoteAddStar_localStale() {
        mHelper.mergeUnsyncedActions(withLocalRemoveRemoteAddLocalStale());
        assertThatMergedStarSessionsHas(SESSION_ONE_ID);
    }

    @Test
    public void mergeUnsyncedActions_localViewedVideosOnly() {
        mHelper.mergeUnsyncedActions(withViewedVideoLocalActions());
        assertThatMergedViewedVideosHas(LOCAL_VIDEO_ID);
    }

    @Test
    public void mergeUnsyncedActions_localViewedVideosOnly_withoutRequiresSync() {
        mHelper.mergeUnsyncedActions(withViewedVideoLocalActionsNoSync());
        assertThatMergedUserDataHasNoVideoIds();
    }

    @Test
    public void mergeUnsyncedActions_remoteViewedVideosOnly() {
        withRemoteViewedVideo();
        mHelper.mergeUnsyncedActions(withNoLocalUserActions());
        assertThatMergedViewedVideosHas(REMOTE_VIDEO_ID);
    }

    @Test
    public void mergeUnsyncedActions_localAndRemoteViewedVideos() {
        withRemoteViewedVideo();
        mHelper.mergeUnsyncedActions(withViewedVideoLocalActions());
        assertThatMergedViewedVideosHas(LOCAL_VIDEO_ID);
        assertThatMergedViewedVideosHas(REMOTE_VIDEO_ID);
    }

    @Test
    public void mergeUnsyncedActions_localFeedbackSubmittedSessionsOnly() {
        mHelper.mergeUnsyncedActions(withLocalFeedbackSubmittedActions());
        assertThatMergeFeedbackSubmittedSessionsHas(LOCAL_SESSION_ID);
    }

    @Test
    public void mergeUnsyncedActions_remoteFeedbackSubmittedSessionsOnly() {
        withRemoteFeedbackSubmittedSession();
        mHelper.mergeUnsyncedActions(withNoLocalUserActions());
        assertThatMergeFeedbackSubmittedSessionsHas(REMOTE_SESSION_ID);
    }

    @Test
    public void mergeUnsyncedActions_localAndRemoteFeedbackSubmittedSessions() {
        withRemoteFeedbackSubmittedSession();
        mHelper.mergeUnsyncedActions(withLocalFeedbackSubmittedActions());
        assertThatMergeFeedbackSubmittedSessionsHas(REMOTE_SESSION_ID);
        assertThatMergeFeedbackSubmittedSessionsHas(LOCAL_SESSION_ID);
    }

    @Test
    public void getPendingFirebaseUpdatesMap_storesMergedGcmKey() {
        withMergedGCMKey();
        assertThat(mHelper.getPendingFirebaseUpdatesMap().values(),
                hasItem(REMOTE_GCM_KEY));
    }

    @Test
    public void getPendingFirebaseUpdatesMap_MergedDataHasSession() {
        withMergedStarredSession(SESSION_ONE_ID);
        assertThatSessionIsInSchedule(SESSION_ONE_ID, true);
        assertThatTimestampIsStored(SESSION_ONE_ID);
    }

    @Test
    public void getPendingFirebaseUpdatesMap_mergedDataHasSession_remoteDataHasSameSession() {
        withMergedStarredSession(SESSION_ONE_ID);
        withRemoteStarredSession(SESSION_ONE_ID);
        assertThatTimestampIsStored(SESSION_ONE_ID);
        assertThatSessionIsInSchedule(SESSION_ONE_ID, true);
    }

    @Test
    public void getPendingFirebaseUpdatesMap_storesMergedViewedVideo() {
        withMergedViewedVideos();
        assertThat(mHelper.getPendingFirebaseUpdatesMap().keySet(),
                hasItem(FirebaseUtils.getViewedVideoChildPath(UID, REMOTE_VIDEO_ID)));
    }

    @Test
    public void getPendingFirebaseUpdatesMap_storesMergedFeedbackSubmittedSessions() {
        withMergedFeedbackSubmittedSessions();
        assertThat(mHelper.getPendingFirebaseUpdatesMap().keySet(),
                hasItem(FirebaseUtils.getFeedbackSubmittedSessionChildPath(UID, LOCAL_SESSION_ID)));
        assertThat(mHelper.getPendingFirebaseUpdatesMap().keySet(),
                hasItem(FirebaseUtils.getFeedbackSubmittedSessionChildPath(UID,
                        REMOTE_SESSION_ID)));
    }

    @Test
    public void buildPendingFirebaseUpdatesMap_storesLastActivityTimestamp() {
        assertThat(mHelper.getPendingFirebaseUpdatesMap().keySet(),
                hasItem(FirebaseUtils.getLastActivityTimestampChildPath(UID)));
    }

    /**
     * Adds a GCM key to the local user data.
     */
    private void withLocalGCMKey() {
        mHelper.getLocalUserData().setGcmKey(LOCAL_GCM_KEY);
    }

    /**
     * Adds a GCM key to the remote user data.
     */
    private void withRemoteGCMKey() {
        mHelper.getRemoteUserData().setGcmKey(REMOTE_GCM_KEY);
    }

    /**
     * Adds a starred session to the remote user data.
     */
    private void withRemoteStarredSession(String sessionId) {
        mHelper.getRemoteUserData().getStarredSessions().put(sessionId,
                new UserData.StarredSession(true,TIMESTAMP));
    }

    /**
     * Creates and returns a {@link UserAction} list with a single local starred session action that
     * requires sync.
     *
     * @param sessionId THe ID of the starred session.
     */
    private List<UserAction> withLocalStarredSessionActions(final String sessionId) {
        return new ArrayList<UserAction>() {{
            add(createAddStarAction(sessionId, TIMESTAMP, true));
        }};
    }

    /**
     * Creates and returns a {@link UserAction} list with a single local starred session action that
     * does not require sync.
     */
    private List<UserAction> withLocalStarredSessionActionsNoSync() {
        return new ArrayList<UserAction>() {{
            add(createAddStarAction(LOCAL_SESSION_ID, TIMESTAMP, false));
        }};
    }

    /**
     * Adds a single starred session to remote user data and creates and returns a {@link
     * UserAction} list with the same starred session. Ensures that the timestamp of the local
     * action is greater than the timestamp of the remote starred session.
     */
    private List<UserAction> withLocalRemoveRemoteAddRemoteStale() {
        mHelper.getRemoteUserData().getStarredSessions().put(SESSION_ONE_ID,
                new UserData.StarredSession(true, STALE_TIMESTAMP));
        return new ArrayList<UserAction>() {{
            add(createRemoveStarAction(SESSION_ONE_ID, CURRENT_TIMESTAMP, true));
        }};
    }

    /**
     * Adds a single starred session to remote user data and creates and returns a {@link
     * UserAction} list with the same starred session. Ensures that the timestamp of the remote
     * starred session is greater than the timestamp of the local action.
     */
    private List<UserAction> withLocalRemoveRemoteAddLocalStale() {
        mHelper.getRemoteUserData().getStarredSessions().put(SESSION_ONE_ID,
                new UserData.StarredSession(true, CURRENT_TIMESTAMP));
        return new ArrayList<UserAction>() {{
            add(createRemoveStarAction(SESSION_ONE_ID, STALE_TIMESTAMP, true));
        }};
    }

    /**
     * Returns a list with no {@link UserAction} objects.
     */
    private List<UserAction> withNoLocalUserActions() {
        return new ArrayList<>();
    }

    /**
     * Creates and returns a {@link UserAction} list which contains a single viewed video action
     * that requires sync.
     */
    private List<UserAction> withViewedVideoLocalActions() {
        return new ArrayList<UserAction>() {{
            add(createViewedVideoAction(LOCAL_VIDEO_ID, true));
        }};
    }

    /**
     * Creates and returns a {@link UserAction} list which contains a single viewed video action
     * that *does not require* sync.
     */
    private List<UserAction> withViewedVideoLocalActionsNoSync() {
        return new ArrayList<UserAction>() {{
            add(createViewedVideoAction(LOCAL_VIDEO_ID, false));
        }};
    }

    /**
     * Adds a starred session to merged user data.
     *
     * @param sessionId The Id of the starred session.
     */
    private void withMergedStarredSession(String sessionId) {
        mHelper.getMergedUserData().getStarredSessions().put(sessionId,
                new UserData.StarredSession(true, CURRENT_TIMESTAMP));
    }

    /**
     * Adds a remote viewed video to the remote user data.
     */
    private void withRemoteViewedVideo() {
        mHelper.getRemoteUserData().getViewedVideoIds().add(REMOTE_VIDEO_ID);
    }

    /**
     * Adds a remote feedback submitted session.
     */
    private void withRemoteFeedbackSubmittedSession() {
        mHelper.getRemoteUserData().getFeedbackSubmittedSessionIds().add(REMOTE_SESSION_ID);
    }

    /**
     * Creates and returns a {@link UserAction} list which contains a single feedback submitted
     * session that requires sync.
     */
    private List<UserAction> withLocalFeedbackSubmittedActions() {
        return new ArrayList<UserAction>() {{
            add(createFeedbackSubmittedSessionAction(LOCAL_SESSION_ID, true));
        }};
    }

    /**
     * Adds a GCM Key to the merged user data.
     */
    private void withMergedGCMKey() {
        mHelper.getMergedUserData().setGcmKey(REMOTE_GCM_KEY);
    }

    /**
     * Adds viewed video IDs to merged user data.
     */
    private void withMergedViewedVideos() {
        mHelper.getMergedUserData().getViewedVideoIds().add(REMOTE_VIDEO_ID);
    }

    /**
     * Adds feedback submitted session IDs to merged user data.
     */
    private void withMergedFeedbackSubmittedSessions() {
        mHelper.getMergedUserData().getFeedbackSubmittedSessionIds().add(LOCAL_SESSION_ID);
        mHelper.getMergedUserData().getFeedbackSubmittedSessionIds().add(REMOTE_SESSION_ID);
    }

    /**
     * Asserts that {@code gcmKey} is stored in merged user data.
     */
    private void assertThatMergedGCMKeyIs(String gcmKey) {
        assertThat(gcmKey, is(equalTo(mHelper.getMergedUserData().getGcmKey())));
    }

    /**
     * Asserts that {@code sessionId} is stored in merged user data.
     *
     * @param sessionId The ID of themerged session.
     */
    private void assertThatMergedStarSessionsHas(String sessionId) {
        assertThat(mHelper.getMergedUserData().getStarredSessions().keySet(),
                hasItem(sessionId));
    }

    /**
     * Asserts that no session IDs are stored in merged user data.
     */
    private void assertThatMergedStarSessionsHasNoSessions() {
        assertThat(mHelper.getMergedUserData().getStarredSessions().keySet(),
                is(Collections.<String>emptySet()));
    }

    /**
     * Asserts whether {@code sessionId} is in schedule or not in merged user data.
     *
     * @param sessionId  The ID of the starred session.
     * @param inSchedule Tracks whether a session is in schedule or not.
     */
    private void assertThatSessionIsInSchedule(String sessionId, boolean inSchedule) {
        String mergedInScheduleKey =
                FirebaseUtils.getStarredSessionInScheduleChildPath(UID, sessionId);
        assertThat((boolean) mHelper.getPendingFirebaseUpdatesMap().get(mergedInScheduleKey),
                is(inSchedule));
    }

    /**
     * Asserts that the timestamp for {@code sessionId} is stored.
     *
     * @param sessionId The ID of the starred session.
     */
    private void assertThatTimestampIsStored(String sessionId) {
        String timestampKey = FirebaseUtils.getStarredSessionTimestampChildPath(UID, sessionId);
        assertThat(mHelper.getPendingFirebaseUpdatesMap().keySet(), hasItem(timestampKey));
    }

    /**
     * Asserts that {@code videoId} is stored in merged viewed videos user data.
     *
     * @param videoId The Id of the viewed video.
     */
    private void assertThatMergedViewedVideosHas(String videoId) {
        assertThat(mHelper.getMergedUserData().getViewedVideoIds(), hasItem(videoId));
    }

    /**
     * Asserts that no video ID was stored in merged user data.
     */
    private void assertThatMergedUserDataHasNoVideoIds() {
        assertThat(mHelper.getMergedUserData().getViewedVideoIds(),
                is(Collections.<String>emptySet()));
    }

    /**
     * Asserts that {@code sessionId} is stored in merged feedback submitted sessions user data.
     *
     * @param sessionId The ID of the feedback submitted session.
     */
    private void assertThatMergeFeedbackSubmittedSessionsHas(String sessionId) {
        assertThat(mHelper.getMergedUserData().getFeedbackSubmittedSessionIds(),
                hasItem(sessionId));
    }
}