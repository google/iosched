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

package com.google.samples.apps.iosched.testutils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.samples.apps.iosched.util.AccountUtils;

/**
 * Methods to help mock login/account status
 */
public class LoginUtils {

    public final static String DUMMY_ACCOUNT_NAME = "testieso";

    /**
     *
     * @return account name, or a test account name
     */
    public static String setFirstAvailableAccountAsActive(Context context) {
        String account;
        AccountManager am =
                AccountManager.get(InstrumentationRegistry.getTargetContext());
        Account[] accountArray =
                am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        if (accountArray.length > 0) {
            account = accountArray[0].name;
        } else {
            account = DUMMY_ACCOUNT_NAME;
        }
        AccountUtils
                .setActiveAccount(InstrumentationRegistry.getTargetContext(),
                        account);
        return account;
    }
}
