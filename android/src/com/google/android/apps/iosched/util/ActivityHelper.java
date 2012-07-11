/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.iosched.util;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.ui.HomeActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A class that handles some common activity-related functionality in the app, such as setting up
 * the action bar. This class provides functionality useful for both phones and tablets, and does
 * not require any Android 3.0-specific features.
 */
public class ActivityHelper {
    protected Activity mActivity;

    /**
     * Factory method for creating {@link ActivityHelper} objects for a given activity. Depending
     * on which device the app is running, either a basic helper or Honeycomb-specific helper will
     * be returned.
     */
    public static ActivityHelper createInstance(Activity activity) {
        return UIUtils.isHoneycomb() ?
                new ActivityHelperHoneycomb(activity) :
                new ActivityHelper(activity);
    }

    protected ActivityHelper(Activity activity) {
        mActivity = activity;
    }

    public void onPostCreate(Bundle savedInstanceState) {
        // Create the action bar
        SimpleMenu menu = new SimpleMenu(mActivity);
        mActivity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu);
        // TODO: call onPreparePanelMenu here as well
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            addActionButtonCompatFromMenuItem(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        mActivity.getMenuInflater().inflate(R.menu.default_menu_items, menu);
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                goSearch();
                return true;
        }
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goHome();
            return true;
        }
        return false;
    }

    /**
     * Method, to be called in <code>onPostCreate</code>, that sets up this activity as the
     * home activity for the app.
     */
    public void setupHomeActivity() {
    }

    /**
     * Method, to be called in <code>onPostCreate</code>, that sets up this activity as a
     * sub-activity in the app.
     */
    public void setupSubActivity() {
    }

    /**
     * Invoke "home" action, returning to {@link com.google.android.apps.iosched.ui.HomeActivity}.
     */
    public void goHome() {
        if (mActivity instanceof HomeActivity) {
            return;
        }

        final Intent intent = new Intent(mActivity, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivity.startActivity(intent);

        if (!UIUtils.isHoneycomb()) {
            mActivity.overridePendingTransition(R.anim.home_enter, R.anim.home_exit);
        }
    }

    /**
     * Invoke "search" action, triggering a default search.
     */
    public void goSearch() {
        mActivity.startSearch(null, false, Bundle.EMPTY, false);
    }

    /**
     * Sets up the action bar with the given title and accent color. If title is null, then
     * the app logo will be shown instead of a title. Otherwise, a home button and title are
     * visible. If color is null, then the default colorstrip is visible.
     */
    public void setupActionBar(CharSequence title, int color) {
        final ViewGroup actionBarCompat = getActionBarCompat();
        if (actionBarCompat == null) {
            return;
        }

        LinearLayout.LayoutParams springLayoutParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.FILL_PARENT);
        springLayoutParams.weight = 1;

        View.OnClickListener homeClickListener = new View.OnClickListener() {
            public void onClick(View view) {
                goHome();
            }
        };

        if (title != null) {
            // Add Home button
            addActionButtonCompat(R.drawable.ic_title_home, R.string.description_home,
                    homeClickListener, true);

            // Add title text
            TextView titleText = new TextView(mActivity, null, R.attr.actionbarCompatTextStyle);
            titleText.setLayoutParams(springLayoutParams);
            titleText.setText(title);
            actionBarCompat.addView(titleText);

        } else {
            // Add logo
            ImageButton logo = new ImageButton(mActivity, null, R.attr.actionbarCompatLogoStyle);
            logo.setOnClickListener(homeClickListener);
            actionBarCompat.addView(logo);

            // Add spring (dummy view to align future children to the right)
            View spring = new View(mActivity);
            spring.setLayoutParams(springLayoutParams);
            actionBarCompat.addView(spring);
        }

        setActionBarColor(color);
    }

    /**
     * Sets the action bar color to the given color.
     */
    public void setActionBarColor(int color) {
        if (color == 0) {
            return;
        }

        final View colorstrip = mActivity.findViewById(R.id.colorstrip);
        if (colorstrip == null) {
            return;
        }

        colorstrip.setBackgroundColor(color);
    }

    /**
     * Sets the action bar title to the given string.
     */
    public void setActionBarTitle(CharSequence title) {
        ViewGroup actionBar = getActionBarCompat();
        if (actionBar == null) {
            return;
        }

        TextView titleText = (TextView) actionBar.findViewById(R.id.actionbar_compat_text);
        if (titleText != null) {
            titleText.setText(title);
        }
    }

    /**
     * Returns the {@link ViewGroup} for the action bar on phones (compatibility action bar).
     * Can return null, and will return null on Honeycomb.
     */
    public ViewGroup getActionBarCompat() {
        return (ViewGroup) mActivity.findViewById(R.id.actionbar_compat);
    }

    /**
     * Adds an action bar button to the compatibility action bar (on phones).
     */
    private View addActionButtonCompat(int iconResId, int textResId,
            View.OnClickListener clickListener, boolean separatorAfter) {
        final ViewGroup actionBar = getActionBarCompat();
        if (actionBar == null) {
            return null;
        }

        // Create the separator
        ImageView separator = new ImageView(mActivity, null, R.attr.actionbarCompatSeparatorStyle);
        separator.setLayoutParams(
                new ViewGroup.LayoutParams(2, ViewGroup.LayoutParams.FILL_PARENT));

        // Create the button
        ImageButton actionButton = new ImageButton(mActivity, null,
                R.attr.actionbarCompatButtonStyle);
        actionButton.setLayoutParams(new ViewGroup.LayoutParams(
                (int) mActivity.getResources().getDimension(R.dimen.actionbar_compat_height),
                ViewGroup.LayoutParams.FILL_PARENT));
        actionButton.setImageResource(iconResId);
        actionButton.setScaleType(ImageView.ScaleType.CENTER);
        actionButton.setContentDescription(mActivity.getResources().getString(textResId));
        actionButton.setOnClickListener(clickListener);

        // Add separator and button to the action bar in the desired order

        if (!separatorAfter) {
            actionBar.addView(separator);
        }

        actionBar.addView(actionButton);

        if (separatorAfter) {
            actionBar.addView(separator);
        }

        return actionButton;
    }

    /**
     * Adds an action button to the compatibility action bar, using menu information from a
     * {@link MenuItem}. If the menu item ID is <code>menu_refresh</code>, the menu item's state
     * can be changed to show a loading spinner using
     * {@link ActivityHelper#setRefreshActionButtonCompatState(boolean)}.
     */
    private View addActionButtonCompatFromMenuItem(final MenuItem item) {
        final ViewGroup actionBar = getActionBarCompat();
        if (actionBar == null) {
            return null;
        }

        // Create the separator
        ImageView separator = new ImageView(mActivity, null, R.attr.actionbarCompatSeparatorStyle);
        separator.setLayoutParams(
                new ViewGroup.LayoutParams(2, ViewGroup.LayoutParams.FILL_PARENT));

        // Create the button
        ImageButton actionButton = new ImageButton(mActivity, null,
                R.attr.actionbarCompatButtonStyle);
        actionButton.setId(item.getItemId());
        actionButton.setLayoutParams(new ViewGroup.LayoutParams(
                (int) mActivity.getResources().getDimension(R.dimen.actionbar_compat_height),
                ViewGroup.LayoutParams.FILL_PARENT));
        actionButton.setImageDrawable(item.getIcon());
        actionButton.setScaleType(ImageView.ScaleType.CENTER);
        actionButton.setContentDescription(item.getTitle());
        actionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mActivity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
            }
        });

        actionBar.addView(separator);
        actionBar.addView(actionButton);

        if (item.getItemId() == R.id.menu_refresh) {
            // Refresh buttons should be stateful, and allow for indeterminate progress indicators,
            // so add those.
            int buttonWidth = mActivity.getResources()
                    .getDimensionPixelSize(R.dimen.actionbar_compat_height);
            int buttonWidthDiv3 = buttonWidth / 3;
            ProgressBar indicator = new ProgressBar(mActivity, null,
                    R.attr.actionbarCompatProgressIndicatorStyle);
            LinearLayout.LayoutParams indicatorLayoutParams = new LinearLayout.LayoutParams(
                    buttonWidthDiv3, buttonWidthDiv3);
            indicatorLayoutParams.setMargins(buttonWidthDiv3, buttonWidthDiv3,
                    buttonWidth - 2 * buttonWidthDiv3, 0);
            indicator.setLayoutParams(indicatorLayoutParams);
            indicator.setVisibility(View.GONE);
            indicator.setId(R.id.menu_refresh_progress);
            actionBar.addView(indicator);
        }

        return actionButton;
    }

    /**
     * Sets the indeterminate loading state of a refresh button added with
     * {@link ActivityHelper#addActionButtonCompatFromMenuItem(android.view.MenuItem)}
     * (where the item ID was menu_refresh).
     */
    public void setRefreshActionButtonCompatState(boolean refreshing) {
        View refreshButton = mActivity.findViewById(R.id.menu_refresh);
        View refreshIndicator = mActivity.findViewById(R.id.menu_refresh_progress);

        if (refreshButton != null) {
            refreshButton.setVisibility(refreshing ? View.GONE : View.VISIBLE);
        }
        if (refreshIndicator != null) {
            refreshIndicator.setVisibility(refreshing ? View.VISIBLE : View.GONE);
        }
    }
}
