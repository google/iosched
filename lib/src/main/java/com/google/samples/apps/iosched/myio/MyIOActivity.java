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

package com.google.samples.apps.iosched.myio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.myschedule.ScheduleView;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.signin.SignInListener;
import com.google.samples.apps.iosched.signin.SignInManager;
import com.google.samples.apps.iosched.ui.BaseActivity;

import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.WelcomeUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Activity that shows a user's schedule and allows the user to sign in and sign out.
 */
public class MyIOActivity extends BaseActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, SignInListener {

    private static final String TAG = makeLogTag(MyIOActivity.class);

    private static final String SCREEN_LABEL = "My I/O";

    private GoogleApiClient mGoogleApiClient;

    /**
     * Used when sign out is initiated and GoogleApiClient isn't connected.
     */
    private boolean mSignOutPending = false;

    private MenuItem mAvatar;

    private SignInManager mSignInManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myio_act);

        GoogleSignInOptions gso = SignInManager.getGoogleSignInOptions(
                BuildConfig.DEFAULT_WEB_CLIENT_ID);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mSignInManager = new SignInManager(this, this, mGoogleApiClient);
    }

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.MY_IO;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        final Fragment contentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.my_content);

        if (contentFragment instanceof ScheduleView) {
            return ((ScheduleView) contentFragment).canSwipeRefreshChildScrollUp();
        }

        return false;
    }

    @Override
    protected String getScreenLabel() {
        return SCREEN_LABEL;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mAvatar = menu.findItem(R.id.menu_avatar);
        if (AccountUtils.hasActiveAccount(this)) {
            showAvatar();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.my_io, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_avatar) {
            showDialogFragment();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mSignInManager.onActivityResult(requestCode, resultCode, data);
    }

    private void showAvatar() {
        Uri photoUrl = AccountUtils.getActiveAccountPhotoUrl(this);
        if (photoUrl == null) {
            return;
        }
        Glide.with(this).load(photoUrl.toString()).asBitmap()
             .into(new SimpleTarget<Bitmap>(100, 100) {
                 @Override
                 public void onResourceReady(Bitmap resource,
                         GlideAnimation glideAnimation) {
                     RoundedBitmapDrawable circularBitmapDrawable =
                             RoundedBitmapDrawableFactory.create(getResources(), resource);
                     circularBitmapDrawable.setCircular(true);
                     mAvatar.setIcon(circularBitmapDrawable);
                 }
             });
    }


    void showDialogFragment() {
        FragmentManager fm = getSupportFragmentManager();
        MyIODialogFragment myIODialogFragment = MyIODialogFragment.newInstance();
        myIODialogFragment.show(fm, "my_io_signed_in_dialog_frag");
    }

    public void signIn() {
        mSignInManager.signIn();
    }

    public void signOut() {
        if (!mGoogleApiClient.isConnected()) {
            mSignOutPending = true;
            mGoogleApiClient.connect();
        }
        mSignInManager.signOut();
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        LOGD(TAG, "GoogleApiClient connected");
        if (mSignOutPending) {
            mSignInManager.signOut();
            mSignOutPending = false;
        }
    }

    /**
     Asks {@link MyIOFragment} to remove the post onboarding message card.
     */
    public void removePostOnboardingMessageCard() {
        WelcomeUtils.markHidePostOnboardingCard(this);
        final MyIOFragment contentFragment = (MyIOFragment) getSupportFragmentManager()
                .findFragmentById(R.id.my_content);
        contentFragment.removePostOnboardingMessageCard();
    }

    @Override
    public void onConnectionSuspended(final int i) {
        LOGW(TAG, "GoogleApiClient suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        LOGW(TAG, "onConnectionFailed");
        // Anything resolvable is automatically resolved by automanage. Failure is not resolvable.
        Toast.makeText(this, R.string.google_play_services_failed,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSignIn(GoogleSignInResult result) {
        showAvatar();
        removePostOnboardingMessageCard();
    }

    @Override
    public void onSignInFailed(GoogleSignInResult result) {
        LOGW(TAG, "Failed to sign in: " + result);
        Toast.makeText(this, getString(R.string.signin_failed_text), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSignOut(Status status) {
        removePostOnboardingMessageCard();
        mAvatar.setIcon(getResources().getDrawable(R.drawable.ic_default_avatar));
    }

    @Override
    public void onSignOutFailed(Status status) {
        Toast.makeText(this, getString(R.string.signout_failed_text), Toast.LENGTH_LONG).show();
    }
}
