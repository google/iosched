/*
 * Copyright (c) 2017 Google Inc.
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

package com.google.samples.apps.iosched.welcome;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.WelcomeUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Fragment that provides optional auth functionality to users.
 */
public class SignInFragment extends WelcomeFragment
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = makeLogTag(WelcomeFragment.class);

    private static final String KEY_RESOLVING = "resolving";

    private GoogleApiClient mGoogleApiClient;

    // Tracks whether we're already resolving a failed GoogleApiClient connection.
    private boolean mResolving = false;

    public SignInFragment() {}

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_RESOLVING, mResolving);
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mResolving = savedInstanceState.getBoolean(KEY_RESOLVING, false);
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.welcome_sign_in_fragment, container, false);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean shouldDisplay(final Context context) {
        /* Display if the user hasn't signed in and also not pressed skip. */
        return AccountUtils.getActiveAccount(context) == null &&
                !WelcomeUtils.hasUserRefusedSignInDuringOnboarding(context);
    }

    @Override
    protected String getPrimaryButtonText() {
        return getString(R.string.signin_prompt);
    }

    @Override
    protected String getSecondaryButtonText() {
        return getString(R.string.signin_prompt_dismiss);
    }

    @Override
    protected View.OnClickListener getPrimaryButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, AccountUtils.RC_SIGN_IN);
            }
        };
    }

    @Override
    protected View.OnClickListener getSecondaryButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                WelcomeUtils.markUserRefusedSignInDuringOnboarding(mActivity, true);
                doNext();
            }
        };
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        LOGI(TAG, "onConnectionFailed");
        if (mResolving) {
            return;
        }
        if (connectionResult.hasResolution()) {
            mResolving = true;
            try {
                connectionResult.startResolutionForResult(mActivity,
                        WelcomeActivity.REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                LOGW(TAG, "Could not start resolution for result: " + e);
                mResolving = false;
                mGoogleApiClient.connect();
            }
        } else {
            Toast.makeText(mActivity, R.string.google_play_services_disconnected,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AccountUtils.RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        LOGD(TAG, "handleSignInResult: " + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            if (acct != null) {
                LOGI(TAG, "signed in successfully: " + acct.getEmail());
                AccountUtils.setActiveAccount(mActivity, acct.getEmail());
                AccountUtils.setActiveAccountDisplayName(mActivity, acct.getDisplayName());
                AccountUtils.setActiveAccountPhotoUrl(mActivity, acct.getPhotoUrl());

            }
        } else {
            LOGW(TAG, "Initial sign in failed: " + result);
            AccountUtils.clearActiveAccount(mActivity);
        }
        doNext();
    }
}