/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.iosched.service;

import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.android.apps.dashclock.api.ExtensionData;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.HomeActivity;
import com.google.android.apps.iosched.util.PrefUtils;
import com.google.android.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.LOGE;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * An I/O 2013 extension for DashClock.
 */
public class DashClockExtension extends com.google.android.apps.dashclock.api.DashClockExtension {
    private static final String TAG = makeLogTag(DashClockExtension.class);

    private static final long MINUTE_MILLIS = 60 * 1000;
    private static final long NOW_BUFFER_TIME_MILLIS = 15 * MINUTE_MILLIS;
    private static final int MAX_BLOCKS = 5;
    private static final long CONTENT_CHANGE_DELAY_MILLIS = 5 * 1000;

    private static long mLastChange = 0;

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        setUpdateWhenScreenOn(true);
        addWatchContentUris(new String[]{
                ScheduleContract.Sessions.CONTENT_URI.toString()
        });
    }

    @Override
    protected void onUpdateData(int reason) {
        if (reason == DashClockExtension.UPDATE_REASON_CONTENT_CHANGED) {
            long time = System.currentTimeMillis();
            if (time < mLastChange + CONTENT_CHANGE_DELAY_MILLIS) {
                return;
            }

            mLastChange = time;
        }

        long currentTime = UIUtils.getCurrentTime(this);
        if (currentTime >= UIUtils.CONFERENCE_END_MILLIS) {
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.dashclock_extension)
                    .status(getString(R.string.whats_on_thank_you_short))
                    .expandedTitle(getString(R.string.whats_on_thank_you_title))
                    .expandedBody(getString(R.string.whats_on_thank_you_subtitle))
                    .clickIntent(new Intent(this, HomeActivity.class)));
            return;
        }

        Cursor cursor = tryOpenBlocksCursor();
        if (cursor == null) {
            LOGE(TAG, "Null blocks cursor, short-circuiting.");
            return;
        }

        StringBuilder buffer = new StringBuilder();
        Formatter formatter = new Formatter(buffer, Locale.getDefault());

        String firstBlockStartTime = null;
        List<String> blocks = new ArrayList<String>();

        long lastBlockStart = 0;
        while (cursor.moveToNext()) {
            if (blocks.size() >= MAX_BLOCKS) {
                break;
            }

            final String type = cursor.getString(BlocksQuery.BLOCK_TYPE);
            final String blockTitle = cursor.getString(BlocksQuery.BLOCK_TITLE);
            final String blockType = cursor.getString(BlocksQuery.BLOCK_TYPE);
            final long blockStart = cursor.getLong(BlocksQuery.BLOCK_START);

            buffer.setLength(0);

            boolean showWeekday = !DateUtils.isToday(blockStart)
                    && !UIUtils.isSameDayDisplay(lastBlockStart, blockStart, this);

            String blockStartTime = DateUtils.formatDateRange(this, formatter,
                    blockStart, blockStart,
                    DateUtils.FORMAT_SHOW_TIME | (showWeekday
                            ? DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY
                            : 0),
                    PrefUtils.getDisplayTimeZone(this).getID()).toString();

            lastBlockStart = blockStart;

            if (firstBlockStartTime == null) {
                firstBlockStartTime = blockStartTime;
            }

            if (ScheduleContract.Blocks.BLOCK_TYPE_SESSION.equals(type)
                    || ScheduleContract.Blocks.BLOCK_TYPE_CODELAB.equals(type)
                    || ScheduleContract.Blocks.BLOCK_TYPE_OFFICE_HOURS.equals(blockType)) {
                final int numStarredSessions = cursor.getInt(BlocksQuery.NUM_STARRED_SESSIONS);

                if (numStarredSessions == 1) {
                    // exactly 1 session starred
                    String title = cursor.getString(BlocksQuery.STARRED_SESSION_TITLE);
                    String room = cursor.getString(BlocksQuery.STARRED_SESSION_ROOM_NAME);
                    if (room == null) {
                        room = getString(R.string.unknown_room);
                    }

                    blocks.add(blockStartTime + ", " + room + " — " + title);

                } else {
                    // 2 or more sessions starred
                    String title = getString(R.string.schedule_conflict_title,
                            numStarredSessions);

                    blocks.add(blockStartTime + " — " + title);
                }

            } else if (ScheduleContract.Blocks.BLOCK_TYPE_KEYNOTE.equals(type)) {
                final String title = cursor.getString(BlocksQuery.STARRED_SESSION_TITLE);
                blocks.add(blockStartTime + " — " + title);

            } else {
                blocks.add(blockStartTime + " — " + blockTitle);
            }
        }

        cursor.close();

        LOGD(TAG, blocks.size() + " blocks");
        if (blocks.size() > 0) {
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.dashclock_extension)
                    .status(firstBlockStartTime)
                    .expandedTitle(blocks.get(0))
                    .expandedBody(TextUtils.join("\n", blocks.subList(1, blocks.size())))
                    .clickIntent(new Intent(this, HomeActivity.class)));
        } else {
            publishUpdate(new ExtensionData().visible(false));
        }
    }

    private Cursor tryOpenBlocksCursor() {
        try {
            String liveStreamedOnlyBlocksSelection = "("
                    + (UIUtils.shouldShowLiveSessionsOnly(this)
                    ? ScheduleContract.Blocks.BLOCK_TYPE + " NOT IN ('"
                    + ScheduleContract.Blocks.BLOCK_TYPE_SESSION + "','"
                    + ScheduleContract.Blocks.BLOCK_TYPE_CODELAB + "','"
                    + ScheduleContract.Blocks.BLOCK_TYPE_OFFICE_HOURS + "','"
                    + ScheduleContract.Blocks.BLOCK_TYPE_FOOD + "')"
                    + " OR " + ScheduleContract.Blocks.NUM_LIVESTREAMED_SESSIONS + ">1 "
                    : "1==1") + ")";
            String onlyStarredSelection = "("
                    + ScheduleContract.Blocks.BLOCK_TYPE + " NOT IN ('"
                    + ScheduleContract.Blocks.BLOCK_TYPE_SESSION + "','"
                    + ScheduleContract.Blocks.BLOCK_TYPE_CODELAB + "','"
                    + ScheduleContract.Blocks.BLOCK_TYPE_OFFICE_HOURS + "') "
                    + " OR " + ScheduleContract.Blocks.NUM_STARRED_SESSIONS + ">0)";
            String excludeSandbox = ScheduleContract.Blocks.BLOCK_TYPE + " != '"
                    + ScheduleContract.Blocks.BLOCK_TYPE_SANDBOX + "'";
            return getContentResolver().query(ScheduleContract.Blocks.CONTENT_URI,
                    BlocksQuery.PROJECTION,
                    ScheduleContract.Blocks.BLOCK_START + " >= ? AND "
                            + liveStreamedOnlyBlocksSelection + " AND "
                            + onlyStarredSelection + " AND "
                            + excludeSandbox,
                    new String[]{
                            Long.toString(UIUtils.getCurrentTime(this) - NOW_BUFFER_TIME_MILLIS)
                    },
                    ScheduleContract.Blocks.DEFAULT_SORT);

        } catch (Exception e) {
            LOGE(TAG, "Error querying I/O 2013 content provider", e);
            return null;
        }
    }

    public interface BlocksQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Blocks.BLOCK_TITLE,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_TYPE,
                ScheduleContract.Blocks.NUM_STARRED_SESSIONS,
                ScheduleContract.Blocks.STARRED_SESSION_TITLE,
                ScheduleContract.Blocks.STARRED_SESSION_ROOM_NAME,
                ScheduleContract.Blocks.NUM_LIVESTREAMED_SESSIONS,
        };

        int _ID = 0;
        int BLOCK_TITLE = 1;
        int BLOCK_START = 2;
        int BLOCK_TYPE = 3;
        int NUM_STARRED_SESSIONS = 4;
        int STARRED_SESSION_TITLE = 5;
        int STARRED_SESSION_ROOM_NAME = 6;
    }
}
