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
import com.google.android.apps.iosched.util.UIUtils;

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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A fragment used in {@link HomeActivity} that shows either a countdown,
 * announcements, or 'thank you' text, at different times (before/during/after
 * the conference).
 */
public class WhatsOnFragment extends Fragment implements
        LoaderCallbacks<Cursor> {

    private Handler mHandler = new Handler();

    private TextView mCountdownTextView;
    private ViewGroup mRootView;
    private Cursor mAnnouncementsCursor;
    private LayoutInflater mInflater;
    private int mTitleCol = -1;
    private int mDateCol = -1;
    private int mUrlCol = -1;

    private static final int ANNOUNCEMENTS_LOADER_ID = 0;

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
        mHandler.removeCallbacks(mCountdownRunnable);
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    private void refresh() {
        mHandler.removeCallbacks(mCountdownRunnable);
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
    private final Runnable mCountdownRunnable = new Runnable() {
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
                Announcements.CONTENT_URI, null, null, null,
                Announcements.ANNOUNCEMENT_DATE + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (getActivity() == null) {
            return;
        }
        if (data != null && data.getCount() > 0) {
            mTitleCol = data.getColumnIndex(Announcements.ANNOUNCEMENT_TITLE);
            mDateCol = data.getColumnIndex(Announcements.ANNOUNCEMENT_DATE);
            mUrlCol = data.getColumnIndex(Announcements.ANNOUNCEMENT_URL);
            showAnnouncements(data);
        } else {
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
    private void showAnnouncements(Cursor announcements) {
        mAnnouncementsCursor = announcements;

        ViewGroup announcementsRootView = (ViewGroup) mInflater.inflate(
                R.layout.whats_on_announcements, mRootView, false);
        final ViewPager pager = (ViewPager) announcementsRootView.findViewById(
                R.id.announcements_pager);
        final View previousButton = announcementsRootView.findViewById(
                R.id.announcements_previous_button);
        final View nextButton = announcementsRootView.findViewById(
                R.id.announcements_next_button);

        final PagerAdapter adapter = new AnnouncementsAdapter();
        pager.setAdapter(adapter);
        pager.setPageMargin(
                getResources().getDimensionPixelSize(R.dimen.announcements_margin_width));
        pager.setPageMarginDrawable(R.drawable.announcements_divider);

        pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                previousButton.setEnabled(position > 0);
                nextButton.setEnabled(position < adapter.getCount() - 1);
            }
        });

        previousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                pager.setCurrentItem(pager.getCurrentItem() - 1);
            }
        });

        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                pager.setCurrentItem(pager.getCurrentItem() + 1);
            }
        });

        previousButton.setEnabled(false);
        nextButton.setEnabled(adapter.getCount() > 1);
        mRootView.removeAllViews();
        mRootView.addView(announcementsRootView);
    }

    /**
     * Show a placeholder message
     */
    private void showNoAnnouncements() {
        mInflater.inflate(R.layout.empty_announcements, mRootView, true);
    }

    public class AnnouncementsAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup pager, int position) {
            mAnnouncementsCursor.moveToPosition(position);
            LinearLayout rootView = (LinearLayout) mInflater.inflate(
                    R.layout.pager_item_announcement, pager, false);
            TextView titleView = (TextView) rootView.findViewById(R.id.announcement_title);
            TextView subtitleView = (TextView) rootView.findViewById(R.id.announcement_ago);
            titleView.setText(mAnnouncementsCursor.getString(mTitleCol));

            final long date = mAnnouncementsCursor.getLong(mDateCol);
            final String when = UIUtils.getTimeAgo(date, getActivity());
            final String url = mAnnouncementsCursor.getString(mUrlCol);

            subtitleView.setText(when);
            rootView.setTag(url);

            if (!TextUtils.isEmpty(url)) {
                rootView.setClickable(true);
                rootView.setOnClickListener(mAnnouncementClick);
            } else {
                rootView.setClickable(false);
            }

            pager.addView(rootView, 0);
            return rootView;
        }

        private final OnClickListener mAnnouncementClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = (String) v.getTag();
                if (!TextUtils.isEmpty(url)) {
                    UIUtils.safeOpenLink(getActivity(),
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
            }
        };

        @Override
        public void destroyItem(ViewGroup pager, int position, Object view) {
            pager.removeView((View) view);
        }

        @Override
        public int getCount() {
            return mAnnouncementsCursor.getCount();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (getActivity() == null) {
                return;
            }

            Loader<Cursor> loader = getLoaderManager().getLoader(ANNOUNCEMENTS_LOADER_ID);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    };
}
