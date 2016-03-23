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

/**
 * Implement this to react to changes in the login state triggered by the user. The changes in the
 * login state happen in the {@link com.google.samples.apps.iosched.navigation
 * .AppNavigationViewAbstractImpl}.
 */
public interface LoginStateListener {

    /**
     * This is called when the user has requested to sign in. Implements showing the login UI.
     */
    void onSignInOrCreateAccount();

    /**
     * This is called when the user has selected another account. Implements any custom changes
     * required in the feature based on the selected account.
     */
    void onAccountChangeRequested();

    /**
     * This is called when the user has selected another account. Implements login in the user.
     */
    void onStartLoginProcessRequested();

}
