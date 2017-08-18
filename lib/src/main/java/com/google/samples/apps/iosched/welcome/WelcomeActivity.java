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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.myio.MyIOActivity;
import com.google.samples.apps.iosched.settings.SettingsUtils;

import com.google.samples.apps.iosched.signin.SignInListener;
import com.google.samples.apps.iosched.signin.SignInManager;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.RegistrationUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.util.WelcomeUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Responsible for presenting a series of fragments to the user who has just installed the app as
 * part of the welcome/onboarding experience.
 */
public class WelcomeActivity extends AppCompatActivity
        implements WelcomeFragment.WelcomeFragmentContainer,
        GoogleApiClient.OnConnectionFailedListener, SignInListener {

    private static final String TAG = makeLogTag(WelcomeActivity.class);

    private SignInManager mSignInManager;

    private GoogleApiClient mGoogleApiClient;

    WelcomeFragment mContentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        mContentFragment = getCurrentFragment(this);
        // If there's no fragment to use, we're done.
        if (mContentFragment == null) {
            finish();
        } else {
            // Wire up the fragment.
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            Fragment existing = fragmentManager.findFragmentById(R.id.welcome_content);
            if (existing != null) {
                fragmentTransaction.remove(existing);
            }
            fragmentTransaction.add(R.id.welcome_content, mContentFragment);
            fragmentTransaction.commit();
            CollapsingToolbarLayout ctl = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
            ctl.setBackgroundResource(mContentFragment.getHeaderColorRes());
            ImageView logo = (ImageView) ctl.findViewById(R.id.logo);
            logo.setImageResource(mContentFragment.getLogoDrawableRes());
        }
        
        GoogleSignInOptions gso = SignInManager.getGoogleSignInOptions(
                BuildConfig.DEFAULT_WEB_CLIENT_ID);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mSignInManager = new SignInManager(this, this, mGoogleApiClient);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Show the debug warning if debug tools are enabled and it hasn't been shown yet.
        if (!BuildConfig.SUPPRESS_DOGFOOD_WARNING &&
                BuildConfig.ENABLE_DEBUG_TOOLS && !SettingsUtils.wasDebugWarningShown(this)) {
            displayDogfoodWarningDialog();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mSignInManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Displays dogfood build warning and marks that the warning was shown.
     */
    private void displayDogfoodWarningDialog() {
        new AlertDialog.Builder(this)
                .setTitle(Config.DOGFOOD_BUILD_WARNING_TITLE)
                .setMessage(Config.DOGFOOD_BUILD_WARNING_TEXT)
                .setPositiveButton(android.R.string.ok, null).show();
        SettingsUtils.markDebugWarningShown(this);
    }

    /**
     * Gets the current fragment to display.
     *
     * @param context the application context.
     * @return the fragment to display, or null if there is no fragment.
     */
    private static WelcomeFragment getCurrentFragment(Context context) {
        List<WelcomeFragment> welcomeActivityContents = getWelcomeFragments();

        for (WelcomeFragment fragment : welcomeActivityContents) {
            if (fragment.shouldDisplay(context)) {
                return fragment;
            }
        }
        return null;
    }

    /**
     * Tracks whether to display this activity.
     *
     * @param context the application context.
     * @return true if the activity should be displayed, otherwise false.
     */
    public static boolean shouldDisplay(Context context) {
        return getCurrentFragment(context) != null;
    }

    /**
     * Returns all fragments displayed by {@link WelcomeActivity}.
     */
    private static List<WelcomeFragment> getWelcomeFragments() {
        return new ArrayList<WelcomeFragment>() {{
            add(new TosFragment());
            add(new NotificationsFragment());
            add(new SignInFragment());
        }};
    }

    public void signIn() {
        mSignInManager.signIn();
    }

    @Override
    public Button getPrimaryButton() {
        return (Button) findViewById(R.id.button_accept);
    }

    @Override
    public void setPrimaryButtonEnabled(Boolean enabled) {
        getPrimaryButton().setEnabled(enabled);
    }

    @Override
    public Button getSecondaryButton() {
        return (Button) findViewById(R.id.button_decline);
    }

    @Override
    public void setButtonBarVisibility(boolean isVisible) {
        findViewById(R.id.welcome_button_bar).setVisibility(isVisible ? View.VISIBLE : View.GONE);
        if (!isVisible) {
            ((ViewGroup.MarginLayoutParams) findViewById(R.id.welcome_scrolling_content)
                    .getLayoutParams()).bottomMargin = 0;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        LOGW(TAG, "onConnectionFailed");
        // Anything resolvable is automatically resolved by automanage. Failure is not resolvable.
        Toast.makeText(this, R.string.google_play_services_failed,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSignIn(final GoogleSignInResult result) {
        doNext();
    }

    @Override
    public void onSignOut(final Status status) {
        // no-op, since we don't implement sign out in this activity.
    }

    /**
     * Proceed to the next activity.
     */
    public void doNext() {
        LOGD(TAG, "Proceeding to next activity");
        Intent intent = new Intent(this, MyIOActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onSignInFailed(final GoogleSignInResult result) {
        LOGW(TAG, "Failed to sign in: " + result);
        Toast.makeText(this, getString(R.string.signin_failed_text), Toast.LENGTH_LONG).show();
        // TODO: show alert?
        doNext();
    }

    @Override
    public void onSignOutFailed(final Status status) {
        // no-op, since we don't implement sign out in this activity.
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
