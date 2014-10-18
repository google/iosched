/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.PrefUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.*;

public class BrowseSessionsActivity extends BaseActivity implements SessionsFragment.Callbacks {
    private static final String TAG = makeLogTag(BrowseSessionsActivity.class);

    // How is this Activity being used?
    private static final int MODE_EXPLORE = 0; // as top-level "Explore" screen
    private static final int MODE_TIME_FIT = 1; // showing sessions that fit in a time interval

    private static final String STATE_FILTER_0 = "STATE_FILTER_0";
    private static final String STATE_FILTER_1 = "STATE_FILTER_1";
    private static final String STATE_FILTER_2 = "STATE_FILTER_2";

    public static final String EXTRA_FILTER_TAG = "com.google.android.iosched.extra.FILTER_TAG";

    private int mMode = MODE_EXPLORE;

    private final static String SCREEN_LABEL = "Explore";

    private TagMetadata mTagMetadata = null;
    private boolean mSpinnerConfigured = false;

    // filter tags that are currently selected
    private String[] mFilterTags = { "", "", "" };

    // filter tags that we have to restore (as a result of Activity recreation)
    private String[] mFilterTagsToRestore = { null, null, null };

    private ExploreSpinnerAdapter mTopLevelSpinnerAdapter = new ExploreSpinnerAdapter(true);
    private SessionsFragment mSessionsFrag = null;

    private DrawShadowFrameLayout mDrawShadowFrameLayout;
    private View mButterBar;

    // time when the user last clicked "refresh" from the stale data butter bar
    private long mLastDataStaleUserActionTime = 0L;
    private int mHeaderColor = 0; // 0 means not customized

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browse_sessions);

        Toolbar toolbar = getActionBarToolbar();

        long[] interval = ScheduleContract.Sessions.getInterval(getIntent().getData());
        if (interval != null) {
            String title = UIUtils.formatIntervalTimeString(interval[0], interval[1], null, this);
            toolbar.setTitle(title);
            mMode = MODE_TIME_FIT;
            /* [ANALYTICS:SCREEN]
             * TRIGGER:   View the Explore screen to find sessions fitting a time slot
             * LABEL:    'Explore <time interval>'
             * [/ANALYTICS]
             */
            AnalyticsManager.sendScreenView(SCREEN_LABEL + ": " + title);
        } else {
            /* [ANALYTICS:SCREEN]
             * TRIGGER:   View the Explore screen (landing screen)
             * LABEL:    'Explore'
             * [/ANALYTICS]
             */
            AnalyticsManager.sendScreenView(SCREEN_LABEL);
        }

        overridePendingTransition(0, 0);

        if (savedInstanceState != null) {
            mFilterTagsToRestore[0] = mFilterTags[0] = savedInstanceState.getString(STATE_FILTER_0);
            mFilterTagsToRestore[1] = mFilterTags[1] = savedInstanceState.getString(STATE_FILTER_1);
            mFilterTagsToRestore[2] = mFilterTags[2] = savedInstanceState.getString(STATE_FILTER_2);
        } else if (getIntent() != null && getIntent().hasExtra(EXTRA_FILTER_TAG)) {
            mFilterTagsToRestore[0] = getIntent().getStringExtra(EXTRA_FILTER_TAG);
        }

        if (mMode == MODE_EXPLORE) {
            // no title (to make more room for navigation and actions)
            // unless Nav Drawer opens
            toolbar.setTitle(null);
        }

        mButterBar = findViewById(R.id.butter_bar);
        mDrawShadowFrameLayout = (DrawShadowFrameLayout) findViewById(R.id.main_content);
        registerHideableHeaderView(mButterBar);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkShowStaleDataButterBar();
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        if (mSessionsFrag != null) {
            return mSessionsFrag.canCollectionViewScrollUp();
        }
        return super.canSwipeRefreshChildScrollUp();
    }

    private void checkShowStaleDataButterBar() {
        final boolean showingFilters = findViewById(R.id.filters_box) != null
                && findViewById(R.id.filters_box).getVisibility() == View.VISIBLE;
        final long now = UIUtils.getCurrentTime(this);
        final boolean inSnooze = (now - mLastDataStaleUserActionTime < Config.STALE_DATA_WARNING_SNOOZE);
        final long staleTime = now - PrefUtils.getLastSyncSucceededTime(this);
        final long staleThreshold = (now >= Config.CONFERENCE_START_MILLIS && now
                <= Config.CONFERENCE_END_MILLIS) ? Config.STALE_DATA_THRESHOLD_DURING_CONFERENCE :
                Config.STALE_DATA_THRESHOLD_NOT_DURING_CONFERENCE;
        final boolean isStale = (staleTime >= staleThreshold);
        final boolean bootstrapDone = PrefUtils.isDataBootstrapDone(this);
        final boolean mustShowBar = bootstrapDone && isStale && !inSnooze && !showingFilters;

        if (!mustShowBar) {
            mButterBar.setVisibility(View.GONE);
        } else {
            UIUtils.setUpButterBar(mButterBar, getString(R.string.data_stale_warning),
                    getString(R.string.description_refresh), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mButterBar.setVisibility(View.GONE);
                            updateFragContentTopClearance();
                            mLastDataStaleUserActionTime = UIUtils.getCurrentTime(
                                    BrowseSessionsActivity.this);
                            requestDataRefresh();
                        }
                    }
            );
        }
        updateFragContentTopClearance();
    }

    @Override
    protected int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Explore mode.
        return mMode == MODE_EXPLORE ? NAVDRAWER_ITEM_EXPLORE : NAVDRAWER_ITEM_INVALID;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        CollectionView collectionView = (CollectionView) findViewById(R.id.sessions_collection_view);
        if (collectionView != null) {
            enableActionBarAutoHide(collectionView);
        }

        mSessionsFrag = (SessionsFragment) getFragmentManager().findFragmentById(
                R.id.sessions_fragment);
        if (mSessionsFrag != null && savedInstanceState == null) {
            Bundle args = intentToFragmentArguments(getIntent());
            mSessionsFrag.reloadFromArguments(args);
        }

        registerHideableHeaderView(findViewById(R.id.headerbar));
    }

    @Override
    public void onTagMetadataLoaded(TagMetadata metadata) {
        mTagMetadata = metadata;
        if (mSpinnerConfigured) {
            // we need to reconfigure the spinner, so we need to remember our current filter
            // and try to restore it after we set up the spinner again.
            mSpinnerConfigured = false;
            mFilterTagsToRestore[0] = mFilterTags[0];
            mFilterTagsToRestore[1] = mFilterTags[1];
            mFilterTagsToRestore[2] = mFilterTags[2];
        }
        trySetUpActionBarSpinner();
    }

    private void trySetUpActionBarSpinner() {
        Toolbar toolbar = getActionBarToolbar();
        if (mMode != MODE_EXPLORE || mSpinnerConfigured || mTagMetadata == null || toolbar == null) {
            // already done it, or not ready yet, or don't need to do
            LOGD(TAG, "Not configuring Action Bar spinner.");
            return;
        }

        LOGD(TAG, "Configuring Action Bar spinner.");
        mSpinnerConfigured = true;
        mTopLevelSpinnerAdapter.clear();
        mTopLevelSpinnerAdapter.addItem("", getString(R.string.all_sessions), false, 0);

        int itemToSelect = -1;

        int i;
        for (i = 0; i < Config.Tags.EXPLORE_CATEGORIES.length; i++) {
            String category = Config.Tags.EXPLORE_CATEGORIES[i];
            String categoryTitle = getString(Config.Tags.EXPLORE_CATEGORY_TITLE[i]);
            LOGD(TAG, "Processing tag category: " + category + ", title=" + categoryTitle);

            List<TagMetadata.Tag> tags = mTagMetadata.getTagsInCategory(category);
            if (tags != null) {
                mTopLevelSpinnerAdapter.addHeader(categoryTitle);
                for (TagMetadata.Tag tag : mTagMetadata.getTagsInCategory(category)) {
                    LOGD(TAG, "Adding item to spinner: " + tag.getId() + " --> " + tag.getName());
                    int tagColor = Config.Tags.CATEGORY_TOPIC.equals(category) ? tag.getColor() : 0;
                    mTopLevelSpinnerAdapter.addItem(tag.getId(), tag.getName(), true, tagColor);
                    if (!TextUtils.isEmpty(mFilterTagsToRestore[0]) && tag.getId().equals(mFilterTagsToRestore[0])) {
                        mFilterTagsToRestore[0] = null;
                        itemToSelect = mTopLevelSpinnerAdapter.getCount() - 1;
                    }
                }
            } else {
                LOGW(TAG, "Ignoring Explore category with no tags: " + category);
            }
        }
        mFilterTagsToRestore[0] = null;

        View spinnerContainer = LayoutInflater.from(this).inflate(R.layout.actionbar_spinner,
                toolbar, false);
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        toolbar.addView(spinnerContainer, lp);

        Spinner spinner = (Spinner) spinnerContainer.findViewById(R.id.actionbar_spinner);
        spinner.setAdapter(mTopLevelSpinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> spinner, View view, int position, long itemId) {
                onTopLevelTagSelected(mTopLevelSpinnerAdapter.getTag(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        if (itemToSelect >= 0) {
            LOGD(TAG, "Restoring item selection to primary spinner: " + itemToSelect);
            spinner.setSelection(itemToSelect);
        }

        updateHeaderColor();
        showSecondaryFilters();
    }

    private void updateHeaderColor() {
        mHeaderColor = 0;
        for (String tag : mFilterTags) {
            if (tag != null) {
                TagMetadata.Tag tagObj = mTagMetadata.getTag(tag);
                if (tagObj != null && Config.Tags.CATEGORY_TOPIC.equals(tagObj.getCategory())) {
                    mHeaderColor = tagObj.getColor();
                }
            }
        }
        findViewById(R.id.headerbar).setBackgroundColor(
                mHeaderColor == 0
                        ? getResources().getColor(R.color.theme_primary)
                        : mHeaderColor);
        setNormalStatusBarColor(
                mHeaderColor == 0
                        ? getThemedStatusBarColor()
                        : UIUtils.scaleColor(mHeaderColor, 0.8f, false));
    }

    private void onTopLevelTagSelected(String tag) {
        SessionsFragment frag = (SessionsFragment) getFragmentManager().findFragmentById(
                R.id.sessions_fragment);
        if (frag == null) {
            LOGE(TAG, "Sessions fragment not found!");
            return;
        }

        if (tag.equals(mFilterTags[0])) {
            // nothing to do
            return;
        }

        /* [ANALYTICS:EVENT]
         * TRIGGER:   Select a top-level filter on the Explore screen.
         * CATEGORY:  'Explore'
         * ACTION:    'topfilter'
         * LABEL:     The selected tag. For example, "THEME_DEVELOP", "TOPIC_ANDROID", etc.
         * [/ANALYTICS]
         */
        AnalyticsManager.sendEvent(SCREEN_LABEL, "topfilter", tag);
        mFilterTags[0] = tag;

        // Reset secondary filters
        for (int i = 1; i < mFilterTags.length; i++) {
            mFilterTags[i] = "";
        }

        showSecondaryFilters();
        updateHeaderColor();
        reloadFromFilters();
    }

    private void showSecondaryFilters() {
        showFilterBox(false);

        // repopulate secondary filter spinners
        if (!TextUtils.isEmpty(mFilterTags[0])) {
            TagMetadata.Tag topTag = mTagMetadata.getTag(mFilterTags[0]);
            String topCategory = topTag.getCategory();
            if (topCategory.equals(Config.Tags.EXPLORE_CATEGORIES[0])) {
                populateSecondLevelFilterSpinner(0, 1);
                populateSecondLevelFilterSpinner(1, 2);
            } else if (topCategory.equals(Config.Tags.EXPLORE_CATEGORIES[1])) {
                populateSecondLevelFilterSpinner(0, 0);
                populateSecondLevelFilterSpinner(1, 2);
            } else {
                populateSecondLevelFilterSpinner(0, 0);
                populateSecondLevelFilterSpinner(1, 1);
            }
            showFilterBox(true);
        }
    }

    private void showFilterBox(boolean show) {
        View filtersBox = findViewById(R.id.filters_box);
        if (filtersBox != null) {
            filtersBox.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        checkShowStaleDataButterBar();
        updateFragContentTopClearance();
    }

    // Updates the Sessions fragment content top clearance to take our chrome into account
    private void updateFragContentTopClearance() {
        SessionsFragment frag = (SessionsFragment) getFragmentManager().findFragmentById(
                R.id.sessions_fragment);
        if (frag == null) {
            return;
        }

        View filtersBox = findViewById(R.id.filters_box);

        final boolean filterBoxVisible = filtersBox != null
                && filtersBox.getVisibility() == View.VISIBLE;
        final boolean butterBarVisible = mButterBar != null
                && mButterBar.getVisibility() == View.VISIBLE;

        int actionBarClearance = UIUtils.calculateActionBarSize(this);
        int butterBarClearance = butterBarVisible
                ? getResources().getDimensionPixelSize(R.dimen.butter_bar_height) : 0;
        int filterBoxClearance = filterBoxVisible
                ? getResources().getDimensionPixelSize(R.dimen.filterbar_height) : 0;
        int secondaryClearance = butterBarClearance > filterBoxClearance ? butterBarClearance :
                filterBoxClearance;
        int gridPadding = getResources().getDimensionPixelSize(R.dimen.explore_grid_padding);

        setProgressBarTopWhenActionBarShown(actionBarClearance + secondaryClearance);
        mDrawShadowFrameLayout.setShadowTopOffset(actionBarClearance + secondaryClearance);
        frag.setContentTopClearance(actionBarClearance + secondaryClearance + gridPadding);
    }

    private void reloadFromFilters() {
        SessionsFragment frag = (SessionsFragment) getFragmentManager().findFragmentById(
                R.id.sessions_fragment);
        if (frag == null) {
            LOGE(TAG, "Sessions fragment not found! Can't reload from filters.");
            return;
        }

        Bundle args = BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, ScheduleContract.Sessions.buildTagFilterUri(
                        mFilterTags))
                        .putExtra(SessionsFragment.EXTRA_NO_TRACK_BRANDING, mHeaderColor != 0));
        frag.reloadFromArguments(args);

        frag.animateReload();
    }

    private void populateSecondLevelFilterSpinner(final int spinnerIndex, int catIndex) {
        String tagToRestore = mFilterTagsToRestore[spinnerIndex + 1];
        Spinner spinner = (Spinner) findViewById(spinnerIndex == 0 ? R.id.secondary_filter_spinner_1
                : R.id.secondary_filter_spinner_2);
        final int filterIndex = spinnerIndex + 1;
        String tagCategory = Config.Tags.EXPLORE_CATEGORIES[catIndex];
        boolean isTopicCategory = Config.Tags.CATEGORY_TOPIC.equals(tagCategory);
        final ExploreSpinnerAdapter adapter = new ExploreSpinnerAdapter(false);
        adapter.addItem("", getString(Config.Tags.EXPLORE_CATEGORY_ALL_STRING[catIndex]), false, 0);
        List<TagMetadata.Tag> tags = mTagMetadata.getTagsInCategory(tagCategory);

        int itemToSelect = -1;
        if (tags != null) {
            for (TagMetadata.Tag tag : tags) {
                adapter.addItem(tag.getId(), tag.getName(), false,
                        isTopicCategory ? tag.getColor() : 0);
                if (!TextUtils.isEmpty(tagToRestore) && tag.getId().equals(tagToRestore)) {
                    itemToSelect = adapter.getCount() - 1;
                    mFilterTagsToRestore[spinnerIndex + 1] = null;
                }
            }
        } else {
            LOGE(TAG, "Can't populate spinner. Category has no tags: " + tagCategory);
        }
        mFilterTagsToRestore[spinnerIndex + 1] = null;

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectTag(adapter.getTag(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectTag("");
            }

            private void selectTag(String tag) {
                if (!mFilterTags[filterIndex].equals(tag)) {
                    mFilterTags[filterIndex] = tag;
                    /* [ANALYTICS:EVENT]
                     * TRIGGER:   Select a secondary filter on the Explore screen.
                     * CATEGORY:  'Explore'
                     * ACTION:    'secondaryfilter'
                     * LABEL:     The selected tag. For example, "THEME_DEVELOP", "TOPIC_ANDROID", etc.
                     * [/ANALYTICS]
                     */
                    AnalyticsManager.sendEvent(SCREEN_LABEL, "secondaryfilter", tag);
                    updateHeaderColor();
                    reloadFromFilters();
                }
            }
        });

        if (itemToSelect >= 0) {
            LOGD(TAG, "Restoring item selection to secondary spinner #"
                    + spinnerIndex + ": " + itemToSelect);
            spinner.setSelection(itemToSelect, false);
        }
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.browse_sessions, menu);
        // remove actions when in time interval mode:
        if (mMode != MODE_EXPLORE) {
            menu.removeItem(R.id.menu_search);
            menu.removeItem(R.id.menu_refresh);
            menu.removeItem(R.id.menu_wifi);
            menu.removeItem(R.id.menu_debug);
            menu.removeItem(R.id.menu_about);
        } else {
            configureStandardMenuItems(menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                /* [ANALYTICS:EVENT]
                 * TRIGGER:   Click the search button on the Explore screen.
                 * CATEGORY:  'Explore'
                 * ACTION:    'launchsearch'
                 * LABEL:     (none)
                 * [/ANALYTICS]
                 */
                AnalyticsManager.sendEvent(SCREEN_LABEL, "launchsearch", "");
                startActivity(new Intent(this, SearchActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSessionSelected(String sessionId, View clickedView) {
        /* [ANALYTICS:EVENT]
         * TRIGGER:   Click on a session on the Explore screen.
         * CATEGORY:  'Explore'
         * ACTION:    'selectsession'
         * LABEL:     session ID (for example "3284-fac320-2492048-bf391')
         * [/ANALYTICS]
         */
        AnalyticsManager.sendEvent(SCREEN_LABEL, "selectsession", sessionId);
        getLUtils().startActivityWithTransition(new Intent(Intent.ACTION_VIEW,
                        ScheduleContract.Sessions.buildSessionUri(sessionId)),
                clickedView,
                SessionDetailActivity.TRANSITION_NAME_PHOTO);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILTER_0, mFilterTags[0]);
        outState.putString(STATE_FILTER_1, mFilterTags[1]);
        outState.putString(STATE_FILTER_2, mFilterTags[2]);
    }

    private class ExploreSpinnerItem {
        boolean isHeader;
        String tag, title;
        int color;
        boolean indented;

        ExploreSpinnerItem(boolean isHeader, String tag, String title, boolean indented, int color) {
            this.isHeader = isHeader;
            this.tag = tag;
            this.title = title;
            this.indented = indented;
            this.color = color;
        }
    }

    /** Adapter that provides views for our top-level Action Bar spinner. */
    private class ExploreSpinnerAdapter extends BaseAdapter {
        private int mDotSize;
        private boolean mTopLevel;

        private ExploreSpinnerAdapter(boolean topLevel) {
            this.mTopLevel = topLevel;
        }

        // pairs of (tag, title)
        private ArrayList<ExploreSpinnerItem> mItems = new ArrayList<ExploreSpinnerItem>();

        public void clear() {
            mItems.clear();
        }

        public void addItem(String tag, String title, boolean indented, int color) {
            mItems.add(new ExploreSpinnerItem(false, tag, title, indented, color));
        }

        public void addHeader(String title) {
            mItems.add(new ExploreSpinnerItem(true, "", title, false, 0));
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private boolean isHeader(int position) {
            return position >= 0 && position < mItems.size()
                    && mItems.get(position).isHeader;
        }

        @Override
        public View getDropDownView(int position, View view, ViewGroup parent) {
            if (view == null || !view.getTag().toString().equals("DROPDOWN")) {
                view = getLayoutInflater().inflate(R.layout.explore_spinner_item_dropdown,
                        parent, false);
                view.setTag("DROPDOWN");
            }

            TextView headerTextView = (TextView) view.findViewById(R.id.header_text);
            View dividerView = view.findViewById(R.id.divider_view);
            TextView normalTextView = (TextView) view.findViewById(android.R.id.text1);

            if (isHeader(position)) {
                headerTextView.setText(getTitle(position));
                headerTextView.setVisibility(View.VISIBLE);
                normalTextView.setVisibility(View.GONE);
                dividerView.setVisibility(View.VISIBLE);
            } else {
                headerTextView.setVisibility(View.GONE);
                normalTextView.setVisibility(View.VISIBLE);
                dividerView.setVisibility(View.GONE);

                setUpNormalDropdownView(position, normalTextView);
            }

            return view;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null || !view.getTag().toString().equals("NON_DROPDOWN")) {
                view = getLayoutInflater().inflate(mTopLevel
                        ? R.layout.explore_spinner_item_actionbar
                        : R.layout.explore_spinner_item,
                        parent, false);
                view.setTag("NON_DROPDOWN");
            }
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(getTitle(position));
            return view;
        }

        private String getTitle(int position) {
            return position >= 0 && position < mItems.size() ? mItems.get(position).title : "";
        }

        private int getColor(int position) {
            return position >= 0 && position < mItems.size() ? mItems.get(position).color : 0;
        }

        private String getTag(int position) {
            return position >= 0 && position < mItems.size() ? mItems.get(position).tag : "";
        }

        private void setUpNormalDropdownView(int position, TextView textView) {
            textView.setText(getTitle(position));
            ShapeDrawable colorDrawable = (ShapeDrawable) textView.getCompoundDrawables()[2];
            int color = getColor(position);
            if (color == 0) {
                if (colorDrawable != null) {
                    textView.setCompoundDrawables(null, null, null, null);
                }
            } else {
                if (mDotSize == 0) {
                    mDotSize = getResources().getDimensionPixelSize(
                            R.dimen.tag_color_dot_size);
                }
                if (colorDrawable == null) {
                    colorDrawable = new ShapeDrawable(new OvalShape());
                    colorDrawable.setIntrinsicWidth(mDotSize);
                    colorDrawable.setIntrinsicHeight(mDotSize);
                    colorDrawable.getPaint().setStyle(Paint.Style.FILL);
                    textView.setCompoundDrawablesWithIntrinsicBounds(null, null, colorDrawable, null);
                }
                colorDrawable.getPaint().setColor(color);
            }

        }

        @Override
        public boolean isEnabled(int position) {
            return !isHeader(position);
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }
    }
}
