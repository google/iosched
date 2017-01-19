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

package com.google.samples.apps.iosched.sync.account;

import android.app.Activity;

import static android.content.Context.ACCOUNT_SERVICE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Responsible for registering our custom authenticator with the system, and allowing other
 * classes to obtain a handle to our sync account.
 */
public class Account {
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "com.google.samples.apps.iosched";
    // The account name
    public static final String ACCOUNT_NAME = "Sync Account";

    private static final String TAG = makeLogTag(Account.class);
    private static android.accounts.Account mAccount;

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param activity The application context
     */
    public static android.accounts.Account createSyncAccount(Activity activity) {
        android.accounts.AccountManager accountManager =
                (android.accounts.AccountManager) activity.getSystemService(
                        ACCOUNT_SERVICE);

        // Register account with system
        android.accounts.Account account = getAccount();
        if (accountManager.addAccountExplicitly(account, null, null)) {
            return account;
        } else {
            LOGE(TAG, "Unable to create account");
            return null;
        }
    }

    /** Get the account object for this application.
     *
     * <p>Note that, since this is just used for sync adapter purposes, this object will always
     * be the same.
     *
     * @return account
     */
    public static android.accounts.Account getAccount() {
        if (mAccount == null) {
            mAccount = new android.accounts.Account(ACCOUNT_NAME, ACCOUNT_TYPE);
        }
        return mAccount;
    }

}
