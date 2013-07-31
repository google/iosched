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

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract.Announcements;
import com.google.android.apps.iosched.util.TimeUtils;
import com.google.android.apps.iosched.util.UIUtils;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

/**
 * A fragment used in {@link HomeActivity} that shows either a countdown,
 * Announcements, or 'thank you' text, at different times (before/during/after
 * the conference).
 */
public class WhatsOnFragment extends Fragment implements
        LoaderCallbacks<Cursor> {
    private static final int ANNOUNCEMENTS_LOADER_ID = 0;
    private static final int ANNOUNCEMENTS_CYCLE_INTERVAL_MILLIS = 6000;

    private Handler mHandler = new Handler();

    private TextView mCountdownTextView;
    private ViewGroup mRootView;
    private View mAnnouncementView;
    private Cursor mAnnouncementsCursor;
    private String mLatestAnnouncementId;
    private LayoutInflater mInflater;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mInflater = inflater;
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_whats_on,
                container);
        refresh();
        return mRootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mHandler.removeCallbacksAndMessages(null);
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    private void refresh() {
        mHandler.removeCallbacksAndMessages(null);
        mRootView.removeAllViews();

        final long currentTimeMillis = UIUtils.getCurrentTime(getActivity());

        // Show Loading... and load the view corresponding to the current state
        if (currentTimeMillis < UIUtils.CONFERENCE_START_MILLIS) {
            setupBefore();
        } else if (currentTimeMillis > UIUtils.CONFERENCE_END_MILLIS) {
            setupAfter();
        } else {
            setupDuring();
        }
    }

    private void setupBefore() {
        // Before conference, show countdown.
        mCountdownTextView = (TextView) mInflater
                .inflate(R.layout.whats_on_countdown, mRootView, false);
        mRootView.addView(mCountdownTextView);
        mHandler.post(mCountdownRunnable);
    }

    private void setupAfter() {
        // After conference, show canned text.
        mInflater.inflate(R.layout.whats_on_thank_you,
                mRootView, true);
    }

    private void setupDuring() {
        // Start background query to load announcements
        getLoaderManager().initLoader(ANNOUNCEMENTS_LOADER_ID, null, this);
        getActivity().getContentResolver().registerContentObserver(
                Announcements.CONTENT_URI, true, mObserver);
    }

    /**
     * Event that updates countdown timer. Posts itself again to
     * {@link #mHandler} to continue updating time.
     */
    private Runnable mCountdownRunnable = new Runnable() {
        public void run() {
            int remainingSec = (int) Math.max(0,
                    (UIUtils.CONFERENCE_START_MILLIS - UIUtils
                            .getCurrentTime(getActivity())) / 1000);
            final boolean conferenceStarted = remainingSec == 0;

            if (conferenceStarted) {
                // Conference started while in countdown mode, switch modes and
                // bail on future countdown updates.
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        refresh();
                    }
                }, 100);
                return;
            }

            final int secs = remainingSec % 86400;
            final int days = remainingSec / 86400;
            final String str;
            if (days == 0) {
                str = getResources().getString(
                        R.string.whats_on_countdown_title_0,
                        DateUtils.formatElapsedTime(secs));
            } else {
                str = getResources().getQuantityString(
                        R.plurals.whats_on_countdown_title, days, days,
                        DateUtils.formatElapsedTime(secs));
            }
            mCountdownTextView.setText(str);

            // Repost ourselves to keep updating countdown
            mHandler.postDelayed(mCountdownRunnable, 1000);
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                Announcements.CONTENT_URI, AnnouncementsQuery.PROJECTION, null, null,
                Announcements.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (cursor != null && cursor.getCount() > 0) {
            // Need to always set this because original gets unset in onLoaderReset
            mAnnouncementsCursor = cursor;
            cursor.moveToFirst();
            // Only update announcements if there's a new one
            String latestAnnouncementId = cursor.getString(AnnouncementsQuery.ANNOUNCEMENT_ID);
            if (!latestAnnouncementId.equals(mLatestAnnouncementId)) {
                mHandler.removeCallbacks(mCycleAnnouncementsRunnable);
                mLatestAnnouncementId = latestAnnouncementId;
                showAnnouncements();
            }
        } else {
            mHandler.removeCallbacks(mCycleAnnouncementsRunnable);
            showNoAnnouncements();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAnnouncementsCursor = null;
    }

    /**
     * Show the the announcements
     */
    private void showAnnouncements() {
        mAnnouncementsCursor.moveToFirst();

        ViewGroup announcementsRootView = (ViewGroup) mInflater.inflate(
                R.layout.whats_on_announcements, mRootView, false);
        mAnnouncementView = announcementsRootView.findViewById(R.id.announcement_container);

        // Begin cycling in announcements
        mHandler.post(mCycleAnnouncementsRunnable);

        final View moreButton = announcementsRootView.findViewById(R.id.extra_button);
        moreButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AnnouncementsActivity.class));
            }
        });

        mRootView.removeAllViews();
        mRootView.addView(announcementsRootView);
    }

    private Runnable mCycleAnnouncementsRunnable = new Runnable() {
        @Override
        public void run() {
            // First animate the current announcement out
            final int animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            final int height = mAnnouncementView.getHeight();
            TranslateAnimation anim = new TranslateAnimation(0, 0, 0, height);
            anim.setDuration(animationDuration);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    // Set the announcement data
                    TextView titleView = (TextView) mAnnouncementView.findViewById(
                            R.id.announcement_title);
                    TextView agoView = (TextView) mAnnouncementView.findViewById(
                            R.id.announcement_ago);
                    titleView.setText(mAnnouncementsCursor.getString(
                            AnnouncementsQuery.ANNOUNCEMENT_TITLE));

                    long date = mAnnouncementsCursor.getLong(
                            AnnouncementsQuery.ANNOUNCEMENT_DATE);
                    String when = TimeUtils.getTimeAgo(date, getActivity());
                    agoView.setText(when);

                    final String url = mAnnouncementsCursor.getString(
                            AnnouncementsQuery.ANNOUNCEMENT_URL);
                    mAnnouncementView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent announcementIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(url));
                            announcementIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                            UIUtils.preferPackageForIntent(getActivity(), announcementIntent,
                                    UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
                            try {
                                startActivity(announcementIntent);
                            } catch (ActivityNotFoundException ignored) {
                            }
                        }
                    });

                    int nextPosition = (mAnnouncementsCursor.getPosition() + 1)
                            % mAnnouncementsCursor.getCount();
                    mAnnouncementsCursor.moveToPosition(nextPosition);

                    // Animate the announcement in
                    TranslateAnimation anim = new TranslateAnimation(0, 0, height, 0);
                    anim.setDuration(animationDuration);
                    mAnnouncementView.startAnimation(anim);

                    mHandler.postDelayed(mCycleAnnouncementsRunnable,
                            ANNOUNCEMENTS_CYCLE_INTERVAL_MILLIS + animationDuration);
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mAnnouncementView.startAnimation(anim);
        }
    };

    /**
     * Show a placeholder message
     */
    private void showNoAnnouncements() {
        mRootView.removeAllViews();
        mInflater.inflate(R.layout.empty_announcements, mRootView, true);
    }

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (getActivity() == null) {
                return;
            }

            getLoaderManager().restartLoader(ANNOUNCEMENTS_LOADER_ID, null, WhatsOnFragment.this);
        }
    };

    private interface AnnouncementsQuery {
        String[] PROJECTION = {
                Announcements.ANNOUNCEMENT_ID,
                Announcements.ANNOUNCEMENT_TITLE,
                Announcements.ANNOUNCEMENT_DATE,
                Announcements.ANNOUNCEMENT_URL,
        };

        int ANNOUNCEMENT_ID = 0;
        int ANNOUNCEMENT_TITLE = 1;
        int ANNOUNCEMENT_DATE = 2;
        int ANNOUNCEMENT_URL = 3;
    }
}
