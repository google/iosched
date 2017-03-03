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
 * Loads the title and abstract for the very first session scheduled in a room.
 */
public class SingleSessionLoader extends SessionLoader {


    public SingleSessionLoader(Context context, String roomId, String roomTitle, int roomType) {
        super(context, roomId, roomTitle, roomType,
                ScheduleContract.Rooms.buildSessionsDirUri(roomId),
                Query.PROJECTION, null, null, Query.ORDER_LIMIT);

    }


    /**
     * Query Paramters for the "Sessions in room after" query that returns a list of sessions
     * that are following a given time in a particular room. Results are limited to the first
     * session only.
     */
    public static interface Query {

        final String ORDER_LIMIT = ScheduleContract.Sessions.SESSION_START + " ASC LIMIT 1";


        final String[] PROJECTION = {
                ScheduleContract.Sessions._ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT
        };

        int SESSION_TITLE = 1;
        int SESSION_ABSTRACT = 2;
    }
}
