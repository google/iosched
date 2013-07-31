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

package com.google.android.apps.iosched.io;

import com.google.android.apps.iosched.io.model.Room;
import com.google.android.apps.iosched.io.model.Rooms;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.Lists;
import com.google.gson.Gson;

import android.content.ContentProviderOperation;
import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

public class RoomsHandler extends JSONHandler {

    private static final String TAG = makeLogTag(RoomsHandler.class);

    public RoomsHandler(Context context) {
        super(context);
    }

    public ArrayList<ContentProviderOperation> parse(String json) throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        Rooms roomsJson = new Gson().fromJson(json, Rooms.class);
        int noOfRooms = roomsJson.rooms.length;
        for (int i = 0; i < noOfRooms; i++) {
            parseRoom(roomsJson.rooms[i], batch);
        }
        return batch;
    }

    private static void parseRoom(Room room, ArrayList<ContentProviderOperation> batch) {
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(ScheduleContract.addCallerIsSyncAdapterParameter(
                        ScheduleContract.Rooms.CONTENT_URI));
        builder.withValue(ScheduleContract.Rooms.ROOM_ID, room.id);
        builder.withValue(ScheduleContract.Rooms.ROOM_NAME, room.name);
        builder.withValue(ScheduleContract.Rooms.ROOM_FLOOR, room.floor);
        batch.add(builder.build());
    }
}
