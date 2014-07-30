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

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper;
import com.google.samples.apps.iosched.sync.userdata.UserAction;
import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Helper class that syncs starred sessions data with Drive's AppData folder using direct
 * HTTP Drive API through google-api-client library.
 *
 * Based on https://github.com/googledrive/appdatapreferences-android
 */
public class HTTPUserDataSyncHelper extends AbstractUserDataSyncHelper {
    private static final String GCM_KEY_PREFIX = "GCM:";

    private GoogleAccountCredential mCredentials;

    /**
     * Private {@code HTTPUserDataSyncHelper} constructor.
     * @param context Context of the application
     */
    public HTTPUserDataSyncHelper(Context context, String accountName) {
        super(context, accountName);
        mCredentials = GoogleAccountCredential.usingOAuth2(mContext, 
                java.util.Arrays.asList(DriveScopes.DRIVE_APPDATA));
        mCredentials.setSelectedAccountName(mAccountName);
    }

    private String extractGcmKey(Set<String> remote) {
        String remoteGcmKey = null;
        Set<String> toRemove = new HashSet<String>();
        for (String s : remote) {
            if (s.startsWith(GCM_KEY_PREFIX)) {
                toRemove.add(s);
                remoteGcmKey = s.substring(GCM_KEY_PREFIX.length());
                LOGD(TAG, "Remote data came with GCM key: "
                        + AccountUtils.sanitizeGcmKey(remoteGcmKey));
            }
        }
        for (String s : toRemove) {
            remote.remove(s);
        }
        return remoteGcmKey;
    }

    /**
     * Syncs the preferences file with an appdata preferences file.
     *
     * Synchronization steps:
     * 1. If there are local changes, sync the latest local version with remote
     *    and ignore merge conflicts. The last write wins.
     * 2. If there are no local changes, fetch the latest remote version. If
     *    it includes changes, notify that preferences have changed.
     */
    protected boolean syncImpl(List<UserAction> actions, boolean hasPendingLocalData) {
        try {
            LOGD(TAG, "Now syncing user data.");
            Set<String> remote = UserDataHelper.fromString(fetchRemote());
            Set<String> local = UserDataHelper.getSessionIDs(actions);

            String remoteGcmKey = extractGcmKey(remote);
            String localGcmKey = AccountUtils.getGcmKey(mContext, mAccountName);
            LOGD(TAG, "Local GCM key: " + AccountUtils.sanitizeGcmKey(localGcmKey));
            LOGD(TAG, "Remote GCM key: " + (remoteGcmKey == null ? "(null)"
                    : AccountUtils.sanitizeGcmKey(remoteGcmKey)));

            // if the remote data came with a GCM key, it should override ours
            if (!TextUtils.isEmpty(remoteGcmKey)) {
                if (remoteGcmKey.equals(localGcmKey)) {
                    LOGD(TAG, "Remote GCM key is the same as local, so no action necessary.");
                } else {
                    LOGD(TAG, "Remote GCM key is different from local. OVERRIDING local.");
                    localGcmKey = remoteGcmKey;
                    AccountUtils.setGcmKey(mContext, mAccountName, localGcmKey);
                }
            }

            // If remote data is the same as local, and the remote end already has a GCM key,
            // there is nothing we need to do.
            if (remote.equals(local) && !TextUtils.isEmpty(remoteGcmKey)) {
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
        return false;
    }

    /**
     * Constructs a Drive service in the current context and with the
     * credentials use to initiate AppdataPreferences instance.
     * @return Drive service instance.
     */
    public Drive getDriveService() {
        Drive service = new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                new GsonFactory(), mCredentials)
                .setApplicationName(mContext.getApplicationInfo().name)
                .build();
        return service;
    }

    /**
     * Updates the remote preferences file with the given JSON content.
     * @throws IOException
     */
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

    /**
     * Fetches the remote file.
     * @throws IOException
     */
    private String fetchRemote() throws IOException {
        String json = new GetOrCreateFIleDriveTask(getDriveService()).execute();
        Log.v(TAG, "Got this content from remote myschedule: ["+json+"]");
        return json;
    }


    /**
     * Handles API exceptions and notifies OnExceptionListener
     * if given exception is a UserRecoverableAuthIOException.
     * @param exception Exception to handle
     */
    private void handleException(Exception exception) {
        Log.e(TAG, "Could not sync myschedule", exception);
        if (exception != null && exception instanceof UserRecoverableAuthIOException) {
            throw new SyncHelper.AuthException();
        }
    }

    private static final String TAG = makeLogTag(HTTPUserDataSyncHelper.class);

}
