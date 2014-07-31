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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
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
import com.google.samples.apps.iosched.ui.MyScheduleActivity;
import com.google.samples.apps.iosched.ui.SimpleSectionedListAdapter;
import com.google.samples.apps.iosched.ui.TaskStackBuilderProxyActivity;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.PrefUtils;
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
        private int mDefaultStartTimeColor;
        private int mDefaultEndTimeColor;

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
                LOGE(TAG, "Invalid view position passed to MyScheduleAdapter: " + position);
                return VIEW_TYPE_NORMAL;
            }
            ScheduleItem item = mScheduleItems.get(position);
            long now = UIUtils.getCurrentTime(mContext);
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
                int layoutResId = R.layout.widget_schedule_item;
                mDefaultStartTimeColor = R.color.body_text_2;
                mDefaultEndTimeColor = R.color.body_text_3;

                if (itemViewType == VIEW_TYPE_NOW) {
                    isNowPlaying = true;
                    layoutResId = R.layout.widget_schedule_item_now;
                    mDefaultStartTimeColor = mDefaultEndTimeColor = R.color.body_text_1_inverse;
                } else {
                    if (item.type == ScheduleItem.BREAK) {
                        layoutResId = R.layout.widget_schedule_item_break;
                    }
                }

                rv = new RemoteViews(mContext.getPackageName(), layoutResId);


                if (itemPosition < 0 || itemPosition >= mScheduleItems.size()) {
                    LOGE(TAG, "Invalid view position passed to MyScheduleAdapter: " + position);
                    return rv;
                }

                long now = UIUtils.getCurrentTime(mContext);
                boolean showEndTime;
                if (item.startTime <= now) {
                    // session is happening now!
                    rv.setTextViewText(R.id.start_time, mContext.getString(R.string.session_now));
                    showEndTime = nextItem == null || nextItem.startTime != item.endTime;
                } else {
                    // session in the future
                    rv.setTextViewText(R.id.start_time, TimeUtils.formatShortTime(mContext, new Date(item.startTime)));
                    // do we need and end time view?
                    showEndTime = nextItem == null || nextItem.startTime != item.endTime;
                }

                if (showEndTime) {
                    rv.setViewVisibility(R.id.end_time, View.VISIBLE);
                    rv.setTextViewText(R.id.end_time, mContext.getString(R.string.schedule_end_time,
                            TimeUtils.formatShortTime(mContext, new Date(item.endTime))));
                } else {
                    // no need to show end time
                    rv.setViewVisibility(R.id.end_time, View.GONE);
                }


                rv.setViewVisibility(R.id.live_now_badge, View.GONE);

                rv.setViewVisibility(R.id.conflict_warning, View.GONE);

                // Set default colors to time indicators, in case they were overridden by conflict warning:
                if (!isNowPlaying) {
                    rv.setTextColor(R.id.start_time, mContext.getResources().getColor(mDefaultStartTimeColor));
                    rv.setTextColor(R.id.end_time, mContext.getResources().getColor(mDefaultEndTimeColor));

                }

                if (item.type == ScheduleItem.FREE) {

                    rv.setImageViewResource(R.id.background_image, R.drawable.schedule_item_free);

                    rv.setTextViewText(R.id.slot_title, mContext.getText(R.string.browse_sessions));
                    rv.setTextColor(R.id.slot_title, mContext.getResources().getColor(R.color.theme_primary));

                    rv.setTextViewText(R.id.slot_subtitle, item.subtitle);
                    rv.setTextColor(R.id.slot_subtitle, mContext.getResources().getColor(R.color.body_text_2));

                    Intent fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                            homeIntent,
                            new Intent(Intent.ACTION_VIEW, ScheduleContract.Sessions.buildUnscheduledSessionsInInterval(
                        item.startTime, item.endTime))
                    );
                    rv.setOnClickFillInIntent(R.id.box, fillIntent);

                } else if (item.type == ScheduleItem.BREAK) {
                    rv.setImageViewResource(R.id.background_image, R.drawable.schedule_item_break);

                    rv.setTextViewText(R.id.slot_title, item.title);
                    rv.setTextColor(R.id.slot_title, mContext.getResources().getColor(R.color.body_text_1));

                    rv.setTextViewText(R.id.slot_subtitle, item.subtitle);
                    rv.setTextColor(R.id.slot_subtitle, mContext.getResources().getColor(R.color.body_text_2));

                } else if (item.type == ScheduleItem.SESSION) {
                    final int color = UIUtils.scaleSessionColorToDefaultBG(
                            item.backgroundColor == 0 ? mDefaultSessionColor : item.backgroundColor);

                    Bitmap image = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    image.eraseColor(color);
                    rv.setImageViewBitmap(R.id.background_image, image);

                    rv.setTextViewText(R.id.slot_title, item.title);
                    rv.setTextColor(R.id.slot_title, mContext.getResources().getColor(R.color.body_text_1_inverse));

                    rv.setTextViewText(R.id.slot_subtitle, item.subtitle);
                    rv.setTextColor(R.id.slot_subtitle, mContext.getResources().getColor(R.color.body_text_2_inverse));

                    // show or hide the "LIVE NOW" badge
                    final boolean showLiveBadge = 0 != (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM)
                            && now >= item.startTime && now <= item.endTime;
                    rv.setViewVisibility(R.id.live_now_badge, (showLiveBadge ? View.VISIBLE : View.GONE));

                    // show or hide the "conflict" warning
                    if (!isPastDuringConference) {
                        final boolean showConflict = 0 != (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS);
                        rv.setViewVisibility(R.id.conflict_warning, (showConflict ? View.VISIBLE : View.GONE));
                        if (showConflict && !isNowPlaying) {
                            int conflictColor = mContext.getResources().getColor(R.color.my_schedule_conflict);
                            rv.setTextColor(R.id.start_time, conflictColor);
                            rv.setTextColor(R.id.end_time, conflictColor);
                        }
                    }

                    Intent fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                            homeIntent,
                            new Intent(Intent.ACTION_VIEW, ScheduleContract.Sessions.buildSessionUri(item.sessionId)));
                    rv.setOnClickFillInIntent(R.id.box, fillIntent);

                } else {
                    LOGE(TAG, "Invalid item type in MyScheduleAdapter: " + item.type);
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

            String displayTimeZone = PrefUtils.getDisplayTimeZone(mContext).getID();

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
                if (item.endTime <= UIUtils.getCurrentTime(mContext)) {
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
    }
}
