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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationItemEnum;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.ui.widget.NavDrawerItemView;
import com.google.samples.apps.iosched.ui.widget.ScrimInsetsScrollView;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This is the implementation of {@link AppNavigationView} using a {@link DrawerLayout}. This extends
 * {@link AppNavigationViewAbstractImpl} so only UI specific methods are implemented.
 */
public class AppNavigationViewAsDrawerImpl extends AppNavigationViewAbstractImpl {

    private static final String TAG = makeLogTag(AppNavigationViewAsDrawerImpl.class);

    private static final int ACCOUNT_BOX_EXPAND_ANIM_DURATION = 200;

    // Delay to launch nav drawer item, to allow close animation to play
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;

    // Fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;

    private DrawerLayout mDrawerLayout;

    // Views that correspond to each navdrawer item, null if not yet created
    private View[] mNavDrawerItemViews = null;

    // A Runnable that we should execute when the navigation drawer finishes its closing animation
    private Runnable mDeferredOnDrawerClosedRunnable;

    private boolean mAccountBoxExpanded = false;

    private LinearLayout mAccountListContainer;

    private ViewGroup mDrawerItemsListContainer;

    private Handler mHandler;

    private ImageView mExpandAccountBoxIndicator;

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
        setupAccountBox();
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
        ScrimInsetsScrollView navDrawer = (ScrimInsetsScrollView)
                mDrawerLayout.findViewById(R.id.navdrawer);
        if (mSelfItem == NavigationModel.NavigationItemEnum.INVALID) {
            // do not show a nav drawer
            if (navDrawer != null) {
                ((ViewGroup) navDrawer.getParent()).removeView(navDrawer);
            }
            mDrawerLayout = null;
            return;
        }

        if (navDrawer != null) {
            final View chosenAccountContentView = mActivity.findViewById(
                    R.id.chosen_account_content_view);
            final View chosenAccountView = mActivity.findViewById(R.id.chosen_account_view);
            final int navDrawerChosenAccountHeight = mActivity.getResources().getDimensionPixelSize(
                    R.dimen.navdrawer_chosen_account_height);
            navDrawer.setOnInsetsCallback(new ScrimInsetsScrollView.OnInsetsCallback() {
                @Override
                public void onInsetsChanged(Rect insets) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)
                            chosenAccountContentView.getLayoutParams();
                    lp.topMargin = insets.top;
                    chosenAccountContentView.setLayoutParams(lp);

                    ViewGroup.LayoutParams lp2 = chosenAccountView.getLayoutParams();
                    lp2.height = navDrawerChosenAccountHeight + insets.top;
                    chosenAccountView.setLayoutParams(lp2);
                }
            });
        }

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                // run deferred action, if we have one
                if (mDeferredOnDrawerClosedRunnable != null) {
                    mDeferredOnDrawerClosedRunnable.run();
                    mDeferredOnDrawerClosedRunnable = null;
                }
                if (mAccountBoxExpanded) {
                    mAccountBoxExpanded = false;
                    setupAccountBoxToggle();
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

    private View makeNavDrawerItem(final NavigationItemEnum item, ViewGroup container) {
        if (isSeparator(item)) {
            View separator =
                    mActivity.getLayoutInflater()
                             .inflate(R.layout.navdrawer_separator, container, false);
            UIUtils.setAccessibilityIgnore(separator);
            return separator;
        }

        NavDrawerItemView view = (NavDrawerItemView) mActivity.getLayoutInflater().inflate(
                R.layout.navdrawer_item, container, false);
        view.setTag(item);
        view.setContent(item.getIconResource(), item.getTitleResource());
        view.setActivated(mSelfItem == item);
        if (view.isActivated()) {
            view.setContentDescription(
                    mActivity.getString(R.string.navdrawer_selected_menu_item_a11y_wrapper,
                            mActivity.getString(item.getTitleResource())));
        } else {
            view.setContentDescription(
                    mActivity.getString(R.string.navdrawer_menu_item_a11y_wrapper,
                            mActivity.getString(item.getTitleResource())));
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavDrawerItemClicked(item);
            }
        });
        return view;
    }

    private void createNavDrawerItems(NavigationModel.NavigationItemEnum[] items) {
        mDrawerItemsListContainer = (ViewGroup) mActivity.findViewById(R.id.navdrawer_items_list);
        if (mDrawerItemsListContainer == null) {
            return;
        }

        mNavDrawerItemViews = new View[items.length];
        mDrawerItemsListContainer.removeAllViews();
        for (int i = 0; i < items.length; i++) {
            mNavDrawerItemViews[i] = makeNavDrawerItem(items[i], mDrawerItemsListContainer);
            mDrawerItemsListContainer.addView(mNavDrawerItemViews[i]);
        }
    }

    /**
     * Sets up the given navdrawer item's appearance to the selected state. Note: this could also be
     * accomplished (perhaps more cleanly) with state-based layouts.
     */
    private void setSelectedNavDrawerItem(NavigationItemEnum item) {
        if (mNavDrawerItemViews != null) {
            for (int i = 0; i < mNavDrawerItemViews.length; i++) {
                NavigationItemEnum viewItem =
                        (NavigationItemEnum) mNavDrawerItemViews[i].getTag();
                mNavDrawerItemViews[i].setActivated(item == viewItem);
            }
        }
    }

    private void setupAccountBoxToggle() {
        if (mDrawerLayout == null || mSelfItem == NavigationItemEnum.INVALID) {
            // this Activity does not have a nav drawer
            return;
        }
        mExpandAccountBoxIndicator.setImageResource(mAccountBoxExpanded
                ? R.drawable.ic_navview_accounts_collapse
                : R.drawable.ic_navview_accounts_expand);
        int hideTranslateY = -mAccountListContainer.getHeight() / 4; // last 25% of animation
        if (mAccountBoxExpanded && mAccountListContainer.getTranslationY() == 0) {
            // initial setup
            mAccountListContainer.setAlpha(0);
            mAccountListContainer.setTranslationY(hideTranslateY);
        }

        AnimatorSet set = new AnimatorSet();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mDrawerItemsListContainer.setVisibility(mAccountBoxExpanded
                        ? View.INVISIBLE : View.VISIBLE);
                mAccountListContainer.setVisibility(mAccountBoxExpanded
                        ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
            }
        });

        if (mAccountBoxExpanded) {
            mAccountListContainer.setVisibility(View.VISIBLE);
            AnimatorSet subSet = new AnimatorSet();
            subSet.playTogether(
                    ObjectAnimator.ofFloat(mAccountListContainer, View.ALPHA, 1)
                                  .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION),
                    ObjectAnimator.ofFloat(mAccountListContainer, View.TRANSLATION_Y, 0)
                                  .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION));
            set.playSequentially(
                    ObjectAnimator.ofFloat(mDrawerItemsListContainer, View.ALPHA, 0)
                                  .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION),
                    subSet);
            set.start();
        } else {
            mDrawerItemsListContainer.setVisibility(View.VISIBLE);
            AnimatorSet subSet = new AnimatorSet();
            subSet.playTogether(
                    ObjectAnimator.ofFloat(mAccountListContainer, View.ALPHA, 0)
                                  .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION),
                    ObjectAnimator.ofFloat(mAccountListContainer, View.TRANSLATION_Y,
                            hideTranslateY)
                                  .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION));
            set.playSequentially(
                    subSet,
                    ObjectAnimator.ofFloat(mDrawerItemsListContainer, View.ALPHA, 1)
                                  .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION));
            set.start();
        }

        set.start();
    }

    /**
     * Sets up the account box. The account box is the area at the top of the nav drawer that shows
     * which account the user is logged in as, and lets them switch accounts. It also shows the
     * user's Google+ cover photo as background.
     */
    private void setupAccountBox() {
        mAccountListContainer = (LinearLayout) mActivity.findViewById(R.id.account_list);

        if (mAccountListContainer == null) {
            //This activity does not have an account box
            return;
        }

        final View chosenAccountView = mActivity.findViewById(R.id.chosen_account_view);
        Account chosenAccount = AccountUtils.getActiveAccount(mActivity);
        if (chosenAccount == null) {
            // No account logged in; hide account box
            chosenAccountView.setVisibility(View.GONE);
            mAccountListContainer.setVisibility(View.GONE);
            return;
        } else {
            chosenAccountView.setVisibility(View.VISIBLE);
            mAccountListContainer.setVisibility(View.INVISIBLE);
        }

        AccountManager am = AccountManager.get(mActivity);
        Account[] accountArray = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        List<Account> accounts = new ArrayList<Account>(Arrays.asList(accountArray));
        accounts.remove(chosenAccount);

        ImageView coverImageView = (ImageView) chosenAccountView
                .findViewById(R.id.profile_cover_image);
        ImageView profileImageView = (ImageView) chosenAccountView.findViewById(R.id.profile_image);
        TextView nameTextView = (TextView) chosenAccountView.findViewById(R.id.profile_name_text);
        TextView email = (TextView) chosenAccountView.findViewById(R.id.profile_email_text);
        mExpandAccountBoxIndicator = (ImageView) mActivity.findViewById(
                R.id.expand_account_box_indicator);

        String name = AccountUtils.getPlusName(mActivity);
        if (name == null) {
            nameTextView.setVisibility(View.GONE);
        } else {
            nameTextView.setVisibility(View.VISIBLE);
            nameTextView.setText(name);
        }

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
        }

        email.setText(chosenAccount.name);

        if (accounts.isEmpty()) {
            // There's only one account on the device, so no need for a switcher.
            mExpandAccountBoxIndicator.setVisibility(View.GONE);
            mAccountListContainer.setVisibility(View.GONE);
            chosenAccountView.setEnabled(false);
            return;
        }

        chosenAccountView.setEnabled(true);

        mExpandAccountBoxIndicator.setVisibility(View.VISIBLE);
        chosenAccountView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAccountBoxExpanded = !mAccountBoxExpanded;
                setupAccountBoxToggle();
            }
        });
        setupAccountBoxToggle();

        populateAccountList(accounts);
    }

    private void populateAccountList(List<Account> accounts) {
        mAccountListContainer.removeAllViews();

        LayoutInflater layoutInflater = LayoutInflater.from(mActivity);
        for (Account account : accounts) {
            View itemView = layoutInflater.inflate(R.layout.list_item_account,
                    mAccountListContainer, false);
            ((TextView) itemView.findViewById(R.id.profile_email_text))
                    .setText(account.name);
            final String accountName = account.name;
            String imageUrl = AccountUtils.getPlusImageUrl(mActivity, accountName);
            if (!TextUtils.isEmpty(imageUrl)) {
                mImageLoader.loadImage(imageUrl,
                        (ImageView) itemView.findViewById(R.id.profile_image));
            }
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ConnectivityManager cm = (ConnectivityManager)
                            mActivity.getSystemService(Activity.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    if (activeNetwork == null || !activeNetwork.isConnected()) {
                        // If there's no network, don't try to change the selected account
                        Toast.makeText(mActivity, R.string.no_connection_cant_login,
                                Toast.LENGTH_SHORT).show();
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        LOGD(TAG, "User requested switch to account: " + accountName);
                        AccountUtils.setActiveAccount(mActivity, accountName);
                        mLoginStateListener.onAccountChangeRequested();
                        mLoginStateListener.onStartLoginProcessRequested();
                        mAccountBoxExpanded = false;
                        setupAccountBoxToggle();
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                        setupAccountBox();
                    }
                }
            });
            mAccountListContainer.addView(itemView);
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

    private boolean isSeparator(NavigationItemEnum item) {
        return item == NavigationItemEnum.SEPARATOR || item == NavigationItemEnum.SEPARATOR_SPECIAL;
    }

    public interface NavigationDrawerStateListener {

        void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating);

        void onNavDrawerSlide(float slideOffset);
    }
}
