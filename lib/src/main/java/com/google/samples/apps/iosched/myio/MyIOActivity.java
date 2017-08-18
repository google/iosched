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

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.schedule.ScheduleView;
import com.google.samples.apps.iosched.signin.RegistrationStatusService;
import com.google.samples.apps.iosched.signin.SignInListener;
import com.google.samples.apps.iosched.signin.SignInManager;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.RegistrationUtils;
import com.google.samples.apps.iosched.util.SyncUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.WelcomeUtils;

import java.util.Date;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
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

    // intent extras used to show an arbitrary message sent via FCM
    public static final String EXTRA_DIALOG_TITLE
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_TITLE";
    public static final String EXTRA_DIALOG_MESSAGE
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_MESSAGE";
    public static final String EXTRA_DIALOG_YES
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_YES";
    public static final String EXTRA_DIALOG_NO
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_NO";
    public static final String EXTRA_DIALOG_URL
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_URL";

    /** How often the registration check should be refreshed for logged-in users. */
    private static final long REG_CHECK_REFRESH_PERIOD = TimeUtils.HOUR * 12;

    private GoogleApiClient mGoogleApiClient;

    /**
     * Used when sign out is initiated and GoogleApiClient isn't connected.
     */
    private boolean mSignOutPending = false;

    private MenuItem mAvatar;

    private SignInManager mSignInManager;

    private boolean mIsResumed;

    /**
     * Reference to Firebase RTDB.
     */
    private DatabaseReference mDatabaseReference;

    /**
     * Listener used to calculate server time offset.
     * TODO (b/36976685): collect server time offset at other places in the app when connecting to RTDB.
     */
    private ValueEventListener mValueEventListener;


    // -- BaseActivity overrides

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
    protected String getAnalyticsScreenLabel() {
        return SCREEN_LABEL;
    }

    @Override
    protected int getNavigationTitleId() {
        return R.string.title_my_io;
    }

    // -- Lifecycle callbacks

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myio_act);
        setFullscreenLayout();

        GoogleSignInOptions gso = SignInManager.getGoogleSignInOptions(
                BuildConfig.DEFAULT_WEB_CLIENT_ID);

        mGoogleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(
                Auth.GOOGLE_SIGN_IN_API, gso).build();

        mSignInManager = new SignInManager(this, this, mGoogleApiClient);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference(
                SyncUtils.SERVER_TIME_OFFSET_PATH);

        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                double offset = snapshot.getValue(Double.class);
                SyncUtils.setServerTimeOffset(MyIOActivity.this, (int) offset);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                LOGW(TAG, "Listener was cancelled");
            }
        };
    }


    @Override
    protected void onStart() {
        super.onStart();
        // We don't want to keep hitting RTDB needlessly, so we throttle how often we attach
        // a listener to get the server time offset.
        if (SyncUtils.serverTimeOffsetNeverSet(this) ||
                (new Date().getTime() - SyncUtils.SERVER_TIME_OFFSET_INTERVAL) >=
                        SyncUtils.getServerTimeOffsetSetAt(this)) {
            mDatabaseReference.addValueEventListener(mValueEventListener);
        }

        // Check if an earlier attempt to get the user's registration status failed
        // (or somehow never occured), and reattempt if necessary.
        //
        // We'll also periodically re-run this, in case the user's status has changed.
        long timeSinceLastRegCheck = RegistrationUtils.timeSinceLastRegCheck(this);
        LOGI(TAG, "Time since last reg check:" + timeSinceLastRegCheck);
        LOGI(TAG, "Status:" + RegistrationUtils.isRegisteredAttendee(this));

        if (AccountUtils.hasActiveAccount(this) && (
                timeSinceLastRegCheck > REG_CHECK_REFRESH_PERIOD ||
                        RegistrationUtils.isRegisteredAttendee(this) == RegistrationUtils.REGSTATUS_UNKNOWN)) {
            RegistrationStatusService.updateRegStatusInBackground(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResumed = true;
        showAnnouncementDialogIfNeeded(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsResumed = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mValueEventListener != null) {
            mDatabaseReference.removeEventListener(mValueEventListener);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showAnnouncementDialogIfNeeded(intent);
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

    // -- Auth

    private void showAvatar() {
        if (mAvatar == null) {
            // Attempt to update avatar image, but avatar view doesn't exist yet
            return;
        }

        mAvatar.setTitle(R.string.description_avatar_signed_in);
        Uri photoUrl = AccountUtils.getActiveAccountPhotoUrl(this);
        if (photoUrl == null) {
            return;
        }
        Glide.with(this).load(photoUrl.toString()).asBitmap()
             .into(new SimpleTarget<Bitmap>(100, 100) {
                 @Override
                 public void onResourceReady(Bitmap resource,
                         GlideAnimation glideAnimation) {
                     if (mAvatar == null) {
                         return;
                     }
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
        mAvatar.setTitle(R.string.description_avatar_signed_out);
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
     * Asks {@link MyIOFragment} to remove the post onboarding message card.
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
        if(result != null) {
            LOGW(TAG, "Failed to sign in: status code == " + result.getStatus().getStatusCode());
        }
        Toast.makeText(this, getString(R.string.signin_failed_text), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSignOut(Status status) {
        removePostOnboardingMessageCard();
        mAvatar.setIcon(getResources().getDrawable(R.drawable.ic_default_avatar_toolbar));
        mAvatar.setTitle(R.string.description_avatar_signed_out);
    }

    @Override
    public void onSignOutFailed(Status status) {
        Toast.makeText(this, getString(R.string.signout_failed_text), Toast.LENGTH_LONG).show();
    }

    // -- Announcement dialog. TODO this may no longer be used

    private void showAnnouncementDialogIfNeeded(Intent intent) {
        if (!mIsResumed) {
            // we are called from onResume, so defer until then
            return;
        }

        final String title = intent.getStringExtra(EXTRA_DIALOG_TITLE);
        final String message = intent.getStringExtra(EXTRA_DIALOG_MESSAGE);

        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(message)) {
            final String yes = intent.getStringExtra(EXTRA_DIALOG_YES);
            final String no = intent.getStringExtra(EXTRA_DIALOG_NO);
            final String url = intent.getStringExtra(EXTRA_DIALOG_URL);
            LOGD(TAG, String.format(
                    "showAnnouncementDialog: {\ntitle: %s\nmesg: %s\nyes: %s\nno %s\nurl: %s\n}",
                    title, message, yes, no, url));

            final SpannableString spannable = new SpannableString(message == null ? "" : message);
            Linkify.addLinks(spannable, Linkify.WEB_URLS);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title);
            }
            builder.setMessage(spannable);
            if (!TextUtils.isEmpty(no)) {
                builder.setNegativeButton(no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            }
            if (!TextUtils.isEmpty(yes)) {
                builder.setPositiveButton(yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
                });
            }
            final AlertDialog dialog = builder.create();
            dialog.show();
            final TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                // makes the embedded links in the text clickable, if there are any
                messageView.setMovementMethod(LinkMovementMethod.getInstance());
            }

            // remove the extras so we don't trigger again
            intent.removeExtra(EXTRA_DIALOG_TITLE);
            intent.removeExtra(EXTRA_DIALOG_MESSAGE);
            intent.removeExtra(EXTRA_DIALOG_YES);
            intent.removeExtra(EXTRA_DIALOG_NO);
            intent.removeExtra(EXTRA_DIALOG_URL);
            setIntent(intent);
        }
    }
}
