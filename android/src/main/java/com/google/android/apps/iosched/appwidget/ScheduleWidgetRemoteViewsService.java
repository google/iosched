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

package com.google.android.apps.iosched.appwidget;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.HomeActivity;
import com.google.android.apps.iosched.ui.SessionLivestreamActivity;
import com.google.android.apps.iosched.ui.SimpleSectionedListAdapter;
import com.google.android.apps.iosched.ui.SimpleSectionedListAdapter.Section;
import com.google.android.apps.iosched.ui.TaskStackBuilderProxyActivity;
import com.google.android.apps.iosched.util.AccountUtils;
import com.google.android.apps.iosched.util.PrefUtils;
import com.google.android.apps.iosched.util.UIUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import static com.google.android.apps.iosched.util.LogUtils.LOGV;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This is the service that provides the factory to be bound to the collection service.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ScheduleWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetRemoveViewsFactory(this.getApplicationContext());
    }

    /**
     * This is the factory that will provide data to the collection widget.
     */
    private static class WidgetRemoveViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private static final String TAG = makeLogTag(WidgetRemoveViewsFactory.class);

        private Context mContext;
        private Cursor mCursor;
        private SparseIntArray mPMap;
        private List<SimpleSectionedListAdapter.Section> mSections;
        private SparseBooleanArray mHeaderPositionMap;

        StringBuilder mBuffer = new StringBuilder();
        Formatter mFormatter = new Formatter(mBuffer, Locale.getDefault());

        public WidgetRemoveViewsFactory(Context context) {
            mContext = context;
        }

        public void onCreate() {
            // Since we reload the cursor in onDataSetChanged() which gets called immediately after
            // onCreate(), we do nothing here.
        }

        public void onDestroy() {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        public int getCount() {
            if (mCursor == null || !AccountUtils.isAuthenticated(mContext)) {
                return 0;
            }

            int size = mCursor.getCount() + mSections.size();
            if (size < 10) {
                init();
                size = mCursor.getCount() + mSections.size();
            }
            LOGV(TAG, "size returned:" + size);
            return size;
        }

        public RemoteViews getViewAt(int position) {
            RemoteViews rv;
            boolean isSectionHeader = mHeaderPositionMap.get(position);
            int offset = mPMap.get(position);

            Intent homeIntent = new Intent(mContext, HomeActivity.class);

            if (isSectionHeader) {
                rv = new RemoteViews(mContext.getPackageName(), R.layout.list_item_schedule_header);
                Section section = mSections.get(offset - 1);
                rv.setTextViewText(R.id.list_item_schedule_header_textview, section.getTitle());

            } else {
                int cursorPosition = position - offset;
                mCursor.moveToPosition(cursorPosition);

                rv = new RemoteViews(mContext.getPackageName(),
                        R.layout.list_item_schedule_block_widget);
                final String type = mCursor.getString(BlocksQuery.BLOCK_TYPE);

                final String blockId = mCursor.getString(BlocksQuery.BLOCK_ID);
                final String blockTitle = mCursor.getString(BlocksQuery.BLOCK_TITLE);
                final String blockType = mCursor.getString(BlocksQuery.BLOCK_TYPE);
                final String blockMeta = mCursor.getString(BlocksQuery.BLOCK_META);
                final long blockStart = mCursor.getLong(BlocksQuery.BLOCK_START);
                final long blockEnd = mCursor.getLong(BlocksQuery.BLOCK_END);

                final Resources res = mContext.getResources();
                rv.setTextViewText(R.id.block_endtime, null);

                if (ScheduleContract.Blocks.BLOCK_TYPE_SESSION.equals(type)
                        || ScheduleContract.Blocks.BLOCK_TYPE_CODELAB.equals(type)
                        || ScheduleContract.Blocks.BLOCK_TYPE_OFFICE_HOURS.equals(blockType)) {
                    final int numStarredSessions = mCursor.getInt(BlocksQuery.NUM_STARRED_SESSIONS);
                    final String starredSessionId = mCursor
                            .getString(BlocksQuery.STARRED_SESSION_ID);

                    if (numStarredSessions == 0) {
                        // No sessions starred
                        rv.setTextViewText(R.id.block_title, mContext.getString(
                                R.string.schedule_empty_slot_title_template,
                                TextUtils.isEmpty(blockTitle)
                                        ? ""
                                        : (" " + blockTitle.toLowerCase())));
                        rv.setTextColor(R.id.block_title,
                                res.getColor(R.color.body_text_1_positive));
                        rv.setTextViewText(R.id.block_subtitle, mContext.getString(
                                R.string.schedule_empty_slot_subtitle));
                        rv.setViewVisibility(R.id.extra_button, View.GONE);

                        Intent fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                                homeIntent,
                                new Intent(Intent.ACTION_VIEW, ScheduleContract.Blocks
                                        .buildSessionsUri(blockId)));
                        rv.setOnClickFillInIntent(R.id.list_item_middle_container, fillIntent);

                    } else if (numStarredSessions == 1) {
                        // exactly 1 session starred
                        final String starredSessionTitle =
                                mCursor.getString(BlocksQuery.STARRED_SESSION_TITLE);
                        String starredSessionSubtitle =
                                mCursor.getString(BlocksQuery.STARRED_SESSION_ROOM_NAME);
                        if (starredSessionSubtitle == null) {
                            starredSessionSubtitle = mContext.getString(R.string.unknown_room);
                        }

                        // Determine if the session is in the past
                        long currentTimeMillis = UIUtils.getCurrentTime(mContext);
                        boolean conferenceEnded = currentTimeMillis > UIUtils.CONFERENCE_END_MILLIS;
                        boolean blockEnded = currentTimeMillis > blockEnd;
                        if (blockEnded && !conferenceEnded) {
                            starredSessionSubtitle = mContext.getString(R.string.session_finished);
                        }

                        rv.setTextViewText(R.id.block_title, starredSessionTitle);
                        rv.setTextColor(R.id.block_title, res.getColor(R.color.body_text_1));
                        rv.setTextViewText(R.id.block_subtitle, starredSessionSubtitle);
                        rv.setViewVisibility(R.id.extra_button, View.VISIBLE);

                        Intent fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                                homeIntent,
                                new Intent(Intent.ACTION_VIEW, ScheduleContract.Sessions
                                        .buildSessionUri(starredSessionId)));
                        rv.setOnClickFillInIntent(R.id.list_item_middle_container, fillIntent);

                        fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                                homeIntent,
                                new Intent(Intent.ACTION_VIEW, ScheduleContract.Blocks
                                        .buildSessionsUri(blockId)));
                        rv.setOnClickFillInIntent(R.id.extra_button, fillIntent);

                    } else {
                        // 2 or more sessions starred
                        rv.setTextViewText(R.id.block_title,
                                mContext.getString(R.string.schedule_conflict_title,
                                        numStarredSessions));
                        rv.setTextColor(R.id.block_title,
                                res.getColor(R.color.body_text_1));
                        rv.setTextViewText(R.id.block_subtitle,
                                mContext.getString(R.string.schedule_conflict_subtitle));
                        rv.setViewVisibility(R.id.extra_button, View.VISIBLE);

                        Intent fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                                homeIntent,
                                new Intent(Intent.ACTION_VIEW, ScheduleContract.Blocks
                                        .buildStarredSessionsUri(blockId)));
                        rv.setOnClickFillInIntent(R.id.list_item_middle_container, fillIntent);

                        fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                                homeIntent,
                                new Intent(Intent.ACTION_VIEW, ScheduleContract.Blocks
                                        .buildSessionsUri(blockId)));
                        rv.setOnClickFillInIntent(R.id.extra_button, fillIntent);
                    }
                    rv.setTextColor(R.id.block_subtitle, res.getColor(R.color.body_text_2));

                } else if (ScheduleContract.Blocks.BLOCK_TYPE_KEYNOTE.equals(type)) {
                    long currentTimeMillis = UIUtils.getCurrentTime(mContext);
                    boolean past = (currentTimeMillis > blockEnd
                            && currentTimeMillis < UIUtils.CONFERENCE_END_MILLIS);
                    boolean present = !past && (currentTimeMillis >= blockStart);
                    boolean canViewStream = present && UIUtils.hasHoneycomb();

                    final String starredSessionId = mCursor
                            .getString(BlocksQuery.STARRED_SESSION_ID);
                    final String starredSessionTitle =
                            mCursor.getString(BlocksQuery.STARRED_SESSION_TITLE);
                    rv.setTextViewText(R.id.block_title, starredSessionTitle);
                    rv.setTextViewText(R.id.block_subtitle, res.getString(R.string.keynote_room));
                    rv.setTextColor(R.id.block_title, canViewStream
                            ? res.getColor(R.color.body_text_1)
                            : res.getColor(R.color.body_text_disabled));
                    rv.setTextColor(R.id.block_subtitle, canViewStream
                            ? res.getColor(R.color.body_text_2)
                            : res.getColor(R.color.body_text_disabled));
                    rv.setViewVisibility(R.id.extra_button, View.GONE);

                    if (canViewStream) {
                        Intent fillIntent = TaskStackBuilderProxyActivity.getFillIntent(
                                homeIntent,
                                new Intent(Intent.ACTION_VIEW, ScheduleContract.Sessions
                                        .buildSessionUri(starredSessionId))
                                        .setClass(mContext, SessionLivestreamActivity.class));
                        rv.setOnClickFillInIntent(R.id.list_item_middle_container, fillIntent);
                    } else {
                        rv.setOnClickFillInIntent(R.id.list_item_middle_container, new Intent());
                    }

                } else {
                    rv.setTextViewText(R.id.block_title, blockTitle);
                    rv.setTextColor(R.id.block_title, res.getColor(R.color.body_text_disabled));
                    rv.setTextViewText(R.id.block_subtitle, blockMeta);
                    rv.setTextColor(R.id.block_subtitle, res.getColor(R.color.body_text_disabled));
                    rv.setViewVisibility(R.id.extra_button, View.GONE);

                    mBuffer.setLength(0);
                    rv.setTextViewText(R.id.block_endtime,
                            DateUtils.formatDateRange(mContext, mFormatter,
                                    blockEnd, blockEnd,
                                    DateUtils.FORMAT_SHOW_TIME,
                                    PrefUtils.getDisplayTimeZone(mContext).getID()).toString());

                    rv.setOnClickFillInIntent(R.id.list_item_middle_container, new Intent());
                }

                mBuffer.setLength(0);
                rv.setTextViewText(R.id.block_time,
                        DateUtils.formatDateRange(mContext, mFormatter,
                                blockStart, blockStart,
                                DateUtils.FORMAT_SHOW_TIME,
                                PrefUtils.getDisplayTimeZone(mContext).getID()).toString());
            }

            return rv;
        }

        public RemoteViews getLoadingView() {
            return null;
        }

        public int getViewTypeCount() {
            return 2;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return true;
        }

        public void onDataSetChanged() {
            init();
        }

        private void init() {
            if (mCursor != null) {
                mCursor.close();
            }

            String liveStreamedOnlyBlocksSelection = "("
                    + (UIUtils.shouldShowLiveSessionsOnly(mContext)
                    ? ScheduleContract.Blocks.BLOCK_TYPE + " NOT IN ('"
                    + ScheduleContract.Blocks.BLOCK_TYPE_SESSION + "','"
                    + ScheduleContract.Blocks.BLOCK_TYPE_CODELAB + "','"
                    + ScheduleContract.Blocks.BLOCK_TYPE_OFFICE_HOURS + "','"
                    + ScheduleContract.Blocks.BLOCK_TYPE_FOOD + "')"
                    + " OR " + ScheduleContract.Blocks.NUM_LIVESTREAMED_SESSIONS + ">1 "
                    : "1==1") + ")";
            String onlyStarredOfficeHoursSelection = "("
                    + ScheduleContract.Blocks.BLOCK_TYPE + " != '"
                    + ScheduleContract.Blocks.BLOCK_TYPE_OFFICE_HOURS
                    + "' OR " + ScheduleContract.Blocks.NUM_STARRED_SESSIONS + ">0)";
            String excludeSandbox = "("+ScheduleContract.Blocks.BLOCK_TYPE + " != '"
                    + ScheduleContract.Blocks.BLOCK_TYPE_SANDBOX +"')";
            mCursor = mContext.getContentResolver().query(ScheduleContract.Blocks.CONTENT_URI,
                    BlocksQuery.PROJECTION,
                    ScheduleContract.Blocks.BLOCK_END + " >= ? AND "
                            + liveStreamedOnlyBlocksSelection + " AND "
                            + onlyStarredOfficeHoursSelection + " AND "
                            + excludeSandbox,
                    new String[]{
                            Long.toString(UIUtils.getCurrentTime(mContext))
                    },
                    ScheduleContract.Blocks.DEFAULT_SORT);

            String displayTimeZone = PrefUtils.getDisplayTimeZone(mContext).getID();

            mSections = new ArrayList<SimpleSectionedListAdapter.Section>();
            mCursor.moveToFirst();
            long previousTime = -1;
            long time;
            mPMap = new SparseIntArray();
            mHeaderPositionMap = new SparseBooleanArray();
            int offset = 0;
            int globalPosition = 0;
            while (!mCursor.isAfterLast()) {
                time = mCursor.getLong(BlocksQuery.BLOCK_START);
                if (!UIUtils.isSameDayDisplay(previousTime, time, mContext)) {
                    mBuffer.setLength(0);
                    mSections.add(new SimpleSectionedListAdapter.Section(mCursor.getPosition(),
                            DateUtils.formatDateRange(
                                    mContext, mFormatter,
                                    time, time,
                                    DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE
                                            | DateUtils.FORMAT_SHOW_WEEKDAY,
                                    displayTimeZone).toString()));
                    ++offset;
                    mHeaderPositionMap.put(globalPosition, true);
                    mPMap.put(globalPosition, offset);
                    ++globalPosition;
                }
                mHeaderPositionMap.put(globalPosition, false);
                mPMap.put(globalPosition, offset);
                ++globalPosition;
                previousTime = time;
                mCursor.moveToNext();
            }

            LOGV(TAG, "Leaving init()");
        }
    }

    public interface BlocksQuery {

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Blocks.BLOCK_ID,
                ScheduleContract.Blocks.BLOCK_TITLE,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
                ScheduleContract.Blocks.BLOCK_TYPE,
                ScheduleContract.Blocks.BLOCK_META,
                ScheduleContract.Blocks.NUM_STARRED_SESSIONS,
                ScheduleContract.Blocks.NUM_LIVESTREAMED_SESSIONS,
                ScheduleContract.Blocks.STARRED_SESSION_ID,
                ScheduleContract.Blocks.STARRED_SESSION_TITLE,
                ScheduleContract.Blocks.STARRED_SESSION_ROOM_NAME,
        };

        int _ID = 0;
        int BLOCK_ID = 1;
        int BLOCK_TITLE = 2;
        int BLOCK_START = 3;
        int BLOCK_END = 4;
        int BLOCK_TYPE = 5;
        int BLOCK_META = 6;
        int NUM_STARRED_SESSIONS = 7;
        int NUM_LIVESTREAMED_SESSIONS = 8;
        int STARRED_SESSION_ID = 9;
        int STARRED_SESSION_TITLE = 10;
        int STARRED_SESSION_ROOM_NAME = 11;
    }
}
