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
import android.text.TextUtils;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.FirebaseException;
import com.firebase.client.ValueEventListener;
import com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper;
import com.google.samples.apps.iosched.sync.userdata.UserAction;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.FirebaseUtils;

import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Performs Firebase authentication if necessary and syncs local data with remote data in Firebase.
 * This class queries live Firebase data to perform a sync, explicitly not relying on the {@link
 * Firebase#keepSynced(boolean)} functionality in order to have more control over conflict
 * resolution for different types of data being stored.
 */
public class FirebaseUserDataSyncHelper extends AbstractUserDataSyncHelper {
    private static final String TAG = makeLogTag(FirebaseUserDataSyncHelper.class);

    /**
     * The Firebase reference used to sync data.
     */
    private Firebase mFirebaseRef;

    /**
     * Tracks if the sync process changes local data.
     */
    private boolean mDataChanged = false;

    public FirebaseUserDataSyncHelper(Context context, String accountName) {
        super(context, accountName);
    }

    @Override
    protected boolean syncImpl(final List<UserAction> actions, final boolean hasPendingLocalData) {
        // The Firebase shard where data is synced.
        String firebaseUrl = FirebaseUtils.getFirebaseUrl(mContext, mAccountName);

        if (TextUtils.isEmpty(firebaseUrl)) {
            LOGW(TAG, "Cannot proceed with user data sync: Firebase url is not known.");
            return mDataChanged;
        }

        mFirebaseRef = new Firebase(firebaseUrl);
        if (!TextUtils.isEmpty(FirebaseUtils.getFirebaseUid(mContext))) {
            LOGI(TAG, "Already authenticated with Firebase.");
            performFirebaseSync(actions, hasPendingLocalData);
        } else {
            try {
                LOGI(TAG, "Attempting Firebase auth.");
                mFirebaseRef.authWithOAuthToken(AccountUtils.DEFAULT_OAUTH_PROVIDER,
                        AccountUtils.getAuthToken(mContext),
                        new Firebase.AuthResultHandler() {
                            @Override
                            public void onAuthenticated(AuthData authData) {
                                String uid = authData.getUid();
                                LOGI(TAG, "Firebase auth succeeded");
                                FirebaseUtils.setFirebaseUid(mContext, mAccountName, uid);
                                performFirebaseSync(actions, hasPendingLocalData);
                            }

                            @Override
                            public void onAuthenticationError(FirebaseError firebaseError) {
                                LOGW(TAG, "Firebase auth error: " + firebaseError);
                            }
                        });
            } catch (FirebaseException e) {
                LOGW(TAG, "Firebase auth error", e);
            } catch (IllegalArgumentException e) {
                LOGW(TAG, "OAuth token is null", e);
            } catch (IllegalStateException e) {
                LOGW(TAG, "Illegal state", e);
            }
        }
        return mDataChanged;
    }

    /**
     * Syncs local data with remote data in Firebase. Checks if there is a remote GCM key, and if it
     * exists, overrides the local GCM key stored in {@link android.content.SharedPreferences}. See
     * {@link BaseActivity#registerGCMClient()} for how the GCM key is generated and what function
     * it serves.
     *
     * @param actions The user actions that triggered the sync.
     */
    private void performFirebaseSync(final List<UserAction> actions,
            final boolean hasPendingLocalData) {

        final String uid = FirebaseUtils.getFirebaseUid(mContext);
        final Firebase userDataRef = FirebaseUtils.getUserDataRef(mFirebaseRef, uid);

        // Do a one-time read of Firebase data (equivalent to an asynchronously query call).
        userDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                String remoteGcmKey = (String) dataSnapshot.child(
                        FirebaseUtils.FIREBASE_NODE_GCM_KEY).getValue();
                String localGcmKey =
                        AccountUtils.getGcmKey(mContext, mAccountName);
                LOGD(TAG, "Local GCM key: " +
                        AccountUtils.sanitizeGcmKey(localGcmKey));
                LOGD(TAG, "Remote GCM key: " + (remoteGcmKey == null ? "(null)"
                        : AccountUtils.sanitizeGcmKey(remoteGcmKey)));

                if (TextUtils.isEmpty(remoteGcmKey)) {
                    // Write local GCM key to Firebase.
                    userDataRef.child(FirebaseUtils.FIREBASE_NODE_GCM_KEY).setValue(localGcmKey);
                } else if (remoteGcmKey.equals(localGcmKey)) {
                    LOGI(TAG, "Remote GCM key is the same as local, so no action necessary.");
                } else {
                    LOGI(TAG, "Remote GCM key is different from local. OVERRIDING local.");
                    localGcmKey = remoteGcmKey;
                    AccountUtils.setGcmKey(mContext, mAccountName, localGcmKey);
                    FirebaseUserDataSyncHelper.this.mDataChanged = true;
                }
            }

            @Override
            public void onCancelled(final FirebaseError firebaseError) {
                LOGW(TAG, "firebaseError = " + firebaseError);
            }
        });
    }
}
