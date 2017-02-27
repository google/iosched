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
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel;
import com.google.samples.apps.iosched.myschedule.TagFilterHolder;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class ScheduleHelper {

    private static final String TAG = makeLogTag(ScheduleHelper.class);

    private Context mContext;

    public ScheduleHelper(Context context) {
        this.mContext = context;
    }

    public ArrayList<ScheduleItem> getScheduleData(long start, long end, TagFilterHolder filters) {
        // get sessions in my schedule and blocks, starting anytime in the conference day
        final ArrayList<ScheduleItem> items = new ArrayList<>();
        if (filters == null || !filters.showSessionsOnly()) {
            addBlocks(start, end, items);
        }
        addSessions(start, end, items, filters);

        ArrayList<ScheduleItem> result = ScheduleItemHelper.processItems(items);
        if (BuildConfig.DEBUG || Log.isLoggable(TAG, Log.DEBUG)) {
            ScheduleItem previous = null;
            for (ScheduleItem item: result) {
                if ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) != 0) {
                    Log.d(TAG, "Schedule Item conflicts with previous. item="
                            + item + " previous=" + previous);
                }
                previous = item;
            }
        }
        return result;
    }

    public void getScheduleDataAsync(final MyScheduleModel.LoadScheduleDataListener callback,
            long start, long end, final TagFilterHolder filters) {
        AsyncTask<Long, Void, ArrayList<ScheduleItem>> task
                = new AsyncTask<Long, Void, ArrayList<ScheduleItem>>() {
            @Override
            protected ArrayList<ScheduleItem> doInBackground(Long... params) {
                Long start = params[0];
                Long end = params[1];
                return getScheduleData(start, end, filters);
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

    protected void addSessions(final long start, final long end,
            @NonNull final ArrayList<ScheduleItem> items, final TagFilterHolder filters) {
        Cursor cursor = null;
        try {
            Uri uri = Sessions.CONTENT_URI;
            String selection = Sessions.STARTING_AT_TIME_INTERVAL_SELECTION;
            if (filters != null) {
                uri = Sessions.buildCategoryTagFilterUri(uri, filters.toStringArray(),
                        filters.getCategoryCount());
                if (filters.showLiveStreamedOnly()) {
                    selection = DatabaseUtils.concatenateWhere(selection,
                            Sessions.LIVESTREAM_SELECTION);
                }
            }
            cursor = mContext.getContentResolver().query(
                    uri,
                    SessionsQuery.PROJECTION,
                    selection,
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
                    item.sessionType = detectSessionType(cursor.getString(SessionsQuery.SESSION_TAGS));
                    item.mainTag = cursor.getString(SessionsQuery.SESSION_MAIN_TAG);
                    item.inSchedule = cursor.getInt(SessionsQuery.SESSION_IN_SCHEDULE) != 0;
                    items.add(item);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    protected void addBlocks(final long start, final long end,
            @NonNull final ArrayList<ScheduleItem> items) {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    Blocks.CONTENT_URI,
                    BlocksQuery.PROJECTION,
                    // constrain to the specified day
                    Blocks.BLOCK_START + " >= ? and " + Blocks.BLOCK_START + " <= ?",
                    new String[]{String.valueOf(start), String.valueOf(end)},
                    // order by start time
                    Blocks.BLOCK_START);

            if (cursor.moveToFirst()) {
                do {
                    ScheduleItem item = new ScheduleItem();
                    item.setTypeFromBlockType(cursor.getString(BlocksQuery.BLOCK_TYPE));
                    item.title = cursor.getString(BlocksQuery.BLOCK_TITLE);
                    item.room = item.subtitle = cursor.getString(BlocksQuery.BLOCK_SUBTITLE);
                    item.startTime = cursor.getLong(BlocksQuery.BLOCK_START);
                    item.endTime = cursor.getLong(BlocksQuery.BLOCK_END);
                    if (item.type != ScheduleItem.FREE) {
                        item.flags |= ScheduleItem.FLAG_NOT_REMOVABLE;
                        items.add(item);
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
                Sessions.SESSION_TAGS,
                Sessions.SESSION_MAIN_TAG,
                //Sessions.HAS_GIVEN_FEEDBACK
        };

        int SESSION_ID = 0;
        int SESSION_TITLE = 1;
        int SESSION_START = 2;
        int SESSION_END = 3;
        int ROOM_ROOM_NAME = 4;
        int SESSION_IN_SCHEDULE = 5;
        int SESSION_LIVESTREAM_URL = 6;
        int SESSION_SPEAKER_NAMES = 7;
        int SESSION_PHOTO_URL = 8;
        int SESSION_COLOR = 9;
        int SESSION_TAGS = 10;
        int SESSION_MAIN_TAG = 11;
        //int HAS_GIVEN_FEEDBACK = 12;
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

}
