/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.map.util;

import com.google.samples.apps.iosched.provider.ScheduleContract;

import android.content.Context;

/**
 * Loads session information for all sessions scheduled in a particular room after a timestamp.
 */
public class OverviewSessionLoader extends SessionLoader {


    public OverviewSessionLoader(Context context, String roomId, String roomTitle,
            int roomType, long time) {
        super(context, roomId, roomTitle, roomType,
                ScheduleContract.Sessions.buildSessionsInRoomAfterUri(roomId, time),
                Query.PROJECTION,
                null, null, Query.ORDER);

    }


    /**
     * Query Paramters for the "Sessions in room after" query that returns a list of sessions
     * that are following a given time in a particular room.
     */
    public static interface Query {

        final String ORDER = ScheduleContract.Sessions.SESSION_START + " ASC";

        final String[] PROJECTION = {
                ScheduleContract.Sessions._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Sessions.SESSION_TAGS
        };

        int SESSION_ID = 1;
        int SESSION_TITLE = 2;
        int SESSION_START = 3;
        int SESSION_END = 4;
        int SESSION_TAGS = 5;
    }
}
