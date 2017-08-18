/*
 * Copyright 2016 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.sync.userdata;

import android.content.Context;

import com.google.samples.apps.iosched.rpc.userdata.Userdata;
import com.google.samples.apps.iosched.rpc.userdata.model.UserData;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.io.IOException;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * Responsible for managing user data sync. Fetches user data stored in a remote endpoint as well
 * as user data stored in the local SQLite DB. Reconciles differences between the two and
 * optionally updates the remote and local data sources.
 */
class UserDataSyncHelper extends AbstractUserDataSyncHelper {
    private static final String TAG = makeLogTag(UserDataSyncHelper.class);

    /**
     * Tracks if the sync process changed local data.
     */
    private boolean mDataChanged = false;

    private UserDataModel mRemoteUserDataModel;
    private UserDataModel mLocalUserDataModel;
    private UserDataModel mReconciledUserDataModel;

    /**
     * Hook for reading and writing to the remote endpoint.
     */
    private Userdata mUserdataHandler;

    /**
     * Constructor.
     *
     * @param context     The {@link Context}.
     * @param accountName The name associated with the currently chosen account.
     */
    UserDataSyncHelper(final Context context, final String accountName) {
        super(context, accountName);
        mRemoteUserDataModel = new UserDataModel();
    }

    @Override
    protected boolean syncImpl(final List<UserAction> actions, final boolean hasPendingLocalData) {
        performSync(actions);
        return mDataChanged;
    }

    private void performSync(final List<UserAction> actions) {
        mUserdataHandler = RemoteUserDataHelper.getUserdataHandler(mContext);
        try {
            UserData remoteUserDataJson = mUserdataHandler.getAll().execute();
            buildRemoteData(remoteUserDataJson);
            buildLocalData(actions);
            reconcileRemoteAndLocal();
        } catch (IOException e) {
            LOGW(TAG, "Failed to get user data from remote." + e);
        }
    }

    private void buildRemoteData(UserData remoteUserDataJson) {
        mRemoteUserDataModel.setStarredSessions(
                RemoteUserDataHelper.getRemoteBookmarkedSessions(remoteUserDataJson));

        mRemoteUserDataModel.setReservedSessions(
                RemoteUserDataHelper.getRemoteReservedSessions(remoteUserDataJson));

        mRemoteUserDataModel.setFeedbackSubmittedSessionIds(
                RemoteUserDataHelper.getRemoteReviewedSessions(remoteUserDataJson));
    }

    private void buildLocalData(final List<UserAction> actions) {
        mLocalUserDataModel = LocalUserDataHelper.getUserData(actions);
    }

    private void reconcileRemoteAndLocal() {
        mReconciledUserDataModel = UserDataModel.reconciledUserData(mLocalUserDataModel,
                mRemoteUserDataModel);
        if (!mReconciledUserDataModel.equals(mLocalUserDataModel)) {
            mDataChanged = true;
            updateLocal();
        }
        if (!mReconciledUserDataModel.equals(mRemoteUserDataModel)) {
            updateRemote();
        }
    }

    private void updateLocal() {
        String accountName = AccountUtils.getActiveAccountName(mContext);
        LocalUserDataHelper.setLocalUserData(mContext, mReconciledUserDataModel, accountName);
    }

    private void updateRemote() {
        try {
            mUserdataHandler.updateUser(RemoteUserDataHelper.asUserData(mReconciledUserDataModel))
                    .execute();
        } catch (IOException e) {
            LOGW(TAG, "Could not update remote with reconciled user data: " + e);
        }
    }
}
