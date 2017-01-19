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

package com.google.samples.apps.iosched.login;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.util.AccountUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;

/**
 * Wrapper activity responsible for handling user switching using the GoogleApiClient.
 *
 * <p>Rendered as a transparent activity, this class is responsible for launching the user account
 * switcher, and receiving the results once the user has made a selection.
 *
 * <p>Note that invoking this will immediately sign the current user out. In the event the account
 * picker is canceled by the user, we attempt to restore the old user.
 */
public class SwitchUserActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final int RC_SIGN_IN = 1;
    private static final String TAG = "SwitchUserActivity";
    private GoogleApiClient mGoogleApiClient;
    private String mPreviousUser;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure Google API client for use with login API
        GoogleSignInOptions.Builder gsoBuilder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);

        for (String scope : LoginAndAuthWithGoogleApi.GetAuthScopes()) {
            gsoBuilder.requestScopes(new Scope(scope));
        }

        GoogleSignInOptions gso = gsoBuilder.requestEmail().build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        mPreviousUser = AccountUtils.getActiveAccountName(this);
        LOGD(TAG, "Signing current user out (" + mPreviousUser + ")");
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new SignOutCallback());
    }

    @Override
    public void onConnectionSuspended(final int i) {
        Toast.makeText(this, getText(R.string.google_play_services_disconnected),
                Toast.LENGTH_SHORT).show();
        // In this case we terminate, since otherwise we don't have any visible UI in this scenario.
        finish();
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Toast.makeText(this, getText(R.string.google_play_services_disconnected),
                Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    /**
     * Update active account and inform calling task of result completion.
     * Invoked after after the GoogleSignInApi login process completes (which is itself invoked
     * in SignOutCallback).
     *
     * <p>In the event the user cancels login, the last known user is restored.
     *
     * @param result User data from GoogleSignInApi
     */
    private void handleSignInResult(final GoogleSignInResult result) {
        if (result.isSuccess()) {
            final GoogleSignInAccount newUser = result.getSignInAccount();
            if (newUser != null) {
                AccountUtils.setActiveAccount(this, newUser.getEmail());
                LOGI(TAG, "Switched to user: " + newUser);
                setResult(Activity.RESULT_OK);
            }
        } else {
            // Login failed, revert to previous user to simulate spinner behavior
            LOGI(TAG, "Account switch aborted, re-enabling previous user: " + mPreviousUser);
            AccountUtils.setActiveAccount(this, mPreviousUser);
            setResult(Activity.RESULT_CANCELED);
        }
        finish();
    }

    /**
     * Invoked after the user has been signed out. Once this is done, we can safely start a new
     * sign-in process.
     */
    private class SignOutCallback implements ResultCallback<Status> {
        @Override
        public void onResult(@NonNull final Status status) {
            AccountUtils.clearActiveAccount(SwitchUserActivity.this);
            LOGD(TAG, "Launching account switcher");
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            SwitchUserActivity.this.startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    }
}
