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

package com.google.android.apps.iosched.util;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.calendar.SessionCalendarService;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.AccountActivity;
import com.google.android.gcm.GCMRegistrar;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import static com.google.android.apps.iosched.util.LogUtils.LOGE;
import static com.google.android.apps.iosched.util.LogUtils.LOGI;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * An assortment of authentication and login helper utilities.
 */
public class AccountUtils {
    private static final String TAG = makeLogTag(AccountUtils.class);

    private static final String PREF_CHOSEN_ACCOUNT = "chosen_account";
    private static final String PREF_AUTH_TOKEN = "auth_token";

    // The auth scope required for the app. In our case we use the "conference API"
    // (not currently open source) which requires the developerssite (and readonly variant) scope.
    private static final String AUTH_TOKEN_TYPE =
            "oauth2:"
                    + "https://www.googleapis.com/auth/developerssite "
                    + "https://www.googleapis.com/auth/developerssite.readonly ";

    public static boolean isAuthenticated(final Context context) {
        return !TextUtils.isEmpty(getChosenAccountName(context));
    }

    public static String getChosenAccountName(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREF_CHOSEN_ACCOUNT, null);
    }

    private static void setChosenAccountName(final Context context, final String accountName) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                        sp.edit().putString(PREF_CHOSEN_ACCOUNT, accountName).commit();
    }

    public static String getAuthToken(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREF_AUTH_TOKEN, null);
    }

    private static void setAuthToken(final Context context, final String authToken) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(PREF_AUTH_TOKEN, authToken).commit();
    }

    public static void invalidateAuthToken(final Context context) {
        AccountManager am = AccountManager.get(context);
        am.invalidateAuthToken(GoogleAccountManager.ACCOUNT_TYPE, getAuthToken(context));
        setAuthToken(context, null);
    }

    public static interface AuthenticateCallback {
        public boolean shouldCancelAuthentication();
        public void onAuthTokenAvailable(String authToken);
    }

    public static void tryAuthenticate(Activity activity, AuthenticateCallback callback,
            int activityRequestCode, Account account) {
        //noinspection deprecation
        AccountManager.get(activity).getAuthToken(
                account,
                AUTH_TOKEN_TYPE,
                false,
                getAccountManagerCallback(callback, account, activity, activity,
                        activityRequestCode),
                null);
    }

    public static void tryAuthenticateWithErrorNotification(Context context,
            AuthenticateCallback callback, Account account) {
        //noinspection deprecation
        AccountManager.get(context).getAuthToken(
                account,
                AUTH_TOKEN_TYPE,
                true,
                getAccountManagerCallback(callback, account, context, null, 0),
                null);
    }

    private static AccountManagerCallback<Bundle> getAccountManagerCallback(
            final AuthenticateCallback callback, final Account account,
            final Context context, final Activity activity, final int activityRequestCode) {
        return new AccountManagerCallback<Bundle>() {
            public void run(AccountManagerFuture<Bundle> future) {
                if (callback != null && callback.shouldCancelAuthentication()) {
                    return;
                }

                try {
                    Bundle bundle = future.getResult();
                    if (activity != null && bundle.containsKey(AccountManager.KEY_INTENT)) {
                        Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
                        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivityForResult(intent, activityRequestCode);

                    } else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                        final String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                        setAuthToken(context, token);
                        setChosenAccountName(context, account.name);
                        if (callback != null) {
                            callback.onAuthTokenAvailable(token);
                        }
                    }
                } catch (Exception e) {
                    LOGE(TAG, "Authentication error", e);
                }
            }
        };
    }

    public static void signOut(final Context context) {
        // Clear out all Google I/O-created sessions from Calendar
        if (UIUtils.hasICS()) {
            LOGI(TAG, "Clearing all sessions from Google Calendar using SessionCalendarService.");
            Toast.makeText(context, R.string.toast_deleting_sessions_from_calendar,
                    Toast.LENGTH_LONG).show();
            context.startService(
                    new Intent(SessionCalendarService.ACTION_CLEAR_ALL_SESSIONS_CALENDAR)
                            .setClass(context, SessionCalendarService.class)
                            .putExtra(SessionCalendarService.EXTRA_ACCOUNT_NAME,
                                    getChosenAccountName(context)));
        }

        invalidateAuthToken(context);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().clear().commit();
        context.getContentResolver().delete(ScheduleContract.BASE_CONTENT_URI, null, null);
        GCMRegistrar.unregister(context);
    }

    public static void startAuthenticationFlow(final Context context, final Intent finishIntent) {
        Intent loginFlowIntent = new Intent(context, AccountActivity.class);
        loginFlowIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        loginFlowIntent.putExtra(AccountActivity.EXTRA_FINISH_INTENT, finishIntent);
        context.startActivity(loginFlowIntent);
    }
}
