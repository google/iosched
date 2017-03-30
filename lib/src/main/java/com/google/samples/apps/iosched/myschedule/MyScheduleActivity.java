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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleQueryEnum;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleUserActionEnum;
import com.google.samples.apps.iosched.myschedule.ScheduleFilterFragment
        .ScheduleFiltersFragmentListener;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.session.SessionDetailActivity;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.Date;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This shows the schedule of the logged in user, organised per day.
 * <p/>
 * Depending on the device, this Activity uses a {@link ViewPager} with a {@link
 * MyScheduleSingleDayFragment} for its page, which uses a {@link RecyclerView} for each day.
 * Each day data is backed by a {@link MyScheduleDayAdapter}.
 * <p/>
 * If the user attends the conference, all time slots that have sessions are shown, with a button to
 * allow the user to see all sessions in that slot.
 */
public class MyScheduleActivity extends BaseActivity implements ScheduleViewParent {
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

    public static final String EXTRA_FILTER_TAG = ScheduleFilterFragment.FILTER_TAG;
    public static final String EXTRA_SHOW_LIVE_STREAM_SESSIONS =
            ScheduleFilterFragment.SHOW_LIVE_STREAMED_ONLY;

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

    private static final String SCREEN_LABEL = "My Schedule";

    private static final String TAG = makeLogTag(MyScheduleActivity.class);

    private DrawerLayout mDrawerLayout;

    private ScheduleFilterFragment mScheduleFilterFragment;

    private boolean mShowedAnnouncementDialog = false;

    private final Handler mUpdateUiHandler = new Handler();

    private MyScheduleModel mModel; // TODO decouple this
    private PresenterImpl<MyScheduleModel, MyScheduleQueryEnum, MyScheduleUserActionEnum>
            mPresenter;

    public static void launchScheduleWithFilterTag(Context context, Tag tag) {
        Intent intent = new Intent(context, MyScheduleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (tag != null) {
            intent.putExtra(EXTRA_FILTER_TAG, tag.getId());
        }
        context.startActivity(intent);
    }

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.MY_SCHEDULE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_schedule_act);

        launchSessionDetailIfRequiredByIntent(getIntent());

        mScheduleFilterFragment = (ScheduleFilterFragment) getSupportFragmentManager()
                .findFragmentById(R.id.filter_drawer);
        mScheduleFilterFragment.setListener(new ScheduleFiltersFragmentListener() {
            @Override
            public void onFiltersChanged(TagFilterHolder filterHolder) {
                reloadSchedule(filterHolder);
            }
        });

        if (savedInstanceState == null) {
            mScheduleFilterFragment.initWithArguments(intentToFragmentArguments(getIntent()));
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        initPresenter();

        overridePendingTransition(0, 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (TimeUtils.isConferenceInProgress(this)) {
            scheduleNextUiUpdate();
        }

        showAnnouncementDialogIfNeeded(getIntent());
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

    private void initPresenter() {
        mModel = ModelProvider.provideMyScheduleModel(
                new ScheduleHelper(this),
                new SessionsHelper(this),
                this);
        TagFilterHolder filters = mScheduleFilterFragment.getFilters();
        mModel.setFilters(filters);

        final MySchedulePagerFragment contentFragment =
                (MySchedulePagerFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.my_content);
        contentFragment.onFiltersChanged(filters);

        // Each fragment in the pager adapter is an updatable view that the presenter must know
        mPresenter = new PresenterImpl<>(
                mModel,
                contentFragment.getDayFragments(),
                MyScheduleModel.MyScheduleUserActionEnum.values(),
                MyScheduleModel.MyScheduleQueryEnum.values());
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

        setIntent(intent);
        mScheduleFilterFragment.initWithArguments(intentToFragmentArguments(intent));
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        final Fragment contentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.my_content);

        if (contentFragment instanceof ScheduleView) {
            return ((ScheduleView) contentFragment).canSwipeRefreshChildScrollUp();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.my_schedule, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_filter) {
            mDrawerLayout.openDrawer(GravityCompat.END);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final Runnable mUpdateUIRunnable = new Runnable() {
        @Override
        public void run() {
            MyScheduleActivity activity = MyScheduleActivity.this;
            if (activity.isDestroyed()) {
                LOGD(TAG, "Activity is not valid anymore. Stopping UI Updater");
                return;
            }

            LOGD(TAG, "Running MySchedule UI updater (now=" +
                    new Date(TimeUtils.getCurrentTime(activity)) + ")");

            mPresenter.onUserAction(MyScheduleModel.MyScheduleUserActionEnum.REDRAW_UI, null);

            if (TimeUtils.isConferenceInProgress(activity)) {
                scheduleNextUiUpdate();
            }
        }
    };

    void scheduleNextUiUpdate() {
        // Remove existing UI update runnable, if any
        mUpdateUiHandler.removeCallbacks(mUpdateUIRunnable);
        // Post runnable with delay
        mUpdateUiHandler.postDelayed(mUpdateUIRunnable, INTERVAL_TO_REDRAW_UI);
    }

    private void reloadSchedule(TagFilterHolder filterHolder) {
        mModel.setFilters(filterHolder);
        mPresenter.onUserAction(MyScheduleUserActionEnum.RELOAD_DATA, null);
        final Fragment contentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.my_content);

        if (contentFragment instanceof ScheduleView) {
            ((ScheduleView) contentFragment).onFiltersChanged(filterHolder);
        }
    }

    @Override
    public void onRequestClearFilters() {
        mScheduleFilterFragment.clearFilters();
    }

    @Override
    public void onRequestFilterByTag(Tag tag) {
        mScheduleFilterFragment.addFilter(tag);
    }

    @Override
    protected String getScreenLabel() {
        return SCREEN_LABEL;
    }
}
