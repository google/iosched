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

package com.google.samples.apps.iosched.sync.userdata.firebase;

import android.content.Context;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.FirebaseException;
import com.google.samples.apps.iosched.util.AccountUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Helper for performing Firebase authentication, which uses the auth token generated when the user
 * selects a Google account.
 */
public class FirebaseAuthHelper {
    private static final String TAG = makeLogTag(FirebaseAuthHelper.class);

    private Context mContext;
    private Firebase mFirebaseRef;
    private FirebaseAuthCallbacks mFirebaseAuthCallbacks;

    public FirebaseAuthHelper(Context context, Firebase firebaseRef,
            FirebaseAuthCallbacks firebaseAuthCallbacks) {
        mContext = context;
        mFirebaseRef = firebaseRef;
        mFirebaseAuthCallbacks = firebaseAuthCallbacks;
    }

    /**
     * Attempts authentication and informs clients who have implemented {@link
     * FirebaseAuthCallbacks} whether authentication succeeded or failed.
     */
    public void authenticate() {
        try {
            LOGI(TAG, "Attempting Firebase auth.");
            mFirebaseRef.authWithOAuthToken(AccountUtils.DEFAULT_OAUTH_PROVIDER,
                    AccountUtils.getAuthToken(mContext), new Firebase.AuthResultHandler() {
                        @Override
                        public void onAuthenticated(AuthData authData) {
                            LOGI(TAG, "Firebase auth succeeded");
                            mFirebaseAuthCallbacks.onAuthSucceeded(authData.getUid());
                        }

                        @Override
                        public void onAuthenticationError(FirebaseError firebaseError) {
                            LOGW(TAG, "Firebase auth error: " + firebaseError);
                            mFirebaseAuthCallbacks.onAuthFailed();
                        }
                    });
        } catch (FirebaseException | IllegalArgumentException | IllegalStateException e) {
            LOGW(TAG, "Firebase auth error", e);
            mFirebaseAuthCallbacks.onAuthFailed();
        }
    }
}
