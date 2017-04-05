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

package com.google.samples.apps.iosched.signin;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;


import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.sync.account.Account;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.lang.ref.WeakReference;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Manages sign in and sign out functionality. Designed to be used with an activity, which
 * is responsible for building and connecting a {@link GoogleApiClient}.
 */
public class SignInManager {

    private static final String TAG = makeLogTag(SignInManager.class);

    public static GoogleSignInOptions getGoogleSignInOptions(String webClientId) {
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
    }

    /**
     * Reference to the Activity this object is bound to (we use a weak ref to avoid context leaks).
     */
    private WeakReference<Activity> mActivityRef;

    /**
     * Callbacks interface we invoke to notify the user of this class of events.
     */
    private WeakReference<SignInListener> mSignInListenerRef;

    /**
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;

    private FirebaseAuth mFirebaseAuth;

    public SignInManager(Activity activity, SignInListener signInListener,
            GoogleApiClient googleApiClient) {
        mActivityRef = new WeakReference<>(activity);
        mSignInListenerRef = new WeakReference<>(signInListener);
        mGoogleApiClient = googleApiClient;
        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    public void signIn() {
        LOGD(TAG, "Starting sign in");

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        activity.startActivityForResult(signInIntent, AccountUtils.RC_SIGN_IN);
    }

    public void signOut() {
        LOGD(TAG, "Signing out user");

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final SignInListener signInListener = getSignInListener();
        if (signInListener == null) {
            return;
        }

        // Signing out requires a connected GoogleApiClient. It is the responsibility of the bound
        // activity to ensure that GoogleApiClient is connected.
        if (!mGoogleApiClient.isConnected()) {
            return;
        }

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            performPostSignOutTasks(status);
                        } else {
                            LOGW(TAG, "Failed to sign out");
                            if (signInListener != null) {
                                signInListener.onSignOutFailed(status);
                            }
                        }
                    }
                });
    }

    /**
     * Method for processing sign in logic in the bonding activity's
     * {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The requestCode argument of the activity's onActivityResult.
     * @param resultCode  The resultCode argument of the activity's onActivityResult.
     * @param data        The Intent argument of the activity's onActivityResult.
     */
    public void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {

        final SignInListener signInListener = getSignInListener();
        if (signInListener == null) {
            return;
        }

        if (requestCode == AccountUtils.RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount acct = result.getSignInAccount();
                if (acct != null) {
                    performPostSignInTasks(acct, result);
                }
            } else {
                LOGW(TAG, "Sign in failed");
                signInListener.onSignInFailed(result);
            }
        }
    }

    /**
     * Called once a user has signed in.
     *
     * @param acct   The sign in account.
     * @param result The sign in result.
     */
    private void performPostSignInTasks(GoogleSignInAccount acct, GoogleSignInResult result) {

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final SignInListener signInListener = getSignInListener();
        if (signInListener == null) {
            return;
        }

        // Tasks we always want to execute upon sign in.
        AccountUtils.setActiveAccount(activity, acct.getEmail());
        AccountUtils.setActiveAccountDisplayName(activity, acct.getDisplayName());
        AccountUtils.setActiveAccountPhotoUrl(activity, acct.getPhotoUrl());
        firebaseAuthWithGoogle(acct);
        SyncHelper.requestManualSync(true);

        // Tasks executed by the binding activity on sign in.
        signInListener.onSignIn(result);
    }

    /**
     * Called once a user has signed out.
     *
     * @param status The status returned when Auth.GoogleSignInApi.signOut(...) is called.
     */
    private void performPostSignOutTasks(Status status) {
        LOGD(TAG, "Successfully signed out.");

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final SignInListener signInListener = getSignInListener();
        if (signInListener == null) {
            return;
        }

        // Tasks we always want to execute upon sign out.
        AccountUtils.clearActiveAccount(activity);
        FirebaseAuth.getInstance().signOut();

        // Tasks executed by the binding activity upon sign out.
        signInListener.onSignOut(status);
    }

    private Activity getActivity() {
        Activity activity = mActivityRef.get();
        if (activity == null) {
            LOGD(TAG, "Activity is null");
        }
        return activity;
    }

    private SignInListener getSignInListener() {
        SignInListener signInListner = mSignInListenerRef.get();
        if (signInListner == null) {
            LOGD(TAG, "SignInListener is null");
        }
        return signInListner;
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        LOGD(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        mFirebaseAuth.signInWithCredential(credential)
                     .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                         @Override
                         public void onComplete(@NonNull Task<AuthResult> task) {
                             if (!task.isSuccessful()) {
                                 LOGW(TAG, "signInWithCredential", task.getException());
                                 return;
                             }

                             if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                 LOGD(TAG,
                                         "signInWithCredential:onComplete:" + task.isSuccessful());
                                 // TODO: kick off code to check if user is registered.
                             }
                         }
                     });
    }
}
