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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.MyScheduleView;
import com.google.samples.apps.iosched.ui.widget.SlidingTabLayout;
import com.google.samples.apps.iosched.util.*;

import java.lang.ref.WeakReference;
import java.util.*;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class MyScheduleActivity extends BaseActivity implements MyScheduleFragment.Listener {

    // Interval that a timer will redraw the UI when in conference day, so that time sensitive
    // widgets, like the "Now" and "Ended" indicators can be properly updated.
    private static final long INTERVAL_TO_REDRAW_UI = 60 * 1000L;

    private static final String SCREEN_LABEL = "My Schedule";
    private static final String TAG = makeLogTag(MyScheduleActivity.class);

    // If true, we are in the wide (tablet) mode where we show conference days side by side;
    // if false, we are in narrow (handset) mode where we use a ViewPager and show only
    // one conference day at a time.
    private boolean mWideMode = false;

    // If in wide mode, we have MyScheduleView widgets showing each day
    private MyScheduleView[] mMyScheduleViewWide = new MyScheduleView[2];

    // The adapters that serves as the source of data for the UI, indicating the available
    // items. We have one adapter per day of the conference. When we push new data into these
    // adapters, the corresponding UIs update automatically.
    private MyScheduleAdapter[] mScheduleAdapters = new MyScheduleAdapter[
            Config.CONFERENCE_DAYS.length];

    // The ScheduleHelper is responsible for feeding data in a format suitable to the Adapter.
    private ScheduleHelper mDataHelper;

    // View pager and adapter (for narrow mode)
    ViewPager mViewPager = null;
    OurViewPagerAdapter mViewPagerAdapter = null;
    SlidingTabLayout mSlidingTabLayout = null;
    ScrollView mScrollViewWide;

    // Login failed butter bar
    View mButterBar;

    boolean mDestroyed = false;

    private static final String ARG_CONFERENCE_DAY_INDEX
            = "com.google.samples.apps.iosched.ARG_CONFERENCE_DAY_INDEX";

    private Set<MyScheduleFragment> mMyScheduleFragments = new HashSet<MyScheduleFragment>();

    public static final String EXTRA_DIALOG_TITLE
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_TITLE";
    public static final String EXTRA_DIALOG_MESSAGE
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_MESSAGE";
    public static final String EXTRA_DIALOG_YES
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_YES";
    public static final String EXTRA_DIALOG_NO
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_NO";
    public static final String EXTRA_DIALOG_URL
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_URL";

    private boolean mShowedAnnouncementDialog = false;

    public MyScheduleActivity() {
        mDataHelper = new ScheduleHelper(this);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_MY_SCHEDULE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_schedule);

        /* [ANALYTICS:SCREEN]
         * TRIGGER:   View the My Schedule screen.
         * LABEL:     'My Schedule'
         * [/ANALYTICS]
         */
        AnalyticsManager.sendScreenView(SCREEN_LABEL);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mScrollViewWide = (ScrollView) findViewById(R.id.main_content_wide);
        mWideMode = findViewById(R.id.my_schedule_first_day) != null;

        int i;
        for (i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            mScheduleAdapters[i] = new MyScheduleAdapter(this, getLUtils());
        }

        mViewPagerAdapter = new OurViewPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);

        if (mWideMode) {
            mMyScheduleViewWide[0] = (MyScheduleView) findViewById(R.id.my_schedule_first_day);
            mMyScheduleViewWide[0].setAdapter(mScheduleAdapters[0]);
            mMyScheduleViewWide[1] = (MyScheduleView) findViewById(R.id.my_schedule_second_day);
            mMyScheduleViewWide[1].setAdapter(mScheduleAdapters[1]);

            TextView firstDayHeaderView = (TextView) findViewById(R.id.day_label_first_day);
            TextView secondDayHeaderView = (TextView) findViewById(R.id.day_label_second_day);
            if (firstDayHeaderView != null) {
                firstDayHeaderView.setText(getDayName(0));
            }
            if (secondDayHeaderView != null) {
                secondDayHeaderView.setText(getDayName(1));
            }
        } else {
            // it's PagerAdapter set.
            mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
            mSlidingTabLayout.setCustomTabView(R.layout.tab_indicator, android.R.id.text1);

            setSlidingTabLayoutContentDescriptions();

            Resources res = getResources();
            mSlidingTabLayout.setSelectedIndicatorColors(res.getColor(R.color.tab_selected_strip));
            mSlidingTabLayout.setDistributeEvenly(true);
            mSlidingTabLayout.setViewPager(mViewPager);
        }

        mButterBar = findViewById(R.id.butter_bar);
        removeLoginFailed();

        if (mSlidingTabLayout != null) {
            mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset,
                        int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mSlidingTabLayout.announceForAccessibility(
                                getString(R.string.my_schedule_page_desc_a11y,
                                        getDayName(position)));
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    enableDisableSwipeRefresh(state == ViewPager.SCROLL_STATE_IDLE);
                }
            });
        }

        overridePendingTransition(0, 0);
        addDataObservers();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mViewPager != null) {
            long now = UIUtils.getCurrentTime(this);
            for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
                if (now >= Config.CONFERENCE_DAYS[i][0] && now <= Config.CONFERENCE_DAYS[i][1]) {
                    mViewPager.setCurrentItem(i);
                    setTimerToUpdateUI(i);
                    break;
                }
            }
        }
        setProgressBarTopWhenActionBarShown((int)
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                        getResources().getDisplayMetrics()));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LOGD(TAG, "onNewIntent, extras " + intent.getExtras());
        if (intent.hasExtra(EXTRA_DIALOG_MESSAGE)) {
            mShowedAnnouncementDialog = false;
            showAnnouncementDialogIfNeeded(intent);
        }
    }

    private String getDayName(int position) {
        if (position >= 0 && position < Config.CONFERENCE_DAYS.length) {
            long timestamp = Config.CONFERENCE_DAYS[position][0];
            return TimeUtils.formatHumanFriendlyShortDate(this, timestamp);
        } else {
            return "";
        }
    }

    private void setSlidingTabLayoutContentDescriptions() {
        for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            mSlidingTabLayout.setContentDescription(i,
                    getString(R.string.my_schedule_tab_desc_a11y, getDayName(i)));
        }
    }

    private void removeLoginFailed() {
        mButterBar.setVisibility(View.GONE);
        deregisterHideableHeaderView(mButterBar);
    }

    @Override
    public void onAuthFailure(String accountName) {
        super.onAuthFailure(accountName);
        UIUtils.setUpButterBar(mButterBar, getString(R.string.login_failed_text),
                getString(R.string.login_failed_text_retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeLoginFailed();
                        retryAuth();

                    }
                }
        );
        registerHideableHeaderView(findViewById(R.id.butter_bar));
    }

    @Override
    protected void onAccountChangeRequested() {
        super.onAccountChangeRequested();
        removeLoginFailed();
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        if (mWideMode) {
            return ViewCompat.canScrollVertically(mScrollViewWide, -1);
        }

        for (MyScheduleFragment fragment : mMyScheduleFragments) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (!fragment.getUserVisibleHint()) {
                    continue;
                }
            }

            return ViewCompat.canScrollVertically(fragment.getListView(), -1);
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
        showAnnouncementDialogIfNeeded(getIntent());
    }

    private void showAnnouncementDialogIfNeeded(Intent intent) {
        final String title = intent.getStringExtra(EXTRA_DIALOG_TITLE);
        final String message = intent.getStringExtra(EXTRA_DIALOG_MESSAGE);

        if (!mShowedAnnouncementDialog && !TextUtils.isEmpty(title) && !TextUtils
                .isEmpty(message)) {
            LOGD(TAG, "showAnnouncementDialogIfNeeded, title: " + title);
            LOGD(TAG, "showAnnouncementDialogIfNeeded, message: " + message);
            final String yes = intent.getStringExtra(EXTRA_DIALOG_YES);
            LOGD(TAG, "showAnnouncementDialogIfNeeded, yes: " + yes);
            final String no = intent.getStringExtra(EXTRA_DIALOG_NO);
            LOGD(TAG, "showAnnouncementDialogIfNeeded, no: " + no);
            final String url = intent.getStringExtra(EXTRA_DIALOG_URL);
            LOGD(TAG, "showAnnouncementDialogIfNeeded, url: " + url);
            final SpannableString spannable = new SpannableString(message == null ? "" : message);
            Linkify.addLinks(spannable, Linkify.WEB_URLS);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title);
            }
            builder.setMessage(spannable);
            if (!TextUtils.isEmpty(no)) {
                builder.setNegativeButton(no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            }
            if (!TextUtils.isEmpty(yes)) {
                builder.setPositiveButton(yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
                });
            }
            final AlertDialog dialog = builder.create();
            dialog.show();
            final TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                // makes the embedded links in the text clickable, if there are any
                messageView.setMovementMethod(LinkMovementMethod.getInstance());
            }
            mShowedAnnouncementDialog = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
        removeDataObservers();
    }

    protected void updateData() {
        for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            mDataHelper.getScheduleDataAsync(mScheduleAdapters[i],
                    Config.CONFERENCE_DAYS[i][0], Config.CONFERENCE_DAYS[i][1]);
        }
    }

    @Override
    public void onFragmentViewCreated(ListFragment fragment) {
        fragment.getListView().addHeaderView(
                getLayoutInflater().inflate(R.layout.reserve_action_bar_space_header_view, null));
        int dayIndex = fragment.getArguments().getInt(ARG_CONFERENCE_DAY_INDEX, 0);
        fragment.setListAdapter(mScheduleAdapters[dayIndex]);
        fragment.getListView().setRecyclerListener(mScheduleAdapters[dayIndex]);
    }

    @Override
    public void onFragmentAttached(MyScheduleFragment fragment) {
        mMyScheduleFragments.add(fragment);
    }

    @Override
    public void onFragmentDetached(MyScheduleFragment fragment) {
        mMyScheduleFragments.remove(fragment);
    }

    private class OurViewPagerAdapter extends FragmentPagerAdapter {

        public OurViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            LOGD(TAG, "Creating fragment #" + position);
            MyScheduleFragment frag = new MyScheduleFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_CONFERENCE_DAY_INDEX, position);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public int getCount() {
            return Config.CONFERENCE_DAYS.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getDayName(position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.my_schedule, menu);
        configureStandardMenuItems(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                /* [ANALYTICS:EVENT]
                 * TRIGGER:   Click the search button on the Schedule screen.
                 * CATEGORY:  'Schedule'
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

    protected void addDataObservers() {
        getContentResolver().registerContentObserver(
                ScheduleContract.BASE_CONTENT_URI, true, mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    public void removeDataObservers() {
        getContentResolver().unregisterContentObserver(mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
                    LOGD(TAG, "sharedpreferences key " + key + " changed, maybe reloading data.");
                    for (MyScheduleAdapter adapter : mScheduleAdapters) {
                        if (PrefUtils.PREF_LOCAL_TIMES.equals(key)) {
                            adapter.forceUpdate();
                        } else if (PrefUtils.PREF_ATTENDEE_AT_VENUE.equals(key)) {
                            updateData();
                        }
                    }
                }
            };

    private final ContentObserver mObserver = new ThrottledContentObserver(
            new ThrottledContentObserver.Callbacks() {
                @Override
                public void onThrottledContentObserverFired() {
                    LOGD(TAG, "content may be changed, reloading data");
                    updateData();
                }
            });

    /**
     * If in conference day, redraw the day's UI every @{link #INTERVAL_TO_REDRAW_UI} ms, so
     * that time sensitive widgets, like "now", "ended" and appropriate styles are updated.
     *
     * @param today the index in the conference days array that corresponds to the current day.
     */
    private void setTimerToUpdateUI(final int today) {
        new UpdateUIRunnable(this, today, new Handler()).scheduleNextRun();
    }

    boolean hasBeenDestroyed() {
        return mDestroyed;
    }

    static final class UpdateUIRunnable implements Runnable {

        final WeakReference<MyScheduleActivity> weakRefToParent;
        final Handler handler;
        final int today;

        public UpdateUIRunnable(MyScheduleActivity activity, int today, Handler handler) {
            weakRefToParent = new WeakReference<MyScheduleActivity>(activity);
            this.handler = handler;
            this.today = today;
        }

        public void scheduleNextRun() {
            handler.postDelayed(this, INTERVAL_TO_REDRAW_UI);
        }

        @Override
        public void run() {
            MyScheduleActivity activity = weakRefToParent.get();
            if (activity == null || activity.hasBeenDestroyed()) {
                LOGD(TAG, "Ativity is not valid anymore. Stopping UI Updater");
                return;
            }
            LOGD(TAG, "Running MySchedule UI updater (now=" +
                    new Date(UIUtils.getCurrentTime(activity)) + ")");
            if (activity.mScheduleAdapters != null
                    && activity.mScheduleAdapters.length > today
                    && activity.mScheduleAdapters[today] != null) {
                try {
                    activity.mScheduleAdapters[today].forceUpdate();
                } finally {
                    // schedule again
                    this.scheduleNextRun();
                }
            }
        }
    }

}
