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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import com.google.samples.apps.iosched.provider.ScheduleContract;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Background task that queries the content provider and prepares a {@link JSONObject} that contains
 * Geo Json describing the markers.
 */
public class MarkerLoadingTask extends AsyncTaskLoader<JSONObject> {

    public MarkerLoadingTask(Context context) {
        super(context);
    }

    @Override
    public JSONObject loadInBackground() {
        try {
            final Uri uri = ScheduleContract.MapGeoJson.buildGeoJsonUri();
            Cursor cursor = getContext().getContentResolver().query(uri, MarkerQuery.PROJECTION,
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                final String id = cursor.getString(MarkerQuery.GEOJSON);
                return new JSONObject(id);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private interface MarkerQuery {
        String[] PROJECTION = {
                ScheduleContract.MapGeoJson.GEOJSON
        };

        int GEOJSON = 0;
    }

}
