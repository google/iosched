/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.explore;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.google.samples.apps.iosched.explore.data.SessionData;
import com.google.samples.apps.iosched.provider.ScheduleContract;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a convenience class for handling lists of sessions, this still needs updating to the full
 * MVP architecture
 */
public class ExploreSessionsModel {

    private final List<SessionData> mSessionData;

    private Context mContext;

    public ExploreSessionsModel(Cursor cursor, Context context) {
        mContext = context;
        if (cursor != null && cursor.moveToFirst()) {
            mSessionData = new ArrayList<>(cursor.getCount());
            do {
                mSessionData.add(createSessionData(cursor));
            } while (cursor.moveToNext());
        } else {
            mSessionData = null;
        }
    }

    public List<SessionData> getSessionData() {
        return mSessionData;
    }

    private SessionData createSessionData(Cursor cursor) {
        return new SessionData(mContext,
                cursor.getString(ExploreSessionsQuery.TITLE),
                cursor.getString(ExploreSessionsQuery.ABSTRACT),
                cursor.getString(ExploreSessionsQuery.SESSION_ID),
                cursor.getString(ExploreSessionsQuery.PHOTO_URL),
                cursor.getString(ExploreSessionsQuery.MAIN_TAG),
                cursor.getLong(ExploreSessionsQuery.SESSION_START),
                cursor.getLong(ExploreSessionsQuery.SESSION_END),
                cursor.getString(ExploreSessionsQuery.LIVESTREAM_ID),
                cursor.getString(ExploreSessionsQuery.YOUTUBE_URL),
                cursor.getString(ExploreSessionsQuery.TAGS),
                cursor.getLong(ExploreSessionsQuery.IN_MY_SCHEDULE) == 1L);
    }

    public interface ExploreSessionsQuery {
        int NORMAL_TOKEN = 0x1;
        int SEARCH_TOKEN = 0x3;
        String[] NORMAL_PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_MAIN_TAG,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
                ScheduleContract.Sessions.SESSION_YOUTUBE_URL,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_ID
        };
        String[] SEARCH_PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_MAIN_TAG,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
                ScheduleContract.Sessions.SESSION_YOUTUBE_URL,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_ID,
                ScheduleContract.Sessions.SEARCH_SNIPPET
        };

        int _ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int ABSTRACT = 3;
        int SESSION_START = 4;
        int SESSION_END = 5;
        int ROOM_NAME = 6;
        int URL = 7;
        int MAIN_TAG = 8;
        int TAGS = 9;
        int PHOTO_URL = 10;
        int IN_MY_SCHEDULE = 11;
        int YOUTUBE_URL = 12;
        int LIVESTREAM_ID = 13;
        int SEARCH_SNIPPET = 14;
    }
}
