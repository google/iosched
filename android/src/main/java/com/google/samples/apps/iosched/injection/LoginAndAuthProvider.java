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

package com.google.samples.apps.iosched.injection;

import android.app.Activity;

import com.google.samples.apps.iosched.login.LoginAndAuth;
import com.google.samples.apps.iosched.login.LoginAndAuthListener;
import com.google.samples.apps.iosched.login.LoginAndAuthWithGoogleApi;

/**
 * Provides a way to inject stub classes when running integration tests.
 */
public class LoginAndAuthProvider {

    private static LoginAndAuth stubLoginAndAuth;

    public static void setStubLoginAndAuth(LoginAndAuth login) {
        stubLoginAndAuth = login;
    }

    public static LoginAndAuth provideLoginAndAuth(Activity activity, LoginAndAuthListener callback,
            String accountName) {
        if (stubLoginAndAuth != null) {
            return stubLoginAndAuth;
        } else {
            return new LoginAndAuthWithGoogleApi(activity, callback, accountName);
        }
    }
}
