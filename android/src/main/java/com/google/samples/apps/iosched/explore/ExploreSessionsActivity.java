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

package com.google.samples.apps.iosched.explore;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.ui.SearchActivity;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.List;

import static com.google.samples.apps.iosched.ui.widget.CollectionView.Inventory;
import static com.google.samples.apps.iosched.ui.widget.CollectionView.InventoryGroup;
import static com.google.samples.apps.iosched.ui.widget.CollectionView.OnClickListener;
import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This activity displays all sessions based on the selected filters.
 * <p/>
 * It can either be invoked with specific filters or the user can choose the filters
 * to use from the alt_nav_bar.
 */
public class ExploreSessionsActivity extends BaseActivity
        implements Toolbar.OnMenuItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_FILTER_TAG =
            "com.google.samples.apps.iosched.explore.EXTRA_FILTER_TAG";
    public static final String EXTRA_SHOW_LIVE_STREAM_SESSIONS =
            "com.google.samples.apps.iosched.explore.EXTRA_SHOW_LIVE_STREAM_SESSIONS";

    // The saved instance state filters
    private static final String STATE_FILTER_TAGS =
            "com.google.samples.apps.iosched.explore.STATE_FILTER_TAGS";
    private static final String STATE_CURRENT_URI =
            "com.google.samples.apps.iosched.explore.STATE_CURRENT_URI";

    private static final String SCREEN_LABEL = "ExploreSessions";

    private static final String TAG = makeLogTag(ExploreSessionsActivity.class);
    private static final int TAG_METADATA_TOKEN = 0x8;

    private static final int GROUP_TOPIC_TYPE_OR_THEME = 0;
    private static final int GROUP_LIVE_STREAM = 1;

    private static final int MODE_TIME_FIT = 1;
    private static final int MODE_EXPLORE = 2;

    private CollectionView mDrawerCollectionView;
    private DrawerLayout mDrawerLayout;

    private TagMetadata mTagMetadata;
    private TagFilterHolder mTagFilterHolder;
    // Keep track of the current URI. This can diverge from Intent.getData() if the user
    // dismisses a particular timeslot. At that point, the Activity switches the mode
    // as well as the Uri used.
    private Uri mCurrentUri;

    // The OnClickListener for the Switch widgets on the navigation filter.
    private final View.OnClickListener mDrawerItemCheckBoxClickListener =
            new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean isChecked = ((CheckBox)v).isChecked();
            TagMetadata.Tag theTag = (TagMetadata.Tag)v.getTag();
            LOGD(TAG, "Checkbox with tag: " + theTag.getName() + " isChecked => " + isChecked);
            if (isChecked) {
                mTagFilterHolder.add(theTag.getId(), theTag.getCategory());
            } else {
                mTagFilterHolder.remove(theTag.getId(), theTag.getCategory());
            }
            reloadFragment();
        }
    };
    private ExploreSessionsFragment mFragment;
    private int mMode;
    private View mTimeSlotLayout;
    private View mTimeSlotDivider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore_sessions_act);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerCollectionView = (CollectionView) findViewById(R.id.drawer_collection_view);
        mTimeSlotLayout = findViewById(R.id.timeslot_view);
        mTimeSlotDivider = findViewById(R.id.timeslot_divider);
        TextView timeSlotTextView = (TextView) findViewById(R.id.timeslot);
        ImageButton dismissTimeSlotButton = (ImageButton) findViewById(R.id.close_timeslot);
        registerHideableHeaderView(findViewById(R.id.headerbar));

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_flipped, GravityCompat.END);

        mFragment = (ExploreSessionsFragment) getFragmentManager()
                .findFragmentById(R.id.explore_sessions_frag);

        if (savedInstanceState != null) {

            mTagFilterHolder = savedInstanceState.getParcelable(STATE_FILTER_TAGS);
            mCurrentUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);

        } else if (getIntent() != null) {
            mCurrentUri = getIntent().getData();
        }

        // Build the tag URI
        long[] interval = ScheduleContract.Sessions.getInterval(mCurrentUri);
        if (interval != null) {
            mMode = MODE_TIME_FIT;

            String title = getString(R.string.explore_sessions_time_slot_title,
                    getString(R.string.explore_sessions_show_day_n,
                            UIUtils.startTimeToDayIndex(interval[0])),
                    UIUtils.formatTime(interval[0], this));
            setTitle(title);

            mTimeSlotLayout.setVisibility(View.VISIBLE);
            mTimeSlotDivider.setVisibility(View.VISIBLE);
            timeSlotTextView.setText(title);
            dismissTimeSlotButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTimeSlotLayout.setVisibility(View.GONE);
                    mTimeSlotDivider.setVisibility(View.GONE);
                    mMode = MODE_EXPLORE;
                    mCurrentUri = null;
                    reloadFragment();
                }
            });
        } else {
            mMode = MODE_EXPLORE;
        }

        // Add the back button to the toolbar.
        Toolbar toolbar = getActionBarToolbar();
        toolbar.setNavigationIcon(R.drawable.ic_up);
        toolbar.setNavigationContentDescription(R.string.close_and_go_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpOrBack(ExploreSessionsActivity.this, null);
            }
        });

        // Start loading the tag metadata. This will in turn call the fragment with the
        // correct arguments.
        getLoaderManager().initLoader(TAG_METADATA_TOKEN, null, this);

        // ANALYTICS SCREEN: View the Explore Sessions screen
        // Contains: Nothing (Page name is a constant)
        AnalyticsHelper.sendScreenView(SCREEN_LABEL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Add the filter & search buttons to the toolbar.
        Toolbar toolbar = getActionBarToolbar();
        toolbar.inflateMenu(R.menu.explore_sessions_filtered);
        toolbar.setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_FILTER_TAGS, mTagFilterHolder);
        outState.putParcelable(STATE_CURRENT_URI, mCurrentUri);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_filter:
                mDrawerLayout.openDrawer(GravityCompat.END);
                return true;
            case R.id.menu_search:
                // ANALYTICS EVENT: Click the search button on the ExploreSessions screen
                // Contains: No data (Just that a search occurred, no search term)
                AnalyticsHelper.sendEvent(SCREEN_LABEL, "launchsearch", "");
                startActivity(new Intent(this, SearchActivity.class));
                return true;
        }
        return false;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        enableActionBarAutoHide((CollectionView) findViewById(R.id.collection_view));
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        DrawShadowFrameLayout frame = (DrawShadowFrameLayout) findViewById(R.id.main_content);
        frame.setShadowVisible(shown, shown);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == TAG_METADATA_TOKEN) {
            return TagMetadata.createCursorLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case TAG_METADATA_TOKEN:
                mTagMetadata = new TagMetadata(cursor);
                onTagMetadataLoaded();
                break;
            default:
                cursor.close();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    private void onTagMetadataLoaded() {
        if (mTagFilterHolder == null) {
            // Use the Intent Extras to set up the TagFilterHolder
            mTagFilterHolder = new TagFilterHolder();

            String tag = getIntent().getStringExtra(EXTRA_FILTER_TAG);
            TagMetadata.Tag userTag = mTagMetadata.getTag(tag);
            String userTagCategory = userTag == null ? null : userTag.getCategory();
            if (tag != null && userTagCategory != null) {
                mTagFilterHolder.add(tag, userTagCategory);
            }

            mTagFilterHolder.setShowLiveStreamedSessions(
                    getIntent().getBooleanExtra(EXTRA_SHOW_LIVE_STREAM_SESSIONS, false));

            // update the selected filters using the following logic:
            // a) For onsite attendees, we should default to showing all 'types'
            // (i.e. Sessions, code labs, sandbox, misc).
            if (SettingsUtils.isAttendeeAtVenue(this)) {
                List<TagMetadata.Tag> tags =
                        mTagMetadata.getTagsInCategory(Config.Tags.CATEGORY_TYPE);
                // Here we only add all 'types' if the user has not explicitly selected
                // one of the category_type tags.
                if (tags != null && !TextUtils.equals(userTagCategory, Config.Tags.CATEGORY_TYPE)) {
                    for (TagMetadata.Tag theTag : tags) {
                        mTagFilterHolder.add(theTag.getId(), theTag.getCategory());
                    }
                }
            } else {
                // b) For remote users, default to only showing Sessions that are Live streamed.
                TagMetadata.Tag theTag = mTagMetadata.getTag(Config.Tags.SESSIONS);
                if (!TextUtils.equals(theTag.getCategory(), userTagCategory)) {
                    mTagFilterHolder.add(theTag.getId(), theTag.getCategory());
                }
                mTagFilterHolder.setShowLiveStreamedSessions(true);
            }
        }
        reloadFragment();
        TagAdapter tagAdapter = new TagAdapter();
        mDrawerCollectionView.setCollectionAdapter(tagAdapter);
        mDrawerCollectionView.updateInventory(tagAdapter.getInventory());
    }

    /**
     * Set the activity title to be that of the selected tag name.
     * If the user chosen tag's category is present in the filter and there is a single tag
     * with that category then set the title to the specific tag name else
     * set the title to R.string.explore.
     */
    private void setActivityTitle() {
        if (mMode == MODE_EXPLORE && mTagMetadata != null) {
            String tag = getIntent().getStringExtra(EXTRA_FILTER_TAG);
            TagMetadata.Tag titleTag = tag == null ? null : mTagMetadata.getTag(tag);
            String title = null;
            if (titleTag != null &&
                    mTagFilterHolder.getCountByCategory(titleTag.getCategory()) == 1) {
                for (String tagId : mTagFilterHolder.getSelectedFilters()) {
                    TagMetadata.Tag theTag = mTagMetadata.getTag(tagId);
                    if (TextUtils.equals(titleTag.getCategory(), theTag.getCategory())) {
                        title = theTag.getName();
                    }
                }
            }
            setTitle(title == null ? getString(R.string.title_explore) : title);
        }
    }

    private void reloadFragment() {
        // Build the tag URI
        Uri uri = mCurrentUri;

        if (uri == null) {
            uri = ScheduleContract.Sessions.buildCategoryTagFilterUri(
                    ScheduleContract.Sessions.CONTENT_URI,
                    mTagFilterHolder.toStringArray(),
                    mTagFilterHolder.getCategoryCount());
        } else { // build a uri with the specific filters
            uri = ScheduleContract.Sessions.buildCategoryTagFilterUri(uri,
                    mTagFilterHolder.toStringArray(),
                    mTagFilterHolder.getCategoryCount());
        }
        setActivityTitle();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(ExploreSessionsFragment.EXTRA_SHOW_LIVESTREAMED_SESSIONS,
                mTagFilterHolder.isShowLiveStreamedSessions());

        LOGD(TAG, "Reloading fragment with categories " + mTagFilterHolder.getCategoryCount() +
                " uri: " + uri +
                " showLiveStreamedEvents: " + mTagFilterHolder.isShowLiveStreamedSessions());

        mFragment.reloadFromArguments(intentToFragmentArguments(intent));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Adapter responsible for showing the alt_nav with the tags from
     * {@link com.google.samples.apps.iosched.model.TagMetadata}
     */
    private class TagAdapter implements CollectionViewCallbacks {

        /**
         * Returns a new instance of {@link Inventory}. It always contains three
         * {@link InventoryGroup} groups.
         * <ul>
         *     <li>Themes group containing themes such as Develop, Distribute etc.</li>
         *     <li>Types group containing tags for all types of sessions, codelabs etc.</li>
         *     <li>Topics group containing tags for specific topics such as Android, Cloud etc.</li>
         * </ul>
         *
         * @return A new instance of {@link Inventory}.
         */
        public Inventory getInventory() {
            List<TagMetadata.Tag> themes =
                    mTagMetadata.getTagsInCategory(Config.Tags.CATEGORY_THEME);
            Inventory inventory = new Inventory();

            InventoryGroup themeGroup = new InventoryGroup(GROUP_TOPIC_TYPE_OR_THEME)
                    .setDisplayCols(1)
                    .setDataIndexStart(0)
                    .setShowHeader(false);

            if (themes != null && themes.size() > 0) {
                for (TagMetadata.Tag type : themes) {
                    themeGroup.addItemWithTag(type);
                }
                inventory.addGroup(themeGroup);
            }

            InventoryGroup typesGroup = new InventoryGroup(GROUP_TOPIC_TYPE_OR_THEME)
                    .setDataIndexStart(0)
                    .setShowHeader(true);
            List<TagMetadata.Tag> data = mTagMetadata.getTagsInCategory(Config.Tags.CATEGORY_TYPE);

            if (data != null && data.size() > 0) {
                for (TagMetadata.Tag tag : data) {
                    typesGroup.addItemWithTag(tag);
                }
                inventory.addGroup(typesGroup);
            }

            // We need to add the Live streamed section after the Type category
            InventoryGroup liveStreamGroup = new InventoryGroup(GROUP_LIVE_STREAM)
                    .setDataIndexStart(0)
                    .setShowHeader(true)
                    .addItemWithTag("Livestreamed");
            inventory.addGroup(liveStreamGroup);

            InventoryGroup topicsGroup = new InventoryGroup(GROUP_TOPIC_TYPE_OR_THEME)
                    .setDataIndexStart(0)
                    .setShowHeader(true);

            List<TagMetadata.Tag> topics =
                    mTagMetadata.getTagsInCategory(Config.Tags.CATEGORY_TOPIC);
            if (topics != null && topics.size() > 0) {
                for (TagMetadata.Tag topic : topics) {
                    topicsGroup.addItemWithTag(topic);
                }
                inventory.addGroup(topicsGroup);
            }

            return inventory;
        }

        @Override
        public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.explore_sessions_list_item_alt_header, parent, false);
            // We do not want the divider/header to be read out by TalkBack, so
            // inform the view that this is not important for accessibility.
            UIUtils.setAccessibilityIgnore(view);
            return view;
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId,
                                             String headerLabel, Object headerTag) {
        }

        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(groupId == GROUP_LIVE_STREAM ?
                    R.layout.explore_sessions_list_item_livestream_alt_drawer :
                    R.layout.explore_sessions_list_item_alt_drawer, parent, false);
        }

        @Override
        public void bindCollectionItemView(Context context, View view, int groupId,
                                           int indexInGroup, int dataIndex, Object tag) {
            final CheckBox checkBox = (CheckBox) view.findViewById(R.id.filter_checkbox);
            if (groupId == GROUP_LIVE_STREAM) {
                checkBox.setChecked(mTagFilterHolder.isShowLiveStreamedSessions());
                checkBox.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTagFilterHolder.setShowLiveStreamedSessions(checkBox.isChecked());
                        // update the fragment to reflect the changes.
                        reloadFragment();
                    }
                });

            } else {
                TagMetadata.Tag theTag = (TagMetadata.Tag) tag;
                if (theTag != null) {
                    ((TextView) view.findViewById(R.id.text_view)).setText(theTag.getName());
                    // set the original checked state by looking up our tags.
                    checkBox.setChecked(mTagFilterHolder.contains(theTag.getId()));
                    checkBox.setTag(theTag);
                    checkBox.setOnClickListener(mDrawerItemCheckBoxClickListener);
                }
            }
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.performClick();
                }
            });
        }
    }
}
