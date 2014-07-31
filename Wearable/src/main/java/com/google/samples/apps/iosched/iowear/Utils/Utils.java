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

package com.google.samples.apps.iosched.iowear.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A utility class. All methods are stateless and static.
 */
public class Utils {

    private static final String TAG = "Utils";
    private static final String PREF_SESSION_ID = "pref-session-id";
    private static final String[] PREF_RESPONSES = {"pref-response-0", "pref-response-1",
            "pref-response-2", "pref-response-3"};

    /**
     * Returns the persisted responses for the feedback with the provided session id. The return is
     * an int array of size <code>4</code>. A value <code>-1</code> indicates that there is no
     * response for that question.
     */
    public static int[] getPersistedResponses(Context context, String sessionId) {
        String oldSession = getSessionIdFromPreference(context);
        if (null == oldSession || !sessionId.equals(oldSession)) {
            // saved data is for a different session
            LOGD(TAG, "Clearing persisted data, oldSession = " + oldSession + ", new session: "
                    + sessionId);
            clearResponses(context);
            return new int[]{-1, -1, -1, -1};
        }
        int[] responses = new int[4];
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        for (int i = 0; i < PREF_RESPONSES.length; i++) {
            responses[i] = pref.getInt(PREF_RESPONSES[i], -1);
        }
        return responses;
    }

    /**
     * Persists the response to a question for future retrieval. Passing a <code>-1</code> for a
     * response amounts to clearing the response for that question.
     */
    public static void saveResponse(Context context, int question, int response) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (response == -1) {
            // we want to remove
            pref.edit().remove(PREF_RESPONSES[question]).apply();
        } else {
            pref.edit().putInt(PREF_RESPONSES[question], response).apply();
        }
    }

    /**
     * Clears all the persisted responses.
     */
    public static void clearResponses(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().remove(PREF_SESSION_ID).apply();
        for (String name : PREF_RESPONSES) {
            pref.edit().remove(name).apply();
        }
    }

    /**
     * Saves the sessionId.
     */
    public static void saveSessionId(Context context, String sessionId) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (null == sessionId) {
            // we want to remove
            pref.edit().remove(PREF_SESSION_ID).apply();
        } else {
            pref.edit().putString(PREF_SESSION_ID, sessionId).apply();
        }
    }

    /**
     * Returns the persisted session id.
     */
    private static String getSessionIdFromPreference(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(PREF_SESSION_ID, null);
    }

    /**
     * A simple wrapper around LOGD
     */
    public static final void LOGD(String TAG, String message) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LOGD(TAG, message);
        }
    }

    public static String makeLogTag(String str) {
        if (str.length() > 23) {
            return str.substring(0, 23);
        }

        return str;
    }

}
