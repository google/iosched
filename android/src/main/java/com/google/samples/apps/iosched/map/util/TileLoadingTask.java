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

import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.MapUtils;

import com.jakewharton.disklrucache.DiskLruCache;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Background task that queries the content provider and prepares a list of
 * {@link com.google.android.gms.maps.model.TileOverlay}s
 * for addition to the map.
 * A tile overlay is always tied to a floor in the venue and is loaded directly from an SVG file.
 * A {@link DiskLruCache} is used to create a {@link CachedTileProvider} for each overlay.
 * <p>Note: The CachedTileProvider <b>must</b> be closed when the encapsulating map is stopped.
 * (See
 * {@link CachedTileProvider#closeCache()}
 */
public class TileLoadingTask extends AsyncTaskLoader<List<TileLoadingTask.TileEntry>> {

    private static final String TAG = makeLogTag(TileLoadingTask.class);


    private final float mDPI;

    public TileLoadingTask(Context context, float dpi) {
        super(context);
        mDPI = dpi;
    }


    @Override
    public List<TileEntry> loadInBackground() {
        List<TileEntry> list = null;
        // Create a URI to get a cursor for all map tile entries.
        final Uri uri = ScheduleContract.MapTiles.buildUri();
        Cursor cursor = getContext().getContentResolver().query(uri,
                OverlayQuery.PROJECTION, null, null, null);

        if (cursor != null) {
            // Create a TileProvider for each entry in the cursor
            final int count = cursor.getCount();

            // Initialise the tile cache that is reused for all TileProviders.
            // Note that the cache *MUST* be closed when the encapsulating Fragment is stopped.
            DiskLruCache tileCache = MapUtils.openDiskCache(getContext());

            list = new ArrayList<>(count);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                final int floor = cursor.getInt(OverlayQuery.TILE_FLOOR);
                final String file = cursor.getString(OverlayQuery.TILE_FILE);

                File f = MapUtils.getTileFile(getContext().getApplicationContext(), file);
                if (f == null || !f.exists()) {
                    // Skip the file if it is invalid or does not exist.
                    LOGE(TAG, "Tile file not found for floor " + floor);
                    break;
                }

                CachedTileProvider provider;
                try {
                    SVGTileProvider svgProvider = new SVGTileProvider(f, mDPI);
                    // Wrap the SVGTileProvider in a CachedTileProvider for caching on disk.
                    provider = new CachedTileProvider(Integer.toString(floor), svgProvider,
                            tileCache);
                } catch (IOException e) {
                    LOGD(TAG, "Could not create Tile Provider.");
                    break;
                }
                list.add(new TileEntry(floor, provider));
                cursor.moveToNext();
            }

            cursor.close();
        }

        return list;
    }


    private interface OverlayQuery {

        String[] PROJECTION = {
                ScheduleContract.MapTiles.TILE_FLOOR,
                ScheduleContract.MapTiles.TILE_FILE
        };

        int TILE_FLOOR = 0;
        int TILE_FILE = 1;
    }

    public class TileEntry {

        public CachedTileProvider provider;
        public int floor;

        TileEntry(int floor, CachedTileProvider provider) {
            this.floor = floor;
            this.provider = provider;
        }
    }

}
