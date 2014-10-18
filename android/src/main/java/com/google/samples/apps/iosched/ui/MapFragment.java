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

package com.google.samples.apps.iosched.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.ui.IconGenerator;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.MapUtils;
import com.google.samples.apps.iosched.util.PrefUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Shows a map of the conference venue.
 */
public class MapFragment extends com.google.android.gms.maps.MapFragment implements
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnIndoorStateChangeListener, LoaderCallbacks<Cursor>,
        GoogleMap.OnMapLoadedCallback {

    private static final LatLng MOSCONE =  new LatLng(37.783107, -122.403789);
    private static final LatLng MOSCONE_CAMERA =  new LatLng(37.78308931536713, -122.40409433841705);

    // Initial camera zoom
    private static final float CAMERA_ZOOM = 18.19f;
    private static final float CAMERA_BEARING = 234.2f;

    private static final int INVALID_FLOOR = Integer.MIN_VALUE;

    // Estimated number of floors used to initialise data structures with appropriate capacity
    private static final int INITIAL_FLOOR_COUNT = 3;

    // Default level (index of level in IndoorBuilding object for Moscone)
    private static final int MOSCONE_DEFAULT_LEVEL_INDEX = 1;

    private static final String TAG = makeLogTag(MapFragment.class);

    // Marker types
    public static final String TYPE_SESSION = "session";
    public static final String TYPE_PARTNER = "partner";
    public static final String TYPE_PLAIN_SESSION = "plainsession";
    public static final String TYPE_LABEL = "label";
    public static final String TYPE_MOSCONE = "moscone";
    public static final String TYPE_INACTIVE = "inactive";

    // Tile Providers
    private SparseArray<TileProvider> mTileProviders =
            new SparseArray<TileProvider>(INITIAL_FLOOR_COUNT);
    private SparseArray<TileOverlay> mTileOverlays =
            new SparseArray<TileOverlay>(INITIAL_FLOOR_COUNT);

    private DiskLruCache mTileCache;

    // Markers stored by id
    private HashMap<String, MarkerModel> mMarkers = new HashMap<String, MarkerModel>();
    // Markers stored by floor
    private SparseArray<ArrayList<Marker>> mMarkersFloor =
            new SparseArray<ArrayList<Marker>>(INITIAL_FLOOR_COUNT);

    private boolean mOverlaysLoaded = false;
    private boolean mMarkersLoaded = false;

    // Cached size of view
    private int mWidth, mHeight;

    // Padding for #centerMap
    private int mShiftRight = 0;
    private int mShiftTop = 0;

    // Screen DPI
    private float mDPI = 0;

    // Indoor maps representation of Moscone Center
    private IndoorBuilding mMosconeBuilding = null;

    // currently displayed floor
    private int mFloor = INVALID_FLOOR;


    // Show markers at default zoom level
    private boolean mShowMarkers = true;

    private boolean mAtMoscone = false;
    private Marker mMosconeMaker = null;

    private GoogleMap mMap;
    private Rect mMapInsets = new Rect();

    private MapInfoWindowAdapter mInfoAdapter;

    private IconGenerator mIconGenerator;

    // Handler for info window queries
    private AsyncQueryHandler mQueryHandler;

    //For Analytics tracking
    public static final String SCREEN_LABEL = "Map";

    public interface Callbacks {
        public void onSessionRoomSelected(String roomId, String roomTitle);
        public void onShowPartners();
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onSessionRoomSelected(String roomId, String roomTitle) {
        }

        @Override
        public void onShowPartners() {}
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    private String mHighlightedRoom = null;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* [ANALYTICS:SCREEN]
         * TRIGGER:   View the Map screen.
         * LABEL:     'Map'
         * [/ANALYTICS]
         */
        AnalyticsManager.sendScreenView(SCREEN_LABEL);

        // get DPI
        mDPI = getActivity().getResources().getDisplayMetrics().densityDpi / 160f;

        // setup the query handler to populate info windows
        mQueryHandler = createInfowindowHandler(getActivity().getContentResolver());

        mIconGenerator = new IconGenerator(getActivity());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View mapView = super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_map, container, false);
        FrameLayout layout = (FrameLayout) v.findViewById(R.id.map_container);

        layout.addView(mapView, 0);

        // get the height and width of the view
        mapView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @SuppressWarnings("deprecation")
                    @SuppressLint("NewApi")
                    @Override
                    public void onGlobalLayout() {
                        final View v = getView();
                        mHeight = v.getHeight();
                        mWidth = v.getWidth();

                        // also requires width and height
                        enableMapElements();

                        if (v.getViewTreeObserver().isAlive()) {
                            // remove this layout listener
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                v.getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);
                            } else {
                                v.getViewTreeObserver()
                                        .removeGlobalOnLayoutListener(this);
                            }
                        }
                    }
                }
        );

        clearMap();

        if (mMap == null) {
            setupMap(true);
        }

        setMapInsets(mMapInsets);

        // load all markers
        LoaderManager lm = getLoaderManager();
        lm.initLoader(MarkerQuery._TOKEN, null, this);

        // load the tile overlays
        lm.initLoader(OverlayQuery._TOKEN, null, this);

        return v;
    }

    public void setMapInsets(Rect insets) {
        mMapInsets.set(insets);
        if (mMap != null) {
            mMap.setPadding(mMapInsets.left, mMapInsets.top, mMapInsets.right, mMapInsets.bottom);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Open tile disk cache
        mTileCache = MapUtils.openDiskCache(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mTileCache != null) {
            try {
                mTileCache.close();
            } catch (IOException e) {
                // Ignore
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

        // Clear all map elements
        mTileProviders.clear();
        mTileOverlays.clear();

        mMarkers.clear();
        mMarkersFloor.clear();

        mFloor = INVALID_FLOOR;
    }

    private void setupMap(boolean resetCamera) {
        mInfoAdapter = new MapInfoWindowAdapter(LayoutInflater.from(getActivity()), getResources(),
                mMarkers);
        mMap = getMap();

        // Add a Marker for Moscone
        mMosconeMaker = mMap.addMarker(MapUtils.createMosconeMarker(mIconGenerator,
                MOSCONE, getActivity()).visible(false));

        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnIndoorStateChangeListener(this);
        mMap.setOnMapLoadedCallback(this);
        mMap.setInfoWindowAdapter(mInfoAdapter);

        if (resetCamera) {
            // Move camera directly to Moscone
           centerOnMoscone(false);
        }

        mMap.setIndoorEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.setMyLocationEnabled(false);

        Bundle data = getArguments();
        if (data != null && data.containsKey(BaseMapActivity.EXTRA_ROOM)) {
            mHighlightedRoom = data.getString(BaseMapActivity.EXTRA_ROOM);
        }

        LOGD(TAG, "Map setup complete.");
    }

    @Override
    public void onMapLoaded() {
        // Enable indoor maps once the map has loaded for the first time
        // Workaround for issue where floor picker is not displayed for the first active building
        mMap.setIndoorEnabled(true);
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

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;

        getActivity().getContentResolver().unregisterContentObserver(mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    /**
     * Moves the camera to Moscone Center (as defined in {@link #MOSCONE} and {@link #CAMERA_ZOOM}.
     * @param animate Animates the camera if true, otherwise it is moved
     */
    private void centerOnMoscone(boolean animate) {
        CameraUpdate camera = CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder().bearing(CAMERA_BEARING).target(MOSCONE_CAMERA).zoom(CAMERA_ZOOM).tilt(0f).build());
        if (animate) {
            mMap.animateCamera(camera);
        } else {
            mMap.moveCamera(camera);
        }
    }

    /**
     * Switches the displayed floor for which elements are displayed.
     * If the map is not initialised yet or no data has been loaded, nothing will be displayed.
     * If an invalid floor is specified and elements are currently on the map, all visible
     * elements will be hidden.
     * If this floor is not active for the indoor building, it is made active.
     *
     * @param floor index of the floor to display. It requires an overlay and at least one Marker to
     *              be defined for it and it has to be a valid index in the
     *              {@link com.google.android.gms.maps.model.IndoorBuilding} object that
     *              describes Moscone.
     */
    private void showFloorElementsIndex(int floor) {
        LOGD(TAG, "Show floor " + floor);

        if (mFloor == floor) {
            return;
        }

        // Hide previous floor elements if it is valid
        if (isValidFloor(mFloor)) {
            setFloorElementsVisible(mFloor, false);
        }

        mFloor = floor;

        if (isValidFloor(mFloor) && mAtMoscone) {
            // Always hide the Moscone marker if a floor is shown
            mMosconeMaker.setVisible(false);
            setFloorElementsVisible(mFloor, true);
        } else {
            // Show Moscone marker if this is not a valid floor
            mMosconeMaker.setVisible(true);
        }
    }

    /**
     * Change the active floor of Moscone Center
     * to the given floor index. See {@link #showFloorElementsIndex(int)}.
     *
     * @param floor Index of the floor to show.
     * @see #showFloorElementsIndex(int)
     */
    private void showFloorIndex(int floor) {
        if (isValidFloor(floor) && mAtMoscone) {

            if (mMap.getFocusedBuilding().getActiveLevelIndex() == floor) {
                // This floor is already active, show its elements
                showFloorElementsIndex(floor);
            } else {
                // This floor is not shown yet, switch to this floor on the map
                mMap.getFocusedBuilding().getLevels().get(floor).activate();
            }

        } else {
            LOGD(TAG, "Can't show floor index " + floor + ".");
        }
    }

    /**
     * Change the visibility of all Markers and TileOverlays for a floor.
     *
     * @param floor
     * @param visible
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
     * A floor is valid if it has tiles, overlays and the Moscone building contains that floor.
     *
     * @param floor
     * @return
     */
    private boolean isValidFloor(int floor) {
        return mMarkersFloor.get(floor) != null && mTileOverlays.get(floor) != null
                && floor < mMosconeBuilding.getLevels().size();
    }

    /**
     * Display map features when all loaders have finished.
     * This ensures that only complete data for the correct floor is shown.
     */
    private void enableMapElements() {
        if (mOverlaysLoaded && mMarkersLoaded && mWidth > 0 && mHeight > 0
                && mMosconeBuilding != null) {
            // Enable indoor map again if data has been reloaded
            // Tiles and markers are enabled in the indoor API callbacks
            mMap.setIndoorEnabled(true);

            // If already focused on Moscone, show the elements for the active floor
            // (The indoor callbacks will not be received in this case.)
            if(mAtMoscone){
                showFloorIndex(mMosconeBuilding.getActiveLevelIndex());
            }
        }
    }

    private void onDefocusMoscone() {
        // Hide all markers and tile overlays
        showFloorElementsIndex(INVALID_FLOOR);
    }

    private void onFocusMoscone() {
        // Highlight a room if argument is set and it exists, otherwise show the default floor
        if (mHighlightedRoom != null && mMarkers.containsKey(mHighlightedRoom)) {
            highlightRoom(mHighlightedRoom);
            mHighlightedRoom = null;
        } else {
            // Switch to the default level for Moscone
                showFloorIndex(MOSCONE_DEFAULT_LEVEL_INDEX);
        }




    }

    @Override
    public void onIndoorBuildingFocused() {
        IndoorBuilding building = mMap.getFocusedBuilding();

        if (building != null && mMosconeBuilding == null
                && mMap.getProjection().getVisibleRegion().latLngBounds.contains(MOSCONE)) {
            // Store the first active building. This will always be Moscone
            mMosconeBuilding = building;
            enableMapElements();
        }

        if (building != null && mMosconeBuilding.equals(building)) {
            // Map is focused on Moscone Center
            mAtMoscone = true;
            onFocusMoscone();
        } else if(mAtMoscone){
            // Map is no longer focused on Moscone Center
            mAtMoscone = false;
            onDefocusMoscone();
        }
    }

    @Override
    public void onIndoorLevelActivated(IndoorBuilding indoorBuilding) {
        if (indoorBuilding.equals(mMosconeBuilding)) {
            // Show map elements for this floor if at Moscone
            showFloorElementsIndex(indoorBuilding.getActiveLevelIndex());
        }
    }

    void addTileProvider(int floor, File f) {
        if (!f.exists()) {
            return;
        }
        TileProvider provider;
        try {
            SVGTileProvider svgProvider = new SVGTileProvider(f, mDPI);
            if (mTileCache == null) {
                // Use the SVGTileProvider directly as the TileProvider without a cache
                provider = svgProvider;
            } else {
                // Wrap the SVGTileProvider ina a CachedTileProvider for caching on disk
                provider = new CachedTileProvider(Integer.toString(floor), svgProvider, mTileCache);
            }
        } catch (IOException e) {
            LOGD(TAG, "Could not create Tile Provider.");
            return;
        }

        TileOverlayOptions tileOverlay = new TileOverlayOptions()
                .tileProvider(provider).visible(false);

        mTileProviders.put(floor, provider);
        mTileOverlays.put(floor, mMap.addTileOverlay(tileOverlay));
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final String snippet = marker.getSnippet();
        final String title = marker.getTitle();

        // Log clicks on session and partner info windows
        if (TYPE_SESSION.equals(snippet) || TYPE_PARTNER.equals(snippet)) {
            /* [ANALYTICS:EVENT]
             * TRIGGER:   Click on a pin that represents a room on the map.
             * CATEGORY:  'Map'
             * ACTION:    'infoclick'
             * LABEL:     room ID (for example "Room_10")
             * [/ANALYTICS]
             */
            AnalyticsManager.sendEvent("Map", "infoclick", title, 0L);
        }
        if (TYPE_SESSION.equals(snippet)) {
            mCallbacks.onSessionRoomSelected(title, mMarkers.get(title).label);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        final String snippet = marker.getSnippet();
        final String title = marker.getTitle();

        // Log clicks on session and partner markers
        if (TYPE_SESSION.equals(snippet) || TYPE_PARTNER.equals(snippet)) {
            /* [ANALYTICS:EVENT]
             * TRIGGER:   Click on a marker on the map.
             * CATEGORY:  'Map'
             * ACTION:    'markerclick'
             * LABEL:     marker ID (for example room UUID or partner marker id)
             * [/ANALYTICS]
             */
            AnalyticsManager.sendEvent("Map", "markerclick", title, 0L);
        }

        if(marker.equals(mMosconeMaker)){
            // Return camera to Moscone
            LOGD(TAG, "Clicked on Moscone marker, return to initial display.");
            centerOnMoscone(true);
        } else if (TYPE_SESSION.equals(snippet)) {
            final long time = UIUtils.getCurrentTime(getActivity());
            Uri uri = ScheduleContract.Sessions.buildSessionsInRoomAfterUri(title, time);
            final String order = ScheduleContract.Sessions.SESSION_START + " ASC";

            mQueryHandler.startQuery(SessionAfterQuery._TOKEN, title, uri,
                    SessionAfterQuery.PROJECTION, null, null, order);
        } else if (TYPE_PARTNER.equals(snippet)) {
            mCallbacks.onShowPartners();
        } else if (TYPE_PLAIN_SESSION.equals(snippet)) {
            // Show a basic info window with a title only
            marker.showInfoWindow();
        }
        // ignore other markers

        //centerMap(marker.getPosition());
        return true;
    }

    private void centerMap(LatLng position) {
        // calculate the new center of the map, taking into account optional
        // padding
        Projection proj = mMap.getProjection();
        Point p = proj.toScreenLocation(position);

        // apply padding
        p.x = (int) (p.x - Math.round(mWidth * 0.5)) + mShiftRight;
        p.y = (int) (p.y - Math.round(mHeight * 0.5)) + mShiftTop;

        mMap.animateCamera(CameraUpdateFactory.scrollBy(p.x, p.y));
    }

    /**
     * Set the padding around centered markers. Specified in the percentage of
     * the screen space of the map.
     */
    public void setCenterPadding(float xFraction, float yFraction) {
        int oldShiftRight = mShiftRight;
        int oldShiftTop = mShiftTop;

        mShiftRight = Math.round(xFraction * mWidth);
        mShiftTop = Math.round(yFraction * mWidth);

        // re-center the map, shift displayed map by x and y fraction if map is
        // ready
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.scrollBy(mShiftRight - oldShiftRight, mShiftTop
                    - oldShiftTop));
        }

    }

    private void highlightRoom(String roomId) {
        MarkerModel m = mMarkers.get(roomId);
        if (m != null) {
            showFloorIndex(m.floor);

            // explicitly show the marker before info window is shown.
            m.marker.setVisible(true);
            onMarkerClick(m.marker);
            centerMap(m.marker.getPosition());
        }
    }

    /**
     * Create an {@link AsyncQueryHandler} for use with the
     * {@link MapInfoWindowAdapter}.
     */
    private AsyncQueryHandler createInfowindowHandler(ContentResolver contentResolver) {
        return new AsyncQueryHandler(contentResolver) {
            StringBuilder mBuffer = new StringBuilder();
            Formatter mFormatter = new Formatter(mBuffer, Locale.getDefault());

            @Override
            protected void onQueryComplete(int token, Object cookie,
                    Cursor cursor) {

                MarkerModel model = mMarkers.get(cookie);

                mInfoAdapter.clearData();

                if (model == null || cursor == null) {
                    // query did not complete or incorrect data was loaded
                    return;
                }

                final long time = UIUtils.getCurrentTime(getActivity());

                switch (token) {
                    case SessionAfterQuery._TOKEN: {
                        extractSession(cursor, model, time);
                    }
                    break;
                }

                // update the displayed window
                model.marker.showInfoWindow();
            }

            private static final long SHOW_UPCOMING_TIME = 24 * 60 * 60 * 1000; // 24 hours

            private void extractSession(Cursor cursor, MarkerModel model, long time) {

                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    String currentTitle = null;
                    String nextTitle = null;
                    String nextTime = null;

                    final long blockStart = cursor.getLong(SessionAfterQuery.SESSION_START);
                    final long blockEnd = cursor.getLong(SessionAfterQuery.SESSION_END);
                    boolean inProgress = time >= blockStart && time <= blockEnd;

                    if (inProgress) {
                        // A session is running, display its name and optionally
                        // the next session
                        currentTitle = cursor.getString(SessionAfterQuery.SESSION_TITLE);

                        //move to the next entry
                        cursor.moveToNext();
                    }

                    if (!cursor.isAfterLast()) {
                        //There is a session coming up next, display only it if it's within 24 hours of the current time
                        final long nextStart = cursor.getLong(SessionAfterQuery.SESSION_START);

                        if (nextStart < time + SHOW_UPCOMING_TIME) {
                            nextTitle = cursor.getString(SessionAfterQuery.SESSION_TITLE);
                            mBuffer.setLength(0);

                            boolean showWeekday = !DateUtils.isToday(blockStart)
                                    && !UIUtils.isSameDayDisplay(UIUtils.getCurrentTime(getActivity()), blockStart, getActivity());

                            nextTime = DateUtils.formatDateRange(getActivity(), mFormatter,
                                    nextStart, nextStart,
                                    DateUtils.FORMAT_SHOW_TIME | (showWeekday
                                            ? DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY
                                            : 0),
                                    PrefUtils.getDisplayTimeZone(getActivity()).getID()
                            ).toString();
                        }
                    }

                    // populate the info window adapter
                    mInfoAdapter.setSessionData(model.marker, model.label, currentTitle,
                            nextTitle,
                            nextTime,
                            inProgress);


                } else {
                    // No entries, display name of room only
                    mInfoAdapter.setMarker(model.marker, model.label);
                }
            }

        };
    }

    // Loaders
    private void onMarkerLoaderComplete(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                // get data
                final String id = cursor.getString(MarkerQuery.MARKER_ID);
                final int floor = cursor.getInt(MarkerQuery.MARKER_FLOOR);
                final float lat = cursor.getFloat(MarkerQuery.MARKER_LATITUDE);
                final float lon = cursor.getFloat(MarkerQuery.MARKER_LONGITUDE);
                final String type = cursor.getString(MarkerQuery.MARKER_TYPE);
                final String label = cursor.getString(MarkerQuery.MARKER_LABEL);

                final LatLng position = new LatLng(lat, lon);
                MarkerOptions marker = null;
                if (TYPE_SESSION.equals(type) || TYPE_PLAIN_SESSION.equals(type)) {
                    marker = MapUtils.createSessionMarker(id, type, position);
                } else if (TYPE_PARTNER.equals(type)) {
                    marker = MapUtils.createPartnerMarker(id, position);
                } else if (TYPE_LABEL.equals(type)) {
                    marker = MapUtils.createLabelMarker(mIconGenerator, id, position, label);
                }

                // add marker to map
                if (marker != null) {
                    Marker m = mMap.addMarker(marker);

                    MarkerModel model = new MarkerModel(id, floor, type, label, m);

                    ArrayList<Marker> markerList = mMarkersFloor.get(floor);
                    if (markerList == null) {
                        // Initialise the list of Markers for this floor
                        markerList = new ArrayList<Marker>();
                        mMarkersFloor.put(floor, markerList);
                    }
                    markerList.add(m);
                    mMarkers.put(id, model);
                }

                cursor.moveToNext();
            }
            // no more markers to load
            mMarkersLoaded = true;
            enableMapElements();
        }

    }

    private void onOverlayLoaderComplete(Cursor cursor) {

        if (cursor != null && cursor.getCount() > 0) {

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                final int floor = cursor.getInt(OverlayQuery.TILE_FLOOR);
                final String file = cursor.getString(OverlayQuery.TILE_FILE);

                File f = MapUtils.getTileFile(getActivity().getApplicationContext(), file);
                if (f != null) {
                    addTileProvider(floor, f);
                }

                cursor.moveToNext();
            }

        }

        mOverlaysLoaded = true;
        enableMapElements();
    }

    private interface MarkerQuery {
        int _TOKEN = 0x1;

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

    private interface OverlayQuery {
        int _TOKEN = 0x3;

        String[] PROJECTION = {
                ScheduleContract.MapTiles.TILE_FLOOR,
                ScheduleContract.MapTiles.TILE_FILE
        };

        int TILE_FLOOR = 0;
        int TILE_FILE = 1;
    }


    private interface SessionAfterQuery {
        int _TOKEN = 0x5;

        String[] PROJECTION = {
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_START, ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME
        };

        int SESSION_TITLE = 0;
        int SESSION_START = 1;
        int SESSION_END = 2;
        int ROOM_NAME = 3;

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        switch (id) {
            case MarkerQuery._TOKEN: {
                Uri uri = ScheduleContract.MapMarkers.buildMarkerUri();
                return new CursorLoader(getActivity(), uri, MarkerQuery.PROJECTION,
                        null, null, null);
            }
            case OverlayQuery._TOKEN: {
                Uri uri = ScheduleContract.MapTiles.buildUri();
                return new CursorLoader(getActivity(), uri,
                        OverlayQuery.PROJECTION, null, null, null);
            }
        }
        return null;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }
        switch (loader.getId()) {
            case MarkerQuery._TOKEN:
                onMarkerLoaderComplete(cursor);
                break;
            case OverlayQuery._TOKEN:
                onOverlayLoaderComplete(cursor);
                break;
        }
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (!isAdded()) {
                return;
            }

            //clear map reload all data
            clearMap();
            setupMap(false);

            // reload data from loaders
            LoaderManager lm = getActivity().getLoaderManager();
            mMarkersLoaded = false;
            mOverlaysLoaded = false;

            Loader<Cursor> loader =
                    lm.getLoader(MarkerQuery._TOKEN);
            if (loader != null) {
                loader.forceLoad();
            }

            loader = lm.getLoader(OverlayQuery._TOKEN);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    };

    /**
     * A structure to store information about a Marker.
     */
    public static class MarkerModel {
        String id;
        int floor;
        String type;
        String label;
        Marker marker;

        public MarkerModel(String id, int floor, String type, String label, Marker marker) {
            this.id = id;
            this.floor = floor;
            this.type = type;
            this.label = label;
            this.marker = marker;
        }
    }
}
