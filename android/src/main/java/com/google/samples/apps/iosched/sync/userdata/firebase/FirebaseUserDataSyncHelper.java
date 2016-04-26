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

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper;
import com.google.samples.apps.iosched.sync.userdata.UserAction;
import com.google.samples.apps.iosched.util.FirebaseUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
public class FirebaseUserDataSyncHelper extends AbstractUserDataSyncHelper
        implements FirebaseAuthCallbacks {
    private static final String TAG = makeLogTag(FirebaseUserDataSyncHelper.class);

    /**
     * Wait time for the Firebase sync to complete before we exit from {@code syncImpl()}.
     */
    public static final int AWAIT_TIMEOUT_IN_MILLISECONDS = 10000; // 10 seconds.

    /**
     * Tracks if the sync process changed local data.
     */
    private boolean mDataChanged = false;

    /**
     * Lock used to prevent {@code syncImpl()} from exiting before we're done syncing with
     * Firebase.
     */
    private CountDownLatch mCountDownLatch;

    /**
     * A list of {@link UserAction}s that are involved in the data sync.
     */
    private List<UserAction> mActions;

    /**
     * Constructor.
     *
     * @param context     The {@link Context}.
     * @param accountName The name associated with the currently chosen account.
     */
    public FirebaseUserDataSyncHelper(Context context, String accountName) {
        super(context, accountName);
    }

    @Override
    protected boolean syncImpl(final List<UserAction> actions, final boolean hasPendingLocalData) {
        mActions = actions;
        mCountDownLatch = new CountDownLatch(1);

        // The Firebase shard where data is synced.
        String firebaseUrl = FirebaseUtils.getFirebaseUrl(mContext, mAccountName);
        if (TextUtils.isEmpty(firebaseUrl)) {
            LOGW(TAG, "Cannot proceed with user data sync: Firebase url is not known.");
            return mDataChanged;
        }

        // The Firebase reference used to sync data.
        final Firebase firebaseRef = new Firebase(firebaseUrl);
        boolean authenticated = !TextUtils.isEmpty(FirebaseUtils.getFirebaseUid(mContext));

        if (authenticated) {
            LOGW(TAG, "Already authenticated with Firebase.");
            performSync(actions);
        } else {
            // Authenticate and wait for onAuthSucceeded() to fire before performing sync.
            new FirebaseAuthHelper(mContext, firebaseRef, this).authenticate();
        }

        try {
            // Make the current thread wait until we've heard back from Firebase.
            LOGW(TAG, "Waiting until the latch has counted down to zero");
            mCountDownLatch.await(AWAIT_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            LOGW(TAG, "Waiting thread awakened prematurely", exception);
        }
        LOGD(TAG, "local data changed after sync = " + mDataChanged);
        return mDataChanged;
    }

    /**
     * Syncs local data with remote data in Firebase. Assumes Firebase authentication has
     * successfully completed. See {@link FirebaseDataReconciler} for details on how remote and
     * local data is merged.
     *
     * @param actions The user actions that triggered the sync.
     */
    private void performSync(final List<UserAction> actions) {
        // Do a one-time read of Firebase data (equivalent to an asynchronous query call) located
        // at /<data_path>/<uid>/.
        FirebaseUtils.getDataUIDRef(mContext, mAccountName).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        FirebaseDataReconciler firebaseDataReconciler =
                                new FirebaseDataReconciler(mContext, mAccountName, actions,
                                        dataSnapshot);
                        firebaseDataReconciler.buildRemoteDataObject()
                                              .buildLocalDataObject()
                                              .merge()
                                              .updateRemote()
                                              .updateLocal();
                        FirebaseUserDataSyncHelper.this.mDataChanged =
                                firebaseDataReconciler.localDataChanged();
                        LOGW(TAG, "Done syncing with Firebase. Decrementing latch count.");
                        mCountDownLatch.countDown();
                    }

                    @Override
                    public void onCancelled(final FirebaseError firebaseError) {
                        LOGW(TAG, "firebaseError = " + firebaseError);
                        mCountDownLatch.countDown();
                    }
                });
    }

    @Override
    public void onAuthSucceeded(final String uid) {
        FirebaseUtils.setFirebaseUid(mContext, mAccountName, uid);
        performSync(mActions);
    }

    @Override
    public void onAuthFailed() {
        // Clear out the <uid> previously obtained from Firebase.
        FirebaseUtils.setFirebaseUid(mContext, mAccountName, "");
        mCountDownLatch.countDown();
        incrementIoExceptions();
    }
}