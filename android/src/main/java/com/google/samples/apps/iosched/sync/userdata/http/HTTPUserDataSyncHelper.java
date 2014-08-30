/**
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

package com.google.samples.apps.iosched.sync.userdata.http;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.samples.apps.iosched.port.superbus.SyncAllRsvpsCommand;
import com.google.samples.apps.iosched.port.tasks.AllRsvpsRequest;
import com.google.samples.apps.iosched.port.tasks.AppPrefs;
import com.google.samples.apps.iosched.port.tasks.DataHelper;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper;
import com.google.samples.apps.iosched.sync.userdata.UserAction;
import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.touchlab.android.superbus.appsupport.CommandBusHelper;
import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;
import retrofit.RestAdapter;
import retrofit.client.Response;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Helper class that syncs starred sessions data with Drive's AppData folder using direct
 * HTTP Drive API through google-api-client library.
 *
 * Based on https://github.com/googledrive/appdatapreferences-android
 */
public class HTTPUserDataSyncHelper extends AbstractUserDataSyncHelper
{

    private static final String GCM_KEY_PREFIX = "GCM:";

    public HTTPUserDataSyncHelper(Context context, String accountName) {
        super(context, accountName);
    }

    protected boolean syncImpl(List<UserAction> actions, boolean hasPendingLocalData)
    {
        if(AppPrefs.getInstance(mContext).isLoggedIn())
            CommandBusHelper.submitCommandSync(mContext, new SyncAllRsvpsCommand(mAccountName));
        return false;
        /*try {
            LOGD(TAG, "Now syncing user data.");
            Set<String> remote = UserDataHelper.fromString(fetchRemote());
            Set<String> local = UserDataHelper.getSessionIDs(actions);

            // If remote data is the same as local, and the remote end already has a GCM key,
            // there is nothing we need to do.
            if (remote.equals(local)) {
                LOGD(TAG, "Update is not needed (local is same as remote, and remote has key)");
                return false;
            }

            Set<String> merged;
            if (hasPendingLocalData || TextUtils.isEmpty(remoteGcmKey)) {
                // merge local dirty actions into remote content
                if (hasPendingLocalData) {
                    LOGD(TAG, "Has pending local data, merging.");
                    merged = mergeDirtyActions(actions, remote);
                } else {
                    LOGD(TAG, "No pending local data, just updating remote GCM key.");
                    merged = remote;
                }
                // add the GCM key special item
                merged.add(GCM_KEY_PREFIX + localGcmKey);
                // save to remote
                LOGD(TAG, "Sending user data to Drive, gcm key "
                        + AccountUtils.sanitizeGcmKey(localGcmKey));
                new UpdateFileDriveTask(getDriveService()).execute(
                        UserDataHelper.toSessionsString(merged));
            } else {
                merged = remote;
            }

            UserDataHelper.setLocalStarredSessions(mContext, merged, mAccountName);
            return true;
        } catch (IOException e) {
            handleException(e);
        }
        return false;*/
    }

    private Set<String> mergeDirtyActions(List<UserAction> actions, Set<String> starredSessions)
            throws IOException {
        // apply "dirty" actions:
        for (UserAction action: actions) {
            if (action.requiresSync) {
                if (UserAction.TYPE.ADD_STAR.equals(action.type)) {
                    starredSessions.add(action.sessionId);
                } else {
                    starredSessions.remove(action.sessionId);
                }
            }
        }
        return starredSessions;
    }




    private void handleException(Exception exception) {
        Log.e(TAG, "Could not sync myschedule", exception);
        if (exception != null && exception instanceof UserRecoverableAuthIOException) {
            throw new SyncHelper.AuthException();
        }
    }

    private static final String TAG = makeLogTag(HTTPUserDataSyncHelper.class);

}
