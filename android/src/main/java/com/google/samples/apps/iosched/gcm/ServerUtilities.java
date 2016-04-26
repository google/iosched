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
package com.google.samples.apps.iosched.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGV;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Helper class used to communicate with the demo server.
 */
public final class ServerUtilities {
    private static final String TAG = makeLogTag("GCMs");

    private static final String PREFERENCES = "com.google.samples.apps.iosched.gcm";
    private static final String PROPERTY_REGISTERED_TS = "registered_ts";
    private static final String PROPERTY_REG_ID = "gcm_id";
    private static final String PROPERTY_GCM_KEY = "gcm_key";
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;

    private static final Random sRandom = new Random();

    private static boolean checkGcmEnabled() {
        if (TextUtils.isEmpty(BuildConfig.GCM_SERVER_URL)) {
            LOGD(TAG, "GCM feature disabled (no URL configured)");
            return false;
        } else if (TextUtils.isEmpty(BuildConfig.GCM_API_KEY)) {
            LOGD(TAG, "GCM feature disabled (no API key configured)");
            return false;
        } else if (TextUtils.isEmpty(BuildConfig.GCM_SENDER_ID)) {
            LOGD(TAG, "GCM feature disabled (no sender ID configured)");
            return false;
        }
        return true;
    }

    /**
     * Register this account/device pair within the server.
     *
     * @param context Current context
     * @param regId   The GCM registration ID for this device
     * @param gcmKey  The GCM key with which to register.
     * @return whether the registration succeeded or not.
     */
    public static boolean register(final Context context, final String regId, final String gcmKey) {
        if (!checkGcmEnabled()) {
            return false;
        }

        LOGD(TAG, "registering device (reg_id = " + regId + ")");
        String serverUrl = BuildConfig.GCM_SERVER_URL + "/register";
        LOGI(TAG, "registering on GCM with GCM key: " + AccountUtils.sanitizeGcmKey(gcmKey));

        Map<String, String> params = new HashMap<String, String>();
        params.put(PROPERTY_REG_ID, regId);
        params.put(PROPERTY_GCM_KEY, gcmKey);
        long backoff = BACKOFF_MILLI_SECONDS + sRandom.nextInt(1000);
        // Once GCM returns a registration id, we need to register it in the
        // demo server. As the server might be down, we will retry it a couple
        // times.
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            LOGV(TAG, "Attempt #" + i + " to register");
            try {
                post(serverUrl, params, BuildConfig.GCM_API_KEY);
                setRegisteredOnServer(context, true, regId, gcmKey);
                return true;
            } catch (IOException e) {
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
                LOGE(TAG, "Failed to register on attempt " + i, e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    LOGV(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    LOGD(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return false;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        return false;
    }

    /**
     * Unregister this account/device pair within the server.
     *
     * @param regId  The InstanceID token for this application instance.
     * @param gcmKey The user identifier used to pair a user with an InstanceID token.
     */
    static void unregister(final String regId, final String gcmKey) {
        if (!checkGcmEnabled()) {
            return;
        }

        LOGI(TAG, "unregistering device (regId = " + regId + ")");
        String serverUrl = BuildConfig.GCM_SERVER_URL + "/unregister";
        Map<String, String> params = new HashMap<String, String>();
        params.put(PROPERTY_GCM_KEY, gcmKey);
        params.put(PROPERTY_REG_ID, regId);
        try {
            post(serverUrl, params, BuildConfig.GCM_API_KEY);
        } catch (IOException e) {
            // At this point the device is unregistered from GCM, but still
            // registered on the server.
            // We could try to unregister again, but it is not necessary:
            // if the server tries to send a message to the device, it will get
            // a "NotRegistered" error message and should unregister the device.
            LOGD(TAG, "Unable to unregister from application server", e);
        }
    }

    /**
     * Request user data sync.
     *
     * @param context Current context
     */
    public static void notifyUserDataChanged(final Context context) {
        if (!checkGcmEnabled()) {
            return;
        }

        LOGI(TAG, "Notifying GCM that user data changed");
        String serverUrl = BuildConfig.GCM_SERVER_URL + "/send/self/sync_user";
        try {
            String gcmKey =
                    AccountUtils.getGcmKey(context, AccountUtils.getActiveAccountName(context));
            if (gcmKey != null) {
                post(serverUrl, new HashMap<String, String>(), gcmKey);
            }
        } catch (IOException e) {
            LOGE(TAG, "Unable to notify GCM about user data change", e);
        }
    }

    /**
     * Sets whether the device was successfully registered in the server side.
     *
     * @param context Current context
     * @param flag    True if registration was successful, false otherwise
     * @param regId   InstanceID token generated to represent the current instance of the
     *                Application.
     * @param gcmKey  User identifier paired with regId on server
     */
    protected static void setRegisteredOnServer(Context context, boolean flag, String regId,
            String gcmKey) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
        LOGD(TAG, "Setting registered on server status as: " + flag + ", gcmKey="
                + AccountUtils.sanitizeGcmKey(gcmKey));
        Editor editor = prefs.edit();
        if (flag) {
            editor.putLong(PROPERTY_REGISTERED_TS, new Date().getTime());
            editor.putString(PROPERTY_GCM_KEY, gcmKey == null ? "" : gcmKey);
            editor.putString(PROPERTY_REG_ID, regId);
        } else {
            editor.remove(PROPERTY_REG_ID);
        }
        editor.apply();
    }

    /**
     * Checks whether the device was successfully registered in the server side.
     *
     * @param context Current context
     * @return True if registration was successful, false otherwise
     */
    public static boolean isRegisteredOnServer(Context context, String gcmKey) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
        // Find registration threshold
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        long yesterdayTS = cal.getTimeInMillis();
        long regTS = prefs.getLong(PROPERTY_REGISTERED_TS, 0);

        gcmKey = gcmKey == null ? "" : gcmKey;

        if (regTS > yesterdayTS) {
            LOGV(TAG, "GCM registration current. regTS=" + regTS + " yesterdayTS=" + yesterdayTS);

            final String registeredGcmKey = prefs.getString(PROPERTY_GCM_KEY, "");
            if (registeredGcmKey.equals(gcmKey)) {
                LOGD(TAG, "GCM registration is valid and for the correct gcm key: "
                        + AccountUtils.sanitizeGcmKey(registeredGcmKey));
                return true;
            }
            LOGD(TAG, "GCM registration is for DIFFERENT gcm key "
                    + AccountUtils.sanitizeGcmKey(registeredGcmKey) + ". We were expecting "
                    + AccountUtils.sanitizeGcmKey(gcmKey));
            return false;
        } else {
            LOGV(TAG, "GCM registration expired. regTS=" + regTS + " yesterdayTS=" + yesterdayTS);
            return false;
        }
    }

    public static String getGcmRegId(Context context) {
        final SharedPreferences prefs =
                context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getString(PROPERTY_REG_ID, null);
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params   request parameters.
     * @throws java.io.IOException propagated from POST.
     */
    private static void post(String endpoint, Map<String, String> params, String key)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        params.put("key", key);
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                       .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        LOGW(TAG, "Posting to " + url);
        LOGV(TAG, "Posting '" + body + "'");
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setChunkedStreamingMode(0);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("Content-Length",
                    Integer.toString(body.length()));
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes());
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
