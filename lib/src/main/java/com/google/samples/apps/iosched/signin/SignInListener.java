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


import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.Status;

/**
 * Interface for activities that need sign in and sign out functionality.
 */
public interface SignInListener {

    /**
     * Called when sign in succeeds.
     * @param result    The {@link GoogleSignInResult result}.
     */
    void onSignIn(GoogleSignInResult result);

    /**
     * Called when sign out succeeds.
     * @param status    The {@link Status status}.
     */
    void onSignOut(Status status);

    /**
     * Called when sign in fails.
     * @param result    The {@link GoogleSignInResult result}.
     */
    void onSignInFailed(GoogleSignInResult result);

    /**
     * Called when sign out fails.
     * @param status    The {@link Status status}.
     */
    void onSignOutFailed(Status status);
}
