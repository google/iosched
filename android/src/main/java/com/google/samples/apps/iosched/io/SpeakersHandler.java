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

import com.google.samples.apps.iosched.io.model.Speaker;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.google.samples.apps.iosched.util.LogUtils.*;

public class SpeakersHandler extends JSONHandler {
    private static final String TAG = makeLogTag(SpeakersHandler.class);
    private HashMap<String, Speaker> mSpeakers = new HashMap<String, Speaker>();

    public SpeakersHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Speaker speaker : new Gson().fromJson(element, Speaker[].class)) {
            mSpeakers.put(speaker.id, speaker);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Speakers.CONTENT_URI);
        HashMap<String, String> speakerHashcodes = loadSpeakerHashcodes();
        HashSet<String> speakersToKeep = new HashSet<String>();
        boolean isIncrementalUpdate = speakerHashcodes != null && speakerHashcodes.size() > 0;

        if (isIncrementalUpdate) {
            LOGD(TAG, "Doing incremental update for speakers.");
        } else {
            LOGD(TAG, "Doing FULL (non incremental) update for speakers.");
            list.add(ContentProviderOperation.newDelete(uri).build());
        }

        int updatedSpeakers = 0;
        for (Speaker speaker : mSpeakers.values()) {
            String hashCode = speaker.getImportHashcode();
            speakersToKeep.add(speaker.id);

            // add speaker, if necessary
            if (!isIncrementalUpdate || !speakerHashcodes.containsKey(speaker.id) ||
                    !speakerHashcodes.get(speaker.id).equals(hashCode)) {
                ++updatedSpeakers;
                boolean isNew = !isIncrementalUpdate || !speakerHashcodes.containsKey(speaker.id);
                buildSpeaker(isNew, speaker, list);
            }
        }

        int deletedSpeakers = 0;
        if (isIncrementalUpdate) {
            for (String speakerId : speakerHashcodes.keySet()) {
                if (!speakersToKeep.contains(speakerId)) {
                    buildDeleteOperation(speakerId, list);
                    ++deletedSpeakers;
                }
            }
        }

        LOGD(TAG, "Speakers: " + (isIncrementalUpdate ? "INCREMENTAL" : "FULL") + " update. " +
                updatedSpeakers + " to update, " + deletedSpeakers + " to delete. New total: " +
                mSpeakers.size());
    }

    private void buildSpeaker(boolean isInsert, Speaker speaker,
                              ArrayList<ContentProviderOperation> list) {
        Uri allSpeakersUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Speakers.CONTENT_URI);
        Uri thisSpeakerUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Speakers.buildSpeakerUri(speaker.id));

        ContentProviderOperation.Builder builder;
        if (isInsert) {
            builder = ContentProviderOperation.newInsert(allSpeakersUri);
        } else {
            builder = ContentProviderOperation.newUpdate(thisSpeakerUri);
        }

        list.add(builder.withValue(ScheduleContract.SyncColumns.UPDATED, System.currentTimeMillis())
                .withValue(ScheduleContract.Speakers.SPEAKER_ID, speaker.id)
                .withValue(ScheduleContract.Speakers.SPEAKER_NAME, speaker.name)
                .withValue(ScheduleContract.Speakers.SPEAKER_ABSTRACT, speaker.bio)
                .withValue(ScheduleContract.Speakers.SPEAKER_COMPANY, speaker.company)
                .withValue(ScheduleContract.Speakers.SPEAKER_IMAGE_URL, speaker.thumbnailUrl)
                .withValue(ScheduleContract.Speakers.SPEAKER_URL, speaker.plusoneUrl)
                .withValue(ScheduleContract.Speakers.SPEAKER_IMPORT_HASHCODE,
                        speaker.getImportHashcode())
                .build());
    }

    private void buildDeleteOperation(String speakerId, ArrayList<ContentProviderOperation> list) {
        Uri speakerUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Speakers.buildSpeakerUri(speakerId));
        list.add(ContentProviderOperation.newDelete(speakerUri).build());
    }

    private HashMap<String, String> loadSpeakerHashcodes() {
        Uri uri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Speakers.CONTENT_URI);
        Cursor cursor = mContext.getContentResolver().query(uri, SpeakerHashcodeQuery.PROJECTION,
                null, null, null);
        if (cursor == null) {
            LOGE(TAG, "Error querying speaker hashcodes (got null cursor)");
            return null;
        }
        if (cursor.getCount() < 1) {
            LOGE(TAG, "Error querying speaker hashcodes (no records returned)");
            return null;
        }
        HashMap<String, String> result = new HashMap<String, String>();
        while (cursor.moveToNext()) {
            String speakerId = cursor.getString(SpeakerHashcodeQuery.SPEAKER_ID);
            String hashcode = cursor.getString(SpeakerHashcodeQuery.SPEAKER_IMPORT_HASHCODE);
            result.put(speakerId, hashcode == null ? "" : hashcode);
        }
        cursor.close();
        return result;
    }

    public HashMap<String, Speaker> getSpeakerMap() {
        return mSpeakers;
    }

    private interface SpeakerHashcodeQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Speakers.SPEAKER_ID,
                ScheduleContract.Speakers.SPEAKER_IMPORT_HASHCODE
        };
        final int _ID = 0;
        final int SPEAKER_ID = 1;
        final int SPEAKER_IMPORT_HASHCODE = 2;
    }
}
