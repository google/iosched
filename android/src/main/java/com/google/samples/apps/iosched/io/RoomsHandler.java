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

package com.google.samples.apps.iosched.io;

import com.google.samples.apps.iosched.io.model.Room;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class RoomsHandler extends JSONHandler {
    private static final String TAG = makeLogTag(RoomsHandler.class);

    // map from room ID to Room model object
    private HashMap<String, Room> mRooms = new HashMap<String, Room>();

    public RoomsHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Room room : new Gson().fromJson(element, Room[].class)) {
            mRooms.put(room.id, room);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Rooms.CONTENT_URI);

        // The list of rooms is not large, so for simplicity we delete all of them and repopulate
        list.add(ContentProviderOperation.newDelete(uri).build());
        for (Room room : mRooms.values()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            builder.withValue(ScheduleContract.Rooms.ROOM_ID, room.id);
            builder.withValue(ScheduleContract.Rooms.ROOM_NAME, room.name);
            builder.withValue(ScheduleContract.Rooms.ROOM_FLOOR, room.floor);
            list.add(builder.build());
        }
    }
}
