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

package com.google.samples.apps.iosched.appwidget;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.myschedule.MyScheduleActivity;
import com.google.samples.apps.iosched.ui.SimpleSectionedListAdapter;
import com.google.samples.apps.iosched.ui.TaskStackBuilderProxyActivity;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.*;

import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * This is the service that provides the factory to be bound to the collection service.
 */
public class ScheduleWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetRemoteViewsFactory(this.getApplicationContext());
    }

    /**
     * This is the factory that will provide data to the collection widget.
     */
    private static class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private static final String TAG = makeLogTag(WidgetRemoteViewsFactory.class);

        private Context mContext;
        private SparseIntArray mPMap;
        private List<SimpleSectionedListAdapter.Section> mSections;
        private SparseBooleanArray mHeaderPositionMap;

        StringBuilder mBuffer = new StringBuilder();
        Formatter mFormatter = new Formatter(mBuffer, Locale.getDefault());
        private ArrayList<ScheduleItem> mScheduleItems;
        private int mDefaultSessionColor;
        private int mDefaultStartEndTimeColor;

        public WidgetRemoteViewsFactory(Context context) {
            mContext = context;
        }

        public void onCreate() {
            // Since we reload the cursor in onDataSetChanged() which gets called immediately after
            // onCreate(), we do nothing here.
        }

        public void onDestroy() {

        }

        public int getCount() {
            if (mScheduleItems == null || !AccountUtils.hasActiveAccount(mContext)) {
                return 0;
            }
            if (mScheduleItems.size() < 10) {
                init();
            }
            return mScheduleItems.size();
        }

        public int getItemViewType(int position) {
            if (position < 0 || position >= mScheduleItems.size()) {
                LOGE(TAG, "Invalid view position passed to MyScheduleDayAdapter: " + position);
                return VIEW_TYPE_NORMAL;
            }
            ScheduleItem item = mScheduleItems.get(position);
            long now = TimeUtils.getCurrentTime(mContext);
            if (item.startTime <= now && now <= item.endTime && item.type == ScheduleItem.SESSION) {
                return VIEW_TYPE_NOW;
            } else {
                return VIEW_TYPE_NORMAL;
            }
        }

        private static final int VIEW_TYPE_NORMAL = 0;
        private static final int VIEW_TYPE_NOW = 1;

        public RemoteViews getViewAt(int position) {
            RemoteViews rv;

            boolean isSectionHeader = mHeaderPositionMap.get(position);
            int offset = mPMap.get(position);

            if (isSectionHeader) {
                rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_schedule_header);
                SimpleSectionedListAdapter.Section section = mSections.get(offset - 1);
                rv.setTextViewText(R.id.widget_schedule_day, section.getTitle());

            } else {
                int itemPosition = position - offset;

                Intent homeIntent = new Intent(mContext, MyScheduleActivity.class);

                final ScheduleItem item = mScheduleItems.get(itemPosition);
                ScheduleItem nextItem = (itemPosition < mScheduleItems.size() - 1) ? mScheduleItems.get(itemPosition + 1) : null;

                if (mDefaultSessionColor < 0) {
                    mDefaultSessionColor = mContext.getResources().getColor(R.color.default_session_color);
                }

                int itemViewType = getItemViewType(itemPosition);
                boolean isNowPlaying = false;
                boolean isPastDuringConference = false;
                mDefaultStartEndTimeColor = R.color.body_text_2;

                if (itemViewType == VIEW_TYPE_NOW) {
                    isNowPlaying = true;
                    mDefaultStartEndTimeColor = R.color.body_text_1;
                }

                rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_schedule_item);


                if (itemPosition < 0 || itemPosition >= mScheduleItems.size()) {
                    LOGE(TAG, "Invalid view position passed to MyScheduleDayAdapter: " + position);
                    return rv;
                }

                long now = TimeUtils.getCurrentTime(mContext);
                rv.setTextViewText(R.id.start_end_time, formatTime(now, item));

                rv.setViewVisibility(R.id.live_now_badge, View.GONE);

                // Set default colors to time indicators, in case they were overridden by conflict warning:
                if (!isNowPlaying) {
                    rv.setTextColor(R.id.start_end_time, mContext.getResources().getColor(mDefaultStartEndTimeColor));
                }

                if (item.type == ScheduleItem.FREE) {
                    rv.setImageViewResource(R.id.icon, R.drawable.ic_browse);

                    rv.setTextViewText(R.id.slot_title, mContext.getText(R.string.browse_sessions));
                    rv.setTextColor(R.id.slot_title, mContext.getResources().getColor(R.color.flat_button_text));

                    rv.setTextViewText(R.id.slot_room, item.subtitle);
                    rv.setTextColor(R.id.slot_room, mContext.getResources().getColor(R.color.body_text_2));

                    Intent fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                            homeIntent,
                            new Intent(Intent.ACTION_VIEW, ScheduleContract.Sessions.buildUnscheduledSessionsInInterval(
                                    item.startTime, item.endTime))
                    );
                    rv.setOnClickFillInIntent(R.id.box, fillIntent);

                } else if (item.type == ScheduleItem.BREAK) {
                    rv.setImageViewResource(R.id.icon, UIUtils.getBreakIcon(item.title));

                    rv.setTextViewText(R.id.slot_title, item.title);
                    rv.setTextColor(R.id.slot_title, mContext.getResources().getColor(R.color.body_text_1));

                    rv.setTextViewText(R.id.slot_room, item.room);
                    rv.setTextColor(R.id.slot_room, mContext.getResources().getColor(R.color.body_text_2));

                } else if (item.type == ScheduleItem.SESSION) {
                    rv.setImageViewResource(R.id.icon, UIUtils.getSessionIcon(item.sessionType));

                    rv.setTextViewText(R.id.slot_title, item.title);
                    rv.setTextColor(R.id.slot_title, mContext.getResources().getColor(R.color.body_text_1));

                    rv.setTextViewText(R.id.slot_room, item.room);
                    rv.setTextColor(R.id.slot_room, mContext.getResources().getColor(R.color.body_text_2));

                    // show or hide the "LIVE NOW" badge
                    final boolean showLiveBadge = 0 != (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM)
                            && now >= item.startTime && now <= item.endTime;
                    rv.setViewVisibility(R.id.live_now_badge, (showLiveBadge ? View.VISIBLE : View.GONE));

                    // show or hide the "conflict" warning
                    if (!isPastDuringConference) {
                        final boolean showConflict = 0 != (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS);
                        if (showConflict && !isNowPlaying) {
                            int conflictColor = mContext.getResources().getColor(R.color.my_schedule_conflict);
                            rv.setTextColor(R.id.start_end_time, conflictColor);
                        }
                    }

                    Intent fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                            homeIntent,
                            new Intent(Intent.ACTION_VIEW, ScheduleContract.Sessions.buildSessionUri(item.sessionId)));
                    rv.setOnClickFillInIntent(R.id.box, fillIntent);

                } else {
                    LOGE(TAG, "Invalid item type in MyScheduleDayAdapter: " + item.type);
                }
            }

            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onDataSetChanged() {
            init();
        }

        private void init() {
            ScheduleHelper scheduleHelper = new ScheduleHelper(mContext);

            //Fetch all sessions and blocks
            List<ScheduleItem> allScheduleItems = scheduleHelper.getScheduleData(Long.MIN_VALUE, Long.MAX_VALUE);

            String displayTimeZone = SettingsUtils.getDisplayTimeZone(mContext).getID();

            mSections = new ArrayList<SimpleSectionedListAdapter.Section>();
            long previousTime = -1;
            long time;
            mPMap = new SparseIntArray();
            mHeaderPositionMap = new SparseBooleanArray();
            int offset = 0;
            int globalPosition = 0;
            int position = 0;
            mScheduleItems = new ArrayList<ScheduleItem>();
            for (ScheduleItem item : allScheduleItems) {
                if (item.endTime <= TimeUtils.getCurrentTime(mContext)) {
                    continue;
                }
                mScheduleItems.add(item);
                time = item.startTime;
                if (!UIUtils.isSameDayDisplay(previousTime, time, mContext)) {
                    mBuffer.setLength(0);
                    mSections.add(new SimpleSectionedListAdapter.Section(position,
                            DateUtils.formatDateRange(
                                    mContext, mFormatter,
                                    time, time,
                                    DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE,
                                    displayTimeZone
                            ).toString()
                    ));
                    ++offset;
                    mHeaderPositionMap.put(globalPosition, true);
                    mPMap.put(globalPosition, offset);
                    ++globalPosition;
                }
                mHeaderPositionMap.put(globalPosition, false);
                mPMap.put(globalPosition, offset);
                ++globalPosition;
                ++position;
                previousTime = time;
            }
        }

        private String formatTime(long now, ScheduleItem item) {
            StringBuilder time = new StringBuilder();
            if (item.startTime <= now) {
                // session is happening now!
                if (0 != (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM)) {
                    // session has live stream
                    time.append(mContext.getString(R.string.watch_now));
                } else {
                    time.append(mContext.getString(R.string.session_now));
                }
            } else {
                // session in the future
                time.append(TimeUtils.formatShortTime(mContext, new Date(item.startTime)));
            }
            time.append(" - ");
            time.append(TimeUtils.formatShortTime(mContext, new Date(item.endTime)));
            return time.toString();
        }
    }
}
