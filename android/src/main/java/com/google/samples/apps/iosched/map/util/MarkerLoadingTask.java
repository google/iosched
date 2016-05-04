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

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.MapUtils;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * Background task that queries the content provider and prepares a list of {@link MarkerModel}s
 * wrapped in a {@link com.google.samples.apps.iosched.map.util.MarkerLoadingTask.MarkerEntry}
 * that can be used to create Markers.
 */
public class MarkerLoadingTask extends AsyncTaskLoader<List<MarkerLoadingTask.MarkerEntry>> {

    public MarkerLoadingTask(Context context) {
        super(context);
    }

    @Override
    public List<MarkerEntry> loadInBackground() {
        List<MarkerEntry> list = null;

        // Create a URI to get a cursor of all map markers
        final Uri uri = ScheduleContract.MapMarkers.buildMarkerUri();
        Cursor cursor = getContext().getContentResolver().query(uri, MarkerQuery.PROJECTION,
                null, null, null);

        // Create a MarkerModel for each entry
        final int count = cursor.getCount();
        if (cursor != null) {

            list = new ArrayList<>(count);
            final IconGenerator labelIconGenerator = MapUtils.getLabelIconGenerator(getContext());
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                // get data
                final String id = cursor.getString(MarkerQuery.MARKER_ID);
                final int floor = cursor.getInt(MarkerQuery.MARKER_FLOOR);
                final float lat = cursor.getFloat(MarkerQuery.MARKER_LATITUDE);
                final float lon = cursor.getFloat(MarkerQuery.MARKER_LONGITUDE);
                final String typeString = cursor.getString(MarkerQuery.MARKER_TYPE);
                final int type = MapUtils.detectMarkerType(typeString);
                final String label = cursor.getString(MarkerQuery.MARKER_LABEL);

                final LatLng position = new LatLng(lat, lon);
                MarkerOptions marker = null;
                if (type == MarkerModel.TYPE_LABEL) {
                    // Label markers contain the label as its icon
                    marker = MapUtils.createLabelMarker(labelIconGenerator, id, position, label);
                } else if (type == MarkerModel.TYPE_ICON) {
                    // An icon marker is mapped to a drawable based on its full type name
                    marker = MapUtils.createIconMarker(typeString, id, position, getContext());
                } else if (type != MarkerModel.TYPE_INACTIVE) {
                    // All other markers (that are not inactive) contain a pin icon
                    marker = MapUtils.createPinMarker(id, position);
                }

                MarkerModel model = new MarkerModel(id, floor, type, label, null);
                MarkerEntry entry = new MarkerEntry(model, marker);

                list.add(entry);

                cursor.moveToNext();
            }
            cursor.close();
        }

        return list;
    }


    private interface MarkerQuery {

        String[] PROJECTION = {
                ScheduleContract.MapMarkers.MARKER_ID,
                ScheduleContract.MapMarkers.MARKER_FLOOR,
                ScheduleContract.MapMarkers.MARKER_LATITUDE,
                ScheduleContract.MapMarkers.MARKER_LONGITUDE,
                ScheduleContract.MapMarkers.MARKER_TYPE,
                ScheduleContract.MapMarkers.MARKER_LABEL
        };

        int MARKER_ID = 0;
        int MARKER_FLOOR = 1;
        int MARKER_LATITUDE = 2;
        int MARKER_LONGITUDE = 3;
        int MARKER_TYPE = 4;
        int MARKER_LABEL = 5;
    }

    public class MarkerEntry {

        public MarkerModel model;
        public MarkerOptions options;

        public MarkerEntry(MarkerModel model, MarkerOptions options) {
            this.model = model;
            this.options = options;
        }
    }
}
