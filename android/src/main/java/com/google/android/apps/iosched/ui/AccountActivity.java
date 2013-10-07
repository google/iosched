/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.sync.SyncHelper;
import com.google.android.apps.iosched.util.AccountUtils;
import com.google.android.apps.iosched.util.PrefUtils;
import com.google.android.apps.iosched.util.WiFiUtils;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.PersonBuffer;

import java.util.Arrays;
import java.util.List;

import static com.google.android.apps.iosched.util.LogUtils.LOGE;
import static com.google.android.apps.iosched.util.LogUtils.LOGW;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

public class AccountActivity extends ActionBarActivity
        implements AccountUtils.AuthenticateCallback, GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, PlusClient.OnPeopleLoadedListener {

    private static final String TAG = makeLogTag(AccountActivity.class);

    public static final String EXTRA_FINISH_INTENT
            = "com.google.android.iosched.extra.FINISH_INTENT";

    private static final int SETUP_ATTENDEE = 1;
    private static final int SETUP_WIFI = 2;

    private static final String KEY_CHOSEN_ACCOUNT = "chosen_account";

    private static final int REQUEST_AUTHENTICATE = 100;
    private static final int REQUEST_RECOVER_FROM_AUTH_ERROR = 101;
    private static final int REQUEST_RECOVER_FROM_PLAY_SERVICES_ERROR = 102;
    private static final int REQUEST_PLAY_SERVICES_ERROR_DIALOG = 103;

    private static final String POST_AUTH_CATEGORY
            = "com.google.android.iosched.category.POST_AUTH";

    private Account mChosenAccount;
    private Intent mFinishIntent;
    private boolean mCancelAuth = false;
    private boolean mAuthInProgress = false;
    private boolean mAuthProgressFragmentResumed = false;
    private boolean mCanRemoveAuthProgressFragment = false;
    private PlusClient mPlusClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_letterboxed_when_large);

        final Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_FINISH_INTENT)) {
            mFinishIntent = intent.getParcelableExtra(EXTRA_FINISH_INTENT);
        }

        if (savedInstanceState == null) {
            if (!AccountUtils.isAuthenticated(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.root_container, new SignInMainFragment(), "signin_main")
                        .commit();
            } else {
                mChosenAccount = new Account(AccountUtils.getChosenAccountName(this), "com.google");
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.root_container,
                                SignInSetupFragment.makeFragment(SETUP_ATTENDEE), "setup_attendee")
                        .commit();
            }
        } else {
            String accountName = savedInstanceState.getString(KEY_CHOSEN_ACCOUNT);
            if (accountName != null) {
                mChosenAccount = new Account(accountName, "com.google");
                mPlusClient = (new PlusClient.Builder(this, this, this))
                        .setAccountName(accountName)
                        .setScopes(AccountUtils.AUTH_SCOPES)
                        .build();
            }
        }
    }

    @Override
    public void onRecoverableException(final int code) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Dialog d = GooglePlayServicesUtil.getErrorDialog(
                        code,
                        AccountActivity.this,
                        REQUEST_RECOVER_FROM_PLAY_SERVICES_ERROR);
                d.show();
            }
        });
    }

    @Override
    public void onUnRecoverableException(final String errorMessage) {
        LOGW(TAG, "Encountered unrecoverable exception: " + errorMessage);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mChosenAccount != null)
            outState.putString(KEY_CHOSEN_ACCOUNT, mChosenAccount.name);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_AUTHENTICATE ||
                requestCode == REQUEST_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_PLAY_SERVICES_ERROR_DIALOG) {
            if (resultCode == RESULT_OK) {
                if (mPlusClient != null) mPlusClient.connect();
            } else {
                if (mAuthProgressFragmentResumed) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getSupportFragmentManager().popBackStack();
                        }
                    });
                } else {
                    mCanRemoveAuthProgressFragment = true;
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthInProgress) mCancelAuth = true;
        if (mPlusClient != null)
            mPlusClient.disconnect();
    }

    @Override
    public void onPeopleLoaded(ConnectionResult status, PersonBuffer personBuffer,
            String nextPageToken) {
        if (status.isSuccess()) {
            if (personBuffer != null && personBuffer.getCount() > 0) {
                // Set the profile id
                AccountUtils.setPlusProfileId(this, personBuffer.get(0).getId());
            }
        } else {
            LOGE(TAG, "Got " + status.getErrorCode() + ". Could not load plus profile.");
        }
    }

    public static class SignInMainFragment extends Fragment {
        public SignInMainFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.fragment_login_main, container, false);
            TextView descriptionTextView = (TextView) rootView.findViewById(
                    R.id.sign_in_description);
            descriptionTextView.setText(Html.fromHtml(getString(R.string.description_sign_in_main)));
            SignInButton signinButtonView = (SignInButton) rootView.findViewById(R.id.sign_in_button);
            signinButtonView.setSize(SignInButton.SIZE_WIDE);
            signinButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.root_container, new ChooseAccountFragment(),
                                    "choose_account")
                            .addToBackStack("signin_main")
                            .commit();
                }
            });
            return rootView;
        }
    }

    public static class SignInSetupFragment extends ListFragment {
        private static final String ARG_SETUP_ID = "setupId";

        private int mDescriptionHeaderResId = 0;
        private int mDescriptionBodyResId = 0;
        private int mSelectionResId = 0;
        private int mSetupId;

        private static final int ATCONF_DIMEN_INDEX = 1;

        public SignInSetupFragment() {}

        public static Fragment makeFragment(int setupId) {
            Bundle args = new Bundle();
            args.putInt(ARG_SETUP_ID, setupId);
            SignInSetupFragment fragment = new SignInSetupFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            mSetupId = getArguments().getInt(ARG_SETUP_ID);
            switch (mSetupId) {
                case SETUP_ATTENDEE:
                    mDescriptionHeaderResId = R.string.description_setup_attendee_mode_header;
                    mDescriptionBodyResId = R.string.description_setup_attendee_mode_body;
                    mSelectionResId = R.array.selection_setup_attendee;
                    break;
                case SETUP_WIFI:
                    mDescriptionHeaderResId = R.string.description_setup_wifi_header;
                    mDescriptionBodyResId = R.string.description_setup_wifi_body;
                    mSelectionResId = R.array.selection_setup_wifi;
                    break;
                default:
                    throw new IllegalArgumentException("Undefined setup id.");
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.fragment_login_setup, container, false);
            final TextView descriptionView = (TextView) rootView.findViewById(
                    R.id.login_setup_desc);
            descriptionView.setText(Html.fromHtml(getString(mDescriptionHeaderResId) +
                    getString(mDescriptionBodyResId)));
            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            setListAdapter(
                    new ArrayAdapter<String> (getActivity(),
                            R.layout.list_item_login_option,
                            getResources().getStringArray(mSelectionResId)));
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            final Activity activity = getActivity();
            if (mSetupId == SETUP_ATTENDEE) {
                if (position == 0) {
                    // Attendee is at the conference.  If WiFi AP isn't set already, go to
                    // the WiFi set up screen.  Otherwise, set up is done.
                    PrefUtils.setAttendeeAtVenue(activity, true);
                    PrefUtils.setUsingLocalTime(activity, false);
                    // If WiFi has already been configured, set up is complete.  Otherwise,
                    // show the WiFi AP configuration screen.
                    //if (WiFiUtils.shouldInstallWiFi(activity)) {
                    if (WiFiUtils.shouldBypassWiFiSetup(activity)) {
                        ((AccountActivity)activity).finishSetup();
                    } else {
                        getFragmentManager().beginTransaction()
                                .replace(R.id.root_container,
                                        SignInSetupFragment.makeFragment(SETUP_WIFI), "setup_wifi")
                                .addToBackStack("setup_attendee")
                                .commit();
                    }
                    EasyTracker.getTracker()
                            .setCustomDimension(ATCONF_DIMEN_INDEX,"conference attendee");

                } else if (position == 1) {
                    // Attendee is remote.  Set up is done.
                    PrefUtils.setAttendeeAtVenue(activity, false);
                    PrefUtils.setUsingLocalTime(activity, true);
                    EasyTracker.getTracker()
                            .setCustomDimension(ATCONF_DIMEN_INDEX,"remote attendee");
                    ((AccountActivity)activity).finishSetup();
                }
            } else if (mSetupId == SETUP_WIFI) {
                if (position == 0) {
                    WiFiUtils.setWiFiConfigStatus(activity, WiFiUtils.WIFI_CONFIG_REQUESTED);
                }
                // Done with set up.
                ((AccountActivity)activity).finishSetup();
            }
        }
    }

    public static class ChooseAccountFragment extends ListFragment {
        public ChooseAccountFragment() {
        }

        @Override
        public void onResume() {
            super.onResume();
            reloadAccountList();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.fragment_login_choose_account, container, false);
            TextView descriptionView = (TextView) rootView.findViewById(R.id.choose_account_intro);
            descriptionView.setText(Html.fromHtml(getString(R.string.description_choose_account)));
            return rootView;
        }

        private AccountListAdapter mAccountListAdapter;

        private void reloadAccountList() {
            if (mAccountListAdapter != null) {
                mAccountListAdapter = null;
            }

            AccountManager am = AccountManager.get(getActivity());
            Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            mAccountListAdapter = new AccountListAdapter(getActivity(), Arrays.asList(accounts));
            setListAdapter(mAccountListAdapter);
        }

        private class AccountListAdapter extends ArrayAdapter<Account> {
            private static final int LAYOUT_RESOURCE = R.layout.list_item_login_option;

            public AccountListAdapter(Context context, List<Account> accounts) {
                super(context, LAYOUT_RESOURCE, accounts);
            }

            private class ViewHolder {
                TextView text1;
            }

            @Override
            public int getCount() {
                return super.getCount() + 1;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;

                if (convertView == null) {
                    convertView = getLayoutInflater(null).inflate(LAYOUT_RESOURCE, null);

                    holder = new ViewHolder();
                    holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);

                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                if (position == getCount() - 1) {
                    holder.text1.setText(R.string.description_add_account);
                } else {
                    final Account account = getItem(position);

                    if (account != null) {
                        holder.text1.setText(account.name);
                    } else {
                        holder.text1.setText("");
                    }
                }

                return convertView;
            }
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            if (position == mAccountListAdapter.getCount() - 1) {
                Intent addAccountIntent = new Intent(Settings.ACTION_ADD_ACCOUNT);
                addAccountIntent.putExtra(Settings.EXTRA_AUTHORITIES,
                        new String[]{ScheduleContract.CONTENT_AUTHORITY});
                startActivity(addAccountIntent);
                return;
            }

            AccountActivity activity = (AccountActivity) getActivity();
            ConnectivityManager cm = (ConnectivityManager)
                    activity.getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                Toast.makeText(activity, R.string.no_connection_cant_login,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            activity.mCancelAuth = false;
            activity.mChosenAccount = mAccountListAdapter.getItem(position);
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.root_container, new AuthProgressFragment(), "loading")
                    .addToBackStack("choose_account")
                    .commit();

            PlusClient.Builder builder = new PlusClient.Builder(activity, activity, activity);
            activity.mPlusClient = builder
                    .setScopes(AccountUtils.AUTH_SCOPES)
                    .setAccountName(activity.mChosenAccount.name).build();
            activity.mPlusClient.connect();
        }
    }

    public static class AuthProgressFragment extends Fragment {
        private static final int TRY_AGAIN_DELAY_MILLIS = 7 * 1000; // 7 seconds
        private final Handler mHandler = new Handler();

        public AuthProgressFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_login_loading,
                    container, false);

            final View takingAWhilePanel = rootView.findViewById(R.id.taking_a_while_panel);
            final View tryAgainButton = rootView.findViewById(R.id.retry_button);
            tryAgainButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getFragmentManager().popBackStack();
                }
            });

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isAdded()) {
                        return;
                    }
                    if (AccountUtils.isAuthenticated(getActivity())) {
                        ((AccountActivity) getActivity()).onAuthTokenAvailable();
                        return;
                    }
                    takingAWhilePanel.setVisibility(View.VISIBLE);
                }
            }, TRY_AGAIN_DELAY_MILLIS);
            return rootView;
        }

        @Override
        public void onPause() {
            super.onPause();
            ((AccountActivity) getActivity()).mAuthProgressFragmentResumed = false;
        }

        @Override
        public void onResume() {
            super.onResume();
            ((AccountActivity) getActivity()).mAuthProgressFragmentResumed = true;
            if (((AccountActivity) getActivity()).mCanRemoveAuthProgressFragment) {
                ((AccountActivity) getActivity()).mCanRemoveAuthProgressFragment = false;
                getFragmentManager().popBackStack();
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            ((AccountActivity) getActivity()).mCancelAuth = true;
        }
    }

    private void tryAuthenticate() {
        // Authenticate through the Google Play OAuth client.
        mAuthInProgress = true;
        AccountUtils.tryAuthenticate(this, this, mChosenAccount.name,
                REQUEST_RECOVER_FROM_AUTH_ERROR);
    }

    @Override
    public boolean shouldCancelAuthentication() {
        return mCancelAuth;
    }

    @Override
    public void onAuthTokenAvailable() {
        // Cancel progress fragment.
        // Create set up fragment.
        mAuthInProgress = false;
        if (mAuthProgressFragmentResumed) {
            getSupportFragmentManager().popBackStack();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.root_container,
                            SignInSetupFragment.makeFragment(SETUP_ATTENDEE), "setup_attendee")
                    .addToBackStack("signin_main")
                    .commit();
        }
    }

    private void finishSetup() {
        ContentResolver.setIsSyncable(mChosenAccount, ScheduleContract.CONTENT_AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(mChosenAccount, ScheduleContract.CONTENT_AUTHORITY, true);
        SyncHelper.requestManualSync(mChosenAccount);
        PrefUtils.markSetupDone(this);

        if (mFinishIntent != null) {
            // Ensure the finish intent is unique within the task. Otherwise, if the task was
            // started with this intent, and it finishes like it should, then startActivity on
            // the intent again won't work.
            mFinishIntent.addCategory(POST_AUTH_CATEGORY);
            startActivity(mFinishIntent);
        }

        finish();
    }

    // Google Plus client callbacks.
    @Override
    public void onConnected(Bundle connectionHint) {
        // It is possible that the authenticated account doesn't have a profile.
        mPlusClient.loadPeople(this, "me");
        tryAuthenticate();
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this,
                        REQUEST_RECOVER_FROM_AUTH_ERROR);
            } catch (IntentSender.SendIntentException e) {
                LOGE(TAG, "Internal error encountered: " + e.getMessage());
            }
            return;
        }

        final int errorCode = connectionResult.getErrorCode();
        if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
            GooglePlayServicesUtil.getErrorDialog(errorCode, this,
                    REQUEST_PLAY_SERVICES_ERROR_DIALOG).show();
        }
    }
}
