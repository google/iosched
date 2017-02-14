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

package com.google.samples.apps.iosched.myschedule;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleQueryEnum;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleUserActionEnum;
import com.google.samples.apps.iosched.myschedule.SessionsFilterAdapter.OnFiltersChangedListener;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.session.SessionDetailActivity;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This shows the schedule of the logged in user, organised per day.
 * <p/>
 * Depending on the device, this Activity uses either a {@link ViewPager} with a {@link
 * MyScheduleSingleDayFragment} for its page (the "narrow" layout) or a {@link
 * MyScheduleAllDaysFragment}, which uses a {@link MyScheduleSingleDayNoScrollView} for each day.
 * Each day data is backed by a {@link MyScheduleDayAdapter} (the "wide" layout).
 * <p/>
 * If the user attends the conference, all time slots that have sessions are shown, with a button to
 * allow the user to see all sessions in that slot.
 */
public class MyScheduleActivity extends BaseActivity implements
        MyScheduleSingleDayFragment.Listener, LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * This is used in the narrow mode, to pass in the day index to the {@link
     * MyScheduleSingleDayFragment}.
     */
    public static final String ARG_CONFERENCE_DAY_INDEX
            = "com.google.samples.apps.iosched.ARG_CONFERENCE_DAY_INDEX";

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

    public static final String EXTRA_FILTER_TAG =
            "com.google.samples.apps.iosched.myschedule.EXTRA_FILTER_TAG";
    public static final String EXTRA_SHOW_LIVE_STREAM_SESSIONS =
            "com.google.samples.apps.iosched.myschedule.EXTRA_SHOW_LIVE_STREAM_SESSIONS";

    // The saved instance state filters
    private static final String STATE_FILTER_TAGS =
            "com.google.samples.apps.iosched.myschedule.STATE_FILTER_TAGS";
    private static final String STATE_CURRENT_URI =
            "com.google.samples.apps.iosched.myschedule.STATE_CURRENT_URI";
    /**
     * Interval that a timer will redraw the UI during the conference, so that time sensitive
     * widgets, like the "Now" and "Ended" indicators can be properly updated.
     */
    private static final long INTERVAL_TO_REDRAW_UI = 1 * TimeUtils.MINUTE;

    /**
     * The key used to save the tags for {@link MyScheduleSingleDayFragment}s so the automatically
     * recreated fragments can be reused by {@link #mViewPagerAdapter}.
     */
    private static final String SINGLE_DAY_FRAGMENTS_TAGS = "single_day_fragments_tags";

    /**
     * The key used to save the position in the {@link #mViewPagerAdapter} for the current {@link
     * MyScheduleSingleDayFragment}s.
     */
    private static final String CURRENT_SINGLE_DAY_FRAGMENT_POSITION =
            "current_single_day_fragments_position";

    private static final String SCREEN_LABEL = "My Schedule";

    private static final String TAG = makeLogTag(MyScheduleActivity.class);

    private static final int TAG_METADATA_TOKEN = 0x8;

    /**
     * If true, we are in the wide (tablet landscape) mode where we show conference days side by
     * side; if false, we are in narrow (non tablet landscape) mode where we use a ViewPager and
     * show one conference day per page.
     */
    private boolean mWideMode = false;

    /**
     * This is used for narrow mode only, to switch between days, it is null in wide mode
     */
    private ViewPager mViewPager;

    /**
     * This is used for narrow mode only, it is empty in wide mode
     */
    private Set<MyScheduleSingleDayFragment> mMyScheduleSingleDayFragments = new HashSet<>();

    /**
     * This is used for narrow mode only, it is null in wide mode. Each page in the {@link
     * #mViewPager} is a {@link MyScheduleSingleDayFragment}.
     */
    private MyScheduleDayViewPagerAdapter mViewPagerAdapter;

    /**
     * This is used for narrow mode only, to display the conference days, it is null in wide mode
     */
    private TabLayout mTabLayout;

    /**
     * This is used in wide mode only, it is null in narrow mode
     */
    private ScrollView mScrollViewWide;

    /**
     * This is a view displayed when login has failed
     */
    private View mFailedLoginView;

    private DrawerLayout mDrawerLayout;
    private RecyclerView mFiltersList;
    private View mClearFilters;
    private SessionsFilterAdapter mFilterAdapter;
    private TagMetadata mTagMetadata;
    private TagFilterHolder mTagFilterHolder;

    /**
     * During the conference, this is set to the current day, eg 1 for the first day, 2 for the
     * second etc Outside of conference period, this is set to 1.
     */
    private int mToday;

    /**
     * True during the conference or pre conference, false otherwise
     */
    private boolean mConferenceInProgress;

    private boolean mDestroyed = false;

    private boolean mShowedAnnouncementDialog = false;

    private PresenterImpl<MyScheduleModel, MyScheduleQueryEnum, MyScheduleUserActionEnum>
            mPresenter;

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.MY_SCHEDULE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_schedule_act);

        launchSessionDetailIfRequiredByIntent(getIntent());

        // ANALYTICS SCREEN: View the My Schedule screen
        // Contains: Nothing (Page name is a constant)
        AnalyticsHelper.sendScreenView(SCREEN_LABEL);

        String[] singleDayFragmentsTags = null;
        int currentSingleDayFragment = 0;

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SINGLE_DAY_FRAGMENTS_TAGS)) {
                singleDayFragmentsTags = savedInstanceState.getStringArray(
                        SINGLE_DAY_FRAGMENTS_TAGS);
            }
            if (savedInstanceState.containsKey(CURRENT_SINGLE_DAY_FRAGMENT_POSITION)) {
                currentSingleDayFragment = savedInstanceState.getInt(
                        CURRENT_SINGLE_DAY_FRAGMENT_POSITION);
            }

            mTagFilterHolder = savedInstanceState.getParcelable(STATE_FILTER_TAGS);
            // TODO when filters work
//            mCurrentUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);
        }

        initViews(singleDayFragmentsTags, currentSingleDayFragment);
        initPresenter();

        overridePendingTransition(0, 0);

        // Start loading the tag metadata
        getSupportLoaderManager().initLoader(TAG_METADATA_TOKEN, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        calculateCurrentDay();

        if (mConferenceInProgress) {
            scheduleNextUIUpdate();
        }

        showAnnouncementDialogIfNeeded(getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mViewPagerAdapter != null && mViewPagerAdapter.getFragments() != null) {
            MyScheduleSingleDayFragment[] singleDayFragments = mViewPagerAdapter.getFragments();
            String[] tags = new String[singleDayFragments.length];
            for (int i = 0; i < tags.length; i++) {
                tags[i] = singleDayFragments[i].getTag();
            }
            outState.putStringArray(SINGLE_DAY_FRAGMENTS_TAGS, tags);
            outState.putInt(CURRENT_SINGLE_DAY_FRAGMENT_POSITION, mViewPager.getCurrentItem());
        }

        outState.putParcelable(STATE_FILTER_TAGS, mTagFilterHolder);
        // TODO when filter works
//        outState.putParcelable(STATE_CURRENT_URI, mCurrentUri);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
    }

    /**
     * Pre-process the {@code intent} received to open this activity to determine if it was a deep
     * link to a SessionDetail. Typically you wouldn't use this type of logic, but we need to
     * because of the path of session details page on the website is only /schedule and session ids
     * are part of the query parameters ("sid").
     */
    private void launchSessionDetailIfRequiredByIntent(Intent intent) {
        if (intent != null && !TextUtils.isEmpty(intent.getDataString())) {
            String intentDataString = intent.getDataString();
            try {
                Uri dataUri = Uri.parse(intentDataString);

                // Website sends sessionId in query parameter "sid". If present, show
                // SessionDetailActivity
                String sessionId = dataUri.getQueryParameter("sid");
                if (!TextUtils.isEmpty(sessionId)) {
                    LOGD(TAG, "SessionId received from website: " + sessionId);
                    SessionDetailActivity.startSessionDetailActivity(MyScheduleActivity.this,
                            sessionId);
                    finish();
                } else {
                    LOGD(TAG, "No SessionId received from website");
                }
            } catch (Exception exception) {
                LOGE(TAG, "Data uri existing but wasn't parsable for a session detail deep link");
            }
        }
    }

    /**
     * @param singleDayFragmentsTags   The tags of the recreated fragments, if this is an Activity
     *                                 recreation, or null
     * @param currentSingleDayFragment The position of the current single day fragment (ie the
     *                                 position of the current tab)
     */
    private void initViews(String[] singleDayFragmentsTags, int currentSingleDayFragment) {
        // Set up view to show login failure
        mFailedLoginView = findViewById(R.id.butter_bar);
        hideLoginFailureView();

        // Set up correct view mode
        detectNarrowOrWideMode();
        if (mWideMode) {
            setUpViewForWideMode();
        } else {
            setUpViewPagerForNarrowMode(singleDayFragmentsTags, currentSingleDayFragment);
        }

        setupFilterDrawer();
    }

    private void initPresenter() {
        MyScheduleModel model =
                ModelProvider.provideMyScheduleModel(new ScheduleHelper(this), this);
        if (mWideMode) {
            MyScheduleAllDaysFragment fragment = (MyScheduleAllDaysFragment)
                    getSupportFragmentManager().findFragmentById(R.id.myScheduleWideFrag);
            mPresenter = new PresenterImpl<>(model,
                    fragment,
                    MyScheduleModel.MyScheduleUserActionEnum.values(),
                    MyScheduleModel.MyScheduleQueryEnum.values());
            mPresenter.loadInitialQueries();
        } else {
            // Each fragment in the pager adapter is an updatable view that the presenter must know
            MyScheduleSingleDayFragment[] fragments = mViewPagerAdapter.getFragments();
            mPresenter = new PresenterImpl<>(model, fragments,
                    MyScheduleModel.MyScheduleUserActionEnum.values(),
                    MyScheduleModel.MyScheduleQueryEnum.values());
        }
    }

    private void detectNarrowOrWideMode() {
        // When changing orientation, if previously in wide mode, the system recreates the wide
        // fragment, so need to check also that view pager isn't visible
        mWideMode = getSupportFragmentManager().findFragmentById(R.id.myScheduleWideFrag) != null &&
                findViewById(R.id.view_pager).getVisibility() == View.GONE;
    }

    private void setUpViewForWideMode() {
        mScrollViewWide = (ScrollView) findViewById(R.id.main_content_wide);

        // Nothing else to do, as wide mode only uses MyScheduleAllDaysFragment, which will set
        // itself up
    }

    /**
     * @param singleDayFragmentsTags   The tags of the recreated fragments, if this is an Activity
     *                                 recreation, or null
     * @param currentSingleDayFragment The position of the current single day fragment (ie the
     *                                 position of the current tab)
     */
    private void setUpViewPagerForNarrowMode(String[] singleDayFragmentsTags,
            int currentSingleDayFragment) {
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPagerAdapter = new MyScheduleDayViewPagerAdapter(this, getSupportFragmentManager(),
                MyScheduleModel.showPreConferenceData(this));
        mViewPagerAdapter.setRetainedFragmentsTags(singleDayFragmentsTags);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setCurrentItem(currentSingleDayFragment);

        mTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        mViewPager.setPageMargin(getResources()
                .getDimensionPixelSize(R.dimen.my_schedule_page_margin));
        mViewPager.setPageMarginDrawable(R.drawable.page_margin);
    }

    private void setupFilterDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mFiltersList = (RecyclerView) findViewById(R.id.filters);
        mClearFilters = findViewById(R.id.clear_filters);
        mClearFilters.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mFilterAdapter.clearAllFilters();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        calculateCurrentDay();
        if (mViewPager != null) {
            showDay(mToday);
        }

    }

    private void calculateCurrentDay() {
        long now = TimeUtils.getCurrentTime(this);

        // If we are before or after the conference, the first day is considered the current day
        mToday = 1;
        mConferenceInProgress = false;

        for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            if (now >= Config.CONFERENCE_DAYS[i][0] && now <= Config.CONFERENCE_DAYS[i][1]) {
                // mToday is set to 1 for the first day, 2 for the second etc
                mToday = i + 1;
                mConferenceInProgress = true;
                break;
            }
        }
    }

    /**
     * @param day Pass in 1 for the first day, 2 for the second etc
     */
    private void showDay(int day) {
        int preConferenceDays = MyScheduleModel.showPreConferenceData(this) ? 1 : 0;
        mViewPager.setCurrentItem(day - 1 + preConferenceDays);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        launchSessionDetailIfRequiredByIntent(intent);
        LOGD(TAG, "onNewIntent, extras " + intent.getExtras());
        if (intent.hasExtra(EXTRA_DIALOG_MESSAGE)) {
            mShowedAnnouncementDialog = false;
            showAnnouncementDialogIfNeeded(intent);
        }
    }

    private void hideLoginFailureView() {
        mFailedLoginView.setVisibility(View.GONE);
    }

    @Override
    public void onAuthFailure(String accountName) {
        super.onAuthFailure(accountName);
        UIUtils.setUpButterBar(mFailedLoginView, getString(R.string.login_failed_text),
                getString(R.string.login_failed_text_retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideLoginFailureView();
                        retryAuth();

                    }
                }
        );
    }

    @Override
    public void onAccountChangeRequested() {
        super.onAccountChangeRequested();
        hideLoginFailureView();
        reloadData();
    }

    private void reloadData() {
        if (mPresenter != null) {
            mPresenter.onUserAction(MyScheduleModel.MyScheduleUserActionEnum.RELOAD_DATA, null);
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        if (mWideMode) {
            return ViewCompat.canScrollVertically(mScrollViewWide, -1);
        }

        for (MyScheduleSingleDayFragment fragment : mMyScheduleSingleDayFragments) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (!fragment.getUserVisibleHint()) {
                    continue;
                }
            }

            return ViewCompat.canScrollVertically(fragment.getListView(), -1);
        }

        return false;
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
    public void onSingleDayFragmentAttached(MyScheduleSingleDayFragment fragment) {
        mMyScheduleSingleDayFragments.add(fragment);
    }

    @Override
    public void onSingleDayFragmentDetached(MyScheduleSingleDayFragment fragment) {
        mMyScheduleSingleDayFragments.remove(fragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.my_schedule, menu);
        return true;
    }


    private Runnable mUpdateUIRunnable = new Runnable() {

        @Override
        public void run() {
            MyScheduleActivity activity = MyScheduleActivity.this;
            if (activity.hasBeenDestroyed()) {
                LOGD(TAG, "Activity is not valid anymore. Stopping UI Updater");
                return;
            }

            LOGD(TAG, "Running MySchedule UI updater (now=" +
                    new Date(TimeUtils.getCurrentTime(activity)) + ")");

            mPresenter.onUserAction(MyScheduleModel.MyScheduleUserActionEnum.REDRAW_UI, null);

            if (mConferenceInProgress) {
                scheduleNextUIUpdate();
            }
        }
    };

    private Handler mUpdateUIHandler = new Handler();

    private void scheduleNextUIUpdate() {
        // Remove existing UI update runnable, if any
        mUpdateUIHandler.removeCallbacks(mUpdateUIRunnable);

        // Post runnable with delay
        mUpdateUIHandler.postDelayed(mUpdateUIRunnable, INTERVAL_TO_REDRAW_UI);
    }

    private boolean hasBeenDestroyed() {
        return mDestroyed;
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
        if (loader.getId() == TAG_METADATA_TOKEN) {
            mTagMetadata = new TagMetadata(cursor);
            onTagMetadataLoaded();
        } else {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void onTagMetadataLoaded() {
        // TODO refactor this to a common location
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
                List<Tag> tags =
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

        // TODO reloadFragments();

        mFilterAdapter = new SessionsFilterAdapter(this, mTagFilterHolder, mTagMetadata);
        mFilterAdapter.setSessionFilterAdapterListener(new OnFiltersChangedListener() {
            @Override
            public void onFiltersChanged(TagFilterHolder filterHolder) {
                updateScheduleFilters(filterHolder);
            }
        });
        mFiltersList.setAdapter(mFilterAdapter);
    }

    private void updateScheduleFilters(TagFilterHolder filterHolder) {
        mClearFilters.setVisibility(filterHolder.hasAnyFilters() ? View.VISIBLE : View.INVISIBLE);
        // TODO update fragments
    }
}
