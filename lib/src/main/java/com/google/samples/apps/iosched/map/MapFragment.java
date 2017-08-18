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

package com.google.samples.apps.iosched.map;

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.map.util.CachedTileProvider;
import com.google.samples.apps.iosched.map.util.MarkerLoadingTask;
import com.google.samples.apps.iosched.map.util.MarkerModel;
import com.google.samples.apps.iosched.map.util.TileLoadingTask;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.MapUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Shows a map of the conference venue.
 */
public class MapFragment extends com.google.android.gms.maps.SupportMapFragment implements
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    private static final String TAG = makeLogTag(MapFragment.class);

    /**
     * Extras parameter for highlighting a specific room when the map is loaded.
     */
    private static final String EXTRAS_HIGHLIGHT_ROOM = "EXTRAS_HIGHLIGHT_ROOM";

    /**
     * Area covered by the venue. Determines the viewport of the map.
     */
    private static final LatLngBounds VIEWPORT =
            new LatLngBounds(BuildConfig.MAP_VIEWPORT_NW, BuildConfig.MAP_VIEWPORT_SE);

    /**
     * Default position of the camera that shows the venue.
     */
    private static final CameraPosition VENUE_CAMERA =
            new CameraPosition.Builder().bearing(BuildConfig.MAP_DEFAULTCAMERA_BEARING)
                    .target(BuildConfig.MAP_DEFAULTCAMERA_TARGET)
                    .zoom(BuildConfig.MAP_DEFAULTCAMERA_ZOOM)
                    .tilt(BuildConfig.MAP_DEFAULTCAMERA_TILT)
                    .build();
    private boolean mMyLocationEnabled = false;

    // Tile Provider
    private CachedTileProvider mTileProvider;
    private TileOverlay mTileOverlay;


    // Markers stored by id
    protected HashMap<String, GeoJsonFeature> mMarkers = new HashMap<>();

    protected GeoJsonLayer mGeoJsonLayer;

    // Screen DPI
    private float mDPI = 0;

    private GeoJsonFeature mActiveMarker = null;
    private BitmapDescriptor ICON_ACTIVE;
    private BitmapDescriptor ICON_NORMAL;

    protected GoogleMap mMap;
    private Rect mMapInsets = new Rect();

    private String mHighlightedRoomId = null;

    private static final int TOKEN_LOADER_MARKERS = 0x1;
    private static final int TOKEN_LOADER_TILES = 0x2;
    //For Analytics tracking
    public static final String SCREEN_LABEL = "Map";


    public interface Callbacks {

        void onInfoHide();

        void onInfoShowTitle(String label, String subtitle, int roomType, String iconType);

        void onInfoShowSessionList(String roomId, String roomTitle, int roomType,
                                   String iconType);

        void onInfoShowFirstSessionTitle(String roomId, String roomTitle, int roomType,
                                         String iconType);

    }

    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public void onInfoHide() {
        }

        @Override
        public void onInfoShowTitle(String label, String subtitle, int roomType, String iconType) {
        }

        @Override
        public void onInfoShowSessionList(String roomId, String roomTitle, int roomType,
                                          String iconType) {

        }

        @Override
        public void onInfoShowFirstSessionTitle(String roomId, String roomTitle, int roomType,
                                                String iconType) {
        }

    };

    private Callbacks mCallbacks = sDummyCallbacks;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    public static MapFragment newInstance(String highlightedRoomId) {
        MapFragment fragment = new MapFragment();

        Bundle arguments = new Bundle();
        arguments.putString(EXTRAS_HIGHLIGHT_ROOM, highlightedRoomId);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static MapFragment newInstance(Bundle savedState) {
        MapFragment fragment = new MapFragment();
        fragment.setArguments(savedState);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActiveMarker != null) {
            // A marker is currently selected, restore its selection.
            outState.putString(EXTRAS_HIGHLIGHT_ROOM, mActiveMarker.getProperty("title"));
        } else {
            // No marker is selected, store the active floor if at venue.
            outState.putString(EXTRAS_HIGHLIGHT_ROOM, null);
        }

        LOGD(TAG, "Saved state: " + outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ANALYTICS SCREEN: View the Map screen
        // Contains: Nothing (Page name is a constant)
        AnalyticsHelper.sendScreenView(SCREEN_LABEL, getActivity());

        // get DPI
        mDPI = getActivity().getResources().getDisplayMetrics().densityDpi / 160f;

        // Get the arguments and restore the highlighted room or displayed floor.
        Bundle data = getArguments();
        if (data != null) {
            mHighlightedRoomId = data.getString(EXTRAS_HIGHLIGHT_ROOM, null);
        }

        getMapAsync(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mapView = super.onCreateView(inflater, container, savedInstanceState);
        setMapInsets(mMapInsets);
        return mapView;
    }

    public void setMapInsets(int left, int top, int right, int bottom) {
        mMapInsets.set(left, top, right, bottom);
        if (mMap != null) {
            mMap.setPadding(mMapInsets.left, mMapInsets.top, mMapInsets.right, mMapInsets.bottom);
        }
    }

    public void setMapInsets(Rect insets) {
        mMapInsets.set(insets.left, insets.top, insets.right, insets.bottom);
        if (mMap != null) {
            mMap.setPadding(mMapInsets.left, mMapInsets.top, mMapInsets.right, mMapInsets.bottom);
        }
    }

    /**
     * Toggles the 'my location' button. Note that the location permission <b>must</b> have already
     * been granted when this call is made.
     *
     * @param setEnabled
     */
    public void setMyLocationEnabled(final boolean setEnabled) {
        mMyLocationEnabled = setEnabled;

        if (mMap == null) {
            return;
        }
        //noinspection MissingPermission
        mMap.setMyLocationEnabled(mMyLocationEnabled);
    }

    @Override
    public void onStop() {
        super.onStop();

        closeTileCache();
    }

    /**
     * Closes the caches of all allocated tile providers.
     *
     * @see CachedTileProvider#closeCache()
     */
    private void closeTileCache() {
        try {
            if (mTileProvider != null) {
                mTileProvider.closeCache();
            }
        } catch (IOException e) {
        }

    }

    /**
     * Clears the map and initialises all map variables that hold markers and overlays.
     */
    private void clearMap() {
        // Empty the map
        if (mMap != null) {
            mMap.clear();
        }

        // Close all tile provider caches
        closeTileCache();

        // Clear all map objects
        mTileProvider = null;
        mTileOverlay = null;
        mMarkers.clear();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        // Initialise marker icons.
        ICON_ACTIVE = BitmapDescriptorFactory.fromBitmap(
                UIUtils.drawableToBitmap(getContext(), R.drawable.map_marker_selected));
        ICON_NORMAL = BitmapDescriptorFactory.fromBitmap(
                UIUtils.drawableToBitmap(getContext(), R.drawable.map_marker_unselected));

        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.maps_style));
        mMap.setIndoorEnabled(false);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setLatLngBoundsForCameraTarget(VIEWPORT);
        mMap.setMinZoomPreference(BuildConfig.MAP_VIEWPORT_MINZOOM);
        UiSettings mapUiSettings = mMap.getUiSettings();
        mapUiSettings.setZoomControlsEnabled(false);
        mapUiSettings.setMapToolbarEnabled(false);

        // This state is set via 'setMyLocationLayerEnabled.
        //noinspection MissingPermission
        mMap.setMyLocationEnabled(mMyLocationEnabled);

        // Move camera directly to the venue
        centerOnVenue(false);

        loadMapData();

        LOGD(TAG, "Map setup complete.");
    }

    /**
     * Loads markers and tiles from the content provider.
     *
     * @see #mMarkerLoader
     * @see #mTileLoader
     */
    private void loadMapData() {
        // load all markers
        LoaderManager lm = getLoaderManager();
        lm.initLoader(TOKEN_LOADER_MARKERS, null, mMarkerLoader).forceLoad();

        // load the tile overlays
        lm.initLoader(TOKEN_LOADER_TILES, null, mTileLoader).forceLoad();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException(
                    "Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;

        activity.getContentResolver().registerContentObserver(
                ScheduleContract.MapGeoJson.CONTENT_URI, true, mObserver);
        activity.getContentResolver().registerContentObserver(
                ScheduleContract.MapTiles.CONTENT_URI, true, mObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;

        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    /**
     * Moves the camera to the {@link #VENUE_CAMERA} positon.
     *
     * @param animate Animates the camera if true, otherwise it is moved
     */
    private void centerOnVenue(boolean animate) {
        CameraUpdate camera = CameraUpdateFactory.newCameraPosition(VENUE_CAMERA);
        if (animate) {
            mMap.animateCamera(camera);
        } else {
            mMap.moveCamera(camera);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        deselectActiveMarker();
        mCallbacks.onInfoHide();
    }

    private void deselectActiveMarker() {
        if (mActiveMarker == null) {
            return;
        }

        final String typeString = mActiveMarker.getProperty("type");
        final int type = MapUtils.detectMarkerType(typeString);
        GeoJsonPointStyle style = mActiveMarker.getPointStyle();

        if (type == MarkerModel.TYPE_ICON) {
            // For icon markers, use the Maputils to load the original icon again.
            final Bitmap iconBitmap = MapUtils.getIconMarkerBitmap(getContext(), typeString, false);
            if (iconBitmap != null) {
                style.setIcon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
            }
        } else if (MapUtils.useActiveMarker(type)) {
            // Change the icon back if the generic active marker was used.
            style.setIcon(ICON_NORMAL);
        }
        mActiveMarker.setPointStyle(style);
        mActiveMarker = null;
    }

    private void selectActiveMarker(GeoJsonFeature feature) {
        if (mActiveMarker == feature || feature == null) {
            return;
        }
        final String typeString = feature.getProperty("type");
        final int type = MapUtils.detectMarkerType(typeString);

        mActiveMarker = feature;
        GeoJsonPointStyle style = mActiveMarker.getPointStyle();


        if (type == MarkerModel.TYPE_ICON) {
            // For TYPE_ICON markers, use the MapUtils to generate a tinted icon.
            final Bitmap iconBitmap = MapUtils.getIconMarkerBitmap(getContext(), typeString, true);
            if (iconBitmap != null) {
                style.setIcon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
            }
        } else if (MapUtils.useActiveMarker(type)) {
            // Replace the icon of this feature with the generic active marker.
            style.setIcon(ICON_ACTIVE);
        }
        mActiveMarker.setPointStyle(style);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        final String title = marker.getTitle();
        final GeoJsonFeature feature = mMarkers.get(title);

        // Log clicks on all markers (regardless of type)
        // ANALYTICS EVENT: Click on marker on the map.
        // Contains: Marker ID (for example room UUID)
        AnalyticsHelper.sendEvent("Map", "markerclick", title);
        deselectActiveMarker();
        selectMarker(feature);

        return true;
    }

    private void selectMarker(GeoJsonFeature feature) {
        if (feature == null) {
            mCallbacks.onInfoHide();
            return;
        }

        String type = feature.getProperty("type");
        int markerType = MapUtils.detectMarkerType(type);
        String id = feature.getProperty("id");
        String title = feature.getProperty("title");
        String subtitle = feature.getProperty("description");

        if (MapUtils.hasInfoTitleOnly(markerType)) {
            // Show a basic info window with a title only
            mCallbacks.onInfoShowTitle(title, subtitle, markerType, type);
            selectActiveMarker(feature);

        } else if (MapUtils.hasInfoSessionList(markerType) || MapUtils.hasInfoSessionListIcons(markerType)) {
            // Type has sessions to display
            mCallbacks.onInfoShowSessionList(id, title, markerType, type);
            selectActiveMarker(feature);

        } else if (MapUtils.hasInfoFirstDescriptionOnly(markerType)) {
            // Display the description of the first session only
            mCallbacks.onInfoShowFirstSessionTitle(id, title, markerType, type);
            selectActiveMarker(feature);

        } else {
            // Hide the bottom sheet for unknown markers
            mCallbacks.onInfoHide();
        }

    }

    private void centerMap(LatLng position) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position,
                BuildConfig.MAP_VENUECAMERA_ZOOM));
    }

    private boolean highlightRoom(String roomId) {
        if (roomId == null) {
            return false;
        }
        // Hide the active marker.
        deselectActiveMarker();

        GeoJsonFeature highlightedFeature = mMarkers.get(roomId);
        if (highlightedFeature == null) {
            // Room not found. Deselect active marker and hide the info details anyway.
            mCallbacks.onInfoHide();

            return false;
        }
        selectMarker(highlightedFeature);
        GeoJsonPoint room = (GeoJsonPoint) highlightedFeature.getGeometry();
        centerMap(room.getCoordinates());

        return true;
    }

    private void onMarkersLoaded(JSONObject data) {
        if (data != null) {
            // Parse the JSONObject as GeoJson and add it to the map
            mGeoJsonLayer = MapUtils.processGeoJson(getContext(), mMap, data);
            if (mGeoJsonLayer == null) {
                return;
            }
            mGeoJsonLayer.addLayerToMap();
            for (GeoJsonFeature feature : mGeoJsonLayer.getFeatures()) {
                if (feature == null) {
                    break;
                }
                mMarkers.put(feature.getProperty("id"), feature);
            }
        }

        // Highlight a room if there is a pending id.
        highlightRoom(mHighlightedRoomId);
        mHighlightedRoomId = null;

    }

    /**
     * Add the first TileOverlay to the map.
     */
    private void onTilesLoaded(List<TileLoadingTask.TileEntry> list) {
        if (list.isEmpty()) {
            return;
        }
        // Get the first entry and make it visible
        TileLoadingTask.TileEntry entry = list.get(0);
        TileOverlayOptions tileOverlay = new TileOverlayOptions()
                .tileProvider(entry.provider).visible(true);

        // Store the tile overlay and provider
        mTileProvider = entry.provider;
        mTileOverlay = mMap.addTileOverlay(tileOverlay);
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(final boolean selfChange, final Uri uri) {
            if (!isAdded()) {
                return;
            }

            // Clear the map, but don't reset the camera.
            clearMap();

            // Reload data from loaders. Initialise the loaders first if they are not active yet.
            LoaderManager lm = getLoaderManager();
            lm.initLoader(TOKEN_LOADER_MARKERS, null, mMarkerLoader).forceLoad();
            lm.initLoader(TOKEN_LOADER_TILES, null, mTileLoader).forceLoad();
        }
    };


    /**
     * LoaderCallbacks for the {@link MarkerLoadingTask} that loads all markers from the database
     * as a JSONOBject, ready for processing into a GeoJsonLayer on this thread.
     *
     * @see MapUtils#processGeoJson(Context, GoogleMap, JSONObject)
     */
    private LoaderCallbacks<JSONObject> mMarkerLoader
            = new LoaderCallbacks<JSONObject>() {
        @Override
        public Loader<JSONObject> onCreateLoader(int id, Bundle args) {
            return new MarkerLoadingTask(getActivity());
        }

        @Override
        public void onLoadFinished(final Loader<JSONObject> loader,
                                   final JSONObject jsonObject) {
            onMarkersLoaded(jsonObject);
        }

        @Override
        public void onLoaderReset(Loader<JSONObject> loader) {
        }
    };

    /**
     * LoaderCallbacks for the {@link TileLoadingTask} that loads the first tile overlay for the
     * map.
     */
    private LoaderCallbacks<List<TileLoadingTask.TileEntry>> mTileLoader
            = new LoaderCallbacks<List<TileLoadingTask.TileEntry>>() {
        @Override
        public Loader<List<TileLoadingTask.TileEntry>> onCreateLoader(int id, Bundle args) {
            return new TileLoadingTask(getActivity(), mDPI);
        }

        @Override
        public void onLoadFinished(Loader<List<TileLoadingTask.TileEntry>> loader,
                                   List<TileLoadingTask.TileEntry> data) {
            onTilesLoaded(data);
        }

        @Override
        public void onLoaderReset(Loader<List<TileLoadingTask.TileEntry>> loader) {
        }
    };

}
