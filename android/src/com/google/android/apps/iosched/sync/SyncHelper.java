/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.sync;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.io.AnnouncementsHandler;
import com.google.android.apps.iosched.io.BlocksHandler;
import com.google.android.apps.iosched.io.HandlerException;
import com.google.android.apps.iosched.io.JSONHandler;
import com.google.android.apps.iosched.io.RoomsHandler;
import com.google.android.apps.iosched.io.SandboxHandler;
import com.google.android.apps.iosched.io.SearchSuggestHandler;
import com.google.android.apps.iosched.io.SessionsHandler;
import com.google.android.apps.iosched.io.SpeakersHandler;
import com.google.android.apps.iosched.io.TracksHandler;
import com.google.android.apps.iosched.io.model.EditMyScheduleResponse;
import com.google.android.apps.iosched.io.model.ErrorResponse;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.calendar.SessionCalendarService;
import com.google.android.apps.iosched.util.AccountUtils;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.LOGI;
import static com.google.android.apps.iosched.util.LogUtils.LOGV;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A helper class for dealing with sync and other remote persistence operations.
 * All operations occur on the thread they're called from, so it's best to wrap
 * calls in an {@link android.os.AsyncTask}, or better yet, a
 * {@link android.app.Service}.
 */
public class SyncHelper {

    private static final String TAG = makeLogTag(SyncHelper.class);

    static {
        // Per http://android-developers.blogspot.com/2011/09/androids-http-clients.html
        if (!UIUtils.hasFroyo()) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    public static final int FLAG_SYNC_LOCAL = 0x1;
    public static final int FLAG_SYNC_REMOTE = 0x2;

    private static final int LOCAL_VERSION_CURRENT = 19;

    private Context mContext;
    private String mAuthToken;
    private String mUserAgent;

    public SyncHelper(Context context) {
        mContext = context;
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
    public void performSync(SyncResult syncResult, int flags) throws IOException {
        mAuthToken = AccountUtils.getAuthToken(mContext);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final int localVersion = prefs.getInt("local_data_version", 0);

        // Bulk of sync work, performed by executing several fetches from
        // local and online sources.
        final ContentResolver resolver = mContext.getContentResolver();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        LOGI(TAG, "Performing sync");

        if ((flags & FLAG_SYNC_LOCAL) != 0) {
            final long startLocal = System.currentTimeMillis();
            final boolean localParse = localVersion < LOCAL_VERSION_CURRENT;
            LOGD(TAG, "found localVersion=" + localVersion + " and LOCAL_VERSION_CURRENT="
                    + LOCAL_VERSION_CURRENT);
            // Only run local sync if there's a newer version of data available
            // than what was last locally-sync'd.
            if (localParse) {
                // Load static local data
                batch.addAll(new RoomsHandler(mContext).parse(
                        JSONHandler.loadResourceJson(mContext, R.raw.rooms)));
                batch.addAll(new BlocksHandler(mContext).parse(
                        JSONHandler.loadResourceJson(mContext, R.raw.common_slots)));
                batch.addAll(new TracksHandler(mContext).parse(
                        JSONHandler.loadResourceJson(mContext, R.raw.tracks)));
                batch.addAll(new SpeakersHandler(mContext, true).parse(
                        JSONHandler.loadResourceJson(mContext, R.raw.speakers)));
                batch.addAll(new SessionsHandler(mContext, true, false).parse(
                        JSONHandler.loadResourceJson(mContext, R.raw.sessions)));
                batch.addAll(new SandboxHandler(mContext, true).parse(
                        JSONHandler.loadResourceJson(mContext, R.raw.sandbox)));
                batch.addAll(new SearchSuggestHandler(mContext).parse(
                        JSONHandler.loadResourceJson(mContext, R.raw.search_suggest)));
                prefs.edit().putInt("local_data_version", LOCAL_VERSION_CURRENT).commit();
                if (syncResult != null) {
                    ++syncResult.stats.numUpdates;
                    ++syncResult.stats.numEntries;
                }
            }

            LOGD(TAG, "Local sync took " + (System.currentTimeMillis() - startLocal) + "ms");

            try {
                // Apply all queued up batch operations for local data.
                resolver.applyBatch(ScheduleContract.CONTENT_AUTHORITY, batch);
            } catch (RemoteException e) {
                throw new RuntimeException("Problem applying batch operation", e);
            } catch (OperationApplicationException e) {
                throw new RuntimeException("Problem applying batch operation", e);
            }

            batch = new ArrayList<ContentProviderOperation>();
        }

        if ((flags & FLAG_SYNC_REMOTE) != 0 && isOnline()) {
            try {
                boolean auth = !UIUtils.isGoogleTV(mContext) &&
                        AccountUtils.isAuthenticated(mContext);
                final long startRemote = System.currentTimeMillis();
                LOGI(TAG, "Remote syncing speakers");
                batch.addAll(executeGet(Config.GET_ALL_SPEAKERS_URL,
                        new SpeakersHandler(mContext, false), auth));
                LOGI(TAG, "Remote syncing sessions");
                batch.addAll(executeGet(Config.GET_ALL_SESSIONS_URL,
                        new SessionsHandler(mContext, false, mAuthToken != null), auth));
                LOGI(TAG, "Remote syncing sandbox");
                batch.addAll(executeGet(Config.GET_SANDBOX_URL,
                        new SandboxHandler(mContext, false), auth));
                LOGI(TAG, "Remote syncing announcements");
                batch.addAll(executeGet(Config.GET_ALL_ANNOUNCEMENTS_URL,
                        new AnnouncementsHandler(mContext, false), auth));
                // GET_ALL_SESSIONS covers the functionality GET_MY_SCHEDULE provides here.
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
                AccountUtils.invalidateAuthToken(mContext);
                AccountUtils.tryAuthenticateWithErrorNotification(mContext, null,
                        new Account(AccountUtils.getChosenAccountName(mContext),
                                GoogleAccountManager.ACCOUNT_TYPE));
            }
            // all other IOExceptions are thrown
        }

        try {
            // Apply all queued up remaining batch operations (only remote content at this point).
            resolver.applyBatch(ScheduleContract.CONTENT_AUTHORITY, batch);

            // Delete empty blocks
            Cursor emptyBlocksCursor = resolver.query(ScheduleContract.Blocks.CONTENT_URI,
                    new String[]{ScheduleContract.Blocks.BLOCK_ID,ScheduleContract.Blocks.SESSIONS_COUNT},
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
        Intent intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
        intent.setClass(mContext, SessionCalendarService.class);
        mContext.startService(intent);
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
    }

    private ArrayList<ContentProviderOperation> executeGet(String urlString, JSONHandler handler,
            boolean authenticated) throws IOException {
        LOGD(TAG, "Requesting URL: " + urlString);
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", mUserAgent);
        if (authenticated && mAuthToken != null) {
            urlConnection.setRequestProperty("Authorization", "Bearer " + mAuthToken);
        }

        urlConnection.connect();
        throwErrors(urlConnection);

        String response = readInputStream(urlConnection.getInputStream());
        LOGV(TAG, "HTTP response: " + response);
        return handler.parse(response);
    }

    private void throwErrors(HttpURLConnection urlConnection) throws IOException {
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

    private static String readInputStream(InputStream inputStream)
            throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String responseLine;
        StringBuilder responseBuilder = new StringBuilder();
        while ((responseLine = bufferedReader.readLine()) != null) {
            responseBuilder.append(responseLine);
        }
        return responseBuilder.toString();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}
