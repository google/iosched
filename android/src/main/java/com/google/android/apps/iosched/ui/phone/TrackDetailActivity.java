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

package com.google.android.apps.iosched.ui.phone;

import android.view.Menu;
import android.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.*;
import com.google.android.apps.iosched.util.UIUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.apps.iosched.provider.ScheduleContract.Sessions.QUERY_PARAMETER_FILTER;
import static com.google.android.apps.iosched.provider.ScheduleContract.Sessions.QUERY_VALUE_FILTER_SESSIONS_CODELABS_ONLY;
import static com.google.android.apps.iosched.provider.ScheduleContract.Sessions.QUERY_VALUE_FILTER_OFFICE_HOURS_ONLY;
import static com.google.android.apps.iosched.util.LogUtils.LOGD;

public class TrackDetailActivity extends BaseActivity implements
        ActionBar.TabListener,
        ViewPager.OnPageChangeListener,
        SessionsFragment.Callbacks,
        SandboxFragment.Callbacks,
        TrackInfoHelperFragment.Callbacks {

    private static final int TAB_SESSIONS = 100;
    private static final int TAB_OFFICE_HOURS = 101;
    private static final int TAB_SANDBOX = 102;

    private ViewPager mViewPager;
    private String mTrackId;
    private String mHashtag;

    private List<Integer> mTabs = new ArrayList<Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_detail);

        Uri trackUri = getIntent().getData();
        mTrackId = ScheduleContract.Tracks.getTrackId(trackUri);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new TrackDetailPagerAdapter(getSupportFragmentManager()));
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
        mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(TrackInfoHelperFragment.newFromTrackUri(trackUri), "track_info")
                    .commit();
        }
    }

    @Override
    public Intent getParentActivityIntent() {
        return new Intent(this, HomeActivity.class)
                .putExtra(HomeActivity.EXTRA_DEFAULT_TAB, HomeActivity.TAB_EXPLORE);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {
    }

    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);
        
        int titleId = -1;
        switch (position) {
            case 0:
                titleId = R.string.title_sessions;
                break;
            case 1:
                titleId = R.string.title_office_hours;
                break;
            case 2:
                titleId = R.string.title_sandbox;
                break;
        }
        
        String title = getString(titleId);
        EasyTracker.getTracker().sendView(title + ": " + getTitle());
        LOGD("Tracker", title + ": " + getTitle());
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    private class TrackDetailPagerAdapter extends FragmentPagerAdapter {

        public TrackDetailPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            boolean allTracks = (ScheduleContract.Tracks.ALL_TRACK_ID.equals(mTrackId));
            switch (mTabs.get(position)) {
                case TAB_SESSIONS: {
                    Fragment fragment = new SessionsFragment();
                    fragment.setArguments(BaseActivity.intentToFragmentArguments(new Intent(
                            Intent.ACTION_VIEW,
                            (allTracks
                                    ? ScheduleContract.Sessions.CONTENT_URI
                                    : ScheduleContract.Tracks.buildSessionsUri(mTrackId))
                                    .buildUpon()
                                    .appendQueryParameter(QUERY_PARAMETER_FILTER,
                                            QUERY_VALUE_FILTER_SESSIONS_CODELABS_ONLY)
                                    .build()
                    )));
                    return fragment;
                }
                case TAB_OFFICE_HOURS: {
                    Fragment fragment = new SessionsFragment();
                    fragment.setArguments(BaseActivity.intentToFragmentArguments(new Intent(
                            Intent.ACTION_VIEW,
                            (allTracks
                                    ? ScheduleContract.Sessions.CONTENT_URI
                                    : ScheduleContract.Tracks.buildSessionsUri(mTrackId))
                                    .buildUpon()
                                    .appendQueryParameter(QUERY_PARAMETER_FILTER,
                                            QUERY_VALUE_FILTER_OFFICE_HOURS_ONLY)
                                    .build())));
                    return fragment;
                }
                case TAB_SANDBOX:
                default: {
                    Fragment fragment = new SandboxFragment();
                    fragment.setArguments(BaseActivity.intentToFragmentArguments(new Intent(
                            Intent.ACTION_VIEW,
                            allTracks
                                    ? ScheduleContract.Sandbox.CONTENT_URI
                                    : ScheduleContract.Tracks.buildSandboxUri(mTrackId))));
                    return fragment;
                }
            }
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.track_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_social_stream:
                Intent intent = new Intent(this, SocialStreamActivity.class);
                intent.putExtra(SocialStreamFragment.EXTRA_QUERY,
                        UIUtils.getSessionHashtagsString(mHashtag));
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTrackInfoAvailable(String trackId, TrackInfo track) {
        setTitle(track.name);
        setActionBarTrackIcon(track.name, track.color);
        mHashtag = track.hashtag;

        switch (track.meta) {
            case ScheduleContract.Tracks.TRACK_META_SESSIONS_ONLY:
                mTabs.add(TAB_SESSIONS);
                break;

            case ScheduleContract.Tracks.TRACK_META_SANDBOX_OFFICE_HOURS_ONLY:
                mTabs.add(TAB_OFFICE_HOURS);
                mTabs.add(TAB_SANDBOX);
                break;

            case ScheduleContract.Tracks.TRACK_META_OFFICE_HOURS_ONLY:
                mTabs.add(TAB_OFFICE_HOURS);
                break;

            case ScheduleContract.Tracks.TRACK_META_NONE:
            default:
                mTabs.add(TAB_SESSIONS);
                mTabs.add(TAB_OFFICE_HOURS);
                mTabs.add(TAB_SANDBOX);
                break;
        }

        mViewPager.getAdapter().notifyDataSetChanged();

        if (mTabs.size() > 1) {
            setHasTabs();
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            for (int tab : mTabs) {
                int titleResId;
                switch (tab) {
                    case TAB_SANDBOX:
                        titleResId = R.string.title_sandbox;
                        break;

                    case TAB_OFFICE_HOURS:
                        titleResId = R.string.title_office_hours;
                        break;

                    case TAB_SESSIONS:
                    default:
                        titleResId = R.string.title_sessions;
                        break;
                }

                actionBar.addTab(actionBar.newTab().setText(titleResId).setTabListener(this));
            }
        }
    }

    @Override
    public boolean onSessionSelected(String sessionId) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                ScheduleContract.Sessions.buildSessionUri(sessionId)));
        return false;
    }

    @Override
    public boolean onCompanySelected(String companyId) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                ScheduleContract.Sandbox.buildCompanyUri(companyId)));
        return false;
    }
}
