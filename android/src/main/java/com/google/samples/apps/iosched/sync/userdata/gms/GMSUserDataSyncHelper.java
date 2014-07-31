/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.sync.userdata.gms;

import android.content.Context;

import com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper;
import com.google.samples.apps.iosched.sync.userdata.UserAction;

import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;


/**
 * Helper class that syncs starred sessions data with Drive's AppData folder using GMS
 * support for Drive AppData folder.
 */
public class GMSUserDataSyncHelper extends AbstractUserDataSyncHelper {
    private static final String TAG = makeLogTag(GMSUserDataSyncHelper.class);

    public GMSUserDataSyncHelper(Context context, String accountName) {
        super(context, accountName);
    }

    @Override
    protected boolean syncImpl(List<UserAction> actions, boolean hasPendingLocalData) {
        /*
        DriveAppAsyncTask task = new DriveAppAsyncTask(getContext()) {
            @Override
            protected void onPostExecute(Boolean requiresUIRefresh) {
                Log.d(TAG, "Finished DriveAppAsyncTask. RequiresUIRefresh = " + requiresUIRefresh);
                updateSharedPreferences(getDriveID(), getLastModifiedDate());
                if (requiresUIRefresh != null && requiresUIRefresh) {
                    notifyDataHasChanged();
                }
                if (callback != null) {
                    callback.onSuccess();
                }
            }
        };
        task.execute(params);*/
        throw new RuntimeException("Unsupported implementation of GMSUserDataSyncHelper");
    }

}
