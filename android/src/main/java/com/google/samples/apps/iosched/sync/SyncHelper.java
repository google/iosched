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
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.feedback.FeedbackApiHelper;
import com.google.samples.apps.iosched.feedback.FeedbackSyncHelper;
import com.google.samples.apps.iosched.io.BlocksHandler;
import com.google.samples.apps.iosched.io.HandlerException;
import com.google.samples.apps.iosched.io.HashtagsHandler;
import com.google.samples.apps.iosched.io.JSONHandler;
import com.google.samples.apps.iosched.io.MapPropertyHandler;
import com.google.samples.apps.iosched.io.RoomsHandler;
import com.google.samples.apps.iosched.io.SearchSuggestHandler;
import com.google.samples.apps.iosched.io.SessionsHandler;
import com.google.samples.apps.iosched.io.SpeakersHandler;
import com.google.samples.apps.iosched.io.TagsHandler;
import com.google.samples.apps.iosched.io.TracksHandler;
import com.google.samples.apps.iosched.io.VideosHandler;
import com.google.samples.apps.iosched.io.model.ErrorResponse;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.service.DataBootstrapService;
import com.google.samples.apps.iosched.service.SessionAlarmService;
import com.google.samples.apps.iosched.service.SessionCalendarService;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper;
import com.google.samples.apps.iosched.sync.userdata.UserDataSyncHelperFactory;
import com.google.samples.apps.iosched.tracking.android.EasyTracker;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.turbomanage.httpclient.BasicHttpClient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import no.java.schedule.BuildConfig;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGV;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A helper class for dealing with conference data synchronization. All operations occur on the
 * thread they're called from, so it's best to wrap calls in an {@link android.os.AsyncTask}, or
 * better yet, a {@link android.app.Service}.
 */
public class SyncHelper {

    private static final String TAG = makeLogTag(SyncHelper.class);

    private Context mContext;

    private ConferenceDataHandler mConferenceDataHandler;

    private RemoteConferenceDataFetcher mRemoteDataFetcher;

    private BasicHttpClient mHttpClient;

    private String mUserAgent;

    public SyncHelper(Context context) {
        mContext = context;
        mConferenceDataHandler = new ConferenceDataHandler(mContext);
        mRemoteDataFetcher = new RemoteConferenceDataFetcher(mContext);
        mHttpClient = new BasicHttpClient();
        mUserAgent = buildUserAgent(context);
    }

    /**
     * Loads conference information (sessions, rooms, tracks, speakers, etc.)
     * from a local static cache data and then syncs down data from the
     * Conference API.
     *
     * @param syncResult Optional {@link SyncResult} object to populate.
     * @throws IOException
     */

    public void performSync(SyncResult syncResult,Account account, Bundle extras) throws IOException {

        final ContentResolver resolver = mContext.getContentResolver();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        LOGI(TAG, "Performing sync");


        try {
            final long startRemote = System.currentTimeMillis();
            LOGI(TAG, "Syncing rooms");
            batch.addAll(fetchResource(Config.EMS_ROOMS, new RoomsHandler(mContext)));
            LOGI(TAG, "Syncing sessions");
            batch.addAll(fetchResource(Config.GET_ALL_SESSIONS_URL, new SessionsHandler(mContext, false, false)));
            LOGI(TAG, "Syncing tracks");
            batch.addAll(fetchResource(Config.GET_ALL_SESSIONS_URL, new TracksHandler(mContext)));
            //mHandlerForKey.put(DATA_KEY_MAP, mMapPropertyHandler = new MapPropertyHandler(mContext));
            //mHandlerForKey.put(DATA_KEY_HASHTAGS, mHashtagsHandler = new HashtagsHandler(mContext));
            // mHandlerForKey.put(DATA_KEY_VIDEOS, mVideosHandler = new VideosHandler(mContext));


            //TODO Enable announcements for JavaZone
            //LOGI(TAG, "Remote syncing announcements");
            //batch.addAll(executeGet(Config.GET_ALL_ANNOUNCEMENTS_URL,
            //        new AnnouncementsHandler(mContext, false), auth));

            LOGD(TAG, "Remote sync took " + (System.currentTimeMillis() - startRemote) + "ms");
            if (syncResult != null) {
                ++syncResult.stats.numUpdates;
                ++syncResult.stats.numEntries;
            }

            EasyTracker.getTracker().dispatch();

        } catch (HandlerException.UnauthorizedException e) {
            LOGI(TAG, "Unauthorized; getting a new auth token.", e);
            if (syncResult != null) {
                ++syncResult.stats.numAuthExceptions;
            }

        }

        try {
            // Apply all queued up remaining batch operations (only remote content at this point).
            resolver.applyBatch(ScheduleContract.CONTENT_AUTHORITY, batch);

            // Delete empty blocks
            Cursor emptyBlocksCursor = resolver.query(ScheduleContract.Blocks.CONTENT_URI,
                    new String[]{ScheduleContract.Blocks.BLOCK_ID, ScheduleContract.Blocks.SESSIONS_COUNT},
                    ScheduleContract.Blocks.EMPTY_SESSIONS_SELECTION, null, null);
            batch = new ArrayList<ContentProviderOperation>();
            int numDeletedEmptyBlocks = 0;
            while (emptyBlocksCursor.moveToNext()) {
                batch.add(ContentProviderOperation
                        .newDelete(ScheduleContract.Blocks.buildBlockUri(
                                emptyBlocksCursor.getString(0)))
                        .build());
                ++numDeletedEmptyBlocks;
            }
            emptyBlocksCursor.close();
            resolver.applyBatch(ScheduleContract.CONTENT_AUTHORITY, batch);
            LOGD(TAG, "Deleted " + numDeletedEmptyBlocks + " empty session blocks.");
        } catch (RemoteException e) {
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            throw new RuntimeException("Problem applying batch operation", e);
        }

        if (UIUtils.hasICS()) {
            LOGD(TAG, "Done with sync'ing conference data. Starting to sync "
                    + "session with Calendar.");
            syncCalendar();
        }
    }

    private void syncCalendar() {
        //Intent intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
        //intent.setClass(mContext, SessionCalendarService.class);
        //mContext.startService(intent);
    }

    /**
     * Build and return a user-agent string that can identify this application
     * to remote servers. Contains the package name and version code.
     */
    private static String buildUserAgent(Context context) {
        String versionName = "unknown";
        int versionCode = 0;

        try {
            final PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            versionName = info.versionName;
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return context.getPackageName() + "/" + versionName + " (" + versionCode + ") (gzip)";
    }

    public void addOrRemoveSessionFromSchedule(Context context, String sessionId,
                                               boolean inSchedule) throws IOException {
        //TODO sync at EMS server
        /**
         mAuthToken = AccountUtils.getAuthToken(mContext);
         JsonObject starredSession = new JsonObject();
         starredSession.addProperty("sessionid", sessionId);

         byte[] postJsonBytes = new Gson().toJson(starredSession).getBytes();

         URL url = new URL(Config.EDIT_MY_SCHEDULE_URL + (inSchedule ? "add" : "remove"));
         HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
         urlConnection.setRequestProperty("User-Agent", mUserAgent);
         urlConnection.setRequestProperty("Content-Type", "application/json");
         urlConnection.setRequestProperty("Authorization", "Bearer " + mAuthToken);
         urlConnection.setDoOutput(true);
         urlConnection.setFixedLengthStreamingMode(postJsonBytes.length);

         LOGD(TAG, "Posting to URL: " + url);
         OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
         out.write(postJsonBytes);
         out.flush();

         urlConnection.connect();
         throwErrors(urlConnection);
         String json = readInputStream(urlConnection.getInputStream());
         EditMyScheduleResponse response = new Gson().fromJson(json,
         EditMyScheduleResponse.class);
         if (!response.success) {
         String responseMessageLower = (response.message != null)
         ? response.message.toLowerCase()
         : "";

         if (responseMessageLower.contains("no profile")) {
         throw new HandlerException.NoDevsiteProfileException();
         }
         }
         **/
    }

    public static void requestManualSync(Account mChosenAccount) {
        requestManualSync(mChosenAccount, false);
    }

    public static void requestManualSync(Account mChosenAccount, boolean userDataSyncOnly) {
        if (mChosenAccount != null) {
            LOGD(TAG, "Requesting manual sync for account " + mChosenAccount.name
                    + " userDataSyncOnly=" + userDataSyncOnly);
            Bundle b = new Bundle();
            b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            if (userDataSyncOnly) {
                b.putBoolean(SyncAdapter.EXTRA_SYNC_USER_DATA_ONLY, true);
            }
            ContentResolver
                    .setSyncAutomatically(mChosenAccount, ScheduleContract.CONTENT_AUTHORITY, true);
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
     * Attempts to perform data synchronization. There are 3 types of data: conference, user
     * schedule and user feedback.
     * <p/>
     * The conference data sync is handled by {@link RemoteConferenceDataFetcher}. For more details
     * about conference data, refer to the documentation at
     * https://github.com/google/iosched/blob/master/doc/SYNC.md. The user schedule data sync is
     * handled by {@link AbstractUserDataSyncHelper}. The user feedback sync is handled by
     * {@link FeedbackSyncHelper}.
     *
     * @param syncResult The sync result object to update with statistics.
     * @param account    The account associated with this sync
     * @param extras     Specifies additional information about the sync. This must contain key
     *                   {@code SyncAdapter.EXTRA_SYNC_USER_DATA_ONLY} with boolean value
     * @return true if the sync changed the data.
     */
    /*
    public boolean performSync(@Nullable SyncResult syncResult, Account account, Bundle extras) {
        boolean dataChanged = false;

        if (!SettingsUtils.isDataBootstrapDone(mContext)) {
            LOGD(TAG, "Sync aborting (data bootstrap not done yet)");
            // Start the bootstrap process so that the next time sync is called,
            // it is already bootstrapped.
            DataBootstrapService.startDataBootstrapIfNecessary(mContext);
            return false;
        }

        final boolean userDataScheduleOnly = extras
                .getBoolean(SyncAdapter.EXTRA_SYNC_USER_DATA_ONLY, false);

        LOGI(TAG, "Performing sync for account: " + account);
        SettingsUtils.markSyncAttemptedNow(mContext);
        long opStart;
        long syncDuration, choresDuration;

        opStart = System.currentTimeMillis();

        // Sync consists of 1 or more of these operations. We try them one by one and tolerate
        // individual failures on each.
        final int OP_CONFERENCE_DATA_SYNC = 0;
        final int OP_USER_SCHEDULE_DATA_SYNC = 1;
        final int OP_USER_FEEDBACK_DATA_SYNC = 2;

        int[] opsToPerform = userDataScheduleOnly ?
                new int[]{OP_USER_SCHEDULE_DATA_SYNC} :
                new int[]{OP_CONFERENCE_DATA_SYNC, OP_USER_SCHEDULE_DATA_SYNC,
                        OP_USER_FEEDBACK_DATA_SYNC};

        for (int op : opsToPerform) {
            try {
                switch (op) {
                    case OP_CONFERENCE_DATA_SYNC:
                        dataChanged |= doConferenceDataSync();
                        break;
                    case OP_USER_SCHEDULE_DATA_SYNC:
                        dataChanged |= doUserDataSync(account.name);
                        break;
                    case OP_USER_FEEDBACK_DATA_SYNC:
                        // User feedback data sync is an outgoing sync only so not affecting
                        // {@code dataChanged} value.
                        doUserFeedbackDataSync();
                        break;
                }
            } catch (AuthException ex) {
                syncResult.stats.numAuthExceptions++;

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
        syncDuration = System.currentTimeMillis() - opStart;

        opStart = System.currentTimeMillis();
        if (dataChanged) {
            try {
                performPostSyncChores(mContext);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                LOGE(TAG, "Error performing post sync chores.");
            }
        }
        choresDuration = System.currentTimeMillis() - opStart;

        int operations = mConferenceDataHandler.getContentProviderOperationsDone();
        if (syncResult != null && syncResult.stats != null) {
            syncResult.stats.numEntries += operations;
            syncResult.stats.numUpdates += operations;
        }

        if (dataChanged) {
            long totalDuration = choresDuration + syncDuration;
            LOGD(TAG, "SYNC STATS:\n" +
                    " *  Account synced: " + (account == null ? "null" : account.name) + "\n" +
                    " *  Content provider operations: " + operations + "\n" +
                    " *  Sync took: " + syncDuration + "ms\n" +
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
    } */

    public static void performPostSyncChores(final Context context) {
        LOGD(TAG, "Updating search index.");
        context.getContentResolver().update(ScheduleContract.SearchIndex.CONTENT_URI,
                new ContentValues(), null, null);

        LOGD(TAG, "Session data changed. Syncing starred sessions with Calendar.");
        syncCalendar(context);
    }

    private static void syncCalendar(Context context) {
        Intent intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
        intent.setClass(context, SessionCalendarService.class);
        context.startService(intent);
    }

    private void doUserFeedbackDataSync() {
        LOGD(TAG, "Syncing feedback");
        new FeedbackSyncHelper(mContext, new FeedbackApiHelper(mHttpClient,
                BuildConfig.FEEDBACK_API_ENDPOINT)).sync();
    }

    /**
     * Checks if the remote server has new conference data that we need to import. If so, download
     * the new data and import it into the database.
     *
     * @return Whether or not data was changed.
     * @throws IOException if there is a problem downloading or importing the data.
     */
    private boolean doConferenceDataSync() throws IOException {
        if (!isOnline()) {
            LOGD(TAG, "Not attempting remote sync because device is OFFLINE");
            return false;
        }

        LOGD(TAG, "Starting remote sync.");

        // Fetch the remote data files via RemoteConferenceDataFetcher.
        String[] dataFiles = mRemoteDataFetcher.fetchConferenceDataIfNewer(
                mConferenceDataHandler.getDataTimestamp());

        if (dataFiles != null) {
            LOGI(TAG, "Applying remote data.");
            // Save the remote data to the database.
            mConferenceDataHandler.applyConferenceData(dataFiles,
                    mRemoteDataFetcher.getServerDataTimestamp(), true);
            LOGI(TAG, "Done applying remote data.");

            // Mark that conference data sync has succeeded.
            SettingsUtils.markSyncSucceededNow(mContext);
            return true;
        } else {
            // No data to process (everything is up to date).
            // Mark that conference data sync succeeded.
            SettingsUtils.markSyncSucceededNow(mContext);
            return false;
        }
    }

    /**
     * Checks if there are changes on User's Data to sync with/from remote AppData folder.
     *
     * @return Whether or not data was changed.
     * @throws IOException if there is a problem uploading the data.
     */
    private boolean doUserDataSync(String accountName) throws IOException {
        if (!isOnline()) {
            LOGD(TAG, "Not attempting userdata sync because device is OFFLINE");
            return false;
        }

        LOGD(TAG, "Starting user data sync.");

        AbstractUserDataSyncHelper helper = UserDataSyncHelperFactory.buildSyncHelper(
                mContext, accountName);
        boolean modified = helper.sync();
        if (modified) {
            // Schedule notifications for the starred sessions.
            Intent scheduleIntent = new Intent(
                    SessionAlarmService.ACTION_SCHEDULE_ALL_STARRED_BLOCKS,
                    null, mContext, SessionAlarmService.class);
            mContext.startService(scheduleIntent);
        }
        return modified;
    }

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

    public static class AuthException extends RuntimeException {

    }

    private static long calculateRecommendedSyncInterval(final Context context) {
        long now = UIUtils.getCurrentTime(context);
        long aroundConferenceStart = Config.CONFERENCE_START_MILLIS
                - Config.AUTO_SYNC_AROUND_CONFERENCE_THRESH;
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
        long current = SettingsUtils.getCurSyncInterval(context);
        LOGD(TAG, "Recommended sync interval " + recommended + ", current " + current);
        if (recommended != current) {
            LOGD(TAG,
                    "Setting up sync for account " + account + ", interval " + recommended + "ms");
            ContentResolver.setIsSyncable(account, ScheduleContract.CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, ScheduleContract.CONTENT_AUTHORITY, true);
            if (recommended <= 0L) { // Disable periodic sync.
                ContentResolver.removePeriodicSync(account, ScheduleContract.CONTENT_AUTHORITY,
                        new Bundle());
            } else {
                ContentResolver.addPeriodicSync(account, ScheduleContract.CONTENT_AUTHORITY,
                        new Bundle(), recommended / 1000L);
            }
            SettingsUtils.setCurSyncInterval(context, recommended);
        } else {
            LOGD(TAG, "No need to update sync interval.");
        }
    }

    public ArrayList<ContentProviderOperation> fetchResource(String urlString, JSONHandler handler) throws IOException {

        String response = null;
        if (isFirstRun()) {
            response = getLocalResource(mContext, urlString);
        } else if (isOnline(mContext)) {
            response = getHttpResource(urlString);
        }

        if (response!=null && !response.trim().equals("")){
            return handler.parse(response);
        } else {
            return new ArrayList<ContentProviderOperation>();
        }
    }

    private boolean isFirstRun() {
        return isFirstRun(mContext);
    }

    public static boolean isFirstRun(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("first_run", true);
    }

    public static String getLocalResource(Context pContext,String pUrlString) {

        pUrlString = pUrlString.replaceFirst("http://", "");

        //fix file/directory clashes....
        if (pUrlString.endsWith("/sessions")) {
            pUrlString=pUrlString+".json";
        }

        LOGD("LocalResourceUrls", pUrlString);

        try {
            InputStream asset = pContext.getAssets().open(pUrlString);


            LOGD("LocalResourceUrls Found",pUrlString);
            return convertStreamToString(asset);

        }
        catch (IOException e) {
            LOGE("LocalResourceUrls NotFound",pUrlString);
            LOGE(makeLogTag(SyncHelper.class),"Exception reading asset",e);
            return null;
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static String getHttpResource(final String urlString) throws IOException {
        LOGD(TAG, "Requesting URL: " + urlString);
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Androidito JZ15");

        urlConnection.setRequestProperty("Accept", "application/json");

        urlConnection.connect();
        throwErrors(urlConnection);

        String response = readInputStream(urlConnection.getInputStream());
        LOGV(TAG, "HTTP response: " + response);
        return response;
    }

    private static void throwErrors(HttpURLConnection urlConnection) throws IOException {
        final int status = urlConnection.getResponseCode();
        if (status < 200 || status >= 300) {
            String errorMessage = null;
            try {
                String errorContent = readInputStream(urlConnection.getErrorStream());
                LOGV(TAG, "Error content: " + errorContent);
                ErrorResponse errorResponse = new Gson().fromJson(
                        errorContent, ErrorResponse.class);
                errorMessage = errorResponse.error.message;
            } catch (IOException ignored) {
            } catch (JsonSyntaxException ignored) {
            }

            String exceptionMessage = "Error response "
                    + status + " "
                    + urlConnection.getResponseMessage()
                    + (errorMessage == null ? "" : (": " + errorMessage))
                    + " for " + urlConnection.getURL();

            // TODO: the API should return 401, and we shouldn't have to parse the message
            throw (errorMessage != null && errorMessage.toLowerCase().contains("auth"))
                    ? new HandlerException.UnauthorizedException(exceptionMessage)
                    : new HandlerException(exceptionMessage);
        }
    }

    public static String readInputStream(InputStream inputStream)
            throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String responseLine;
        StringBuilder responseBuilder = new StringBuilder();
        while ((responseLine = bufferedReader.readLine()) != null) {
            responseBuilder.append(responseLine);
        }
        return responseBuilder.toString();
    }

    public static boolean isOnline(Context mContext) {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}
