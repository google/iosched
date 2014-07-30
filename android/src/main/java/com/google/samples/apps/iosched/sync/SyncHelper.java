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

package com.google.samples.apps.iosched.sync;

import android.accounts.Account;
import android.content.*;
import android.net.ConnectivityManager;
import android.os.Bundle;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.service.SessionAlarmService;
import com.google.samples.apps.iosched.service.SessionCalendarService;
import com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper;
import com.google.samples.apps.iosched.sync.userdata.UserDataSyncHelperFactory;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.PrefUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.io.IOException;

import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * A helper class for dealing with conference data synchronization.
 * All operations occur on the thread they're called from, so it's best to wrap
 * calls in an {@link android.os.AsyncTask}, or better yet, a
 * {@link android.app.Service}.
 */
public class SyncHelper {
    private static final String TAG = makeLogTag("SyncHelper");

    private Context mContext;
    private ConferenceDataHandler mConferenceDataHandler;
    private RemoteConferenceDataFetcher mRemoteDataFetcher;

    public SyncHelper(Context context) {
        mContext = context;
        mConferenceDataHandler = new ConferenceDataHandler(mContext);
        mRemoteDataFetcher = new RemoteConferenceDataFetcher(mContext);
    }

    public static void requestManualSync(Account mChosenAccount) {
        requestManualSync(mChosenAccount, false);
    }
    public static void requestManualSync(Account mChosenAccount, boolean userDataSyncOnly) {
        if (mChosenAccount != null) {
            LOGD(TAG, "Requesting manual sync for account " + mChosenAccount.name
                    +" userDataSyncOnly="+userDataSyncOnly);
            Bundle b = new Bundle();
            b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            if (userDataSyncOnly) {
                b.putBoolean(SyncAdapter.EXTRA_SYNC_USER_DATA_ONLY, true);
            }
            ContentResolver.setSyncAutomatically(mChosenAccount, ScheduleContract.CONTENT_AUTHORITY, true);
            ContentResolver.setIsSyncable(mChosenAccount, ScheduleContract.CONTENT_AUTHORITY, 1);

            boolean pending = ContentResolver.isSyncPending(mChosenAccount,
                    ScheduleContract.CONTENT_AUTHORITY);
            if (pending) {
                LOGD(TAG, "Warning: sync is PENDING. Will cancel.");
            }
            boolean active = ContentResolver.isSyncActive(mChosenAccount,
                    ScheduleContract.CONTENT_AUTHORITY);
            if (active) {
                LOGD(TAG, "Warning: sync is ACTIVE. Will cancel.");
            }

            if (pending || active) {
                LOGD(TAG, "Cancelling previously pending/active sync.");
                ContentResolver.cancelSync(mChosenAccount, ScheduleContract.CONTENT_AUTHORITY);
            }

            LOGD(TAG, "Requesting sync now.");
            ContentResolver.requestSync(mChosenAccount, ScheduleContract.CONTENT_AUTHORITY, b);
        } else {
            LOGD(TAG, "Can't request manual sync -- no chosen account.");
        }
    }

    /**
     * Attempts to perform conference data synchronization. The data comes from the remote URL
     * configured in {@link com.google.samples.apps.iosched.Config#MANIFEST_URL}. The remote URL
     * must point to a manifest file that, in turn, can reference other files. For more details
     * about conference data synchronization, refer to the documentation at
     * http://code.google.com/p/iosched.
     *
     * @param syncResult (optional) the sync result object to update with statistics.
     * @param account    the account associated with this sync
     * @return Whether or not the synchronization made any changes to the data.
     */
    public boolean performSync(SyncResult syncResult, Account account, Bundle extras) {
        boolean dataChanged = false;

        if (!PrefUtils.isDataBootstrapDone(mContext)) {
            LOGD(TAG, "Sync aborting (data bootstrap not done yet)");
            return false;
        }

        long lastAttemptTime = PrefUtils.getLastSyncAttemptedTime(mContext);
        long now = UIUtils.getCurrentTime(mContext);
        long timeSinceAttempt = now - lastAttemptTime;
        final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        final boolean userDataOnly = extras.getBoolean(SyncAdapter.EXTRA_SYNC_USER_DATA_ONLY, false);

        if (!manualSync && timeSinceAttempt >= 0 && timeSinceAttempt < Config.MIN_INTERVAL_BETWEEN_SYNCS) {
            /*
            Code removed because it was causing a runaway sync; probably because we are setting
            syncResult.delayUntil incorrectly.

            Random r = new Random();
            long toWait = 10000 + r.nextInt(30000) // random jitter between 10 - 40 seconds
                    + Config.MIN_INTERVAL_BETWEEN_SYNCS - timeSinceAttempt;
            LOGW(TAG, "Sync throttled!! Another sync was attempted just " + timeSinceAttempt
                    + "ms ago. Requesting delay of " + toWait + "ms.");
            syncResult.fullSyncRequested = true;
            syncResult.delayUntil = (System.currentTimeMillis() + toWait) / 1000L;
            return false;*/
        }

        LOGI(TAG, "Performing sync for account: " + account);
        PrefUtils.markSyncAttemptedNow(mContext);
        long opStart;
        long remoteSyncDuration, choresDuration;

        opStart = System.currentTimeMillis();

        // remote sync consists of these operations, which we try one by one (and tolerate
        // individual failures on each)
        final int OP_REMOTE_SYNC = 0;
        final int OP_USER_SCHEDULE_SYNC = 1;
        final int OP_USER_FEEDBACK_SYNC = 2;

        int[] opsToPerform = userDataOnly ?
                new int[] { OP_USER_SCHEDULE_SYNC } :
                new int[] { OP_REMOTE_SYNC, OP_USER_SCHEDULE_SYNC, OP_USER_FEEDBACK_SYNC};


        for (int op : opsToPerform) {
            try {
                switch (op) {
                    case OP_REMOTE_SYNC:
                        dataChanged |= doRemoteSync();
                        break;
                    case OP_USER_SCHEDULE_SYNC:
                        dataChanged |= doUserScheduleSync(account.name);
                        break;
                    case OP_USER_FEEDBACK_SYNC:
                        doUserFeedbackSync();
                        break;
                }
            } catch (AuthException ex) {
                syncResult.stats.numAuthExceptions++;

                // if we have a token, try to refresh it
                if (AccountUtils.hasToken(mContext, account.name)) {
                    AccountUtils.refreshAuthToken(mContext);
                } else {
                    LOGW(TAG, "No auth token yet for this account. Skipping remote sync.");
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                LOGE(TAG, "Error performing remote sync.");
                increaseIoExceptions(syncResult);
            }
        }
        remoteSyncDuration = System.currentTimeMillis() - opStart;

        // If data has changed, there are a few chores we have to do
        opStart = System.currentTimeMillis();
        if (dataChanged) {
            try {
                performPostSyncChores(mContext);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                LOGE(TAG, "Error performing post sync chores.");
            }
        }
        clearExpertsIfNecessary();
        choresDuration = System.currentTimeMillis() - opStart;

        int operations = mConferenceDataHandler.getContentProviderOperationsDone();
        if (syncResult != null && syncResult.stats != null) {
            syncResult.stats.numEntries += operations;
            syncResult.stats.numUpdates += operations;
        }

        if (dataChanged) {
            long totalDuration = choresDuration + remoteSyncDuration;
            LOGD(TAG, "SYNC STATS:\n" +
                    " *  Account synced: " + (account == null ? "null" : account.name) + "\n" +
                    " *  Content provider operations: " + operations + "\n" +
                    " *  Remote sync took: " + remoteSyncDuration + "ms\n" +
                    " *  Post-sync chores took: " + choresDuration + "ms\n" +
                    " *  Total time: " + totalDuration + "ms\n" +
                    " *  Total data read from cache: \n" +
                    (mRemoteDataFetcher.getTotalBytesReadFromCache() / 1024) + "kB\n" +
                    " *  Total data downloaded: \n" +
                    (mRemoteDataFetcher.getTotalBytesDownloaded() / 1024) + "kB");
        }

        LOGI(TAG, "End of sync (" + (dataChanged ? "data changed" : "no data change") + ")");

        updateSyncInterval(mContext, account);

        return dataChanged;
    }

    public static void performPostSyncChores(final Context context) {
        // Update search index
        LOGD(TAG, "Updating search index.");
        context.getContentResolver().update(ScheduleContract.SearchIndex.CONTENT_URI,
                new ContentValues(), null, null);

        // Sync calendars
        LOGD(TAG, "Session data changed. Syncing starred sessions with Calendar.");
        syncCalendar(context);
    }

    private static void syncCalendar(Context context) {
        Intent intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
        intent.setClass(context, SessionCalendarService.class);
        context.startService(intent);
    }

    private void doUserFeedbackSync() {
        LOGD(TAG, "Syncing feedback");
        new FeedbackSyncHelper(mContext).sync();
    }

    /**
     * Checks if the remote server has new data that we need to import. If so, download
     * the new data and import it into the database.
     *
     * @return Whether or not data was changed.
     * @throws IOException if there is a problem downloading or importing the data.
     */
    private boolean doRemoteSync() throws IOException {
        if (!isOnline()) {
            LOGD(TAG, "Not attempting remote sync because device is OFFLINE");
            return false;
        }

        LOGD(TAG, "Starting remote sync.");

        // Fetch the remote data files via RemoteConferenceDataFetcher
        String[] dataFiles = mRemoteDataFetcher.fetchConferenceDataIfNewer(
                mConferenceDataHandler.getDataTimestamp());

        if (dataFiles != null) {
            LOGI(TAG, "Applying remote data.");
            // save the remote data to the database
            mConferenceDataHandler.applyConferenceData(dataFiles,
                    mRemoteDataFetcher.getServerDataTimestamp(), true);
            LOGI(TAG, "Done applying remote data.");

            // mark that conference data sync succeeded
            PrefUtils.markSyncSucceededNow(mContext);
            return true;
        } else {
            // no data to process (everything is up to date)

            // mark that conference data sync succeeded
            PrefUtils.markSyncSucceededNow(mContext);
            return false;
        }
    }

    /**
     * Checks if there are changes on MySchedule to sync with/from remote AppData folder.
     *
     * @return Whether or not data was changed.
     * @throws IOException if there is a problem uploading the data.
     */
    private boolean doUserScheduleSync(String accountName) throws IOException {
        if (!isOnline()) {
            LOGD(TAG, "Not attempting myschedule sync because device is OFFLINE");
            return false;
        }

        LOGD(TAG, "Starting user data (myschedule) sync.");

        AbstractUserDataSyncHelper helper = UserDataSyncHelperFactory.buildSyncHelper(
                mContext, accountName);
        boolean modified = helper.sync();
        if (modified) {
            // schedule notifications for the starred sessions
            Intent scheduleIntent = new Intent(
                    SessionAlarmService.ACTION_SCHEDULE_ALL_STARRED_BLOCKS,
                    null, mContext, SessionAlarmService.class);
            mContext.startService(scheduleIntent);
        }
        return modified;
    }

    // Returns whether we are connected to the internet.
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void increaseIoExceptions(SyncResult syncResult) {
        if (syncResult != null && syncResult.stats != null) {
            ++syncResult.stats.numIoExceptions;
        }
    }

    private void increaseSuccesses(SyncResult syncResult) {
        if (syncResult != null && syncResult.stats != null) {
            ++syncResult.stats.numEntries;
            ++syncResult.stats.numUpdates;
        }
    }

    private boolean clearExpertsIfNecessary() {
        if (Config.hasExpertsDirectoryExpired()) {
            return 0 < mContext.getContentResolver()
                    .delete(ScheduleContract.Experts.CONTENT_URI, null, null);
        }
        return false;
    }

    public static class AuthException extends RuntimeException {
    }


    public static long calculateRecommendedSyncInterval(final Context context) {
        long now = UIUtils.getCurrentTime(context);
        long aroundConferenceStart = Config.CONFERENCE_START_MILLIS - Config.AUTO_SYNC_AROUND_CONFERENCE_THRESH;
        if (now < aroundConferenceStart) {
            return Config.AUTO_SYNC_INTERVAL_LONG_BEFORE_CONFERENCE;
        } else if (now <= Config.CONFERENCE_END_MILLIS) {
            return Config.AUTO_SYNC_INTERVAL_AROUND_CONFERENCE;
        } else {
            return Config.AUTO_SYNC_INTERVAL_AFTER_CONFERENCE;
        }
    }

    public static void updateSyncInterval(final Context context, final Account account) {
        LOGD(TAG, "Checking sync interval for " + account);
        long recommended = calculateRecommendedSyncInterval(context);
        long current = PrefUtils.getCurSyncInterval(context);
        LOGD(TAG, "Recommended sync interval " + recommended + ", current " + current);
        if (recommended != current) {
            LOGD(TAG, "Setting up sync for account " + account + ", interval " + recommended + "ms");
            ContentResolver.setIsSyncable(account, ScheduleContract.CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, ScheduleContract.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, ScheduleContract.CONTENT_AUTHORITY,
                    new Bundle(), recommended / 1000L);
            PrefUtils.setCurSyncInterval(context, recommended);
        } else {
            LOGD(TAG, "No need to update sync interval.");
        }
    }
}
