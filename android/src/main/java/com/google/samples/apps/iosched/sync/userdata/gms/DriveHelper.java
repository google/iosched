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

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.*;
import com.google.api.client.util.Charsets;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper.*;
import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class DriveHelper {

    private static final String TAG = makeLogTag(DriveHelper.class);

    // Constants related to the JSON serialization:
    private static final String MIMETYPE_JSON = "application/json";
    public static final String DRIVE_FILENAME = "starred_sessions.json";

    static void saveToDrive(DriveFile file, Set<String> contents,
            GoogleApiClient apiClient) throws IOException {
        DriveApi.ContentsResult contentsResult = file.openContents(apiClient,
                DriveFile.MODE_WRITE_ONLY, null).await();
        checkStatus("Open file for writing", contentsResult.getStatus());
        FileOutputStream os = new FileOutputStream(contentsResult.getContents()
                .getParcelFileDescriptor().getFileDescriptor());
        byte[] serializedContents = toByteArray(contents);
        Log.d(TAG, "Saving contents to drive file: "+new String(serializedContents));
        os.write(serializedContents);
        com.google.android.gms.common.api.Status status =
                file.commitAndCloseContents(apiClient, contentsResult.getContents()).await();
        checkStatus("Commit file contents", status);
    }

    static public DriveFile lookupDriveFile(DriveId driveId, GoogleApiClient apiClient) {
        DriveFile result = null;

        // First, check if ID is valid
        if (driveId != null) {
            Log.d(TAG, "DriveID passed is not null, trying to get the corresponding file");
            try {
                result = Drive.DriveApi.getFile(apiClient, driveId);
                if (result != null)  {
                    // check if metadata is ok. For example, if the file has been directly removed from
                    // the server, the getFile can return a file that is actually not valid. Hopefully
                    // the metadata will get the correct info
                    try {
                        DriveResource.MetadataResult metadataResult = result.getMetadata(apiClient).await();
                        if (!metadataResult.getStatus().isSuccess()) {
                            result = null;
                        }
                    } catch (Exception ex) {
                        result = null;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Saved drive ID "+driveId+" seems to be invalid (message: " +
                        e.getMessage()+"). Ignoring it");
                result = null;
            }
        }

        if (result == null) {
            // search for a file with the expected name (and get the most recent one, if many)
            Log.d(TAG, "DriveID passed is null, looking up for a file named "+DRIVE_FILENAME);
            Metadata metaOfMostRecent = null;
            MetadataBuffer buffer = Drive.DriveApi.getAppFolder(apiClient)
                    .listChildren(apiClient).await().getMetadataBuffer();
            Log.d(TAG, "Found "+buffer.getCount()+" files");
            for (Metadata metadata: buffer) {
                if (metaOfMostRecent != null) {
                    Log.w(TAG, "Warning, found more than one file named "+DRIVE_FILENAME+
                            " in AppData folder. Using the most recently modified.");
                }
                if (metaOfMostRecent == null || metaOfMostRecent
                        .getModifiedDate().compareTo(metadata.getModifiedDate())<0) {
                    metaOfMostRecent = metadata;
                }
            }
            if (metaOfMostRecent != null) {
                driveId = metaOfMostRecent.getDriveId();
                result = Drive.DriveApi.getFile(apiClient, driveId);
            }
            buffer.close();
        }

        return result;
    }

    static void createNewDriveFile(Set<String> contents,
            GoogleApiClient apiClient) throws IOException {

        DriveApi.ContentsResult contentsResult = Drive.DriveApi.newContents(apiClient).await();
        checkStatus("creating new file", contentsResult.getStatus());

        //   query Drive for an AppFolder reference (might be slow: ~4s in my tests)
        DriveFolder appDataFolder = Drive.DriveApi.getAppFolder(apiClient);

        //   create a new file in AppFolder
        MetadataChangeSet metadataChangeSet =
                new MetadataChangeSet.Builder()
                        .setMimeType(MIMETYPE_JSON)
                        .setTitle(DRIVE_FILENAME)
                        .build();
        Contents contentsObj = contentsResult.getContents();

        FileOutputStream os = new FileOutputStream(contentsObj.getParcelFileDescriptor().getFileDescriptor());
        os.write(toByteArray(contents));

        DriveFolder.DriveFileResult fileResult = appDataFolder.createFile(
                apiClient, metadataChangeSet, contentsResult.getContents()).await();

        Log.d(TAG, "Content saved to new Drive file: "+new String(toByteArray(contents),
                Charsets.UTF_8));
        checkStatus("saving contents to new file", fileResult.getStatus());

        // DON'T DO THIS: It seems that a bug makes this driveID being unusable later:
        // params.setDriveId(fileResult.getDriveFile().getDriveId());
    }

    static public void checkStatus(String message, com.google.android.gms.common.api.Status status) {
        if (!status.isSuccess()) {
            throw new RuntimeException("Error "+status.getStatusCode()+" on "+message);
        }
    }

    static public Set<String> loadFromCloud(DriveFile file, GoogleApiClient apiClient)
            throws IOException {
        DriveApi.ContentsResult contentsResult = file.openContents(apiClient,
                DriveFile.MODE_READ_ONLY, null).await();
        checkStatus("Open file for reading", contentsResult.getStatus());

        HashSet<String> result = new HashSet<String>();
        try {
            FileInputStream is = new FileInputStream(contentsResult.getContents()
                    .getParcelFileDescriptor().getFileDescriptor());
            String contents = fromStreamToString(is);
            file.discardContents(apiClient, contentsResult.getContents());

            LOGD(TAG, "Contents in the cloud file: [" + contents + "]");

            return fromString(contents);

        } catch (Exception ex) {
            Log.w(TAG, "Ignoring invalid remote content.", ex);
            return null;
        }
    }

}
