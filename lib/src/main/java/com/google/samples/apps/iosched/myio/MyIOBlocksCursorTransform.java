/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.myio;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.os.CancellationSignal;

import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.CursorModelLoader;


import java.util.ArrayList;
import java.util.List;


class MyIOBlocksCursorTransform implements CursorModelLoader.CursorTransform<List<ScheduleItem>> {

    @Override
    public Cursor performQuery(@NonNull CursorModelLoader<List<ScheduleItem>> loader,
                               @NonNull CancellationSignal cancellationSignal) {
        return ContentResolverCompat.query(
                loader.getContext().getContentResolver(),
                ScheduleContract.Blocks.CONTENT_URI,
                BlocksQuery.PROJECTION,
                BlocksQuery.SELECTION,
                null,
                ScheduleContract.Blocks.BLOCK_START,
                cancellationSignal);
    }

    @Override
    public List<ScheduleItem> cursorToModel(@NonNull CursorModelLoader<List<ScheduleItem>> loader,
                                            @NonNull Cursor cursor) {
            ArrayList<ScheduleItem> items = new ArrayList<>();

            if (cursor.moveToFirst()) {
                do {
                    ScheduleItem item = new ScheduleItem();
                    item.setTypeFromBlockType(cursor.getString(BlocksQuery.BLOCK_TYPE));
                    item.title = cursor.getString(BlocksQuery.BLOCK_TITLE);
                    item.room = item.subtitle = cursor.getString(BlocksQuery.BLOCK_SUBTITLE);
                    item.startTime = cursor.getLong(BlocksQuery.BLOCK_START);
                    item.endTime = cursor.getLong(BlocksQuery.BLOCK_END);
                    item.blockKind = cursor.getString(BlocksQuery.BLOCK_KIND);
                    item.flags |= ScheduleItem.FLAG_NOT_REMOVABLE;
                    items.add(item);
                } while (cursor.moveToNext());

        }
        return items;
    }

    @Override
    public Uri getObserverUri(@NonNull CursorModelLoader<List<ScheduleItem>> loader) {
        return ScheduleContract.Blocks.CONTENT_URI;
    }

    interface BlocksQuery {
        String[] PROJECTION = {
                ScheduleContract.Blocks.BLOCK_TITLE,
                ScheduleContract.Blocks.BLOCK_TYPE,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
                ScheduleContract.Blocks.BLOCK_SUBTITLE,
                ScheduleContract.Blocks.BLOCK_KIND
        };

        // constrain to "break" blocks on the specified day
        String SELECTION = ScheduleContract.Blocks.BLOCK_TYPE + " = '" +
                ScheduleContract.Blocks.BLOCK_TYPE_BREAK + "'";

        int BLOCK_TITLE = 0;
        int BLOCK_TYPE = 1;
        int BLOCK_START = 2;
        int BLOCK_END = 3;
        int BLOCK_SUBTITLE = 4;
        int BLOCK_KIND = 5;
    }
}
