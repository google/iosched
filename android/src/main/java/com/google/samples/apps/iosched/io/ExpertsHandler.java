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

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.io.model.Expert;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.google.samples.apps.iosched.util.LogUtils.*;

public class ExpertsHandler extends JSONHandler {

    private static final String TAG = makeLogTag(ExpertsHandler.class);

    private HashMap<String, Expert> mExperts = new HashMap<String, Expert>();

    public ExpertsHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        if (Config.hasExpertsDirectoryExpired()) {
            return;
        }
        for (Expert expert : new Gson().fromJson(element, Expert[].class)) {
            mExperts.put(expert.id, expert);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        if (Config.hasExpertsDirectoryExpired()) {
            return;
        }
        Uri uri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Experts.CONTENT_URI);
        HashMap<String, String> expertsHashcodes = loadExpertHashcodes();
        HashSet<String> expertsToKeep = new HashSet<String>();
        boolean isIncrementalUpdate = expertsHashcodes != null && expertsHashcodes.size() > 0;

        if (isIncrementalUpdate) {
            LOGD(TAG, "Doing incremental update for experts.");
        } else {
            LOGD(TAG, "Doing FULL (non incremental) update for experts.");
            list.add(ContentProviderOperation.newDelete(uri).build());
        }

        int updatedExperts = 0;
        for (Expert expert : mExperts.values()) {
            String hashCode = expert.getImportHashCode();
            expertsToKeep.add(expert.id);

            // Add the expert, if necessary
            if (!isIncrementalUpdate || !expertsHashcodes.containsKey(expert.id) ||
                    !expertsHashcodes.get(expert.id).equals(hashCode)) {
                ++updatedExperts;
                boolean isNew = !isIncrementalUpdate || !expertsHashcodes.containsKey(expert.id);
                buildExpert(isNew, expert, list);
            }
        }

        int deletedExperts = 0;
        if (isIncrementalUpdate) {
            for (String expertId : expertsHashcodes.keySet()) {
                if (!expertsToKeep.contains(expertId)) {
                    buildDeleteOperation(expertId, list);
                    ++deletedExperts;
                }
            }
        }

        LOGD(TAG, "Experts: " + (isIncrementalUpdate ? "INCREMENTAL" : "FULL") + " update. " +
                updatedExperts + " to update, " + deletedExperts + " to delete. New total: " +
                mExperts.size());
    }

    private void buildExpert(boolean isInsert, Expert expert,
            ArrayList<ContentProviderOperation> list) {
        Uri allExpertsUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Experts.CONTENT_URI);
        Uri thisExpertUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Experts.buildExpertUri(expert.id));

        ContentProviderOperation.Builder builder;
        if (isInsert) {
            builder = ContentProviderOperation.newInsert(allExpertsUri);
        } else {
            builder = ContentProviderOperation.newUpdate(thisExpertUri);
        }

        list.add(builder.withValue(ScheduleContract.Experts.UPDATED, System.currentTimeMillis())
                .withValue(ScheduleContract.Experts.EXPERT_ID, expert.id)
                .withValue(ScheduleContract.Experts.EXPERT_NAME, expert.name)
                .withValue(ScheduleContract.Experts.EXPERT_IMAGE_URL, expert.imageUrl)
                .withValue(ScheduleContract.Experts.EXPERT_TITLE, expert.title)
                .withValue(ScheduleContract.Experts.EXPERT_ABSTRACT, expert.bio)
                .withValue(ScheduleContract.Experts.EXPERT_URL, expert.url)
                .withValue(ScheduleContract.Experts.EXPERT_COUNTRY, expert.country)
                .withValue(ScheduleContract.Experts.EXPERT_CITY, expert.city)
                .withValue(ScheduleContract.Experts.EXPERT_ATTENDING, expert.attending)
                .withValue(ScheduleContract.Experts.EXPERT_IMPORT_HASHCODE,
                        expert.getImportHashCode())
                .build());
    }

    private void buildDeleteOperation(String expertId, ArrayList<ContentProviderOperation> list) {
        Uri expertUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Experts.buildExpertUri(expertId));
        list.add(ContentProviderOperation.newDelete(expertUri).build());
    }

    private HashMap<String, String> loadExpertHashcodes() {
        Uri uri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Experts.CONTENT_URI);
        Cursor cursor = mContext.getContentResolver().query(uri, ExpertHashcodeQuery.PROJECTION,
                null, null, null);
        if (cursor == null) {
            LOGE(TAG, "Error querying expert hashcodes (got null cursor");
            return null;
        }
        if (cursor.getCount() < 1) {
            LOGE(TAG, "Error querying expert hashcodes (no records returned)");
            return null;
        }
        HashMap<String, String> result = new HashMap<String, String>();
        while (cursor.moveToNext()) {
            String expertId = cursor.getString(ExpertHashcodeQuery.EXPERT_ID);
            String hashcode = cursor.getString(ExpertHashcodeQuery.EXPERT_IMPORT_HASHCODE);
            result.put(expertId, hashcode == null ? "" : hashcode);
        }
        cursor.close();
        return result;
    }

    private interface ExpertHashcodeQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Experts.EXPERT_ID,
                ScheduleContract.Experts.EXPERT_IMPORT_HASHCODE,
        };
        final int _ID = 0;
        final int EXPERT_ID = 1;
        final int EXPERT_IMPORT_HASHCODE = 2;
    }

}
