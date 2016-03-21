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

package com.google.samples.apps.iosched.navigation;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationItemEnum;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.ImageLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This is the implementation of {@link AppNavigationView} using a {@link DrawerLayout}. This
 * extends {@link AppNavigationViewAbstractImpl} so only UI specific methods are implemented.
 */
public class AppNavigationViewAsDrawerImpl extends AppNavigationViewAbstractImpl
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = makeLogTag(AppNavigationViewAsDrawerImpl.class);

    // Delay to launch nav drawer item, to allow close animation to play
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;

    // Fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;

    private DrawerLayout mDrawerLayout;

    // A Runnable that we should execute when the navigation drawer finishes its closing animation
    private Runnable mDeferredOnDrawerClosedRunnable;

    private NavigationView mNavigationView;

    private Spinner mAccountSpinner;

    private AccountSpinnerAdapter mAccountSpinnerAdapter;

    private Handler mHandler;

    private ImageLoader mImageLoader;

    private NavigationDrawerStateListener mNavigationDrawerStateListener;

    public AppNavigationViewAsDrawerImpl(ImageLoader imageLoader,
            NavigationDrawerStateListener listener) {
        mImageLoader = imageLoader;
        mNavigationDrawerStateListener = listener;
    }

    @Override
    public void displayNavigationItems(final NavigationModel.NavigationItemEnum[] items) {
        createNavDrawerItems(items);
        setSelectedNavDrawerItem(mSelfItem);
        setupAccountBox();
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem menuItem) {
        NavigationItemEnum item = NavigationItemEnum.getById(menuItem.getItemId());
        onNavDrawerItemClicked(item);
        return true;
    }

    @Override
    public void setUpView() {
        mHandler = new Handler();

        mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }
        mDrawerLayout.setStatusBarBackgroundColor(
                mActivity.getResources().getColor(R.color.theme_primary_dark));

        mNavigationView = (NavigationView) mActivity.findViewById(R.id.nav_view);

        if (mSelfItem == NavigationModel.NavigationItemEnum.INVALID) {
            // do not show a nav drawer
            mDrawerLayout = null;
            return;
        }

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                // run deferred action, if we have one
                if (mDeferredOnDrawerClosedRunnable != null) {
                    mDeferredOnDrawerClosedRunnable.run();
                    mDeferredOnDrawerClosedRunnable = null;
                }
                mNavigationDrawerStateListener.onNavDrawerStateChanged(false, false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                mNavigationDrawerStateListener.onNavDrawerStateChanged(true, false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                mNavigationDrawerStateListener.onNavDrawerStateChanged(isNavDrawerOpen(),
                        newState != DrawerLayout.STATE_IDLE);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                mNavigationDrawerStateListener.onNavDrawerSlide(slideOffset);
            }
        });

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // When the user runs the app for the first time, we want to land them with the
        // navigation drawer open. But just the first time.
        if (!SettingsUtils.isFirstRunProcessComplete(mActivity)) {
            // first run of the app starts with the nav drawer open
            SettingsUtils.markFirstRunProcessesDone(mActivity, true);
            mDrawerLayout.openDrawer(GravityCompat.START);
        }

        setupAccountBox();
    }

    @Override
    public void showNavigation() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    private void createNavDrawerItems(NavigationModel.NavigationItemEnum[] items) {
        if (mNavigationView != null) {
            Menu menu = mNavigationView.getMenu();
            for (int i = 0; i < items.length; i++) {
                MenuItem item = menu.findItem(items[i].getId());
                if (item != null) {
                    item.setVisible(true);
                    item.setIcon(items[i].getIconResource());
                    item.setTitle(items[i].getTitleResource());
                } else {
                    LOGE(TAG, "Menu Item for navigation item with title " +
                            (items[i].getTitleResource() != 0 ? mActivity.getResources().getString(
                                    items[i].getTitleResource()) : "") + "not found");
                }
            }

            mNavigationView.setNavigationItemSelectedListener(this);
        }
    }

    /**
     * Sets up the given navdrawer item's appearance to the selected state. Note: this could also be
     * accomplished (perhaps more cleanly) with state-based layouts.
     */
    private void setSelectedNavDrawerItem(NavigationItemEnum item) {
        if (mNavigationView != null) {
            mNavigationView.getMenu().findItem(item.getId()).setChecked(true);
        }
    }

    /**
     * Sets up the account box. The account box is the area at the top of the nav drawer that shows
     * which account the user is logged in as, and lets them switch accounts. It also shows the
     * user's Google+ cover photo as background.
     */
    private void setupAccountBox() {

        final View chosenAccountView = mActivity.findViewById(R.id.chosen_account_view);

        if (chosenAccountView == null) {
            //This activity does not have an account box
            return;
        }

        Account chosenAccount = AccountUtils.getActiveAccount(mActivity);
        if (chosenAccount == null) {
            // No account logged in; hide account box
            chosenAccountView.setVisibility(View.GONE);
            return;
        } else {
            chosenAccountView.setVisibility(View.VISIBLE);
        }

        AccountManager am = AccountManager.get(mActivity);
        Account[] accountArray = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        List<Account> accounts = new ArrayList<Account>(Arrays.asList(accountArray));

        // Put selected account at index 0
        accounts.remove(chosenAccount);
        accounts.add(0, chosenAccount);

        ImageView coverImageView = (ImageView) chosenAccountView
                .findViewById(R.id.profile_cover_image);
        ImageView profileImageView = (ImageView) chosenAccountView.findViewById(R.id.profile_image);

        String imageUrl = AccountUtils.getPlusImageUrl(mActivity);
        if (imageUrl != null) {
            mImageLoader.loadImage(imageUrl, profileImageView);
        }

        String coverImageUrl = AccountUtils.getPlusCoverUrl(mActivity);
        if (coverImageUrl != null) {
            mActivity.findViewById(R.id.profile_cover_image_placeholder).setVisibility(View.GONE);
            coverImageView.setVisibility(View.VISIBLE);
            coverImageView.setContentDescription(mActivity.getResources().getString(
                    R.string.navview_header_user_image_content_description));
            mImageLoader.loadImage(coverImageUrl, coverImageView);
            coverImageView
                    .setColorFilter(mActivity.getResources().getColor(R.color.light_content_scrim));
        } else {
            mActivity.findViewById(R.id.profile_cover_image_placeholder).setVisibility(View.VISIBLE);
            coverImageView.setVisibility(View.GONE);
        }

        populateAccountList(accounts);
    }

    /**
     * @param accounts The list of available accounts, the current one being at position 0.
     */
    private void populateAccountList(final List<Account> accounts) {
        mAccountSpinner = (Spinner) mActivity.findViewById(R.id.account_spinner);
        mAccountSpinner.getBackground().setColorFilter(
                mActivity.getResources().getColor(R.color.body_text_1_inverse),
                PorterDuff.Mode.SRC_ATOP);
        mAccountSpinnerAdapter =
                new AccountSpinnerAdapter(mActivity, R.id.profile_name_text,
                        accounts.toArray(new Account[accounts.size()]), mImageLoader);
        mAccountSpinner.setAdapter(mAccountSpinnerAdapter);
        mAccountSpinner.setSelection(0);

        if (accounts.size() > 1) {
            mAccountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> parent, final View view,
                        final int position, final long id) {
                    if (position > 0) {
                        ConnectivityManager cm = (ConnectivityManager)
                                mActivity.getSystemService(Activity.CONNECTIVITY_SERVICE);
                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        if (activeNetwork == null || !activeNetwork.isConnected()) {
                            // If there's no network, don't try to change the selected account
                            Toast.makeText(mActivity, R.string.no_connection_cant_login,
                                    Toast.LENGTH_SHORT).show();
                            mDrawerLayout.closeDrawer(GravityCompat.START);
                        } else {
                            LOGD(TAG,
                                    "User requested switch to account: " + accounts.get(position).name);
                            AccountUtils.setActiveAccount(mActivity, accounts.get(position).name);
                            mLoginStateListener.onAccountChangeRequested();
                            mLoginStateListener.onStartLoginProcessRequested();
                            mDrawerLayout.closeDrawer(GravityCompat.START);
                            setupAccountBox();
                        }
                    }
                }

                @Override
                public void onNothingSelected(final AdapterView<?> parent) {

                }

            });

            mAccountSpinner.setEnabled(true);
        } else {
            mAccountSpinner.setEnabled(false);
        }


    }

    public boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    private void onNavDrawerItemClicked(final NavigationItemEnum item) {
        if (item == mSelfItem) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (isSpecialItem(item)) {
            itemSelected(item);
        } else {
            // Launch the target Activity after a short delay, to allow the close animation to play
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    itemSelected(item);
                }
            }, NAVDRAWER_LAUNCH_DELAY);

            // Change the active item on the list so the user can see the item changed
            setSelectedNavDrawerItem(item);
            // Fade out the main content
            View mainContent = mActivity.findViewById(R.id.main_content);
            if (mainContent != null) {
                mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
            }
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private boolean isSpecialItem(NavigationItemEnum item) {
        return item == NavigationItemEnum.SETTINGS;
    }

    public interface NavigationDrawerStateListener {

        void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating);

        void onNavDrawerSlide(float slideOffset);
    }
}
