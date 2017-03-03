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

import android.content.Intent;

/**
 * Implement this to provide login and auth functionality to the {@link
 * com.google.samples.apps.iosched.ui.BaseActivity}.
 */
public interface LoginAndAuth {

    /**
     * @return the account name for the logged in user, or null.
     */
    String getAccountName();

    /**
     * Initiates the login process.
     */
    void start();

    /**
     * @return if this has been started (via {@link #start()}).
     */
    boolean isStarted();

    /**
     * Disconnects the connected user.
     */
    void stop();

    /**
     * Attempts the login process. This is called when the user has requested to do so.
     */
    void retryAuthByUserRequest();

    /**
     * Handles an activity result related to login. Typically, the LoginAndAuth implementation may
     * start an activity for result (for example, to launch a recovery flow). The {@link
     * com.google.samples.apps.iosched.ui.BaseActivity} would received the {@code requestCode},
     * {@code resultCode} and {@code data} in its own {@link android.app
     * .Activity#onActivityResult(int, int, Intent)} method, and it will pass it on to the
     * LoginAndAuth implementation.
     */
    boolean onActivityResult(int requestCode, int resultCode, Intent data);
}
