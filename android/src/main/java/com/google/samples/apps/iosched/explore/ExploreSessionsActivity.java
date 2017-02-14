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

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.myschedule.SessionsFilterAdapter;
import com.google.samples.apps.iosched.myschedule.SessionsFilterAdapter.SessionFilterAdapterListener;
import com.google.samples.apps.iosched.myschedule.TagFilterHolder;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.ui.SearchActivity;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.TagUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.List;

/**
 * This activity displays all sessions based on the selected filters.
 * <p/>
 * It can either be invoked with specific filters or the user can choose the filters to use from the
 * alt_nav_bar.
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

    private TagMetadata mTagMetadata;

    private TagFilterHolder mTagFilterHolder;

    // Keep track of the current URI. This can diverge from Intent.getData() if the user
    // dismisses a particular timeslot. At that point, the Activity switches the mode
    // as well as the Uri used.
    private Uri mCurrentUri;

    private ExploreSessionsFragment mFragment;

    private RecyclerView mFiltersList;

    private DrawerLayout mDrawerLayout;

    private CollapsingToolbarLayout mCollapsingToolbar;

    private ImageView mHeaderImage;

    private TextView mTitle;

    private ImageLoader mImageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore_sessions_act);
        // Transition will be postponed until header image is loaded
        supportPostponeEnterTransition();

        mImageLoader = new ImageLoader(this);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        mHeaderImage = (ImageView) findViewById(R.id.header_image);
        mTitle = (TextView) findViewById(R.id.title);
        mFiltersList = (RecyclerView) findViewById(R.id.filters);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_flipped, GravityCompat.END);

        mFragment = (ExploreSessionsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.explore_sessions_frag);

        if (savedInstanceState != null) {

            mTagFilterHolder = savedInstanceState.getParcelable(STATE_FILTER_TAGS);
            mCurrentUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);

        } else if (getIntent() != null) {
            mCurrentUri = getIntent().getData();
        }

        // Add the back button to the toolbar.
        setToolbarAsUp(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpOrBack(ExploreSessionsActivity.this, null);
            }
        });
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Start loading the tag metadata. This will in turn call the fragment with the
        // correct arguments.
        getSupportLoaderManager().initLoader(TAG_METADATA_TOKEN, null, this);

        // ANALYTICS SCREEN: View the Explore Sessions screen
        // Contains: Nothing (Page name is a constant)
        AnalyticsHelper.sendScreenView(SCREEN_LABEL);
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // This screen sets a status bar color at runtime. The base activity may overwrite this on
        // configuration changes so set the header again to ensure the correct result.
        setHeader();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Add the filter & search buttons to the toolbar.
        Toolbar toolbar = getToolbar();
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

            mTagFilterHolder.setShowLiveStreamedOnly(
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
                mTagFilterHolder.setShowLiveStreamedOnly(true);
            }
        }
        reloadFragment();
        SessionsFilterAdapter adapter = new SessionsFilterAdapter(this, mTagFilterHolder,
                mTagMetadata);
        adapter.setSessionFilterAdapterListener(new SessionFilterAdapterListener() {
            @Override
            public void onFiltersChanged(TagFilterHolder filterHolder) {
                reloadFragment();
            }
        });
        mFiltersList.setAdapter(adapter);
    }

    /**
     * Set the activity title & header image.
     * <p/>
     * If the user is browsing a single track then it's name is shown as the title and a relevant
     * header image; otherwise a generic title and image is shown.
     */
    private void setHeader() {
        if (mTagMetadata != null) {
            String title = null;
            String headerImage = null;
            @ColorInt int trackColor = Color.TRANSPARENT;

            // If exactly one track is selected, show its title and image.
            if (mTagFilterHolder.getSelectedTopicsCount() == 1) {
                for (String tagId : mTagFilterHolder.getSelectedTopics()) {
                    if (TagUtils.isTrackTag(tagId) || TagUtils.isThemeTag(tagId)) {
                        TagMetadata.Tag selectedTag = mTagMetadata.getTag(tagId);
                        title = selectedTag.getName();
                        headerImage = selectedTag.getPhotoUrl();
                        trackColor = selectedTag.getColor();
                        break;
                    }
                }
            }

            if (title == null) {
                title = getString(R.string.title_explore);
            }
            mTitle.setText(title);

            if (headerImage != null) {
                mHeaderImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                mImageLoader.loadImage(headerImage, mHeaderImage,
                        new RequestListener<String, Bitmap>() {
                            @Override
                            public boolean onException(final Exception e, final String model,
                                    final Target<Bitmap> target,
                                    final boolean isFirstResource) {
                                supportStartPostponedEnterTransition();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(final Bitmap resource,
                                    final String model, final Target<Bitmap> target,
                                    final boolean isFromMemoryCache,
                                    final boolean isFirstResource) {
                                target.onResourceReady(resource, null);
                                supportStartPostponedEnterTransition();
                                return true;
                            }
                        });
            } else {
                mHeaderImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                mHeaderImage.setImageResource(R.drawable.ic_hash_io_16_monochrome);
                supportStartPostponedEnterTransition();
            }

            final int statusBarColor =
                    trackColor != Color.TRANSPARENT ? UIUtils.adjustColorForStatusBar(trackColor) :
                            UIUtils.getThemeColor(this, R.attr.colorPrimaryDark,
                                    R.color.theme_primary_dark);
            mCollapsingToolbar.setContentScrimColor(trackColor);
            mDrawerLayout.setStatusBarBackgroundColor(statusBarColor);
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
        setHeader();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(ExploreSessionsFragment.EXTRA_SHOW_LIVESTREAMED_SESSIONS,
                mTagFilterHolder.isShowLiveStreamedOnly());

        LOGD(TAG, "Reloading fragment with categories " + mTagFilterHolder.getCategoryCount() +
                " uri: " + uri +
                " showLiveStreamedEvents: " + mTagFilterHolder.isShowLiveStreamedOnly());

        mFragment.reloadFromArguments(intentToFragmentArguments(intent));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
