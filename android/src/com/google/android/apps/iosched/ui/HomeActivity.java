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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.BuildConfig;
import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.gcm.ServerUtilities;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.gtv.GoogleTVSessionLivestreamActivity;
import com.google.android.apps.iosched.util.AccountUtils;
import com.google.android.apps.iosched.util.BeamUtils;
import com.google.android.apps.iosched.util.HelpUtils;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.android.gcm.GCMRegistrar;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.widget.SearchView;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.LOGI;
import static com.google.android.apps.iosched.util.LogUtils.LOGW;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * The landing screen for the app, once the user has logged in.
 *
 * <p>This activity uses different layouts to present its various fragments, depending on the
 * device configuration. {@link MyScheduleFragment}, {@link ExploreFragment}, and
 * {@link SocialStreamFragment} are always available to the user. {@link WhatsOnFragment} is
 * always available on tablets and phones in portrait, but is hidden on phones held in landscape.
 *
 * <p>On phone-size screens, the three fragments are represented by {@link ActionBar} tabs, and
 * can are held inside a {@link ViewPager} to allow horizontal swiping.
 *
 * <p>On tablets, the three fragments are always visible and are presented as either three panes
 * (landscape) or a grid (portrait).
 */
public class HomeActivity extends BaseActivity implements
        ActionBar.TabListener,
        ViewPager.OnPageChangeListener {

    private static final String TAG = makeLogTag(HomeActivity.class);

    private Object mSyncObserverHandle;

    private MyScheduleFragment mMyScheduleFragment;
    private ExploreFragment mExploreFragment;
    private SocialStreamFragment mSocialStreamFragment;

    private ViewPager mViewPager;
    private Menu mOptionsMenu;
    private AsyncTask<Void, Void, Void> mGCMRegisterTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We're on Google TV; immediately short-circuit the normal behavior and show the
        // Google TV-specific landing page.
        if (UIUtils.isGoogleTV(this)) {
            Intent intent = new Intent(HomeActivity.this, GoogleTVSessionLivestreamActivity.class);
            startActivity(intent);
            finish();
        }

        if (isFinishing()) {
            return;
        }

        UIUtils.enableDisableActivities(this);
        EasyTracker.getTracker().setContext(this);
        setContentView(R.layout.activity_home);
        FragmentManager fm = getSupportFragmentManager();

        mViewPager = (ViewPager) findViewById(R.id.pager);
        String homeScreenLabel;
        if (mViewPager != null) {
            // Phone setup
            mViewPager.setAdapter(new HomePagerAdapter(getSupportFragmentManager()));
            mViewPager.setOnPageChangeListener(this);
            mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
            mViewPager.setPageMargin(getResources()
                    .getDimensionPixelSize(R.dimen.page_margin_width));

            final ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_my_schedule)
                    .setTabListener(this));
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_explore)
                    .setTabListener(this));
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_stream)
                    .setTabListener(this));

            homeScreenLabel = getString(R.string.title_my_schedule);

        } else {
            mExploreFragment = (ExploreFragment) fm.findFragmentById(R.id.fragment_tracks);
            mMyScheduleFragment = (MyScheduleFragment) fm.findFragmentById(
                    R.id.fragment_my_schedule);
            mSocialStreamFragment = (SocialStreamFragment) fm.findFragmentById(R.id.fragment_stream);

            homeScreenLabel = "Home";
        }
        getSupportActionBar().setHomeButtonEnabled(false);

        EasyTracker.getTracker().trackView(homeScreenLabel);
        LOGD("Tracker", homeScreenLabel);

        // Sync data on load
        if (savedInstanceState == null) {
            triggerRefresh();
            registerGCMClient();
        }
    }

    private void registerGCMClient() {
        GCMRegistrar.checkDevice(this);
        if (BuildConfig.DEBUG) {
            GCMRegistrar.checkManifest(this);
        }

        final String regId = GCMRegistrar.getRegistrationId(this);

        if (TextUtils.isEmpty(regId)) {
            // Automatically registers application on startup.
            GCMRegistrar.register(this, Config.GCM_SENDER_ID);

        } else {
            // Device is already registered on GCM, check server.
            if (GCMRegistrar.isRegisteredOnServer(this)) {
                // Skips registration
                LOGI(TAG, "Already registered on the GCM server");

            } else {
                // Try to register again, but not on the UI thread.
                // It's also necessary to cancel the task in onDestroy().
                mGCMRegisterTask = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        boolean registered = ServerUtilities.register(HomeActivity.this, regId);
                        if (!registered) {
                            // At this point all attempts to register with the app
                            // server failed, so we need to unregister the device
                            // from GCM - the app will try to register again when
                            // it is restarted. Note that GCM will send an
                            // unregistered callback upon completion, but
                            // GCMIntentService.onUnregistered() will ignore it.
                            GCMRegistrar.unregister(HomeActivity.this);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mGCMRegisterTask = null;
                    }
                };
                mGCMRegisterTask.execute(null, null, null);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mGCMRegisterTask != null) {
            mGCMRegisterTask.cancel(true);
        }

        try {
            GCMRegistrar.onDestroy(this);
        } catch (Exception e) {
            LOGW(TAG, "GCM unregistration error", e);
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
                titleId = R.string.title_my_schedule;
                break;
            case 1:
                titleId = R.string.title_explore;
                break;
            case 2:
                titleId = R.string.title_stream;
                break;
        }

        String title = getString(titleId);
        EasyTracker.getTracker().trackView(title);
        LOGD("Tracker", title);

    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Since the pager fragments don't have known tags or IDs, the only way to persist the
        // reference is to use putFragment/getFragment. Remember, we're not persisting the exact
        // Fragment instance. This mechanism simply gives us a way to persist access to the
        // 'current' fragment instance for the given fragment (which changes across orientation
        // changes).
        //
        // The outcome of all this is that the "Refresh" menu button refreshes the stream across
        // orientation changes.
        if (mSocialStreamFragment != null) {
            getSupportFragmentManager().putFragment(outState, "stream_fragment",
                    mSocialStreamFragment);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mSocialStreamFragment == null) {
            mSocialStreamFragment = (SocialStreamFragment) getSupportFragmentManager()
                    .getFragment(savedInstanceState, "stream_fragment");
        }
    }

    private class HomePagerAdapter extends FragmentPagerAdapter {
        public HomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return (mMyScheduleFragment = new MyScheduleFragment());

                case 1:
                    return (mExploreFragment = new ExploreFragment());

                case 2:
                    return (mSocialStreamFragment = new SocialStreamFragment());
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getSupportMenuInflater().inflate(R.menu.home, menu);
        setupSearchMenuItem(menu);

        if (!BeamUtils.isBeamUnlocked(this)) {
            // Only show Beam unlocked after first Beam
            MenuItem beamItem = menu.findItem(R.id.menu_beam);
            if (beamItem != null) {
                menu.removeItem(beamItem.getItemId());
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupSearchMenuItem(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null && UIUtils.hasHoneycomb()) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                triggerRefresh();
                return true;

            case R.id.menu_search:
                if (!UIUtils.hasHoneycomb()) {
                    startSearch(null, false, Bundle.EMPTY, false);
                    return true;
                }
                break;

            case R.id.menu_about:
                HelpUtils.showAbout(this);
                return true;

            case R.id.menu_sign_out:
                AccountUtils.signOut(this);
                finish();
                return true;

            case R.id.menu_beam:
                Intent beamIntent = new Intent(this, BeamActivity.class);
                startActivity(beamIntent);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerRefresh() {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        if (!UIUtils.isGoogleTV(this)) {
	        ContentResolver.requestSync(
	                new Account(AccountUtils.getChosenAccountName(this),
	                        GoogleAccountManager.ACCOUNT_TYPE),
	                ScheduleContract.CONTENT_AUTHORITY, extras);
        }

        if (mSocialStreamFragment != null) {
            mSocialStreamFragment.refresh();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    public void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int which) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String accountName = AccountUtils.getChosenAccountName(HomeActivity.this);
                    if (TextUtils.isEmpty(accountName)) {
                        setRefreshActionButtonState(false);
                        return;
                    }

                    Account account = new Account(accountName, GoogleAccountManager.ACCOUNT_TYPE);
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, ScheduleContract.CONTENT_AUTHORITY);
                    boolean syncPending = ContentResolver.isSyncPending(
                            account, ScheduleContract.CONTENT_AUTHORITY);
                    setRefreshActionButtonState(syncActive || syncPending);
                }
            });
        }
    };
}
