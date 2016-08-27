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

import com.google.common.collect.Lists;
import com.google.samples.apps.iosched.io.model.Block;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContractHelper;
import com.google.samples.apps.iosched.util.ParserUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;

import no.java.schedule.v2.io.model.EMSCollection;
import no.java.schedule.v2.io.model.EMSItem;
import no.java.schedule.v2.io.model.JZSlotsResponse;

import static com.google.samples.apps.iosched.util.LogUtils.LOGV;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;


public class BlocksHandler extends JSONHandler {
    private static final String TAG = makeLogTag(BlocksHandler.class);
    private ArrayList<Block> mBlocks = new ArrayList<Block>();

    public BlocksHandler(Context context) {
        super(context);
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Blocks.CONTENT_URI);
        list.add(ContentProviderOperation.newDelete(uri).build());
        for (Block block : mBlocks) {
            outputBlock(block, list);
        }
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(String json) throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        try {
            Gson gson = new Gson();
            JZSlotsResponse response = gson.fromJson(json, JZSlotsResponse.class);
            EMSCollection eventSlots = response.collection;

            for (EMSItem slot : eventSlots.items) {
                parseSlot(slot, batch);
            }
        } catch (Throwable e) {
        }

        return batch;
    }


    private static void parseSlot(EMSItem slot, ArrayList<ContentProviderOperation> batch) {
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(ScheduleContract.addCallerIsSyncAdapterParameter(ScheduleContract.Blocks.CONTENT_URI));
        //LOGD(TAG, "Inside parseSlot:" + date + ",  " + slot);
        String startTime = slot.getValue("start");
        String endTime = slot.getValue("end");

        String type = "N_D";
        if (slot.getValue("type") != null) {
            type = slot.getValue("type");
        }
        String title = "N_D";
        if (slot.getValue("title") != null) {
            title = slot.getValue("title");
        }

        String meta = "N_D";
        if (slot.getValue("meta") != null) {
            title = slot.getValue("meta");
        }

        LOGV(TAG, "startTime:" + startTime);
        long startTimeL = ParserUtils.parseTime(startTime);
        long endTimeL = ParserUtils.parseTime(endTime);
        final String blockId = slot.href.toString();//Blocks.generateBlockId(startTimeL, endTimeL);

        LOGV(TAG, "blockId:" + blockId);
        LOGV(TAG, "title:" + title);
        LOGV(TAG, "start:" + startTimeL);
        builder.withValue(ScheduleContract.Blocks.BLOCK_ID, blockId);
        builder.withValue(ScheduleContract.Blocks.BLOCK_TITLE, title);
        builder.withValue(ScheduleContract.Blocks.BLOCK_START, startTimeL);
        builder.withValue(ScheduleContract.Blocks.BLOCK_END, endTimeL);
        builder.withValue(ScheduleContract.Blocks.BLOCK_TYPE, type);
        builder.withValue(ScheduleContract.Blocks.BLOCK_SUBTITLE, meta);
        batch.add(builder.build());
    }

    @Override
    public void process(JsonElement element) {
        for (Block block : new Gson().fromJson(element, Block[].class)) {
            mBlocks.add(block);
        }
    }

    private static void outputBlock(Block block, ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Blocks.CONTENT_URI);
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
        String title = block.title != null ? block.title : "";
        String meta = block.subtitle != null ? block.subtitle : "";

        String type = block.type;
        if ( ! ScheduleContract.Blocks.isValidBlockType(type)) {
            LOGW(TAG, "block from "+block.start+" to "+block.end+" has unrecognized type ("
                    +type+"). Using "+ ScheduleContract.Blocks.BLOCK_TYPE_BREAK +" instead.");
            type = ScheduleContract.Blocks.BLOCK_TYPE_BREAK;
        }

        long startTimeL = ParserUtils.parseTime(block.start);
        long endTimeL = ParserUtils.parseTime(block.end);
        final String blockId = ScheduleContract.Blocks.generateBlockId(startTimeL, endTimeL);
        builder.withValue(ScheduleContract.Blocks.BLOCK_ID, blockId);
        builder.withValue(ScheduleContract.Blocks.BLOCK_TITLE, title);
        builder.withValue(ScheduleContract.Blocks.BLOCK_START, startTimeL);
        builder.withValue(ScheduleContract.Blocks.BLOCK_END, endTimeL);
        builder.withValue(ScheduleContract.Blocks.BLOCK_TYPE, type);
        builder.withValue(ScheduleContract.Blocks.BLOCK_SUBTITLE, meta);
        list.add(builder.build());
    }
}
