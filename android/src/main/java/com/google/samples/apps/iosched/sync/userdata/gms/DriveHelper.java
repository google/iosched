/*
 * Copyright 2015 Google Inc. All rights reserved.
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;
import com.google.samples.apps.iosched.util.IOUtils;

import java.io.IOException;
import java.util.Date;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A helper class for creating, fetching and editing Drive AppData files
 */
public class DriveHelper {

    private static final String TAG = makeLogTag(DriveHelper.class);

    private final GoogleApiClient mGoogleApiClient;

    /**
     * Construct the helper with a {@link GoogleApiClient} that is connected.
     *
     * @param apiClient The {@link GoogleApiClient} that is either connected or unconnected.
     */
    public DriveHelper(GoogleApiClient apiClient) {
        this.mGoogleApiClient = apiClient;
    }

    /**
     * Connect the {@link GoogleApiClient} if not already connected.
     * Note that this assumes you're already running in a background thread
     * and issues a {@code GoogleApiClient#blockingConnect()} call to connect.
     *
     * @return ConnectionResult or null if already connected.
     */
    public ConnectionResult connectIfNecessary() {
        if (!mGoogleApiClient.isConnected()) {
            return mGoogleApiClient.blockingConnect();
        } else {
            return null;
        }
    }

    /**
     * This is essential to ensure that the Google Play services cache is up-to-date.
     * Call {@link com.google.android.gms.drive.DriveApi#requestSync(GoogleApiClient)}
     *
     * @return {@link com.google.android.gms.common.api.Status}
     */
    public Status requestSync() {
        return Drive.DriveApi.requestSync(mGoogleApiClient).await();
    }

    /**
     * Get or create the {@link DriveFile} named with {@code fileName} with
     * the specific {@code mimeType}.
     *
     * @return Return the {@code DriveId} of the fetched or created file.
     */
    public DriveId getOrCreateFile(String fileName, String mimeType) {
        LOGD(TAG, "getOrCreateFile " + fileName + " mimeType " + mimeType);
        DriveId file = getDriveFile(fileName, mimeType);
        LOGD(TAG, "getDriveFile  returned " + file);
        if (file == null) {
            return createEmptyDriveFile(fileName, mimeType);
        } else {
            return file;
        }
    }

    /**
     * Save the {@code DriveFile} with the specific driveId.
     *
     * @param id {@link DriveId} of the file.
     * @param content The content to be saved in the {@code DriveFile}.
     * @return Return value indicates whether the save was successful.
     */
    public boolean saveDriveFile(DriveId id, String content) throws IOException {
        DriveFile theFile = Drive.DriveApi.getFile(mGoogleApiClient, id);
        DriveApi.DriveContentsResult result =
                theFile.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();

        try {
            IOUtils.writeToStream(content, result.getDriveContents().getOutputStream());
            // Update the last viewed.
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setLastViewedByMeDate(new Date())
                    .build();
            return result.getDriveContents().commit(mGoogleApiClient, changeSet)
                    .await().isSuccess();
        } catch (IOException io) {
            result.getDriveContents().discard(mGoogleApiClient);
            throw io;
        }
    }

    public String getContentsFromDrive(DriveId id) throws IOException {
        DriveFile theFile = Drive.DriveApi.getFile(mGoogleApiClient, id);
        DriveApi.DriveContentsResult result =
                theFile.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
        DriveContents driveContents = result.getDriveContents();
        try {
            if (driveContents != null) {
                return IOUtils.readAsString(driveContents.getInputStream());
            }
        } finally {
            if (driveContents != null) {
                driveContents.discard(mGoogleApiClient);
            }
        }
        return null;
    }

    /**
     * Create an empty file with the given {@code fileName} and {@code mimeType}.
     *
     * @return {@link DriveId} of the specific file.
     */
    private DriveId createEmptyDriveFile(String fileName, String mimeType) {
        DriveApi.DriveContentsResult result =
                Drive.DriveApi.newDriveContents(mGoogleApiClient).await();

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(fileName)
                .setMimeType(mimeType)
                .setStarred(true)
                .build();

        // Create a new file with the given changeSet in the AppData folder.
        DriveFolder.DriveFileResult driveFileResult = Drive.DriveApi.getAppFolder(mGoogleApiClient)
                .createFile(mGoogleApiClient, changeSet, result.getDriveContents())
                .await();
        return driveFileResult.getDriveFile().getDriveId();
    }

    /**
     * Search for a file with the specific name and mimeType
     * @return driveId for the file it if exists.
     */
    private DriveId getDriveFile(String fileName, String mimeType) {
        // Find the named file with the specific Mime type.
        Query query = new Query.Builder()
                .addFilter(Filters.and(
                        Filters.eq(SearchableField.TITLE, fileName),
                        Filters.eq(SearchableField.MIME_TYPE, mimeType)))
                .setSortOrder(new SortOrder.Builder()
                        .addSortDescending(SortableField.MODIFIED_DATE)
                        .build())
                .build();

        MetadataBuffer buffer = null;
        try {
            buffer = Drive.DriveApi.getAppFolder(mGoogleApiClient)
                    .queryChildren(mGoogleApiClient, query).await().getMetadataBuffer();

            if (buffer != null && buffer.getCount() > 0) {
                LOGD(TAG, "got buffer " + buffer.getCount());
                return buffer.get(0).getDriveId();
            }
            return null;
        } finally {
            if (buffer != null) {
                buffer.close();
            }
        }
    }
}
