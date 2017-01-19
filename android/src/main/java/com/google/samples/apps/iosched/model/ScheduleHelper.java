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
package com.google.samples.apps.iosched.model;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.provider.ScheduleContractHelper;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class ScheduleHelper {

    private static final String TAG = makeLogTag(ScheduleHelper.class);

    private Context mContext;

    public ScheduleHelper(Context context) {
        this.mContext = context;
    }

    public ArrayList<ScheduleItem> getScheduleData(long start, long end) {
        // get sessions in my schedule and blocks, starting anytime in the conference day
        ArrayList<ScheduleItem> mutableItems = new ArrayList<ScheduleItem>();
        ArrayList<ScheduleItem> immutableItems = new ArrayList<ScheduleItem>();
        addBlocks(start, end, mutableItems, immutableItems);
        addSessions(start, end, mutableItems, immutableItems);

        ArrayList<ScheduleItem> result = ScheduleItemHelper.processItems(mutableItems, immutableItems);
        if (BuildConfig.DEBUG || Log.isLoggable(TAG, Log.DEBUG)) {
            ScheduleItem previous = null;
            for (ScheduleItem item: result) {
                if ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) != 0) {
                    Log.d(TAG, "Schedule Item conflicts with previous. item="+item+" previous="+previous);
                }
                previous = item;
            }
        }

        setSessionCounters(result, start, end);
        return result;
    }

    /**
     * Fill the number of sessions for FREE blocks:
     */
    protected void setSessionCounters(ArrayList<ScheduleItem> items, long dayStart, long dayEnd) {
        ArrayList<ScheduleItem> free = new ArrayList<ScheduleItem>();

        for (ScheduleItem item: items) {
            if (item.type == ScheduleItem.FREE) {
                free.add(item);
            }
        }

        if (free.isEmpty()) {
            return;
        }

        // Count number of start/end pairs for sessions that are between dayStart and dayEnd and
        // are not in my schedule:
        String liveStreamedOnlySelection = UIUtils.shouldShowLiveSessionsOnly(mContext)
                ? "AND IFNULL(" + ScheduleContract.Sessions.SESSION_LIVESTREAM_ID + ",'')!=''"
                : "";

        Cursor cursor = mContext.getContentResolver().query(
                ScheduleContract.Sessions.buildCounterByIntervalUri(),
                SessionsCounterQuery.PROJECTION,
                Sessions.SESSION_START + ">=? AND "+Sessions.SESSION_START + "<=? AND "+
                Sessions.SESSION_IN_MY_SCHEDULE + " = 0 "+liveStreamedOnlySelection,
                new String[]{String.valueOf(dayStart), String.valueOf(dayEnd)},
                null);

        while (cursor.moveToNext()) {
            long start = cursor.getLong(SessionsCounterQuery.SESSION_INTERVAL_START);
            int counter = cursor.getInt(SessionsCounterQuery.SESSION_INTERVAL_COUNT);

            // Find blocks that this interval applies.
            for (ScheduleItem item: free) {
                // If grouped sessions starts and ends inside the free block, it is considered in it:
                if (item.startTime <= start && start < item.endTime) {
                    item.numOfSessions += counter;
                }
            }
        }
        cursor.close();

        // remove free blocks that have no available sessions or that are in the past
        long now = TimeUtils.getCurrentTime(mContext);
        Iterator<ScheduleItem> it = items.iterator();
        while (it.hasNext()) {
            ScheduleItem i = it.next();
            if (i.type == ScheduleItem.FREE) {
                if (i.endTime < now) {
                    LOGD(TAG, "Removing empty block in the past.");
                    it.remove();
                } else if (i.numOfSessions == 0) {
                    LOGD(TAG, "Removing block with zero sessions: " + new Date(i.startTime) + "-" + new Date(i.endTime));
                    it.remove();
                } else {
                    i.subtitle = mContext.getResources().getQuantityString(
                            R.plurals.schedule_block_subtitle, i.numOfSessions, i.numOfSessions);
                }

            }
        }
    }

    public void getScheduleDataAsync(final MyScheduleModel.LoadScheduleDataListener callback,
            long start, long end) {
        AsyncTask<Long, Void, ArrayList<ScheduleItem>> task
                = new AsyncTask<Long, Void, ArrayList<ScheduleItem>>() {
            @Override
            protected ArrayList<ScheduleItem> doInBackground(Long... params) {
                Long start = params[0];
                Long end = params[1];
                return getScheduleData(start, end);
            }

            @Override
            protected void onPostExecute(ArrayList<ScheduleItem> scheduleItems) {
                callback.onDataLoaded(scheduleItems);
            }
        };
        // On honeycomb and above, AsyncTasks are by default executed one by one. We are using a
        // thread pool instead here, because we want this to be executed independently from other
        // AsyncTasks. See the URL below for detail.
        // http://developer.android.com/reference/android/os/AsyncTask.html#execute(Params...)
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, start, end);
    }

    protected void addSessions(long start, long end,
            ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    ScheduleContractHelper.addOverrideAccountName(Sessions.CONTENT_MY_SCHEDULE_URI,
                            AccountUtils.getActiveAccountName(mContext)),
                    SessionsQuery.PROJECTION,
                    // filter sessions to the specified day
                    Sessions.STARTING_AT_TIME_INTERVAL_SELECTION,
                    new String[]{String.valueOf(start), String.valueOf(end)},
                    // order by session start
                    Sessions.SESSION_START);

            if (cursor.moveToFirst()) {
                do {
                    ScheduleItem item = new ScheduleItem();
                    item.type = ScheduleItem.SESSION;
                    item.sessionId = cursor.getString(SessionsQuery.SESSION_ID);
                    item.title = cursor.getString(SessionsQuery.SESSION_TITLE);
                    item.startTime = cursor.getLong(SessionsQuery.SESSION_START);
                    item.endTime = cursor.getLong(SessionsQuery.SESSION_END);
                    if (!TextUtils.isEmpty(cursor.getString(SessionsQuery.SESSION_LIVESTREAM_URL))) {
                        item.flags |= ScheduleItem.FLAG_HAS_LIVESTREAM;
                    }
                    item.subtitle = UIUtils.formatSessionSubtitle(
                            cursor.getString(SessionsQuery.ROOM_ROOM_NAME),
                            cursor.getString(SessionsQuery.SESSION_SPEAKER_NAMES), mContext);
                    item.room = cursor.getString(SessionsQuery.ROOM_ROOM_NAME);
                    item.backgroundImageUrl = cursor.getString(SessionsQuery.SESSION_PHOTO_URL);
                    item.backgroundColor = cursor.getInt(SessionsQuery.SESSION_COLOR);
                    item.hasGivenFeedback = (cursor.getInt(SessionsQuery.HAS_GIVEN_FEEDBACK) > 0);
                    item.sessionType = detectSessionType(cursor.getString(SessionsQuery.SESSION_TAGS));
                    item.mainTag = cursor.getString(SessionsQuery.SESSION_MAIN_TAG);
                    immutableItems.add(item);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    protected void addBlocks(long start, long end,
            ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    Blocks.CONTENT_URI,
                    BlocksQuery.PROJECTION,

                    // filter sessions on the specified day
                    Blocks.BLOCK_START + " >= ? and " + Blocks.BLOCK_START + " <= ?",
                    new String[]{String.valueOf(start), String.valueOf(end)},

                    // order by session start
                    Blocks.BLOCK_START);

            if (cursor.moveToFirst()) {
                final boolean attendeeAtVenue = SettingsUtils.isAttendeeAtVenue(mContext);
                do {
                    ScheduleItem item = new ScheduleItem();
                    item.setTypeFromBlockType(cursor.getString(BlocksQuery.BLOCK_TYPE));
                    item.title = cursor.getString(BlocksQuery.BLOCK_TITLE);
                    item.room = item.subtitle = cursor.getString(BlocksQuery.BLOCK_SUBTITLE);
                    item.startTime = cursor.getLong(BlocksQuery.BLOCK_START);
                    item.endTime = cursor.getLong(BlocksQuery.BLOCK_END);

                    // Hide BREAK blocks to remote attendees (b/14666391):
                    if (item.type == ScheduleItem.BREAK && !attendeeAtVenue) {
                        continue;
                    }
                    // Currently, only type=FREE is mutable
                    if (item.type == ScheduleItem.FREE) {
                        mutableItems.add(item);
                    } else {
                        immutableItems.add(item);
                        item.flags |= ScheduleItem.FLAG_NOT_REMOVABLE;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static int detectSessionType(String tagsText) {
        if (TextUtils.isEmpty(tagsText)) {
            return ScheduleItem.SESSION_TYPE_MISC;
        }
        String tags = tagsText.toUpperCase(Locale.US);
        if (tags.contains("TYPE_SESSIONS") || tags.contains("KEYNOTE")) {
            return ScheduleItem.SESSION_TYPE_SESSION;
        } else if (tags.contains("TYPE_CODELAB")) {
            return ScheduleItem.SESSION_TYPE_CODELAB;
        } else if (tags.contains("TYPE_SANDBOXTALKS")) {
            return ScheduleItem.SESSION_TYPE_BOXTALK;
        } else if (tags.contains("TYPE_APPREVIEWS") || tags.contains("TYPE_OFFICEHOURS") ||
                tags.contains("TYPE_WORKSHOPS")) {
            return ScheduleItem.SESSION_TYPE_MISC;
        }
        return ScheduleItem.SESSION_TYPE_MISC; // default
    }

    private interface SessionsQuery {
        String[] PROJECTION = {
                Sessions.SESSION_ID,
                Sessions.SESSION_TITLE,
                Sessions.SESSION_START,
                Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME,
                Sessions.SESSION_IN_MY_SCHEDULE,
                Sessions.SESSION_LIVESTREAM_ID,
                Sessions.SESSION_SPEAKER_NAMES,
                Sessions.SESSION_PHOTO_URL,
                Sessions.SESSION_COLOR,
                Sessions.HAS_GIVEN_FEEDBACK,
                Sessions.SESSION_TAGS,
                Sessions.SESSION_MAIN_TAG,
        };

        int SESSION_ID = 0;
        int SESSION_TITLE = 1;
        int SESSION_START = 2;
        int SESSION_END = 3;
        int ROOM_ROOM_NAME = 4;
        int SESSION_LIVESTREAM_URL = 6;
        int SESSION_SPEAKER_NAMES = 7;
        int SESSION_PHOTO_URL = 8;
        int SESSION_COLOR = 9;
        int HAS_GIVEN_FEEDBACK = 10;
        int SESSION_TAGS = 11;
        int SESSION_MAIN_TAG = 12;
    }

    private interface BlocksQuery {
        String[] PROJECTION = {
                Blocks.BLOCK_TITLE,
                Blocks.BLOCK_TYPE,
                Blocks.BLOCK_START,
                Blocks.BLOCK_END,
                Blocks.BLOCK_SUBTITLE
        };

        int BLOCK_TITLE = 0;
        int BLOCK_TYPE= 1;
        int BLOCK_START = 2;
        int BLOCK_END = 3;
        int BLOCK_SUBTITLE = 4;
    }


    private interface SessionsCounterQuery {
        String[] PROJECTION = {
                Sessions.SESSION_START,
                Sessions.SESSION_END,
                Sessions.SESSION_INTERVAL_COUNT,
                Sessions.SESSION_IN_MY_SCHEDULE,
        };

        int SESSION_INTERVAL_START = 0;
        int SESSION_INTERVAL_END = 1;
        int SESSION_INTERVAL_COUNT = 2;
    }

}
