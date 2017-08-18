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
import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.ui.IconGenerator;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.map.util.MarkerModel;
import com.jakewharton.disklrucache.DiskLruCache;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;

public class MapUtils {

    private static final String ICON_RESOURCE_PREFIX = "map_marker_";
    private static final String TILE_PATH = "maptiles";
    private static final String TAG = LogUtils.makeLogTag(MapUtils.class);
    private static final String TYPE_ICON_PREFIX = "ICON_";

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
        } else if (tags.contains("CHAT")) {
            return MarkerModel.TYPE_CHAT;
        } else if (tags.contains("FIRSTSESSION")) {
            return MarkerModel.TYPE_FIRSTSESSION;
        } else if (tags.contains("INACTIVE")) {
            return MarkerModel.TYPE_INACTIVE;
        }
        return MarkerModel.TYPE_INACTIVE; // default
    }

    /**
     * True if the info details for this room type should only contain a title and optional
     * subtitle.
     */
    public static boolean hasInfoTitleOnly(int markerType) {
        return markerType == MarkerModel.TYPE_PLAIN ||
                markerType == MarkerModel.TYPE_OFFICEHOURS ||
                markerType == MarkerModel.TYPE_MISC ||
                markerType == MarkerModel.TYPE_SANDBOX ||
                markerType == MarkerModel.TYPE_ICON ||
                markerType == MarkerModel.TYPE_CODELAB;
    }

    /**
     * True if the info details for this room type contain a title and a list of sessions.
     */
    public static boolean hasInfoSessionList(int markerType) {
        return markerType == MarkerModel.TYPE_SESSION;
    }

    /**
     * True if the info details for this room type contain a title and the description from the
     * first scheduled session.
     */
    public static boolean hasInfoFirstDescriptionOnly(int markerType) {
        return markerType == MarkerModel.TYPE_FIRSTSESSION;
    }

    /**
     * True if the info details for this room type contain a title and a list of sessions, with
     * each row prefixed with the icon for this room.
     */
    public static boolean hasInfoSessionListIcons(int markerType) {
        return markerType == MarkerModel.TYPE_CHAT;
    }

    /**
     * True if the marker for this feature should be changed to a generic "active" marker when
     * clicked, and changed back to the generic marker when deselected.
     */
    public static boolean useActiveMarker(int type) {
        return type != MarkerModel.TYPE_ICON && type != MarkerModel.TYPE_LABEL;
    }

    /**
     * Creates a GeoJsonPointStyle for a session.
     *
     * @param title Id to be embedded as the title
     */
    public static GeoJsonPointStyle createPinMarker(@NonNull Context context, String title) {
        final BitmapDescriptor icon =
                BitmapDescriptorFactory.fromBitmap(
                        UIUtils.drawableToBitmap(context, R.drawable.map_marker_unselected));
        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
        pointStyle.setTitle(title);
        pointStyle.setIcon(icon);
        pointStyle.setVisible(false);
        pointStyle.setAnchor(0.5f, 0.85526f);
        return pointStyle;
    }

    /**
     * Creates a new IconGenerator for labels on the map.
     */
    private static IconGenerator getLabelIconGenerator(Context c) {
        IconGenerator iconFactory = new IconGenerator(c);
        iconFactory.setTextAppearance(R.style.TextApparance_Map_Label);
        iconFactory.setBackground(null);
        return iconFactory;
    }

    /**
     * Creates a GeoJsonPointStyle for a label.
     *
     * @param iconFactory Reusable IconFactory
     * @param title          Id to be embedded as the title
     * @param label       Text to be shown on the label
     */
    private static GeoJsonPointStyle createLabelMarker(IconGenerator iconFactory, String title,
                                                       String label) {
        final BitmapDescriptor icon =
                BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(label));
        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
        pointStyle.setAnchor(0.5f, 0.5f);
        pointStyle.setTitle(title);
        pointStyle.setIcon(icon);
        pointStyle.setVisible(false);
        return pointStyle;
    }

    /**
     * Creates a GeoJsonPointStyle for an icon. The icon is selected
     * in {@link #getDrawableForIconType(Context, String)} and anchored
     * at the bottom center for the location. When isActive is set to true, the icon is tinted.
     */
    private static GeoJsonPointStyle createIconMarker(final String iconType, final String title,
                                                      boolean isActive, Context context) {
        final Bitmap iconBitmap = getIconMarkerBitmap(context, iconType, isActive);
        final BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(iconBitmap);
        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
        pointStyle.setTitle(title);
        pointStyle.setVisible(false);
        pointStyle.setIcon(icon);
        pointStyle.setAnchor(0.5f, 1f);
        return pointStyle;
    }

    /**
     * Loads the marker icon for this ICON_TYPE marker.
     * <p>
     * If isActive is set, the marker is tinted. See {@link UIUtils#tintBitmap(Bitmap, int)}.
     */
    public static Bitmap getIconMarkerBitmap(Context context, String iconType, boolean isActive) {
        final int iconResource = getDrawableForIconType(context, iconType);

        if (iconResource < 1) {
            // Not a valid icon type.
            return null;
        }
        Bitmap iconBitmap = UIUtils.drawableToBitmap(context, iconResource);
        if (isActive) {
            iconBitmap = UIUtils.tintBitmap(iconBitmap,
                    ContextCompat.getColor(context, R.color.map_active_icon_tint));
        }
        return iconBitmap;
    }

    /**
     * Returns the drawable resource id for an icon marker. The resource name is generated by
     * prefixing #ICON_RESOURCE_PREFIX to the icon type in lower case. Returns 0 if no resource with
     * this name exists.
     */
    public static @DrawableRes int getDrawableForIconType(@NonNull Context context,
                                                          @NonNull String iconType) {
        if (iconType == null || !iconType.startsWith(TYPE_ICON_PREFIX)) {
            return 0;
        }

        // Return the ID of the resource that matches the iconType name.
        // If no resources matches this name, returns 0.
        //noinspection DefaultLocale
        return context.getResources().getIdentifier(ICON_RESOURCE_PREFIX + iconType.toLowerCase(),
                "drawable", context.getPackageName());
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

    public static GeoJsonLayer processGeoJson(Context context, GoogleMap mMap, JSONObject j) {
        GeoJsonLayer layer = new GeoJsonLayer(mMap, j);

        Iterator<GeoJsonFeature> iterator = layer.getFeatures().iterator();
        final IconGenerator labelIconGenerator = MapUtils.getLabelIconGenerator(context);
        while (iterator.hasNext()) {
            GeoJsonFeature feature = iterator.next();

            // get data
            final String id = feature.getProperty("id");
            final String typeString = feature.getProperty("type");
            final int type = MapUtils.detectMarkerType(typeString);
            final String label = feature.getProperty("title");

            GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
            if (type == MarkerModel.TYPE_LABEL) {
                // Label markers contain the label as its icon
                pointStyle = MapUtils.createLabelMarker(labelIconGenerator, id, label);
            } else if (type == MarkerModel.TYPE_ICON) {
                // An icon marker is mapped to a drawable based on its full type name
                pointStyle = MapUtils.createIconMarker(typeString, id, false, context);
            } else if (type != MarkerModel.TYPE_INACTIVE) {
                // All other markers (that are not inactive) contain a pin icon
                pointStyle = MapUtils.createPinMarker(context, id);
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
    }
}
