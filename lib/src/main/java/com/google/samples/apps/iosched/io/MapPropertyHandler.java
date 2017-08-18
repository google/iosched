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
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.samples.apps.iosched.io.map.model.MapData;
import com.google.samples.apps.iosched.io.map.model.Marker;
import com.google.samples.apps.iosched.io.map.model.Tile;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContractHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class MapPropertyHandler extends JSONHandler {
    private static final String TAG = makeLogTag(MapPropertyHandler.class);

    // maps floor# to tile overlay for that floor
    private HashMap<String, Tile> mTileOverlays = new HashMap<>();

    private String geojson = null;

    public MapPropertyHandler(Context context) {
        super(context);
    }

    @Override
    public void process(@NonNull Gson gson, @NonNull JsonElement element) {
        for (MapData mapData : gson.fromJson(element, MapData[].class)) {
            if (mapData.tiles != null) {
                processTileOverlays(mapData.tiles);
            }
            if (mapData.markers != null) {
                // Get the geojson data that is stored as 'markers' and verify it's valid JSON.
                geojson = mapData.markers.toString();
            }
        }
    }


    public Collection<Tile> getTileOverlays() {
        return mTileOverlays.values();
    }

    private void processTileOverlays(java.util.Map<String, Tile> mapTiles) {
        for (Map.Entry<String, Tile> entry : mapTiles.entrySet()) {
            mTileOverlays.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        buildMarkers(list);
        buildTiles(list);
    }

    private void buildMarkers(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContractHelper
                .setUriAsCalledFromSyncAdapter(ScheduleContract.MapGeoJson.CONTENT_URI);

        list.add(ContentProviderOperation.newDelete(uri).build());

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
        builder.withValue(ScheduleContract.MapGeoJson.GEOJSON, geojson);
        list.add(builder.build());
    }

    private void buildTiles(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContractHelper
                .setUriAsCalledFromSyncAdapter(ScheduleContract.MapTiles.CONTENT_URI);

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
