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
package com.google.samples.apps.iosched.service;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.io.JSONHandler;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.sync.ConferenceDataHandler;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.util.LogUtils;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;

/**
 * An {@code IntentService} that performs the one-time data bootstrap from a local file,
 * overwriting
 * any existing data.
 *
 * It takes the file "bootstrap_data.json" from external storage, and populates the database.
 * The format of this file should be identical to the boostrap json file packaged with the app.
 * This data contains the sessions, speakers, etc.
 * Note that external data fetching should be disabled, otherwise data will be overwritten by the
 *
 * @see DataBootstrapService
 */
public class LocalRefreshingBootstrapService extends IntentService {

    private static final String TAG = LogUtils.makeLogTag(LocalRefreshingBootstrapService.class);

    private static final String BOOTSTRAP_FILE =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/bootstrap_data.json";

    /**
     * Start the {@link DataBootstrapService}, even if the bootstrap has already been done and the
     * database already contains data.
     *
     * @param context The context for starting the {@link IntentService}.
     */
    public static void startDataBootstrap(Context context) {
        LOGW(TAG, "Starting destructive bootstrap process.");
        context.startService(new Intent(context, LocalRefreshingBootstrapService.class));
    }

    /**
     * Creates a LocalRefreshingBootstrapService.
     */
    public LocalRefreshingBootstrapService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context appContext = getApplicationContext();

        try {
            // Load data from bootstrap a local file

            LOGD(TAG, "Starting full data bootstrap from file: " + BOOTSTRAP_FILE);

            if (!new File(BOOTSTRAP_FILE).canRead()) {
                LOGE(TAG, "File " + BOOTSTRAP_FILE + " could not be read. No bootstrap possible.");
            }

            String bootstrapJson = JSONHandler.parseFile(BOOTSTRAP_FILE);

            // Apply the data we read to the database with the help of the ConferenceDataHandler.
            ConferenceDataHandler dataHandler = new ConferenceDataHandler(appContext);

            dataHandler.applyConferenceData(new String[]{bootstrapJson},
                    BuildConfig.BOOTSTRAP_DATA_TIMESTAMP, false);

            SyncHelper.performPostSyncChores(appContext);

            LOGI(TAG, "End of bootstrap -- successful. Marking bootstrap as done.");
            SettingsUtils.markSyncSucceededNow(appContext);
            SettingsUtils.markDataBootstrapDone(appContext);

            getContentResolver().notifyChange(Uri.parse(ScheduleContract.CONTENT_AUTHORITY),
                    null, false);

        } catch (IOException ex) {
            // This is serious -- if this happens, the app won't work :-(
            // This is unlikely to happen in production, but IF it does, we apply
            // this workaround as a fallback: we pretend we managed to do the bootstrap
            // and hope that a remote sync will work.
            LOGE(TAG, "*** ERROR DURING BOOTSTRAP! Problem in bootstrap data?", ex);
            LOGE(TAG,
                    "Applying fallback -- marking boostrap as done; sync might fix problem.");
            SettingsUtils.markDataBootstrapDone(appContext);
        } finally {
            // Request a manual sync immediately after the bootstrapping process, in case we
            // have an active connection. Otherwise, the scheduled sync could take a while.
            //SyncHelper.requestManualSync(AccountUtils.getActiveAccount(appContext));
        }
    }
}
