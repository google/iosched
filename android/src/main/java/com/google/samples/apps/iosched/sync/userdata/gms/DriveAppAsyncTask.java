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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.*;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 *
 * Async task that syncs a file with the Drive AppData folder.
 *
 **/
public class DriveAppAsyncTask extends ApiClientAsyncTask<Void, Void, Boolean> {

    public DriveAppAsyncTask(Context context) {
        super(context);
    }

    /*
    private static final String TAG = makeLogTag(DriveAppAsyncTask.class);
    private SyncParams mParams;

    protected String getDriveID() {
        return mParams == null ? null : mParams.getDriveId();
    }

    protected Date getLastModifiedDate() {
        return mParams == null ? null : mParams.getLastModifiedDate();
    }

*/
    @Override
    protected Boolean doInBackgroundConnected(Void... params) {
      return true;
    }
    /*
        Log.d(TAG, "on DriveAppAsyncTask");
        this.mParams = params[0];
        GoogleApiClient apiClient = getGoogleApiClient();
        Set<String> ourContent = UserDataHelper.getLocalStarredSessionIDs(getContext());

        boolean requiresUIRefresh = false;

        DriveId currentDriveId = mParams.getDriveId() == null ? null : DriveId.decodeFromString(mParams.getDriveId());
        DriveFile file = DriveHelper.lookupDriveFile(currentDriveId, apiClient);
        mParams.setDriveId(file == null ? null : file.getDriveId().encodeToString());


        try {
            if (file == null)  {
                // File doesn't exist in Drive
                Log.d(TAG, "Creating file on Drive");
                DriveHelper.createNewDriveFile(mParams, ourContent, apiClient);

            } else {
                // File exists in Drive
                boolean requiresCloudUpdate = false;

                /** It seems that, due to a bug on AppData GMS implementation, the metadata is not
                 * being updated correctly when something changes in the cloud. So, we are removing
                 * the fancy logic and keeping it to the bare minimum.
                // Compare last modified date
                Date lastModifiedCloud = metadata.getModifiedDate();
                Log.d(TAG, "Found file in Drive  ID="+file.getDriveId()+
                        "  cloud_last_modified="+lastModifiedCloud+
                        "  local_last_modified="+param.getLastModifiedDate());
                int dataCmp = lastModifiedCloud.compareTo(param.getLastModifiedDate());
                Log.d(TAG, "dataCmp="+dataCmp+"  hasPendingActions="+param.hasPendingActions());

                if (dataCmp > 0) {
                 * * /
                    // If cloud file is newer than ours, merge our content there
                    Log.d(TAG, "File in cloud is newer than ours. Maybe merging.");
                    Set<String> cloudContent = DriveHelper.loadFromCloud(file, apiClient);
                    if (mParams.hasPendingActions()) {
                        // apply our pending actions on top of Drive contents
                        Log.d(TAG, "Local pending actions, applying to the remote file.");
                        if (!cloudContent.equals(ourContent)) {
                            for (UserAction action: mParams.getPendingActions()) {
                                Log.d(TAG, "Applying action "+action);
                                if (action.type == UserAction.TYPE.ADD_STAR) {
                                    cloudContent.add(action.sessionId);
                                } else {
                                    cloudContent.remove(action.sessionId);
                                }
                            }
                            requiresCloudUpdate = true;
                        }
                    }

                    if (!cloudContent.equals(ourContent)) {
                        ourContent = cloudContent;
                        UserDataHelper.setLocalStarredSessions(getContext(), cloudContent);
                        requiresUIRefresh = true;
                    }

                /**
                } else if (dataCmp < 0 || ( dataCmp == 0 && param.hasPendingActions())) {
                    // Replace Drive contents with our content
                    Log.d(TAG, "File in cloud is behind ours. Will replace it.");
                    requiresCloudUpdate = true;
                }
                * * /
                if (requiresCloudUpdate) {
                    DriveHelper.saveToDrive(file, ourContent, apiClient);
                    //lastModifiedCloud = getLastModifiedDate(file, apiClient);
                }
                // param.setLastModifiedDate(lastModifiedCloud);
                mParams.setLastModifiedDate(new Date());
            }

        } catch (IOException e) {
            Log.e(TAG, "IOException while setting content to the new file", e);
            throw new RuntimeException("IOException while setting content to the new file", e);
        }

        return requiresUIRefresh;
    }

    private Date getLastModifiedDate(DriveFile file, GoogleApiClient apiClient) {
        DriveResource.MetadataResult result = file.getMetadata(apiClient).await();
        DriveHelper.checkStatus("Getting last modified date", result.getStatus());
        return result.getMetadata().getModifiedDate();
    }

*/
}
