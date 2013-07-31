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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.graphics.Color;
import com.google.android.apps.iosched.io.model.Track;
import com.google.android.apps.iosched.io.model.Tracks;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.Lists;
import com.google.android.apps.iosched.util.ParserUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

public class TracksHandler extends JSONHandler {

    private static final String TAG = makeLogTag(TracksHandler.class);

    public TracksHandler(Context context) {
        super(context);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(String json)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        batch.add(ContentProviderOperation.newDelete(
                ScheduleContract.addCallerIsSyncAdapterParameter(
                        ScheduleContract.Tracks.CONTENT_URI)).build());
        Tracks tracksJson = new Gson().fromJson(json, Tracks.class);
        int noOfTracks = tracksJson.getTrack().length;
        for (int i = 0; i < noOfTracks; i++) {
            parseTrack(tracksJson.getTrack()[i], batch);
        }
        return batch;
    }

    private static void parseTrack(Track track, ArrayList<ContentProviderOperation> batch) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                ScheduleContract.addCallerIsSyncAdapterParameter(
                        ScheduleContract.Tracks.CONTENT_URI));
        builder.withValue(ScheduleContract.Tracks.TRACK_ID, track.id);
        builder.withValue(ScheduleContract.Tracks.TRACK_NAME, track.name);
        builder.withValue(ScheduleContract.Tracks.TRACK_COLOR, Color.parseColor(track.color));
        builder.withValue(ScheduleContract.Tracks.TRACK_ABSTRACT, track._abstract);
        builder.withValue(ScheduleContract.Tracks.TRACK_LEVEL, track.level);
        builder.withValue(ScheduleContract.Tracks.TRACK_ORDER_IN_LEVEL,
                track.order_in_level);
        builder.withValue(ScheduleContract.Tracks.TRACK_META, track.meta);
        builder.withValue(ScheduleContract.Tracks.TRACK_HASHTAG, ParserUtils.sanitizeId(track.name));
        batch.add(builder.build());
    }
}
