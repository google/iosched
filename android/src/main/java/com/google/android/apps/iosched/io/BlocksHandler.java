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

import com.google.android.apps.iosched.io.model.Day;
import com.google.android.apps.iosched.io.model.EventSlots;
import com.google.android.apps.iosched.io.model.TimeSlot;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.util.Lists;
import com.google.android.apps.iosched.util.ParserUtils;
import com.google.gson.Gson;

import android.content.ContentProviderOperation;
import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.android.apps.iosched.util.LogUtils.LOGE;
import static com.google.android.apps.iosched.util.LogUtils.LOGV;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;


public class BlocksHandler extends JSONHandler {

    private static final String TAG = makeLogTag(BlocksHandler.class);

    public BlocksHandler(Context context) {
        super(context);
    }

    public ArrayList<ContentProviderOperation> parse(String json) throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        try {
            Gson gson = new Gson();
            EventSlots eventSlots = gson.fromJson(json, EventSlots.class);
            int numDays = eventSlots.day.length;
            //2011-05-10T07:00:00.000-07:00
            for (int i = 0; i < numDays; i++) {
                Day day = eventSlots.day[i];
                String date = day.date;
                TimeSlot[] timeSlots = day.slot;
                for (TimeSlot timeSlot : timeSlots) {
                    parseSlot(date, timeSlot, batch);
                }
            }
        } catch (Throwable e) {
            LOGE(TAG, e.toString());
        }
        return batch;
    }

    private static void parseSlot(String date, TimeSlot slot,
            ArrayList<ContentProviderOperation> batch) {
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(ScheduleContract.addCallerIsSyncAdapterParameter(Blocks.CONTENT_URI));
        //LOGD(TAG, "Inside parseSlot:" + date + ",  " + slot);
        String start = slot.start;
        String end = slot.end;

        String type = Blocks.BLOCK_TYPE_GENERIC;
        if (slot.type != null) {
            type = slot.type;
        }
        String title = "N_D";
        if (slot.title != null) {
            title = slot.title;
        }
        String startTime = date + "T" + start + ":00.000-07:00";
        String endTime = date + "T" + end + ":00.000-07:00";
        LOGV(TAG, "startTime:" + startTime);
        long startTimeL = ParserUtils.parseTime(startTime);
        long endTimeL = ParserUtils.parseTime(endTime);
        final String blockId = Blocks.generateBlockId(startTimeL, endTimeL);
        LOGV(TAG, "blockId:" + blockId);
        LOGV(TAG, "title:" + title);
        LOGV(TAG, "start:" + startTimeL);
        builder.withValue(Blocks.BLOCK_ID, blockId);
        builder.withValue(Blocks.BLOCK_TITLE, title);
        builder.withValue(Blocks.BLOCK_START, startTimeL);
        builder.withValue(Blocks.BLOCK_END, endTimeL);
        builder.withValue(Blocks.BLOCK_TYPE, type);
        builder.withValue(Blocks.BLOCK_META, slot.meta);
        batch.add(builder.build());
    }
}
