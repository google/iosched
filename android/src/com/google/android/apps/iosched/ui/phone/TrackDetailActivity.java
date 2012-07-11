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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.BaseActivity;
import com.google.android.apps.iosched.ui.SessionsFragment;
import com.google.android.apps.iosched.ui.SocialStreamActivity;
import com.google.android.apps.iosched.ui.SocialStreamFragment;
import com.google.android.apps.iosched.ui.TrackInfoHelperFragment;
import com.google.android.apps.iosched.ui.VendorsFragment;
import com.google.android.apps.iosched.util.UIUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;

/**
 * A single-pane activity that shows a {@link SessionsFragment} in one tab and a
 * {@link VendorsFragment} in another tab, representing the sessions and developer sandbox companies
 * for the given conference track (Android, Chrome, etc.).
 */
public class TrackDetailActivity extends BaseActivity implements
        ActionBar.TabListener,
        ViewPager.OnPageChangeListener,
        SessionsFragment.Callbacks,
        VendorsFragment.Callbacks,
        TrackInfoHelperFragment.Callbacks {

    private ViewPager mViewPager;
    private String mTrackId;
    private Uri mTrackUri;
    private boolean mShowVendors = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_detail);

        mTrackUri = getIntent().getData();
        mTrackId = ScheduleContract.Tracks.getTrackId(mTrackUri);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new TrackDetailPagerAdapter(getSupportFragmentManager()));
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
        mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));

        mShowVendors = !ScheduleContract.Tracks.CODELABS_TRACK_ID.equals(mTrackId)
                && !ScheduleContract.Tracks.TECH_TALK_TRACK_ID.equals(mTrackId);

        if (mShowVendors) {
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_sessions)
                    .setTabListener(this));
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_vendors)
                    .setTabListener(this));
        }
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(TrackInfoHelperFragment.newFromTrackUri(mTrackUri), "track_info")
                    .commit();
        }
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
                titleId = R.string.title_vendors;
                break;
        }
        
        String title = getString(titleId);
        EasyTracker.getTracker().trackView(title + ": " + getTitle());
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
            if (position == 0) {
                Fragment fragment = new SessionsFragment();
                fragment.setArguments(BaseActivity.intentToFragmentArguments(new Intent(
                        Intent.ACTION_VIEW,
                        allTracks
                                ? ScheduleContract.Sessions.CONTENT_URI
                                : ScheduleContract.Tracks.buildSessionsUri(mTrackId))));
                return fragment;
            } else {
                Fragment fragment = new VendorsFragment();
                fragment.setArguments(BaseActivity.intentToFragmentArguments(new Intent(
                        Intent.ACTION_VIEW,
                        allTracks
                                ? ScheduleContract.Vendors.CONTENT_URI
                                : ScheduleContract.Tracks.buildVendorsUri(mTrackId))));
                return fragment;
            }
        }

        @Override
        public int getCount() {
            return mShowVendors ? 2 : 1;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.track_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_social_stream:
                Intent intent = new Intent(this, SocialStreamActivity.class);
                intent.putExtra(SocialStreamFragment.EXTRA_QUERY,
                        UIUtils.getSessionHashtagsString(mTrackId));
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTrackInfoAvailable(String trackId, String trackName, int trackColor) {
        setTitle(trackName);
        setActionBarColor(trackColor);
        
        EasyTracker.getTracker().trackView(getString(R.string.title_sessions) + ": " + getTitle());
        LOGD("Tracker", getString(R.string.title_sessions) + ": " + getTitle());
    }

    @Override
    public boolean onSessionSelected(String sessionId) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                ScheduleContract.Sessions.buildSessionUri(sessionId)));
        return false;
    }

    @Override
    public boolean onVendorSelected(String vendorId) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                ScheduleContract.Vendors.buildVendorUri(vendorId)));
        return false;
    }
}
