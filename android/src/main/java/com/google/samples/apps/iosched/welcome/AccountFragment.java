/*
 * Copyright 2015 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.welcome;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
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
import com.google.android.gms.common.api.Scope;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.login.LoginAndAuthWithGoogleApi;
import com.google.samples.apps.iosched.util.AccountUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * The account selection and runtime permission enforcement fragment in the welcome screen. Only
 * runtime permissions required for basic app functionality should be included as part of the
 * welcome flow.
 */
public class AccountFragment extends WelcomeFragment implements View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = makeLogTag(AccountFragment.class);
    private static final int SIGN_IN_RESULT = 1;
    private View mLayout;
    private String mSelectedAccount;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected View.OnClickListener getPrimaryButtonListener() {
        return new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(View v) {
                // Ensure we don't run this fragment again
                LOGD(TAG, "Active account set");
                AccountUtils.setActiveAccount(mActivity, mSelectedAccount);
            }
        };
    }

    @Override
    protected String getPrimaryButtonText() {
        return null;
    }

    @Override
    protected String getSecondaryButtonText() {
        return null;
    }

    @Override
    protected View.OnClickListener getSecondaryButtonListener() {
        return null;
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        mLayout.findViewById(R.id.sign_in_button).setOnClickListener(this);
        mLayout.findViewById(R.id.sign_in_button).setEnabled(true);
    }

    @Override
    public void onConnectionSuspended(final int cause) {
        mLayout.findViewById(R.id.sign_in_button).setEnabled(false);
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Toast.makeText(getContext(), "Unable to connect to Google Play Services",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        mLayout = inflater.inflate(R.layout.welcome_account_fragment, container, false);

        if (mActivity instanceof WelcomeFragmentContainer) {
            ((WelcomeFragmentContainer) mActivity).setPrimaryButtonEnabled(false);
        }

        // Configure Google API client for use with login API
        GoogleSignInOptions.Builder gsoBuilder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);

        for (String scope : LoginAndAuthWithGoogleApi.GetAuthScopes()) {
            gsoBuilder.requestScopes(new Scope(scope));
        }

        GoogleSignInOptions gso = gsoBuilder.requestEmail()
                                            .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        return mLayout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSelectedAccount = null;
        mGoogleApiClient.disconnect();
        mGoogleApiClient = null;
    }

    @Override
    public boolean shouldDisplay(Context context) {
        Account account = AccountUtils.getActiveAccount(context);
        return account == null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, SIGN_IN_RESULT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == SIGN_IN_RESULT) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(final GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            final GoogleSignInAccount acct = result.getSignInAccount();
            if (acct != null) {
                AccountUtils.setActiveAccount(getContext(), acct.getEmail());
                doNext();
            }
        }
    }

}
