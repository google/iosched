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

import android.support.annotation.NonNull;

import com.firebase.client.Firebase;
import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;
import com.google.samples.apps.iosched.util.FirebaseUtils;

import java.util.Map;

/**
 * Helper class that prepares data so that multiple Firebase nodes can be updated with a single
 * write using {@link com.firebase.client.Firebase#updateChildren(Map)}.
 */
public class UpdateRemoteHelper {
    /**
     * Holds data obtained by merging data from the local DB and Firebase.
     */
    final UserDataHelper.UserData mMergedUserData;

    /**
     * Stores pending Firebase updates so that they can be written using a single call to {@link
     * Firebase#updateChildren(Map)}. The keys are String paths relative to Firebase root, and the
     * values are the data that is written to those paths.
     */
    Map<String, Object> mPendingFirebaseUpdatesMap;

    public UpdateRemoteHelper(@NonNull UserDataHelper.UserData mergedUserData,
            Map<String, Object> pendingFirebaseUpdatesMap) {
        this.mMergedUserData = mergedUserData;
        this.mPendingFirebaseUpdatesMap = pendingFirebaseUpdatesMap;
    }

    /**
     * Stores pending Firebase updates so that multiple Firebase child nodes can be updated in a
     * single write using {@link Firebase#updateChildren(Map)}.
     */
    public void updatePendingFirebaseUpdatesMap() {
        mPendingFirebaseUpdatesMap.put(FirebaseUtils.FIREBASE_NODE_GCM_KEY,
                mMergedUserData.getGcmKey());

        for (String videoID : mMergedUserData.getViewedVideoIds()) {
            mPendingFirebaseUpdatesMap.put(FirebaseUtils.getViewedVideoChildPath(videoID), true);
        }
    }
}
