/*
 * Copyright 2011 Google Inc.
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
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.widget.BlockView;
import com.google.android.apps.iosched.ui.widget.BlocksLayout;
import com.google.android.apps.iosched.ui.widget.ObservableScrollView;
import com.google.android.apps.iosched.ui.widget.Workspace;
import com.google.android.apps.iosched.util.AnalyticsUtils;
import com.google.android.apps.iosched.util.Maps;
import com.google.android.apps.iosched.util.MotionEventUtils;
import com.google.android.apps.iosched.util.NotifyingAsyncQueryHandler;
import com.google.android.apps.iosched.util.ParserUtils;
import com.google.android.apps.iosched.util.UIUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

/**
 * Shows a horizontally-pageable calendar of conference days. Horizontaly paging is achieved using
 * {@link Workspace}, and the primary UI classes for rendering the calendar are
 * {@link com.google.android.apps.iosched.ui.widget.TimeRulerView},
 * {@link BlocksLayout}, and {@link BlockView}.
 */
public class ScheduleFragment extends Fragment implements
        NotifyingAsyncQueryHandler.AsyncQueryListener,
        ObservableScrollView.OnScrollListener,
        View.OnClickListener {

    private static final String TAG = "ScheduleFragment";

    /**
     * Flags used with {@link android.text.format.DateUtils#formatDateRange}.
     */
    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_DATE
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

    private static final long TUE_START = ParserUtils.parseTime("2011-05-10T00:00:00.000-07:00");
    private static final long WED_START = ParserUtils.parseTime("2011-05-11T00:00:00.000-07:00");

    private static final int DISABLED_BLOCK_ALPHA = 100;

    private static final HashMap<String, Integer> sTypeColumnMap = buildTypeColumnMap();

    // TODO: show blocks that don't fall into columns at the bottom

    public static final String EXTRA_TIME_START = "com.google.android.iosched.extra.TIME_START";
    public static final String EXTRA_TIME_END = "com.google.android.iosched.extra.TIME_END";

    private NotifyingAsyncQueryHandler mHandler;

    private Workspace mWorkspace;
    private TextView mTitle;
    private int mTitleCurrentDayIndex = -1;
    private View mLeftIndicator;
    private View mRightIndicator;

    /**
     * A helper class containing object references related to a particular day in the schedule.
     */
    private class Day {
        private ViewGroup rootView;
        private ObservableScrollView scrollView;
        private View nowView;
        private BlocksLayout blocksView;

        private int index = -1;
        private String label = null;
        private Uri blocksUri = null;
        private long timeStart = -1;
        private long timeEnd = -1;
    }

    private List<Day> mDays = new ArrayList<Day>();

    private static HashMap<String, Integer> buildTypeColumnMap() {
        final HashMap<String, Integer> map = Maps.newHashMap();
        map.put(ParserUtils.BLOCK_TYPE_FOOD, 0);
        map.put(ParserUtils.BLOCK_TYPE_SESSION, 1);
        map.put(ParserUtils.BLOCK_TYPE_OFFICE_HOURS, 2);
        return map;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
        setHasOptionsMenu(true);
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/Schedule");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_schedule, null);

        mWorkspace = (Workspace) root.findViewById(R.id.workspace);

        mTitle = (TextView) root.findViewById(R.id.block_title);

        mLeftIndicator = root.findViewById(R.id.indicator_left);
        mLeftIndicator.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if ((motionEvent.getAction() & MotionEventUtils.ACTION_MASK)
                        == MotionEvent.ACTION_DOWN) {
                    mWorkspace.scrollLeft();
                    return true;
                }
                return false;
            }
        });
        mLeftIndicator.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mWorkspace.scrollLeft();
            }
        });

        mRightIndicator = root.findViewById(R.id.indicator_right);
        mRightIndicator.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if ((motionEvent.getAction() & MotionEventUtils.ACTION_MASK)
                        == MotionEvent.ACTION_DOWN) {
                    mWorkspace.scrollRight();
                    return true;
                }
                return false;
            }
        });
        mRightIndicator.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mWorkspace.scrollRight();
            }
        });

        setupDay(inflater, TUE_START);
        setupDay(inflater, WED_START);

        updateWorkspaceHeader(0);
        mWorkspace.setOnScrollListener(new Workspace.OnScrollListener() {
            public void onScroll(float screenFraction) {
                updateWorkspaceHeader(Math.round(screenFraction));
            }
        }, true);

        return root;
    }

    public void updateWorkspaceHeader(int dayIndex) {
        if (mTitleCurrentDayIndex == dayIndex) {
            return;
        }

        mTitleCurrentDayIndex = dayIndex;
        Day day = mDays.get(dayIndex);
        mTitle.setText(day.label);

        mLeftIndicator
                .setVisibility((dayIndex != 0) ? View.VISIBLE : View.INVISIBLE);
        mRightIndicator
                .setVisibility((dayIndex < mDays.size() - 1) ? View.VISIBLE : View.INVISIBLE);
    }

    private void setupDay(LayoutInflater inflater, long startMillis) {
        Day day = new Day();

        // Setup data
        day.index = mDays.size();
        day.timeStart = startMillis;
        day.timeEnd = startMillis + DateUtils.DAY_IN_MILLIS;
        day.blocksUri = ScheduleContract.Blocks.buildBlocksBetweenDirUri(
                day.timeStart, day.timeEnd);

        // Setup views
        day.rootView = (ViewGroup) inflater.inflate(R.layout.blocks_content, null);

        day.scrollView = (ObservableScrollView) day.rootView.findViewById(R.id.blocks_scroll);
        day.scrollView.setOnScrollListener(this);

        day.blocksView = (BlocksLayout) day.rootView.findViewById(R.id.blocks);
        day.nowView = day.rootView.findViewById(R.id.blocks_now);

        day.blocksView.setDrawingCacheEnabled(true);
        day.blocksView.setAlwaysDrawnWithCacheEnabled(true);

        TimeZone.setDefault(UIUtils.CONFERENCE_TIME_ZONE);
        day.label = DateUtils.formatDateTime(getActivity(), startMillis, TIME_FLAGS);

        mWorkspace.addView(day.rootView);
        mDays.add(day);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Since we build our views manually instead of using an adapter, we
        // need to manually requery every time launched.
        requery();

        getActivity().getContentResolver().registerContentObserver(
                ScheduleContract.Sessions.CONTENT_URI, true, mSessionChangesObserver);

        // Start listening for time updates to adjust "now" bar. TIME_TICK is
        // triggered once per minute, which is how we move the bar over time.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getActivity().registerReceiver(mReceiver, filter, null, new Handler());
    }

    private void requery() {
        for (Day day : mDays) {
            mHandler.startQuery(0, day, day.blocksUri, BlocksQuery.PROJECTION,
                    null, null, ScheduleContract.Blocks.DEFAULT_SORT);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                updateNowView(true);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        getActivity().getContentResolver().unregisterContentObserver(mSessionChangesObserver);
    }

    /**
     * {@inheritDoc}
     */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        Day day = (Day) cookie;

        // Clear out any existing sessions before inserting again
        day.blocksView.removeAllBlocks();

        try {
            while (cursor.moveToNext()) {
                final String type = cursor.getString(BlocksQuery.BLOCK_TYPE);
                final Integer column = sTypeColumnMap.get(type);
                // TODO: place random blocks at bottom of entire layout
                if (column == null) {
                    continue;
                }

                final String blockId = cursor.getString(BlocksQuery.BLOCK_ID);
                final String title = cursor.getString(BlocksQuery.BLOCK_TITLE);
                final long start = cursor.getLong(BlocksQuery.BLOCK_START);
                final long end = cursor.getLong(BlocksQuery.BLOCK_END);
                final boolean containsStarred = cursor.getInt(BlocksQuery.CONTAINS_STARRED) != 0;

                final BlockView blockView = new BlockView(getActivity(), blockId, title, start, end,
                        containsStarred, column);

                final int sessionsCount = cursor.getInt(BlocksQuery.SESSIONS_COUNT);
                if (sessionsCount > 0) {
                    blockView.setOnClickListener(this);
                } else {
                    blockView.setFocusable(false);
                    blockView.setEnabled(false);
                    LayerDrawable buttonDrawable = (LayerDrawable) blockView.getBackground();
                    buttonDrawable.getDrawable(0).setAlpha(DISABLED_BLOCK_ALPHA);
                    buttonDrawable.getDrawable(2).setAlpha(DISABLED_BLOCK_ALPHA);
                }

                day.blocksView.addBlock(blockView);
            }
        } finally {
            cursor.close();
        }
    }

    /** {@inheritDoc} */
    public void onClick(View view) {
        if (view instanceof BlockView) {
            String title = ((BlockView)view).getText().toString();
            AnalyticsUtils.getInstance(getActivity()).trackEvent(
                    "Schedule", "Session Click", title, 0);
            final String blockId = ((BlockView) view).getBlockId();
            final Uri sessionsUri = ScheduleContract.Blocks.buildSessionsUri(blockId);

            final Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
            intent.putExtra(SessionsFragment.EXTRA_SCHEDULE_TIME_STRING,
                    ((BlockView) view).getBlockTimeString());
            ((BaseActivity) getActivity()).openActivityOrFragment(intent);
        }
    }

    /**
     * Update position and visibility of "now" view.
     */
    private boolean updateNowView(boolean forceScroll) {
        final long now = UIUtils.getCurrentTime(getActivity());

        Day nowDay = null; // effectively Day corresponding to today
        for (Day day : mDays) {
            if (now >= day.timeStart && now <= day.timeEnd) {
                nowDay = day;
                day.nowView.setVisibility(View.VISIBLE);
            } else {
                day.nowView.setVisibility(View.GONE);
            }
        }

        if (nowDay != null && forceScroll) {
            // Scroll to show "now" in center
            mWorkspace.setCurrentScreen(nowDay.index);
            final int offset = nowDay.scrollView.getHeight() / 2;
            nowDay.nowView.requestRectangleOnScreen(new Rect(0, offset, 0, offset), true);
            nowDay.blocksView.requestLayout();
            return true;
        }

        return false;
    }

    public void onScrollChanged(ObservableScrollView view) {
        // Keep each day view at the same vertical scroll offset.
        final int scrollY = view.getScrollY();
        for (Day day : mDays) {
            if (day.scrollView != view) {
                day.scrollView.scrollTo(0, scrollY);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.schedule_menu_items, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_now) {
            if (!updateNowView(true)) {
                Toast.makeText(getActivity(), R.string.toast_now_not_visible,
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ContentObserver mSessionChangesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            requery();
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive time update");
            updateNowView(false);
        }
    };

    private interface BlocksQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Blocks.BLOCK_ID,
                ScheduleContract.Blocks.BLOCK_TITLE,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
                ScheduleContract.Blocks.BLOCK_TYPE,
                ScheduleContract.Blocks.SESSIONS_COUNT,
                ScheduleContract.Blocks.CONTAINS_STARRED,
        };

        int _ID = 0;
        int BLOCK_ID = 1;
        int BLOCK_TITLE = 2;
        int BLOCK_START = 3;
        int BLOCK_END = 4;
        int BLOCK_TYPE = 5;
        int SESSIONS_COUNT = 6;
        int CONTAINS_STARRED = 7;
    }
}
