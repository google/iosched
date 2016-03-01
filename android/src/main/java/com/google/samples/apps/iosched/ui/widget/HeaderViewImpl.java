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

package com.google.samples.apps.iosched.ui.widget;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.ListView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.navigation.AppNavigationViewAsDrawerImpl;
import com.google.samples.apps.iosched.util.LUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HeaderViewImpl implements HeaderView {

    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

    private static final int HEADER_HIDE_ANIM_DURATION = 300;

    private AppCompatActivity mActivity;

    private LUtils mLUtils;

    // When set, these components will be shown/hidden in sync with the action bar
    // to implement the "quick recall" effect (the Action Bar and the header views disappear
    // when you scroll down a list, and reappear quickly when you scroll up).
    private ArrayList<View> mHideableHeaderViews = new ArrayList<View>();

    private Toolbar mToolbar;

    private ObjectAnimator mColorAnimator;

    private boolean mAutoHideEnabled = false;

    private int mAutoHideSensivity = 0;

    private int mAutoHideMinY = 0;

    private int mAutoHideSignal = 0;

    private boolean mShown = true;

    private int mThemedStatusBarColor;

    private int mNormalStatusBarColor;

    private int mProgressBarTopWhenShown;

    private AppNavigationViewAsDrawerImpl mDrawer;

    public HeaderViewImpl(AppCompatActivity activity, LUtils lUtils) {
        mActivity = activity;
        mLUtils = lUtils;
        mThemedStatusBarColor = mActivity.getResources().getColor(R.color.theme_primary_dark);
        mNormalStatusBarColor = mThemedStatusBarColor;
    }

    public void setNavigationView(AppNavigationViewAsDrawerImpl drawer) {
        mDrawer = drawer;
    }

    public boolean isActionBarShown() {
        return mShown;
    }

    public int getProgressBarTopWhenActionBarShown() {
        return mProgressBarTopWhenShown;
    }

    @Override
    public void setProgressBarTopWhenActionBarShown(int progressBarTopWhenActionBarShown) {
        mProgressBarTopWhenShown = progressBarTopWhenActionBarShown;
    }

    public boolean isAutoHideEnabled() {
        return mAutoHideEnabled;
    }

    public void autoShowOrHideActionBar(boolean show) {
        if (show == mShown) {
            return;
        }

        mShown = show;
        onActionBarAutoShowOrHide(show);
    }

    private void onActionBarAutoShowOrHide(boolean shown) {
        if (mColorAnimator != null) {
            mColorAnimator.cancel();
        }
        final DrawerLayout drawerLayout = mDrawer.getDrawerLayout();
        mColorAnimator = ObjectAnimator.ofInt(
                (drawerLayout != null) ? drawerLayout : mLUtils,
                (drawerLayout != null) ? "statusBarBackgroundColor" : "statusBarColor",
                shown ? Color.BLACK : mNormalStatusBarColor,
                shown ? mNormalStatusBarColor : Color.BLACK)
                                       .setDuration(250);
        if (drawerLayout != null) {
            mColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    ViewCompat.postInvalidateOnAnimation(drawerLayout);
                }
            });
        }
        mColorAnimator.setEvaluator(ARGB_EVALUATOR);
        mColorAnimator.start();

        for (final View view : mHideableHeaderViews) {
            if (shown) {
                ViewCompat.animate(view)
                          .translationY(0)
                          .alpha(1)
                          .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator())
                                // Setting Alpha animations should be done using the
                                // layer_type set to layer_type_hardware for the duration of the
                                // animation.
                        .withLayer();
            } else {
                ViewCompat.animate(view)
                          .translationY(-view.getBottom())
                          .alpha(0)
                          .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator())
                                // Setting Alpha animations should be done using the
                                // layer_type set to layer_type_hardware for the duration of the
                                // animation.
                        .withLayer();
            }
        }
    }

    @Override
    public void registerHideableHeaderView(View hideableHeaderView) {
        if (!mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.add(hideableHeaderView);
        }
    }

    @Override
    public void deregisterHideableHeaderView(View hideableHeaderView) {
        if (mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.remove(hideableHeaderView);
        }
    }

    @Override
    public Toolbar getActionBarToolbar() {
        if (mToolbar == null) {
            mToolbar = (Toolbar) mActivity.findViewById(R.id.toolbar_actionbar);
            if (mToolbar != null) {
                // Depending on which version of Android you are on the Toolbar or the ActionBar
                // may be
                // active so the a11y description is set here.
                mToolbar.setNavigationContentDescription(mActivity.getResources().getString(R.string
                        .navdrawer_description_a11y));
                mActivity.setSupportActionBar(mToolbar);
            }
        }
        return mToolbar;
    }

    public void setToolbarForNavigation() {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.ic_ab_drawer);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDrawer.showNavigation();
                }
            });
        }
    }

    /**
     * Initializes the Action Bar auto-hide (aka Quick Recall) effect.
     */
    private void initActionBarAutoHide() {
        mAutoHideEnabled = true;
        mAutoHideMinY = mActivity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_min_y);
        mAutoHideSensivity = mActivity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_sensivity);
    }

    /**
     * Indicates that the main content has scrolled (for the purposes of showing/hiding the action
     * bar for the "action bar auto hide" effect).
     * <p/>
     * {@code currentY} and {@code deltaY} may be exact (if the underlying view supports it) or may
     * be approximate indications: {@code deltaY} may be {@link Integer#MAX_VALUE} to mean "scrolled
     * forward indeterminately" and {@link Integer#MIN_VALUE} to mean "scrolled backward
     * indeterminately". {@code currentY} may be 0 to mean "somewhere close to the start of the
     * list" and {@link Integer#MAX_VALUE} to mean "we don't know, but not at the start of the
     * list".
     */
    private void onMainContentScrolled(int currentY, int deltaY) {
        if (deltaY > mAutoHideSensivity) {
            deltaY = mAutoHideSensivity;
        } else if (deltaY < -mAutoHideSensivity) {
            deltaY = -mAutoHideSensivity;
        }

        if (Math.signum(deltaY) * Math.signum(mAutoHideSignal) < 0) {
            // deltaY is a motion opposite to the accumulated signal, so reset signal
            mAutoHideSignal = deltaY;
        } else {
            // add to accumulated signal
            mAutoHideSignal += deltaY;
        }

        boolean shouldShow = currentY < mAutoHideMinY ||
                (mAutoHideSignal <= -mAutoHideSensivity);
        autoShowOrHideActionBar(shouldShow);
    }

    @Override
    public void enableActionBarAutoHide(final ListView listView) {
        initActionBarAutoHide();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            /** The heights of all items. */
            private Map<Integer, Integer> heights = new HashMap<>();
            private int lastCurrentScrollY = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {

                // Get the first visible item's view.
                View firstVisibleItemView = view.getChildAt(0);
                if (firstVisibleItemView == null) {
                    return;
                }

                // Save the height of the visible item.
                heights.put(firstVisibleItem, firstVisibleItemView.getHeight());

                // Calculate the height of all previous (hidden) items.
                int previousItemsHeight = 0;
                for (int i = 0; i < firstVisibleItem; i++) {
                    previousItemsHeight += heights.get(i) != null ? heights.get(i) : 0;
                }

                int currentScrollY = previousItemsHeight - firstVisibleItemView.getTop()
                        + view.getPaddingTop();

                onMainContentScrolled(currentScrollY, currentScrollY - lastCurrentScrollY);

                lastCurrentScrollY = currentScrollY;
            }
        });
    }

}
