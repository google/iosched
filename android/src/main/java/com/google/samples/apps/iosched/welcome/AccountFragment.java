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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.PermissionsUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * The account selection and runtime permission enforcement fragment in the welcome screen. Only
 * runtime permissions required for basic app functionality should be included as part of the
 * welcome flow.
 */
public class AccountFragment extends WelcomeFragment implements RadioGroup.OnCheckedChangeListener {
    public static final String[] APP_REQUIRED_PERMISSIONS =
            new String[]{Manifest.permission.GET_ACCOUNTS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_PERMISSION_REQUEST_CODE = 111;
    private static final String TAG = makeLogTag(AccountFragment.class);
    private AccountManager mAccountManager;
    private List<Account> mAccounts;
    private View mLayout;
    private LayoutInflater mLayoutInflater;
    private String mSelectedAccount;
    private WeakReference<Snackbar> mSnackbar;

    @Override
    protected View.OnClickListener getPrimaryButtonListener() {
        return new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(View v) {
                // Ensure we don't run this fragment again
                LOGD(TAG, "Marking attending flag.");
                AccountUtils.setActiveAccount(mActivity, mSelectedAccount);
                doNext();
            }
        };
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        RadioButton rb = (RadioButton) group.findViewById(checkedId);

        mSelectedAccount = rb.getText().toString();
        LOGD(TAG, "Checked: " + mSelectedAccount);

        if (mActivity instanceof WelcomeFragmentContainer) {
            ((WelcomeFragmentContainer) mActivity).setPrimaryButtonEnabled(true);
        }
    }

    @Override
    protected String getPrimaryButtonText() {
        return getResourceString(R.string.ok);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mLayoutInflater = inflater;

        // Inflate the layout for this fragment
        mLayout = inflater.inflate(R.layout.welcome_account_fragment, container, false);

        if (mActivity instanceof WelcomeFragmentContainer) {
            ((WelcomeFragmentContainer) mActivity).setPrimaryButtonEnabled(false);
        }

        // Force permission request display when Fragment is attached.
        // Note: This can't be done in an onResume lifecycle method because if the permission is in
        // the Do-Not-Again-Ask state a continuous loop is created since the system will auto-deny
        // the permission then resume the activity or fragment.
        displayPermissionRequest(getActivity(), true);

        return mLayout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAccountManager = null;
        mAccounts = null;
        mSelectedAccount = null;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
            @NonNull final String[] permissions,
            @NonNull final int[] grantResults) {
        LOGW(TAG, "onRequestPermissionResult" + grantResults.length + " " + permissions.length);
        if (grantResults.length == APP_REQUIRED_PERMISSIONS.length &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // If permission granted then refresh account list so user can select an account.
            Snackbar snackbar;
            if ((snackbar = mSnackbar.get()) != null && snackbar.isShown()) {
                snackbar.dismiss();
            }
            reloadAccounts();
            refreshAccountListUI();
        } else {
            LOGI(TAG, "onRequestPermissionResult with permissions denied");
            mSnackbar = new WeakReference<>(PermissionsUtils.displayConditionalPermissionDenialSnackbar(getActivity(),
                    R.string.welcome_permissions_rationale, APP_REQUIRED_PERMISSIONS,
                    REQUEST_PERMISSION_REQUEST_CODE));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Display a passive permissions request (Snackbar) any time the activity is resumed, but
        // permission aren't granted.
        if (!displayPermissionRequest(getActivity(), false)) {
            reloadAccounts();
            refreshAccountListUI();
        }
    }

    @Override
    public boolean shouldDisplay(Context context) {
        Account account = AccountUtils.getActiveAccount(context);
        return account == null ||
                !PermissionsUtils.permissionsAlreadyGranted(context, APP_REQUIRED_PERMISSIONS);
    }


    /**
     * Update the UI with the current account list. If no accounts exist the list is cleared.
     */
    private void refreshAccountListUI() {
        // Find the view
        RadioGroup accountsContainer = (RadioGroup) mLayout.findViewById(R.id.welcome_account_list);
        accountsContainer.removeAllViews();
        accountsContainer.setOnCheckedChangeListener(this);

        if (mAccounts == null) {
            LOGW(TAG, "No accounts to display.");
            return;
        }

        // The selected account might be set while the user is on this screen if they revoked some
        // permissions and the app brought them back here.
        String selectedAccount = AccountUtils.getActiveAccountName(getActivity());

        // Create the child views
        for (Account account : mAccounts) {
            LOGD(TAG, "Account: " + account.name);
            final RadioButton accountRadio = (RadioButton) mLayoutInflater.inflate(
                    R.layout.welcome_account_radio, accountsContainer, false);
            accountRadio.setText(account.name);
            accountsContainer.addView(accountRadio);
            if (selectedAccount != null && selectedAccount.equals(account.name)) {
                accountRadio.setSelected(true);
            }
        }
    }

    private void reloadAccounts() {
        mAccountManager = AccountManager.get(getActivity());
        mAccounts = new ArrayList<>(Arrays.asList(
                mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)));
    }

    /**
     * Determines whether permissions rationale is needed. If it is then a Snackbar is presented
     * that will lead to a permissions request resolution, otherwise, if permissions haven't been
     * granted they are requested if the {@code shouldRequestPermissions} is enabled.
     *
     * @return whether a permission request was needed.
     */
    private boolean displayPermissionRequest(final Activity activity,
            final boolean shouldRequestPermissions) {
        LOGW(TAG, "displayPermissionRequest");
        if (PermissionsUtils.permissionsAlreadyGranted(activity, APP_REQUIRED_PERMISSIONS)) {
            return false;
        }

        // Can't check to see if any permissions are in the Do-Not-Ask-Again state here in order
        // to display a Snackbar that forwards to the App Info screen. The approaches to check for
        // this state only work when invoked from the #onRequestPermissionsResult callback.

        if (shouldRequestPermissions) {
            ActivityCompat.requestPermissions(activity, APP_REQUIRED_PERMISSIONS,
                    REQUEST_PERMISSION_REQUEST_CODE);
            return true;
        }

        if (!PermissionsUtils
                .shouldShowAnyPermissionRationale(activity, APP_REQUIRED_PERMISSIONS)) {
            mSnackbar = new WeakReference<>(PermissionsUtils.displayPermissionDeniedAppInfoResolutionSnackbar(activity,
                    R.string.welcome_permissions_rationale));
        } else {
            mSnackbar = new WeakReference<>(PermissionsUtils.displayPermissionRationaleSnackbar(activity,
                    R.string.welcome_permissions_rationale, APP_REQUIRED_PERMISSIONS,
                    REQUEST_PERMISSION_REQUEST_CODE));
        }
        return true;
    }
}
