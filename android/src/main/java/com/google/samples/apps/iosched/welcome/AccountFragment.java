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
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * The attending in person fragment in the welcome screen.
 */
public class AccountFragment extends WelcomeFragment
        implements WelcomeActivity.WelcomeActivityContent, RadioGroup.OnCheckedChangeListener {
    private static final String TAG = makeLogTag(AccountFragment.class);

    private AccountManager mAccountManager;
    private List<Account> mAccounts;
    private String mSelectedAccount;

    @Override
    public boolean shouldDisplay(Context context) {
        Account account = AccountUtils.getActiveAccount(context);
        if (account == null) {
            return true;
        }
        return false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mAccountManager = AccountManager.get(activity);
        mAccounts = new ArrayList<Account>(
                Arrays.asList(mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAccountManager = null;
        mAccounts = null;
        mSelectedAccount = null;
    }

    @Override
    protected View.OnClickListener getPositiveListener() {
        return new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(View v) {
                // Ensure we don't run this fragment again
                LOGD(TAG, "Marking attending flag.");
                AccountUtils.setActiveAccount(mActivity, mSelectedAccount.toString());
                doNext();
            }
        };
    }

    @Override
    protected View.OnClickListener getNegativeListener() {
        return new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(View v) {
                // Nothing to do here
                LOGD(TAG, "User needs to select an account.");
                doFinish();
            }
        };
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        RadioButton rb = (RadioButton) group.findViewById(checkedId);
        mSelectedAccount = rb.getText().toString();
        LOGD(TAG, "Checked: " + mSelectedAccount);

        if (mActivity instanceof WelcomeFragmentContainer) {
            ((WelcomeFragmentContainer) mActivity).setPositiveButtonEnabled(true);
        }
    }

    @Override
    protected String getPositiveText() {
        return getResourceString(R.string.ok);
    }

    @Override
    protected String getNegativeText() {
        return getResourceString(R.string.cancel);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.welcome_account_fragment, container, false);
        if (mAccounts == null) {
            LOGD(TAG, "No accounts to display.");
            return null;
        }

        if (mActivity instanceof WelcomeFragmentContainer) {
            ((WelcomeFragmentContainer) mActivity).setPositiveButtonEnabled(false);
        }

        // Find the view
        RadioGroup accountsContainer = (RadioGroup) layout.findViewById(R.id.welcome_account_list);
        accountsContainer.removeAllViews();
        accountsContainer.setOnCheckedChangeListener(this);

        // Create the child views
        for (Account account : mAccounts) {
            LOGD(TAG, "Account: " + account.name);
            RadioButton button = new RadioButton(mActivity);
            button.setText(account.name);
            accountsContainer.addView(button);
        }

        return layout;
    }
}
