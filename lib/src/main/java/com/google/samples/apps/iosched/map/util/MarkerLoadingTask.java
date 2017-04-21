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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.ui.IconGenerator;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.MapUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Background task that queries the content provider and prepares a {@link GeoJsonLayer} that can be
 * used to create Markers.
 */
public class MarkerLoadingTask extends AsyncTaskLoader<GeoJsonLayer> {

    private GoogleMap mMap;
    private Context mContext;

    public MarkerLoadingTask(GoogleMap map, Context context) {
        super(context);
        mContext = context;
        mMap = map;
    }

    @Override
    public GeoJsonLayer loadInBackground() {
        try {
            final Uri uri = ScheduleContract.MapGeoJson.buildGeoJsonUri();
            Cursor cursor = getContext().getContentResolver().query(uri, MarkerQuery.PROJECTION,
                    null, null, null);

            GeoJsonLayer layer = null;
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    final String id = cursor.getString(MarkerQuery.GEOJSON);
                    JSONObject j = new JSONObject(id);
                    //GeoJsonLayer stores a map, which is only modified when addLayerToMap is called
                    layer = new GeoJsonLayer(mMap, j);
                } else {
                    return null;
                }
                cursor.close();
            }

            Iterator<GeoJsonFeature> iterator = layer.getFeatures().iterator();
            final IconGenerator labelIconGenerator = MapUtils.getLabelIconGenerator(getContext());
            while (iterator.hasNext()) {
                GeoJsonFeature feature = iterator.next();

                // get data
                final String id = feature.getProperty("id");
                GeoJsonPoint point = (GeoJsonPoint) feature.getGeometry();
                final LatLng position = point.getCoordinates();
                final String typeString = feature.getProperty("type");
                final int type = MapUtils.detectMarkerType(typeString);
                final String label = feature.getProperty("title");

                GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                if (type == MarkerModel.TYPE_LABEL) {
                    // Label markers contain the label as its icon
                    pointStyle = MapUtils.createLabelMarker(labelIconGenerator, id, label);
                } else if (type == MarkerModel.TYPE_ICON) {
                    // An icon marker is mapped to a drawable based on its full type name
                    pointStyle = MapUtils.createIconMarker(typeString, id, getContext());
                } else if (type != MarkerModel.TYPE_INACTIVE) {
                    // All other markers (that are not inactive) contain a pin icon
                    pointStyle = MapUtils.createPinMarker(id);
                }

                // If the marker is invalid (e.g. the icon does not exist), remove it from the map.
                if (pointStyle == null) {
                    iterator.remove();
                } else {
                    pointStyle.setVisible(true);
                    feature.setPointStyle(pointStyle);
                }
            }
            return layer;
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
