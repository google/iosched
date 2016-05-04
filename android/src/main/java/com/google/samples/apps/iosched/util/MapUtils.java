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

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.map.util.MarkerModel;

import com.jakewharton.disklrucache.DiskLruCache;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;

public class MapUtils {

    public static final String ICON_RESOURCE_PREFIX = "map_marker_";
    private static final String TILE_PATH = "maptiles";
    private static final String TAG = LogUtils.makeLogTag(MapUtils.class);
    public static final String TYPE_ICON_PREFIX = "ICON_";

    /**
     * Returns the room type for a {@link com.google.samples.apps.iosched.map.util.MarkerModel}
     * for a given String.
     */
    public static int detectMarkerType(String markerType) {
        if (TextUtils.isEmpty(markerType)) {
            return MarkerModel.TYPE_INACTIVE;
        }
        String tags = markerType.toUpperCase(Locale.US);
        if (tags.contains("SESSION")) {
            return MarkerModel.TYPE_SESSION;
        } else if (tags.contains("PLAIN")) {
            return MarkerModel.TYPE_PLAIN;
        } else if (tags.contains("LABEL")) {
            return MarkerModel.TYPE_LABEL;
        } else if (tags.contains("CODELAB")) {
            return MarkerModel.TYPE_CODELAB;
        } else if (tags.contains("SANDBOX")) {
            return MarkerModel.TYPE_SANDBOX;
        } else if (tags.startsWith(TYPE_ICON_PREFIX)) {
            return MarkerModel.TYPE_ICON;
        } else if (tags.contains("OFFICEHOURS")) {
            return MarkerModel.TYPE_OFFICEHOURS;
        } else if (tags.contains("MISC")) {
            return MarkerModel.TYPE_MISC;
        } else if (tags.contains("MOSCONE")) {
            return MarkerModel.TYPE_VENUE;
        } else if (tags.contains("INACTIVE")) {
            return MarkerModel.TYPE_INACTIVE;
        }
        return MarkerModel.TYPE_INACTIVE; // default
    }

    /**
     * Returns the drawable Id of icon to use for a room type.
     */
    public static @DrawableRes int getRoomIcon(int markerType) {
        switch (markerType) {
            case MarkerModel.TYPE_SESSION:
                return R.drawable.ic_map_session;
            case MarkerModel.TYPE_PLAIN:
                return R.drawable.ic_map_pin;
            case MarkerModel.TYPE_CODELAB:
                return R.drawable.ic_map_codelab;
            case MarkerModel.TYPE_SANDBOX:
                return R.drawable.ic_map_sandbox;
            case MarkerModel.TYPE_OFFICEHOURS:
                return R.drawable.ic_map_officehours;
            case MarkerModel.TYPE_MISC:
                return R.drawable.ic_map_misc;
            case MarkerModel.TYPE_VENUE:
                return R.drawable.ic_map_venue;
            default:
                return R.drawable.ic_map_pin;
        }
    }

    /**
     * True if the info details for this room type should only contain a title.
     */
    public static boolean hasInfoTitleOnly(int markerType) {
        return markerType == MarkerModel.TYPE_PLAIN;
    }


    /**
     * True if the info details for this room type contain a title and a list of sessions.
     */
    public static boolean hasInfoSessionList(int markerType) {
        return markerType != MarkerModel.TYPE_INACTIVE && markerType != MarkerModel.TYPE_LABEL
                && markerType != MarkerModel.TYPE_CODELAB && markerType != MarkerModel.TYPE_ICON;
    }

    /**
     * True if the info details for this room type contain a title and a list of sessions.
     */
    public static boolean hasInfoFirstDescriptionOnly(int markerType) {
        return markerType == MarkerModel.TYPE_CODELAB;
    }


    public static boolean hasInfoSessionListIcons(int markerType) {
        return markerType == MarkerModel.TYPE_SANDBOX;
    }

    /**
     * Creates a marker for a session.
     *
     * @param id Id to be embedded as the title
     */
    public static MarkerOptions createPinMarker(String id, LatLng position) {
        final BitmapDescriptor icon =
                BitmapDescriptorFactory.fromResource(R.drawable.map_marker_unselected);
        return new MarkerOptions().position(position).title(id).icon(icon).anchor(0.5f, 0.85526f)
                                  .visible(
                                          false);
    }

    /**
     * Creates a new IconGenerator for labels on the map.
     */
    public static IconGenerator getLabelIconGenerator(Context c) {
        IconGenerator iconFactory = new IconGenerator(c);
        iconFactory.setTextAppearance(R.style.MapLabel);
        iconFactory.setBackground(null);

        return iconFactory;
    }

    /**
     * Creates a marker for a label.
     *
     * @param iconFactory Reusable IconFactory
     * @param id          Id to be embedded as the title
     * @param label       Text to be shown on the label
     */
    public static MarkerOptions createLabelMarker(IconGenerator iconFactory, String id,
            LatLng position, String label) {
        final BitmapDescriptor icon =
                BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(label));

        return new MarkerOptions().position(position).title(id).icon(icon)
                                  .anchor(0.5f, 0.5f)
                                  .visible(false);
    }

    /**
     * Creates a marker for an icon. The icon is selected in {@link #getDrawableForIconType(Context,
     * String)} and anchored at the bottom center for the location.
     */
    public static MarkerOptions createIconMarker(final String iconType, final String id,
            LatLng position, Context context) {

        final int iconResource = getDrawableForIconType(context, iconType);

        if (iconResource < 1) {
            // Not a valid icon type.
            return null;
        }

        final BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(iconResource);

        return new MarkerOptions().position(position).title(id).icon(icon)
                                  .anchor(0.5f, 1f)
                                  .visible(false);
    }

    /**
     * Returns the drawable resource id for an icon marker. The resource name is generated by
     * prefixing #ICON_RESOURCE_PREFIX to the icon type in lower case. Returns 0 if no resource with
     * this name exists.
     */
    private static int getDrawableForIconType(final Context context, final String iconType) {
        if (iconType == null || !iconType.startsWith(TYPE_ICON_PREFIX)) {
            return 0;
        }

        // Return the ID of the resource that matches the iconType name.
        // If no resources matches this name, returns 0.
        return context.getResources()
                      .getIdentifier(ICON_RESOURCE_PREFIX + iconType.toLowerCase(), "drawable",
                              context.getPackageName());
    }

    /**
     * Creates a marker for the venue.
     */
    public static MarkerOptions createVenueMarker(LatLng position) {
        final String title = "VENUE";

        final BitmapDescriptor icon =
                BitmapDescriptorFactory.fromResource(R.drawable.map_marker_venue);

        return new MarkerOptions().position(position).title(title).icon(icon)
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

    /**
     * Checks whether two LatLngBounds intersect.
     *
     * @return true if the given bounds intersect.
     */
    public static boolean boundsIntersect(LatLngBounds first, LatLngBounds second) {
        // First check if the latitudes are not intersecting.
        if (first.northeast.latitude < second.southwest.latitude ||
                first.southwest.latitude > second.northeast.latitude) {
            return false;
        }

        // Next, check if the longitudes are not intersecting.
        if (first.northeast.longitude < second.southwest.longitude ||
                first.southwest.longitude > second.northeast.longitude) {
            return false;
        }

        // Both latitude and longitude are intersecting.
        return true;

    }
}
