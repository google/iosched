/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.ui;


import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.util.BeamUtils;
import com.google.android.apps.iosched.util.ImageLoader;
import com.google.android.apps.iosched.util.ReflectionUtils;
import com.google.android.apps.iosched.util.UIUtils;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;

/**
 * An activity that shows session search results. This activity can be either single
 * or multi-pane, depending on the device configuration.
 */
public class SearchActivity extends BaseActivity implements
        SessionsFragment.Callbacks,
        ImageLoader.ImageLoaderProvider {

    private boolean mTwoPane;

    private SessionsFragment mSessionsFragment;
    private Fragment mDetailFragment;
    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);
        mTwoPane = (findViewById(R.id.fragment_container_detail) != null);

        FragmentManager fm = getSupportFragmentManager();
        mSessionsFragment = (SessionsFragment) fm.findFragmentById(R.id.fragment_container_master);
        if (mSessionsFragment == null) {
            mSessionsFragment = new SessionsFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container_master, mSessionsFragment)
                    .commit();
        }

        mDetailFragment = fm.findFragmentById(R.id.fragment_container_detail);

        mImageLoader = new ImageLoader(this, R.drawable.person_image_empty)
                .setMaxImageSize(getResources().getDimensionPixelSize(R.dimen.speaker_image_size))
                .setFadeInImage(UIUtils.hasHoneycombMR1());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        onNewIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        String query = intent.getStringExtra(SearchManager.QUERY);

        setTitle(Html.fromHtml(getString(R.string.title_search_query, query)));

        mSessionsFragment.reloadFromArguments(intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, Sessions.buildSearchUri(query))));

        EasyTracker.getTracker().sendView("Search: " + query);
        LOGD("Tracker", "Search: " + query);
        
        updateDetailBackground();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search, menu);
        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null && UIUtils.hasHoneycomb()) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                searchView.setQueryRefinementEnabled(true);
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        ReflectionUtils.tryInvoke(searchItem, "collapseActionView");
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String s) {
                        return false;
                    }
                });
                searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                    @Override
                    public boolean onSuggestionSelect(int i) {
                        return false;
                    }

                    @Override
                    public boolean onSuggestionClick(int i) {
                        ReflectionUtils.tryInvoke(searchItem, "collapseActionView");
                        return false;
                    }
                });
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                if (!UIUtils.hasHoneycomb()) {
                    startSearch(null, false, Bundle.EMPTY, false);
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateDetailBackground() {
        if (mTwoPane) {
            findViewById(R.id.fragment_container_detail).setBackgroundResource(
                    (mDetailFragment == null)
                            ? R.drawable.grey_frame_on_white_empty_sessions
                            : R.drawable.grey_frame_on_white);
        }
    }

    @Override
    public boolean onSessionSelected(String sessionId) {
        Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(sessionId);
        Intent detailIntent = new Intent(Intent.ACTION_VIEW, sessionUri);

        if (mTwoPane) {
            BeamUtils.setBeamSessionUri(this, sessionUri);
            SessionDetailFragment fragment = new SessionDetailFragment();
            fragment.setArguments(BaseActivity.intentToFragmentArguments(detailIntent));
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_detail, fragment)
                    .commit();
            mDetailFragment = fragment;
            updateDetailBackground();
            return true;
        } else {
            startActivity(detailIntent);
            return false;
        }
    }

    @Override
    public ImageLoader getImageLoaderInstance() {
        return mImageLoader;
    }
}
