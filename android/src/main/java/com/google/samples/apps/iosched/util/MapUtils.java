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
package com.google.samples.apps.iosched.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.ui.MapFragment;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.ui.IconGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.jakewharton.disklrucache.DiskLruCache;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;

public class MapUtils {

    private static final String TILE_PATH = "maptiles";
    private static final String PREF_MYLOCATION_ENABLED = "map_mylocation_enabled";
    private static final String TAG = LogUtils.makeLogTag(MapUtils.class);

    /**
     * Creates a marker for a session.
     *
     * @param id       Id to be embedded as the title
     * @param position
     * @return
     */
    public static MarkerOptions createSessionMarker(String id, String type, LatLng position) {
        final BitmapDescriptor icon =
                BitmapDescriptorFactory.fromResource(R.drawable.marker_session);
        return new MarkerOptions().position(position).title(id)
                .snippet(type).icon(icon).visible(false);
    }

    /**
     * Creates a marker for a partner marker.
     *
     * @param id       Id to be embedded as the title
     * @param position
     * @return
     */
    public static MarkerOptions createPartnerMarker(String id, LatLng position) {
        final String snippet = MapFragment.TYPE_PARTNER;
        final BitmapDescriptor icon =
                BitmapDescriptorFactory.fromResource(R.drawable.marker_sandbox);
        return new MarkerOptions().position(position).title(id)
                .snippet(snippet).icon(icon).visible(false);
    }

    /**
     * Creates a marker for a label.
     *
     * @param iconFactory Reusable IconFactory
     * @param id          Id to be embedded as the title
     * @param position
     * @param label       Text to be shown on the label
     * @return
     */
    public static MarkerOptions createLabelMarker(IconGenerator iconFactory, String id, LatLng position, String label) {
        final String snippet = MapFragment.TYPE_LABEL;

        iconFactory.setTextAppearance(R.style.MapLabel);
        iconFactory.setBackground(null);

        final BitmapDescriptor icon =
                BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(label));

        return new MarkerOptions().position(position).title(id)
                .snippet(snippet).icon(icon)
                .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV())
                .visible(false);
    }

    /**
     * Creates a marker for Moscone Center.
     *
     * @param iconFactory Reusable IconFactory
     * @param position
     * @param c
     * @return
     */
    public static MarkerOptions createMosconeMarker(IconGenerator iconFactory, LatLng position, Context c) {
        final String snippet = MapFragment.TYPE_MOSCONE;
        iconFactory.setStyle(IconGenerator.STYLE_DEFAULT);

        final BitmapDescriptor icon =
                BitmapDescriptorFactory.fromResource(R.drawable.moscone_marker);

        return new MarkerOptions().position(position).title(snippet)
                .snippet(snippet).icon(icon)
                .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV())
                .visible(false);
    }

    private static String[] mapTileAssets;

    /**
     * Returns true if the given tile file exists as a local asset.
     */
    public static boolean hasTileAsset(Context context, String filename) {

        //cache the list of available files
        if (mapTileAssets == null) {
            try {
                mapTileAssets = context.getAssets().list("maptiles");
            } catch (IOException e) {
                // no assets
                mapTileAssets = new String[0];
            }
        }

        // search for given filename
        for (String s : mapTileAssets) {
            if (s.equals(filename)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copy the file from the assets to the map tiles directory if it was
     * shipped with the APK.
     */
    public static boolean copyTileAsset(Context context, String filename) {
        if (!hasTileAsset(context, filename)) {
            // file does not exist as asset
            return false;
        }

        // copy file from asset to internal storage
        try {
            InputStream is = context.getAssets().open(TILE_PATH + File.separator + filename);
            File f = getTileFile(context, filename);
            FileOutputStream os = new FileOutputStream(f);

            byte[] buffer = new byte[1024];
            int dataSize;
            while ((dataSize = is.read(buffer)) > 0) {
                os.write(buffer, 0, dataSize);
            }
            os.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Return a {@link File} pointing to the storage location for map tiles.
     */
    public static File getTileFile(Context context, String filename) {
        File folder = new File(context.getFilesDir(), TILE_PATH);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, filename);
    }


    public static void removeUnusedTiles(Context mContext, final ArrayList<String> usedTiles) {
        // remove all files are stored in the tile path but are not used
        File folder = new File(mContext.getFilesDir(), TILE_PATH);
        File[] unused = folder.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return !usedTiles.contains(filename);
            }
        });

        if (unused != null) {
            for (File f : unused) {
                f.delete();
            }
        }
    }

    public static boolean hasTile(Context mContext, String filename) {
        return getTileFile(mContext, filename).exists();
    }

    public static boolean getMyLocationEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_MYLOCATION_ENABLED, false);
    }

    public static void setMyLocationEnabled(final Context context, final boolean enableMyLocation) {
        LogUtils.LOGD(TAG, "Set my location enabled: " + enableMyLocation);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_MYLOCATION_ENABLED, enableMyLocation).commit();
    }

    private static final int MAX_DISK_CACHE_BYTES = 1024 * 1024 * 2; // 2MB

    public static DiskLruCache openDiskCache(Context c) {
        File cacheDir = new File(c.getCacheDir(), "tiles");
        try {
            return DiskLruCache.open(cacheDir, 1, 3, MAX_DISK_CACHE_BYTES);
        } catch (IOException e) {
            LOGE(TAG, "Couldn't open disk cache.");

        }
        return null;
    }

    public static void clearDiskCache(Context c) {
        DiskLruCache cache = openDiskCache(c);
        if (cache != null) {
            try {
                LOGD(TAG, "Clearing map tile disk cache");
                cache.delete();
                cache.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
