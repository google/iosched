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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.io.model.Video;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.*;

public class VideosHandler extends JSONHandler {
    private static final String TAG = makeLogTag(VideosHandler.class);
    private HashMap<String, Video> mVideos = new HashMap<String, Video>();

    public VideosHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Video video : new Gson().fromJson(element, Video[].class)) {
            if (TextUtils.isEmpty(video.id)) {
                LOGW(TAG, "Video without valid ID. Using VID instead: " + video.vid);
                video.id = video.vid;
            }
            mVideos.put(video.id, video);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Videos.CONTENT_URI);
        HashMap<String, String> videoHashcodes = loadVideoHashcodes();
        HashSet<String> videosToKeep = new HashSet<String>();
        boolean isIncrementalUpdate = videoHashcodes != null && videoHashcodes.size() > 0;

        if (isIncrementalUpdate) {
            LOGD(TAG, "Doing incremental update for videos.");
        } else {
            LOGD(TAG, "Doing FULL (non incremental) update for videos.");
            list.add(ContentProviderOperation.newDelete(uri).build());
        }

        int updatedVideos = 0;
        for (Video video : mVideos.values()) {
            String hashCode = video.getImportHashcode();
            videosToKeep.add(video.id);

            // add video, if necessary
            if (!isIncrementalUpdate || !videoHashcodes.containsKey(video.id) ||
                    !videoHashcodes.get(video.id).equals(hashCode)) {
                ++updatedVideos;
                boolean isNew = !isIncrementalUpdate || !videoHashcodes.containsKey(video.id);
                buildVideo(isNew, video, list);
            }
        }

        int deletedVideos = 0;
        if (isIncrementalUpdate) {
            for (String videoId : videoHashcodes.keySet()) {
                if (!videosToKeep.contains(videoId)) {
                    buildDeleteOperation(videoId, list);
                    ++deletedVideos;
                }
            }
        }

        LOGD(TAG, "Videos: " + (isIncrementalUpdate ? "INCREMENTAL" : "FULL") + " update. " +
                updatedVideos + " to update, " + deletedVideos + " to delete. New total: " +
                mVideos.size());
    }

    private void buildVideo(boolean isInsert, Video video,
                              ArrayList<ContentProviderOperation> list) {
        Uri allVideosUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Videos.CONTENT_URI);
        Uri thisVideoUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Videos.buildVideoUri(video.id));

        ContentProviderOperation.Builder builder;
        if (isInsert) {
            builder = ContentProviderOperation.newInsert(allVideosUri);
        } else {
            builder = ContentProviderOperation.newUpdate(thisVideoUri);
        }

        if (TextUtils.isEmpty(video.vid)) {
            LOGW(TAG, "Ignoring video with missing video ID.");
            return;
        }

        String thumbUrl = video.thumbnailUrl;
        if (TextUtils.isEmpty(thumbUrl)) {
            // Oops, missing thumbnail URL. Let's improvise.
            // NOTE: this method of obtaining a thumbnail URL from the video ID
            // is unofficial and might not work in the future; that's why we use
            // it only as a fallback in case we don't get a thumbnail URL in the incoming data.
            thumbUrl = String.format(Locale.US, Config.VIDEO_LIBRARY_FALLBACK_THUMB_URL_FMT, video.vid);
            LOGW(TAG, "Video with missing thumbnail URL: " + video.vid
                    + ". Using fallback: " + thumbUrl);
        }

        list.add(builder.withValue(ScheduleContract.Videos.VIDEO_ID, video.id)
                .withValue(ScheduleContract.Videos.VIDEO_YEAR, video.year)
                .withValue(ScheduleContract.Videos.VIDEO_TITLE, video.title.trim())
                .withValue(ScheduleContract.Videos.VIDEO_DESC, video.desc)
                .withValue(ScheduleContract.Videos.VIDEO_VID, video.vid)
                .withValue(ScheduleContract.Videos.VIDEO_TOPIC, video.topic)
                .withValue(ScheduleContract.Videos.VIDEO_SPEAKERS, video.speakers)
                .withValue(ScheduleContract.Videos.VIDEO_THUMBNAIL_URL, thumbUrl)
                .withValue(ScheduleContract.Videos.VIDEO_IMPORT_HASHCODE,
                        video.getImportHashcode())
                .build());
    }

    private void buildDeleteOperation(String videoId, ArrayList<ContentProviderOperation> list) {
        Uri videoUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Videos.buildVideoUri(videoId));
        list.add(ContentProviderOperation.newDelete(videoUri).build());
    }

    private HashMap<String, String> loadVideoHashcodes() {
        Uri uri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Videos.CONTENT_URI);
        Cursor cursor = mContext.getContentResolver().query(uri, VideoHashcodeQuery.PROJECTION,
                null, null, null);
        if (cursor == null) {
            LOGE(TAG, "Error querying video hashcodes (got null cursor)");
            return null;
        }
        if (cursor.getCount() < 1) {
            LOGE(TAG, "Error querying video hashcodes (no records returned)");
            return null;
        }
        HashMap<String, String> result = new HashMap<String, String>();
        while (cursor.moveToNext()) {
            String videoId = cursor.getString(VideoHashcodeQuery.VIDEO_ID);
            String hashcode = cursor.getString(VideoHashcodeQuery.VIDEO_IMPORT_HASHCODE);
            result.put(videoId, hashcode == null ? "" : hashcode);
        }
        cursor.close();
        return result;
    }

    private interface VideoHashcodeQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Videos.VIDEO_ID,
                ScheduleContract.Videos.VIDEO_IMPORT_HASHCODE
        };
        final int _ID = 0;
        final int VIDEO_ID = 1;
        final int VIDEO_IMPORT_HASHCODE = 2;
    }
}
