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

package com.google.samples.apps.iosched.util;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.android.gms.auth.*;
import com.google.android.gms.common.Scopes;

import java.io.IOException;
import java.util.UUID;

import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * Account and login utilities. This class manages a local shared preferences object
 * that stores which account is currently active, and can store associated information
 * such as Google+ profile info (name, image URL, cover URL) and also the auth token
 * associated with the account.
 */
public class AccountUtils {
    private static final String TAG = makeLogTag(AccountUtils.class);

    private static final String PREF_ACTIVE_ACCOUNT = "chosen_account";

    // these names are are prefixes; the account is appended to them
    private static final String PREFIX_PREF_AUTH_TOKEN = "auth_token_";
    private static final String PREFIX_PREF_PLUS_PROFILE_ID = "plus_profile_id_";
    private static final String PREFIX_PREF_PLUS_NAME = "plus_name_";
    private static final String PREFIX_PREF_PLUS_IMAGE_URL = "plus_image_url_";
    private static final String PREFIX_PREF_PLUS_COVER_URL = "plus_cover_url_";
    private static final String PREFIX_PREF_GCM_KEY = "gcm_key_";

    public static final String AUTH_SCOPES[] = {
            Scopes.PLUS_LOGIN,
            Scopes.DRIVE_APPFOLDER,
            "https://www.googleapis.com/auth/userinfo.email"};

    static final String AUTH_TOKEN_TYPE;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("oauth2:");
        for (String scope : AUTH_SCOPES) {
            sb.append(scope);
            sb.append(" ");
        }
        AUTH_TOKEN_TYPE = sb.toString();
    }

    private static SharedPreferences getSharedPreferences(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean hasActiveAccount(final Context context) {
        return !TextUtils.isEmpty(getActiveAccountName(context));
    }

    public static String getActiveAccountName(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(PREF_ACTIVE_ACCOUNT, null);
    }

    public static Account getActiveAccount(final Context context) {
        String account = getActiveAccountName(context);
        if (account != null) {
            return new Account(account, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        } else {
            return null;
        }
    }

    public static boolean setActiveAccount(final Context context, final String accountName) {
        LOGD(TAG, "Set active account to: " + accountName);
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(PREF_ACTIVE_ACCOUNT, accountName).commit();
        return true;
    }

    private static String makeAccountSpecificPrefKey(Context ctx, String prefix) {
        return hasActiveAccount(ctx) ? makeAccountSpecificPrefKey(getActiveAccountName(ctx),
                prefix) : null;
    }

    private static String makeAccountSpecificPrefKey(String accountName, String prefix) {
        return prefix + accountName;
    }

    public static String getAuthToken(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return hasActiveAccount(context) ?
                sp.getString(makeAccountSpecificPrefKey(context, PREFIX_PREF_AUTH_TOKEN), null) : null;
    }

    public static void setAuthToken(final Context context, final String accountName, final String authToken) {
        LOGI(TAG, "Auth token of length "
                + (TextUtils.isEmpty(authToken) ? 0 : authToken.length()) + " for "
                + accountName);
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(makeAccountSpecificPrefKey(accountName, PREFIX_PREF_AUTH_TOKEN),
                authToken).commit();
        LOGV(TAG, "Auth Token: " + authToken);
    }

    public static void setAuthToken(final Context context, final String authToken) {
        if (hasActiveAccount(context)) {
            setAuthToken(context, getActiveAccountName(context), authToken);
        } else {
            LOGE(TAG, "Can't set auth token because there is no chosen account!");
        }
    }

    static void invalidateAuthToken(final Context context) {
        GoogleAuthUtil.invalidateToken(context, getAuthToken(context));
        setAuthToken(context, null);
    }

    public static void setPlusProfileId(final Context context, final String accountName, final String profileId) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(makeAccountSpecificPrefKey(accountName, PREFIX_PREF_PLUS_PROFILE_ID),
                profileId).commit();
    }

    public static String getPlusProfileId(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return hasActiveAccount(context) ? sp.getString(makeAccountSpecificPrefKey(context,
                PREFIX_PREF_PLUS_PROFILE_ID), null) : null;
    }

    public static boolean hasPlusInfo(final Context context, final String accountName) {
        SharedPreferences sp = getSharedPreferences(context);
        return !TextUtils.isEmpty(sp.getString(makeAccountSpecificPrefKey(accountName,
                PREFIX_PREF_PLUS_PROFILE_ID), null));
    }

    public static boolean hasToken(final Context context, final String accountName) {
        SharedPreferences sp = getSharedPreferences(context);
        return !TextUtils.isEmpty(sp.getString(makeAccountSpecificPrefKey(accountName,
                PREFIX_PREF_AUTH_TOKEN), null));
    }

    public static void setPlusName(final Context context, final String accountName, final String name) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(makeAccountSpecificPrefKey(accountName, PREFIX_PREF_PLUS_NAME),
                name).commit();
    }

    public static String getPlusName(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return hasActiveAccount(context) ? sp.getString(makeAccountSpecificPrefKey(context,
                PREFIX_PREF_PLUS_NAME), null) : null;
    }

    public static void setPlusImageUrl(final Context context, final String accountName, final String imageUrl) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(makeAccountSpecificPrefKey(accountName, PREFIX_PREF_PLUS_IMAGE_URL),
                imageUrl).commit();
    }

    public static String getPlusImageUrl(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return hasActiveAccount(context) ? sp.getString(makeAccountSpecificPrefKey(context,
                PREFIX_PREF_PLUS_IMAGE_URL), null) : null;
    }

    public static String getPlusImageUrl(final Context context, final String accountName) {
        SharedPreferences sp = getSharedPreferences(context);
        return hasActiveAccount(context) ? sp.getString(makeAccountSpecificPrefKey(accountName,
                PREFIX_PREF_PLUS_IMAGE_URL), null) : null;
    }

    public static void refreshAuthToken(Context mContext) {
        invalidateAuthToken(mContext);
        tryAuthenticateWithErrorNotification(mContext, ScheduleContract.CONTENT_AUTHORITY);
    }

    public static void setPlusCoverUrl(final Context context, final String accountName, String coverPhotoUrl) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(makeAccountSpecificPrefKey(accountName, PREFIX_PREF_PLUS_COVER_URL),
                coverPhotoUrl).commit();
    }

    public static String getPlusCoverUrl(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return hasActiveAccount(context) ? sp.getString(makeAccountSpecificPrefKey(context,
                PREFIX_PREF_PLUS_COVER_URL), null) : null;
    }

    static void tryAuthenticateWithErrorNotification(Context context, String syncAuthority) {
        try {
            String accountName = getActiveAccountName(context);
            if (accountName != null) {
                LOGI(TAG, "Requesting new auth token (with notification)");
                final String token = GoogleAuthUtil.getTokenWithNotification(context, accountName, AUTH_TOKEN_TYPE,
                        null, syncAuthority, null);
                setAuthToken(context, token);
            } else {
                LOGE(TAG, "Can't try authentication because no account is chosen.");
            }

        } catch (UserRecoverableNotifiedException e) {
            // Notification has already been pushed.
            LOGW(TAG, "User recoverable exception. Check notification.", e);
        } catch (GoogleAuthException e) {
            // This is likely unrecoverable.
            LOGE(TAG, "Unrecoverable authentication exception: " + e.getMessage(), e);
        } catch (IOException e) {
            LOGE(TAG, "transient error encountered: " + e.getMessage());
        }
    }

    public static void setGcmKey(final Context context, final String accountName, final String gcmKey) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(makeAccountSpecificPrefKey(accountName, PREFIX_PREF_GCM_KEY),
                gcmKey).commit();
        LOGD(TAG, "GCM key of account " + accountName + " set to: " + sanitizeGcmKey(gcmKey));
    }

    public static String getGcmKey(final Context context, final String accountName) {
        SharedPreferences sp = getSharedPreferences(context);
        String gcmKey = sp.getString(makeAccountSpecificPrefKey(accountName,
                PREFIX_PREF_GCM_KEY), null);

        // if there is no current GCM key, generate a new random one
        if (TextUtils.isEmpty(gcmKey)) {
            gcmKey = UUID.randomUUID().toString();
            LOGD(TAG, "No GCM key on account " + accountName + ". Generating random one: "
                    + sanitizeGcmKey(gcmKey));
            setGcmKey(context, accountName, gcmKey);
        }

        return gcmKey;
    }

    public static String sanitizeGcmKey(String key) {
        if (key == null) {
            return "(null)";
        } else if (key.length() > 8) {
            return key.substring(0, 4) + "........" + key.substring(key.length() - 4);
        } else {
            return "........";
        }
    }
}
