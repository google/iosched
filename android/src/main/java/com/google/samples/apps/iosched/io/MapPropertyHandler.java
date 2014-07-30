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

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.samples.apps.iosched.io.map.model.MapData;
import com.google.samples.apps.iosched.io.map.model.MapConfig;
import com.google.samples.apps.iosched.io.map.model.Marker;
import com.google.samples.apps.iosched.io.map.model.Tile;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.MapUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapPropertyHandler extends JSONHandler {
    private static final String TAG = makeLogTag(MapPropertyHandler.class);

    // maps floor# to tile overlay for that floor
    private HashMap<String, Tile> mTileOverlays = new HashMap<String, Tile>();

    // maps floor# to a list of markers on that floor
    private HashMap<String, ArrayList<Marker>> mMarkers = new HashMap<String, ArrayList<Marker>>();

    public MapPropertyHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (MapData mapData : new Gson().fromJson(element, MapData[].class)) {
            if (mapData.config != null) {
                processConfig(mapData.config);
            }
            if (mapData.tiles != null) {
                processTileOverlays(mapData.tiles);
            }
            if (mapData.markers != null) {
                processMarkers(mapData.markers);
            }
        }
    }

    public Collection<Tile> getTileOverlays() {
        return mTileOverlays.values();
    }

    private void processConfig(MapConfig mapConfig) {
        MapUtils.setMyLocationEnabled(mContext, mapConfig.enableMyLocation);
    }

    private void processTileOverlays(java.util.Map<String, Tile> mapTiles) {
        for (Map.Entry<String, Tile> entry : mapTiles.entrySet()) {
            mTileOverlays.put(entry.getKey(), entry.getValue());
        }
    }

    private void processMarkers(java.util.Map<String, Marker[]> markers) {
        for (String floor : markers.keySet()) {
            if (!mMarkers.containsKey(floor)) {
                mMarkers.put(floor, new ArrayList<Marker>());
            }
            for (Marker marker : markers.get(floor)) {
                mMarkers.get(floor).add(marker);
            }
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        buildMarkers(list);
        buildTiles(list);
    }

    private void buildMarkers(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContract
                .addCallerIsSyncAdapterParameter(ScheduleContract.MapMarkers.CONTENT_URI);

        list.add(ContentProviderOperation.newDelete(uri).build());

        for (String floor : mMarkers.keySet()) {
            for (Marker marker : mMarkers.get(floor)) {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_ID, marker.id);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_FLOOR, floor);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_LABEL, marker.title);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_LATITUDE, marker.lat);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_LONGITUDE, marker.lng);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_TYPE, marker.type);
                list.add(builder.build());
            }
        }
    }

    private void buildTiles(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContract
                .addCallerIsSyncAdapterParameter(ScheduleContract.MapTiles.CONTENT_URI);

        list.add(ContentProviderOperation.newDelete(uri).build());

        for (String floor : mTileOverlays.keySet()) {
            Tile tileOverlay = mTileOverlays.get(floor);
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            builder.withValue(ScheduleContract.MapTiles.TILE_FLOOR, floor);
            builder.withValue(ScheduleContract.MapTiles.TILE_FILE, tileOverlay.filename);
            builder.withValue(ScheduleContract.MapTiles.TILE_URL, tileOverlay.url);
            list.add(builder.build());
        }
    }
}
