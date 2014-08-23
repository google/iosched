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
import android.text.TextUtils;

import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class AllScheduleHelper extends BaseScheduleHelper {

    private static final String TAG = makeLogTag(AllScheduleHelper.class);

    public AllScheduleHelper(Context context) {
        super(context);
    }

    protected void addSessions(long start, long end,
            ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {

        Cursor cursor = mContext.getContentResolver().query(
                ScheduleContract.addOverrideAccountName(Sessions.CONTENT_URI, AccountUtils.getActiveAccountName(mContext)),
                        SessionsQuery.PROJECTION,
                // filter sessions to the specified day
                Sessions.STARTING_AT_TIME_INTERVAL_SELECTION,
                new String[]{String.valueOf(start), String.valueOf(end)},
                // order by session start
                Sessions.SESSION_START);

        while (cursor.moveToNext()) {
            ScheduleItem item = new ScheduleItem();
            item.type = ScheduleItem.SESSION;
            item.sessionId = cursor.getString(SessionsQuery.SESSION_ID);
            item.title = cursor.getString(SessionsQuery.SESSION_TITLE);
            item.startTime = cursor.getLong(SessionsQuery.SESSION_START);
            item.endTime = cursor.getLong(SessionsQuery.SESSION_END);
            item.mySchedule = cursor.getShort(SessionsQuery.SESSION_IN_MY_SCHEDULE) != 0;
            if (!TextUtils.isEmpty(cursor.getString(SessionsQuery.SESSION_LIVESTREAM_URL))) {
                item.flags |= ScheduleItem.FLAG_HAS_LIVESTREAM;
            }
            item.subtitle = UIUtils.formatSessionSubtitle(
                    cursor.getString(SessionsQuery.ROOM_ROOM_NAME),
                    cursor.getString(SessionsQuery.SESSION_SPEAKER_NAMES), mContext);
            item.backgroundImageUrl = cursor.getString(SessionsQuery.SESSION_PHOTO_URL);
            item.backgroundColor = cursor.getInt(SessionsQuery.SESSION_COLOR);
//            item.hasGivenFeedback = (cursor.getInt(SessionsQuery.HAS_GIVEN_FEEDBACK) > 0);
            immutableItems.add(item);
        }
        cursor.close();
    }

    @Override
    boolean shouldCheckConflicts() {
        return false;
    }

    private interface SessionsQuery {
        String[] PROJECTION = {
                Sessions.SESSION_ID,
                Sessions.SESSION_TITLE,
                Sessions.SESSION_START,
                Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME,
                Sessions.SESSION_IN_MY_SCHEDULE,
                Sessions.SESSION_LIVESTREAM_URL,
                Sessions.SESSION_SPEAKER_NAMES,
                Sessions.SESSION_PHOTO_URL,
                Sessions.SESSION_COLOR
        };

        int SESSION_ID = 0;
        int SESSION_TITLE = 1;
        int SESSION_START = 2;
        int SESSION_END = 3;
        int ROOM_ROOM_NAME = 4;
        int SESSION_IN_MY_SCHEDULE = 5;
        int SESSION_LIVESTREAM_URL = 6;
        int SESSION_SPEAKER_NAMES = 7;
        int SESSION_PHOTO_URL = 8;
        int SESSION_COLOR = 9;
        int HAS_GIVEN_FEEDBACK = 10;
    }

}
