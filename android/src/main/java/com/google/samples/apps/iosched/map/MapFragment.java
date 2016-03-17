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
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.map.util.CachedTileProvider;
import com.google.samples.apps.iosched.map.util.MarkerLoadingTask;
import com.google.samples.apps.iosched.map.util.MarkerModel;
import com.google.samples.apps.iosched.map.util.TileLoadingTask;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Shows a map of the conference venue.
 */
public class MapFragment extends com.google.android.gms.maps.MapFragment implements
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback,
        GoogleMap.OnCameraChangeListener {

    /**
     * Extras parameter for highlighting a specific room when the map is loaded.
     */
    private static final String EXTRAS_HIGHLIGHT_ROOM = "EXTRAS_HIGHLIGHT_ROOM";

    /**
     * Extras parameter for displaying a specific floor when the map is loaded.
     */
    private static final String EXTRAS_ACTIVE_FLOOR = "EXTRAS_ACTIVE_FLOOR";

    /**
     * Area covered by the venue. Determines if the venue is currently visible on screen.
     */
    private static final LatLngBounds VENUE_AREA =
            new LatLngBounds(BuildConfig.MAP_AREA_NW, BuildConfig.MAP_AREA_SE);

    /**
     * Default position of the camera that shows the venue.
     */
    private static final CameraPosition VENUE_CAMERA =
            new CameraPosition.Builder().bearing(BuildConfig.MAP_DEFAULTCAMERA_BEARING)
                                        .target(BuildConfig.MAP_DEFAULTCAMERA_TARGET)
                                        .zoom(BuildConfig.MAP_DEFAULTCAMERA_ZOOM)
                                        .tilt(BuildConfig.MAP_DEFAULTCAMERA_TILT)
                                        .build();

    /**
     * Value that denotes an invalid floor.
     */
    private static final int INVALID_FLOOR = Integer.MIN_VALUE;

    /**
     * Estimated number of floors used to initialise data structures with appropriate capacity.
     */
    private static final int INITIAL_FLOOR_COUNT = 1;

    /**
     * Default floor level to display. In the current implementation there is no support to switch
     * floor levels, so this is always set to 0.
     */
    private static final int VENUE_DEFAULT_LEVEL_INDEX = 0;

    private static final String TAG = makeLogTag(MapFragment.class);
    private boolean mMyLocationEnabled = false;

    // Tile Providers
    private SparseArray<CachedTileProvider> mTileProviders =
            new SparseArray<>(INITIAL_FLOOR_COUNT);
    private SparseArray<TileOverlay> mTileOverlays =
            new SparseArray<>(INITIAL_FLOOR_COUNT);


    // Markers stored by id
    protected HashMap<String, MarkerModel> mMarkers = new HashMap<>();
    // Markers stored by floor
    protected SparseArray<ArrayList<Marker>> mMarkersFloor =
            new SparseArray<>(INITIAL_FLOOR_COUNT);

    // Screen DPI
    private float mDPI = 0;

    // currently displayed floor
    private int mFloor = INVALID_FLOOR;

    /**
     * Indicates if the venue is active and its markers and floor plan is being displayed. Set to
     * false by default, as the venue marker is shown first.
     */
    private boolean mVenueIsActive = false;


    private Marker mActiveMarker = null;
    private BitmapDescriptor ICON_ACTIVE;
    private BitmapDescriptor ICON_NORMAL;

    private Marker mVenueMaker = null;

    protected GoogleMap mMap;
    private Rect mMapInsets = new Rect();

    private String mHighlightedRoomId = null;
    private MarkerModel mHighlightedRoom = null;

    private int mInitialFloor = VENUE_DEFAULT_LEVEL_INDEX;

    private static final int TOKEN_LOADER_MARKERS = 0x1;
    private static final int TOKEN_LOADER_TILES = 0x2;
    //For Analytics tracking
    public static final String SCREEN_LABEL = "Map";


    public interface Callbacks {

        void onInfoHide();

        void onInfoShowVenue();

        void onInfoShowTitle(String label, int roomType);

        void onInfoShowSessionlist(String roomId, String roomTitle, int roomType);

        void onInfoShowFirstSessionTitle(String roomId, String roomTitle, int roomType);

    }

    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public void onInfoHide() {
        }

        @Override
        public void onInfoShowVenue() {
        }

        @Override
        public void onInfoShowTitle(String label, int roomType) {
        }

        @Override
        public void onInfoShowSessionlist(String roomId, String roomTitle, int roomType) {
        }

        @Override
        public void onInfoShowFirstSessionTitle(String roomId, String roomTitle, int roomType) {
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
            outState.putString(EXTRAS_HIGHLIGHT_ROOM, mActiveMarker.getTitle());
            outState.putInt(EXTRAS_ACTIVE_FLOOR, INVALID_FLOOR);
        } else {
            // No marker is selected, store the active floor if at venue.
            outState.putInt(EXTRAS_ACTIVE_FLOOR, mFloor);
            outState.putString(EXTRAS_HIGHLIGHT_ROOM, null);
        }

        LOGD(TAG, "Saved state: " + outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ANALYTICS SCREEN: View the Map screen
        // Contains: Nothing (Page name is a constant)
        AnalyticsHelper.sendScreenView(SCREEN_LABEL);

        // get DPI
        mDPI = getActivity().getResources().getDisplayMetrics().densityDpi / 160f;

        // Get the arguments and restore the highlighted room or displayed floor.
        Bundle data = getArguments();
        if (data != null) {
            mHighlightedRoomId = data.getString(EXTRAS_HIGHLIGHT_ROOM, null);
            mInitialFloor = data.getInt(EXTRAS_ACTIVE_FLOOR, VENUE_DEFAULT_LEVEL_INDEX);
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
        for (int i = 0; i < mTileProviders.size(); i++) {
            try {
                mTileProviders.valueAt(i).closeCache();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Clears the map and initialises all map variables that hold markers and overlays.
     */
    private void clearMap() {
        if (mMap != null) {
            mMap.clear();
        }

        // Close all tile provider caches
        closeTileCache();

        // Clear all map elements
        mTileProviders.clear();
        mTileOverlays.clear();

        mMarkers.clear();
        mMarkersFloor.clear();

        mFloor = INVALID_FLOOR;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        // Initialise marker icons.
        ICON_ACTIVE = BitmapDescriptorFactory.fromResource(R.drawable.map_marker_selected);
        ICON_NORMAL = BitmapDescriptorFactory.fromResource(R.drawable.map_marker_unselected);

        mMap = googleMap;
        mMap.setIndoorEnabled(false);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnCameraChangeListener(this);
        UiSettings mapUiSettings = mMap.getUiSettings();
        mapUiSettings.setZoomControlsEnabled(false);
        mapUiSettings.setMapToolbarEnabled(false);

        // This state is set via 'setMyLocationLayerEnabled.
        //noinspection MissingPermission
        mMap.setMyLocationEnabled(mMyLocationEnabled);

        addVenueMarker();

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

    private void addVenueMarker() {
        mVenueMaker = mMap.addMarker(
                MapUtils.createVenueMarker(BuildConfig.MAP_VENUEMARKER).visible(false));
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
                ScheduleContract.MapMarkers.CONTENT_URI, true, mObserver);
        activity.getContentResolver().registerContentObserver(
                ScheduleContract.MapTiles.CONTENT_URI, true, mObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;

        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public void onCameraChange(final CameraPosition cameraPosition) {
        boolean isVenueInFocus = cameraPosition.zoom >= (double) BuildConfig.MAP_MAXRENDERED_ZOOM
                && isVenueVisible();

        // Check if the camera is focused on the venue. Trigger a callback if the state has changed.
        if (isVenueInFocus && !mVenueIsActive) {
            onFocusVenue();
            mVenueIsActive = true;
        } else if (!isVenueInFocus && mVenueIsActive) {
            onDefocusVenue();
            mVenueIsActive = false;
        }
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

    /**
     * Switches the displayed floor for which elements are displayed. If the map is not initialised
     * yet or no data has been loaded, nothing will be displayed. If an invalid floor is specified
     * and elements are currently on the map, all visible elements will be hidden.
     *
     * @param floor index of the floor to display. It requires an overlay or least one Marker to be
     *              valid.
     */
    private void showFloorElementsIndex(int floor) {
        LOGD(TAG, "Show floor " + floor);

        // Hide previous floor elements if the floor has changed
        if (mFloor != floor) {
            setFloorElementsVisible(mFloor, false);
        }

        mFloor = floor;

        if (isValidFloor(mFloor)) {
            // Always hide the venue marker if a floor is shown
            mVenueMaker.setVisible(false);
            setFloorElementsVisible(mFloor, true);
        } else {
            // Show venue marker if at an invalid floor
            mVenueMaker.setVisible(true);
        }
    }

    /**
     * Change the visibility of all Markers and TileOverlays for a floor.
     */
    private void setFloorElementsVisible(int floor, boolean visible) {
        // Overlays
        final TileOverlay overlay = mTileOverlays.get(floor);
        if (overlay != null) {
            overlay.setVisible(visible);
        }

        // Markers
        final ArrayList<Marker> markers = mMarkersFloor.get(floor);
        if (markers != null) {
            for (Marker m : markers) {
                m.setVisible(visible);
            }
        }
    }

    /**
     * A floor is valid if the venue contains that floor. It is not required for a floor to have a
     * tile overlay AND markers.
     */
    private boolean isValidFloor(int floor) {
        return mTileOverlays.get(floor) != null || mMarkersFloor.get(floor) != null;
    }

    /**
     * Display map features if at the venue. This explicitly enables all elements that should be
     * displayed at the default floor.
     *
     * @see #isVenueVisible()
     */
    private void enableMapElements() {
        if (isVenueVisible()) {
            showFloorElementsIndex(VENUE_DEFAULT_LEVEL_INDEX);
        }
    }

    private void onDefocusVenue() {
        // Hide all markers and tile overlays
        deselectActiveMarker();
        showFloorElementsIndex(INVALID_FLOOR);
        mCallbacks.onInfoShowVenue();
    }

    private void onFocusVenue() {
        // Highlight a room if argument is set and it exists, otherwise show the default floor
        if (mHighlightedRoomId != null && mMarkers.containsKey(mHighlightedRoomId)) {
            highlightRoom(mHighlightedRoomId);
            onFloorActivated(mHighlightedRoom.floor);
            // Reset highlighted room because it has just been displayed.
            mHighlightedRoomId = null;
        } else {
            // Hide the bottom sheet that is displaying the venue details at this point
            mCallbacks.onInfoHide();
            // Switch to the default level for the venue and reset its value
            onFloorActivated(mInitialFloor);
        }
        mInitialFloor = VENUE_DEFAULT_LEVEL_INDEX;
    }

    public boolean isVenueVisible() {
        if (mMap == null) {
            return false;
        }

        LatLngBounds visibleBounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        return MapUtils.boundsIntersect(visibleBounds, VENUE_AREA);
    }

    /**
     * Called when a floor level in the venue building has been activated. If a room is to be
     * highlighted, the map is centered and its marker is activated.
     */
    private void onFloorActivated(int activeLevelIndex) {
        if (mHighlightedRoom != null && mFloor == mHighlightedRoom.floor) {
            // A room highlight is pending. Highlight the marker and display info details.
            onMarkerClick(mHighlightedRoom.marker);
            centerMap(mHighlightedRoom.marker.getPosition());

            // Remove the highlight room flag, because the room has just been highlighted.
            mHighlightedRoom = null;
            mHighlightedRoomId = null;
        } else if (mFloor != activeLevelIndex) {
            // Deselect and hide the info details.
            deselectActiveMarker();
            mCallbacks.onInfoHide();
        }

        // Show map elements for this floor
        showFloorElementsIndex(activeLevelIndex);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        deselectActiveMarker();
        mCallbacks.onInfoHide();
    }

    private void deselectActiveMarker() {
        if (mActiveMarker != null) {
            mActiveMarker.setIcon(ICON_NORMAL);
            mActiveMarker = null;
        }
    }

    private void selectActiveMarker(Marker marker) {
        if (mActiveMarker == marker) {
            return;
        }
        if (marker != null) {
            mActiveMarker = marker;
            mActiveMarker.setIcon(ICON_ACTIVE);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        final String title = marker.getTitle();
        final MarkerModel model = mMarkers.get(title);

        // Log clicks on all markers (regardless of type)
        // ANALYTICS EVENT: Click on marker on the map.
        // Contains: Marker ID (for example room UUID)
        AnalyticsHelper.sendEvent("Map", "markerclick", title);

        deselectActiveMarker();

        // The venue marker can be compared directly.
        // For all other markers the model needs to be looked up first.
        if (marker.equals(mVenueMaker)) {
            // Return camera to the venue
            LOGD(TAG, "Clicked on the venue marker, return to initial display.");
            centerOnVenue(true);

        } else if (model != null && MapUtils.hasInfoTitleOnly(model.type)) {
            // Show a basic info window with a title only
            mCallbacks.onInfoShowTitle(model.label, model.type);
            selectActiveMarker(marker);

        } else if (model != null && MapUtils.hasInfoSessionList(model.type)) {
            // Type has sessions to display
            mCallbacks.onInfoShowSessionlist(model.id, model.label, model.type);
            selectActiveMarker(marker);

        } else if (model != null && MapUtils.hasInfoFirstDescriptionOnly(model.type)) {
            // Display the description of the first session only
            mCallbacks.onInfoShowFirstSessionTitle(model.id, model.label, model.type);
            selectActiveMarker(marker);

        } else {
            // Hide the bottom sheet for unknown markers
            mCallbacks.onInfoHide();
        }

        return true;
    }

    private void centerMap(LatLng position) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position,
                BuildConfig.MAP_VENUECAMERA_ZOOM));
    }

    private void highlightRoom(String roomId) {
        MarkerModel m = mMarkers.get(roomId);
        if (m != null) {
            mHighlightedRoom = m;
            showFloorElementsIndex(m.floor);
        }
    }

    private void onMarkersLoaded(List<MarkerLoadingTask.MarkerEntry> list) {
        if (list != null) {
            for (MarkerLoadingTask.MarkerEntry entry : list) {

                // Skip incomplete entries
                if (entry.options == null || entry.model == null) {
                    break;
                }

                // Add marker to the map
                Marker m = mMap.addMarker(entry.options);
                MarkerModel model = entry.model;
                model.marker = m;

                // Store the marker and its model
                ArrayList<Marker> markerList = mMarkersFloor.get(model.floor);
                if (markerList == null) {
                    // Initialise the list of Markers for this floor
                    markerList = new ArrayList<>();
                    mMarkersFloor.put(model.floor, markerList);
                }
                markerList.add(m);
                mMarkers.put(model.id, model);
            }
        }

        enableMapElements();
    }

    private void onTilesLoaded(List<TileLoadingTask.TileEntry> list) {
        if (list != null) {
            // Display tiles if they have been loaded, skip them otherwise but display the rest of
            // the map.
            for (TileLoadingTask.TileEntry entry : list) {
                TileOverlayOptions tileOverlay = new TileOverlayOptions()
                        .tileProvider(entry.provider).visible(false);

                // Store the tile overlay and provider
                mTileProviders.put(entry.floor, entry.provider);
                mTileOverlays.put(entry.floor, mMap.addTileOverlay(tileOverlay));
            }
        }

        enableMapElements();
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
            addVenueMarker();

            // Reload data from loaders. Initialise the loaders first if they are not active yet.
            LoaderManager lm = getActivity().getLoaderManager();
            lm.initLoader(TOKEN_LOADER_MARKERS, null, mMarkerLoader).forceLoad();
            lm.initLoader(TOKEN_LOADER_TILES, null, mTileLoader).forceLoad();
        }
    };


    /**
     * LoaderCallbacks for the {@link MarkerLoadingTask} that loads all markers for the map.
     */
    private LoaderCallbacks<List<MarkerLoadingTask.MarkerEntry>> mMarkerLoader
            = new LoaderCallbacks<List<MarkerLoadingTask.MarkerEntry>>() {
        @Override
        public Loader<List<MarkerLoadingTask.MarkerEntry>> onCreateLoader(int id, Bundle args) {
            return new MarkerLoadingTask(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<MarkerLoadingTask.MarkerEntry>> loader,
                List<MarkerLoadingTask.MarkerEntry> data) {
            onMarkersLoaded(data);
        }

        @Override
        public void onLoaderReset(Loader<List<MarkerLoadingTask.MarkerEntry>> loader) {
        }
    };

    /**
     * LoaderCallbacks for the {@link TileLoadingTask} that loads all tile overlays for the map.
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
