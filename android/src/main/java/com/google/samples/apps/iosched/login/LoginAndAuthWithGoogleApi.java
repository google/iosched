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

package com.google.samples.apps.iosched.login;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.FirebaseUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This helper handles the UI flow for signing in and authenticating an account. It handles
 * connecting to the Google+ API to fetch profile data (name, cover photo, etc) and also getting the
 * auth token for the necessary scopes. The life of this object is tied to an Activity. Do not
 * attempt to share it across Activities, as unhappiness will result.
 */
public class LoginAndAuthWithGoogleApi
        implements LoginAndAuth, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<People.LoadPeopleResult> {

    // Request codes for the UIs that we show
    private static final int REQUEST_AUTHENTICATE = 100;
    private static final int REQUEST_RECOVER_FROM_AUTH_ERROR = 101;
    private static final int REQUEST_RECOVER_FROM_PLAY_SERVICES_ERROR = 102;
    private static final int REQUEST_PLAY_SERVICES_ERROR_DIALOG = 103;

    // Auth scopes we need
    private static final List<String> AUTH_SCOPES = new ArrayList<>(Arrays.asList(
            Scopes.PLUS_LOGIN,
            Scopes.DRIVE_APPFOLDER,
            "https://www.googleapis.com/auth/plus.profile.emails.read"));

    public static final String AUTH_TOKEN_TYPE;

    static {
        // Initialize oauth scope
        StringBuilder sb = new StringBuilder();
        sb.append("oauth2:");
        for (String scope : AUTH_SCOPES) {
            sb.append(scope);
            sb.append(" ");
        }
        AUTH_TOKEN_TYPE = sb.toString().trim();
    }

    private static final String TAG = makeLogTag(LoginAndAuthWithGoogleApi.class);

    Context mAppContext;

    // Controls whether or not we can show sign-in UI. Starts as true;
    // when sign-in *fails*, we will show the UI only once and set this flag to false.
    // After that, we don't attempt again in order not to annoy the user.
    private static boolean sCanShowSignInUi = true;
    private static boolean sCanShowAuthUi = true;

    // The Activity this object is bound to (we use a weak ref to avoid context leaks)
    WeakReference<Activity> mActivityRef;

    // Callbacks interface we invoke to notify the user of this class of useful events
    WeakReference<LoginAndAuthListener> mCallbacksRef;

    // Name of the account to log in as (e.g. "foo@example.com")
    String mAccountName;

    // API client to interact with Google services
    private GoogleApiClient mGoogleApiClient;

    // Async task that fetches the token
    GetTokenTask mTokenTask = null;

    // Are we in the started state? Started state is between onStart and onStop.
    boolean mStarted = false;

    // True if we are currently showing UIs to resolve a connection error.
    boolean mResolving = false;

    public LoginAndAuthWithGoogleApi(Activity activity, LoginAndAuthListener callback,
            String accountName) {
        LOGD(TAG, "Helper created. Account: " + mAccountName);
        mActivityRef = new WeakReference<Activity>(activity);
        mCallbacksRef = new WeakReference<LoginAndAuthListener>(callback);
        mAppContext = activity.getApplicationContext();
        mAccountName = accountName;
        if (SettingsUtils.hasUserRefusedSignIn(activity)) {
            // If we know the user refused sign-in, let's not annoy them.
            sCanShowSignInUi = sCanShowAuthUi = false;
        }
    }

    /** List of OAuth scopes to be requested from the Google sign-in API */
    public static List<String> GetAuthScopes() {
        return AUTH_SCOPES;
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public String getAccountName() {
        return mAccountName;
    }

    private Activity getActivity(String methodName) {
        Activity activity = mActivityRef.get();
        if (activity == null) {
            LOGD(TAG, "Helper lost Activity reference, ignoring (" + methodName + ")");
        }
        return activity;
    }

    @Override
    public void retryAuthByUserRequest() {
        LOGD(TAG, "Retrying sign-in/auth (user-initiated).");
        if (!mGoogleApiClient.isConnected()) {
            sCanShowAuthUi = sCanShowSignInUi = true;
            SettingsUtils.markUserRefusedSignIn(mAppContext, false);
            mGoogleApiClient.connect();
        } else if (!AccountUtils.hasToken(mAppContext, mAccountName)) {
            sCanShowAuthUi = sCanShowSignInUi = true;
            SettingsUtils.markUserRefusedSignIn(mAppContext, false);
            mTokenTask = new GetTokenTask();
            mTokenTask.execute();
        } else {
            LOGD(TAG,
                    "No need to retry auth: GoogleApiClient is connected and we have auth token.");
        }
    }

    /**
     * Starts the helper. Call this from your Activity's onStart().
     */
    @Override
    public void start() {
        Activity activity = getActivity("start()");
        if (activity == null) {
            return;
        }

        if (mStarted) {
            LOGW(TAG, "Helper already started. Ignoring redundant call.");
            return;
        }

        mStarted = true;
        if (mResolving) {
            // if resolving, don't reconnect the plus client
            LOGD(TAG, "Helper ignoring signal to start because we're resolving a failure.");
            return;
        }
        LOGD(TAG, "Helper starting. Connecting " + mAccountName);
        if (mGoogleApiClient == null) {
            LOGD(TAG, "Creating client.");

            GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity);
            for (String scope : AUTH_SCOPES) {
                builder.addScope(new Scope(scope));
            }
            mGoogleApiClient = builder.addApi(Plus.API)
                                      .addConnectionCallbacks(this)
                                      .addOnConnectionFailedListener(this)
                                      .setAccountName(mAccountName)
                                      .build();
        }
        LOGD(TAG, "Connecting client.");
        mGoogleApiClient.connect();
    }

    // Called when the Google+ client is connected.
    @Override
    public void onConnected(Bundle bundle) {
        Activity activity = getActivity("onConnected()");
        if (activity == null) {
            return;
        }

        LOGD(TAG, "Helper connected, account " + mAccountName);

        // load user's Google+ profile, if we don't have it yet
        if (!AccountUtils.hasPlusInfo(activity, mAccountName)) {
            LOGD(TAG, "We don't have Google+ info for " + mAccountName + " yet, so loading.");
            PendingResult<People.LoadPeopleResult> result =
                    Plus.PeopleApi.load(mGoogleApiClient, "me");
            result.setResultCallback(this);
        } else {
            LOGD(TAG, "No need for Google+ info, we already have it.");
        }

        // try to authenticate, if we don't have a token yet
        if (!AccountUtils.hasToken(activity, mAccountName)) {
            LOGD(TAG, "We don't have auth token for " + mAccountName + " yet, so getting it.");
            mTokenTask = new GetTokenTask();
            mTokenTask.execute();
        } else {
            LOGD(TAG, "No need for auth token, we already have it.");
            reportAuthSuccess(false);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        LOGD(TAG, "onConnectionSuspended.");
    }

    /**
     * Stop the helper. Call this from your Activity's onStop().
     */
    @Override
    public void stop() {
        if (!mStarted) {
            LOGW(TAG, "Helper already stopped. Ignoring redundant call.");
            return;
        }

        LOGD(TAG, "Helper stopping.");
        if (mTokenTask != null) {
            LOGD(TAG, "Helper cancelling token task.");
            mTokenTask.cancel(false);
        }
        mStarted = false;
        if (mGoogleApiClient.isConnected()) {
            LOGD(TAG, "Helper disconnecting client.");
            mGoogleApiClient.disconnect();
        }
        mResolving = false;
    }

    // Called when the connection to Google Play Services fails.
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Activity activity = getActivity("onConnectionFailed()");
        if (activity == null) {
            return;
        }

        if (connectionResult.hasResolution()) {
            if (sCanShowSignInUi) {
                LOGD(TAG, "onConnectionFailed, with resolution. Attempting to resolve.");
                sCanShowSignInUi = false;
                try {
                    mResolving = true;
                    connectionResult.startResolutionForResult(activity,
                            REQUEST_RECOVER_FROM_PLAY_SERVICES_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    LOGE(TAG, "SendIntentException occurred: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                LOGD(TAG, "onConnectionFailed with resolution but sCanShowSignInUi==false.");
                reportAuthFailure();
            }
            return;
        }

        LOGD(TAG, "onConnectionFailed, no resolution.");
        final int errorCode = connectionResult.getErrorCode();
        if (GooglePlayServicesUtil.isUserRecoverableError(errorCode) && sCanShowSignInUi) {
            sCanShowSignInUi = false;
            GooglePlayServicesUtil.getErrorDialog(errorCode, activity,
                    REQUEST_PLAY_SERVICES_ERROR_DIALOG).show();
        } else {
            reportAuthFailure();
        }
    }

    // Called asynchronously -- result of loadPeople() call
    @Override
    public void onResult(People.LoadPeopleResult loadPeopleResult) {
        LOGD(TAG, "onPeopleLoaded, status=" + loadPeopleResult.getStatus().toString());
        if (loadPeopleResult.getStatus().isSuccess()) {
            PersonBuffer personBuffer = loadPeopleResult.getPersonBuffer();
            if (personBuffer != null && personBuffer.getCount() > 0) {
                LOGD(TAG, "Got plus profile for account " + mAccountName);
                Person currentUser = personBuffer.get(0);
                personBuffer.close();

                // Record profile ID, image URL and name
                LOGD(TAG, "Saving plus profile ID: " + currentUser.getId());
                AccountUtils.setPlusProfileId(mAppContext, mAccountName, currentUser.getId());
                String imageUrl = currentUser.getImage().getUrl();
                if (imageUrl != null) {
                    imageUrl = Uri.parse(imageUrl)
                                  .buildUpon().appendQueryParameter("sz", "256").build().toString();
                }
                LOGD(TAG, "Saving plus image URL: " + imageUrl);
                AccountUtils.setPlusImageUrl(mAppContext, mAccountName, imageUrl);
                LOGD(TAG, "Saving plus display name: " + currentUser.getDisplayName());
                AccountUtils.setPlusName(mAppContext, mAccountName, currentUser.getDisplayName());
                Person.Cover cover = currentUser.getCover();
                if (cover != null) {
                    Person.Cover.CoverPhoto coverPhoto = cover.getCoverPhoto();
                    if (coverPhoto != null) {
                        LOGD(TAG, "Saving plus cover URL: " + coverPhoto.getUrl());
                        AccountUtils
                                .setPlusCoverUrl(mAppContext, mAccountName, coverPhoto.getUrl());
                    }
                } else {
                    LOGD(TAG, "Profile has no cover.");
                }

                LoginAndAuthListener callbacks;
                if (null != (callbacks = mCallbacksRef.get())) {
                    callbacks.onPlusInfoLoaded(mAccountName);
                }
            } else {
                LOGE(TAG, "Plus response was empty! Failed to load profile.");
            }
        } else {
            LOGE(TAG, "Failed to load plus proflie, error " +
                    loadPeopleResult.getStatus().getStatusCode());
        }
    }

    /**
     * Handles an Activity result. Call this from your Activity's onActivityResult().
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Activity activity = getActivity("onActivityResult()");
        if (activity == null) {
            return false;
        }

        if (requestCode == REQUEST_AUTHENTICATE ||
                requestCode == REQUEST_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_PLAY_SERVICES_ERROR_DIALOG) {

            LOGD(TAG, "onActivityResult, req=" + requestCode + ", result=" + resultCode);
            if (requestCode == REQUEST_RECOVER_FROM_PLAY_SERVICES_ERROR) {
                mResolving = false;
            }

            if (resultCode == Activity.RESULT_OK) {
                if (mGoogleApiClient != null) {
                    LOGD(TAG, "Since activity result was RESULT_OK, reconnecting client.");
                    mGoogleApiClient.connect();
                } else {
                    LOGD(TAG, "Activity result was RESULT_OK, but we have no client to reconnect.");
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                LOGD(TAG, "User explicitly cancelled sign-in/auth flow.");
                // Save the refusal so the user isn't annoyed again.
                SettingsUtils.markUserRefusedSignIn(mAppContext, true);
            } else {
                LOGW(TAG, "Failed to recover from a login/auth failure, resultCode=" + resultCode);
            }
            return true;
        }
        return false;
    }

    private void showRecoveryDialog(int statusCode) {
        Activity activity = getActivity("showRecoveryDialog()");
        if (activity == null) {
            return;
        }

        if (sCanShowAuthUi) {
            sCanShowAuthUi = false;
            LOGD(TAG, "Showing recovery dialog for status code " + statusCode);
            final Dialog d = GooglePlayServicesUtil.getErrorDialog(
                    statusCode, activity, REQUEST_RECOVER_FROM_PLAY_SERVICES_ERROR);
            d.show();
        } else {
            LOGD(TAG, "Not showing Play Services recovery dialog because sCanShowSignInUi==false.");
            reportAuthFailure();
        }
    }

    private void showAuthRecoveryFlow(Intent intent) {
        Activity activity = getActivity("showAuthRecoveryFlow()");
        if (activity == null) {
            return;
        }

        if (sCanShowAuthUi) {
            sCanShowAuthUi = false;
            LOGD(TAG, "Starting auth recovery Intent.");
            activity.startActivityForResult(intent, REQUEST_RECOVER_FROM_AUTH_ERROR);
        } else {
            LOGD(TAG, "Not showing auth recovery flow because sCanShowSignInUi==false.");
            reportAuthFailure();
        }
    }

    private void reportAuthSuccess(boolean newlyAuthenticated) {
        LOGD(TAG, "Auth success for account " + mAccountName + ", newlyAuthenticated=" +
                newlyAuthenticated);
        LoginAndAuthListener callback;
        if (null != (callback = mCallbacksRef.get())) {
            callback.onAuthSuccess(mAccountName, newlyAuthenticated);
        }
    }

    private void reportAuthFailure() {
        LOGD(TAG, "Auth FAILURE for account " + mAccountName);
        LoginAndAuthListener callback;
        if (null != (callback = mCallbacksRef.get())) {
            callback.onAuthFailure(mAccountName);
        }
    }

    /**
     * Async task that obtains the auth token.
     */
    private class GetTokenTask extends AsyncTask<Void, Void, Void> {

        public GetTokenTask() {}

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (isCancelled()) {
                    LOGD(TAG, "doInBackground: task cancelled, so giving up on auth.");
                    return null;
                }

                LOGD(TAG, "Starting background auth for " + mAccountName);
                final String token = GoogleAuthUtil
                        .getToken(mAppContext, mAccountName, AUTH_TOKEN_TYPE);
                final String accountId = GoogleAuthUtil.getAccountId(mAppContext, mAccountName);

                // Save auth token.
                LOGD(TAG, "Saving token: " + (token == null ? "(null)" : "(length " +
                        token.length() + ")") + " for account " + mAccountName);
                AccountUtils.setAuthToken(mAppContext, mAccountName, token);
                // Set the Firebase shard associated with the chosen account.
                FirebaseUtils.setFirebaseUrl(mAppContext, accountId);
            } catch (GooglePlayServicesAvailabilityException e) {
                postShowRecoveryDialog(e.getConnectionStatusCode());
            } catch (UserRecoverableAuthException e) {
                postShowAuthRecoveryFlow(e.getIntent());
            } catch (IOException e) {
                LOGE(TAG, "IOException encountered: " + e.getMessage());
            } catch (GoogleAuthException e) {
                LOGE(TAG, "GoogleAuthException encountered: " + e.getMessage());
            } catch (RuntimeException e) {
                LOGE(TAG, "RuntimeException encountered: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            super.onPostExecute(nothing);

            if (isCancelled()) {
                LOGD(TAG, "Task cancelled, so not reporting auth success.");
            } else if (!mStarted) {
                LOGD(TAG, "Activity not started, so not reporting auth success.");
            } else {
                LOGD(TAG, "GetTokenTask reporting auth success.");
                reportAuthSuccess(true);
            }
        }

        private void postShowRecoveryDialog(final int statusCode) {
            Activity activity = getActivity("postShowRecoveryDialog()");
            if (activity == null) {
                return;
            }

            if (isCancelled()) {
                LOGD(TAG, "Task cancelled, so not showing recovery dialog.");
                return;
            }

            LOGD(TAG, "Requesting display of recovery dialog for status code " + statusCode);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mStarted) {
                        showRecoveryDialog(statusCode);
                    } else {
                        LOGE(TAG, "Activity not started, so not showing recovery dialog.");
                    }
                }
            });
        }

        private void postShowAuthRecoveryFlow(final Intent intent) {
            Activity activity = getActivity("postShowAuthRecoveryFlow()");
            if (activity == null) {
                return;
            }

            if (isCancelled()) {
                LOGD(TAG, "Task cancelled, so not showing auth recovery flow.");
                return;
            }

            LOGD(TAG, "Requesting display of auth recovery flow.");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mStarted) {
                        showAuthRecoveryFlow(intent);
                    } else {
                        LOGE(TAG, "Activity not started, so not showing auth recovery flow.");
                    }
                }
            });
        }
    }

}
