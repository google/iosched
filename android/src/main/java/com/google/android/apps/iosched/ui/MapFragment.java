/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.ui;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.MapUtils;
import com.google.android.apps.iosched.util.ParserUtils;
import com.google.android.apps.iosched.util.PrefUtils;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

/**
 * Shows a map of the conference venue.
 */
public class MapFragment extends SupportMapFragment implements
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraChangeListener,
        LoaderCallbacks<Cursor> {

    private static final LatLng MOSCONE = new LatLng(37.78353872135503, -122.40336209535599);

    // Initial camera position
    private static final LatLng CAMERA_MOSCONE = new LatLng(37.783107, -122.403789 );
    private static final float CAMERA_ZOOM = 17.75f;

    private static final int NUM_FLOORS = 3; // number of floors

    private static final int INITIAL_FLOOR = 1;

    /**
     * When specified, will automatically point the map to the requested room.
     */
    public static final String EXTRA_ROOM = "com.google.android.iosched.extra.ROOM";

    private static final String TAG = makeLogTag(MapFragment.class);

    // Marker types
    public static final String TYPE_SESSION = "session";
    public static final String TYPE_LABEL = "label";
    public static final String TYPE_SANDBOX = "sandbox";
    public static final String TYPE_INACTIVE = "inactive";

    // Tile Providers
    private TileProvider[] mTileProviders;
    private TileOverlay[] mTileOverlays;

    private Button[] mFloorButtons = new Button[NUM_FLOORS];
    private View mFloorControls;

    // Markers stored by id
    private HashMap<String, MarkerModel> mMarkers = null;
    // Markers stored by floor
    private ArrayList<ArrayList<Marker>> mMarkersFloor = null;

    private boolean mOverlaysLoaded = false;
    private boolean mMarkersLoaded = false;
    private boolean mTracksLoaded = false;

    // Cached size of view
    private int mWidth, mHeight;

    // Padding for #centerMap
    private int mShiftRight = 0;
    private int mShiftTop = 0;

    // Screen DPI
    private float mDPI = 0;

    // currently displayed floor
    private int mFloor = -1;

    // Show markers at default zoom level
    private boolean mShowMarkers = true;

    // Cached tracks data
    private HashMap<String,TrackModel> mTracks = null;

    private GoogleMap mMap;

    private MapInfoWindowAdapter mInfoAdapter;

    private MyLocationManager mMyLocationManager = null;

    // Handler for info window queries
    private AsyncQueryHandler mQueryHandler;

    public interface Callbacks {
        public void onSessionRoomSelected(String roomId, String roomTitle);
        public void onSandboxRoomSelected(String trackId, String roomTitle);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onSessionRoomSelected(String roomId, String roomTitle) {
        }

        @Override
        public void onSandboxRoomSelected(String trackId, String roomTitle) {
        }
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    private String mHighlightedRoom = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EasyTracker.getTracker().sendView("Map");
        LOGD("Tracker", "Map");

        clearMap();

        // get DPI
        mDPI = getActivity().getResources().getDisplayMetrics().densityDpi / 160f;

        // setup the query handler to populate info windows
        mQueryHandler = createInfowindowHandler(getActivity().getContentResolver());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View mapView = super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_map, container, false);
        FrameLayout layout = (FrameLayout) v.findViewById(R.id.map_container);

        layout.addView(mapView, 0);

        mFloorControls = layout.findViewById(R.id.map_floorcontrol);

        // setup floor button handlers
        mFloorButtons[0] = (Button) v.findViewById(R.id.map_floor1);
        mFloorButtons[1] = (Button) v.findViewById(R.id.map_floor2);
        mFloorButtons[2] = (Button) v.findViewById(R.id.map_floor3);

        for (int i = 0; i < mFloorButtons.length; i++) {
            final int j = i;
            mFloorButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFloor(j);
                }
            });
        }

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
                        enableFloors();

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
                });

        if (mMap == null) {
            setupMap(true);
        }

        // load all markers
        LoaderManager lm = getActivity().getSupportLoaderManager();
        lm.initLoader(MarkerQuery._TOKEN, null, this);

        // load the tile overlays
        lm.initLoader(OverlayQuery._TOKEN, null, this);

        // load tracks data
        lm.initLoader(TracksQuery._TOKEN, null, this);

        return v;
    }

    /**
     * Clears the map and initialises all map variables that hold markers and overlays.
     */
    private void clearMap() {
        if (mMap != null) {
            mMap.clear();
        }

        // setup tile provider arrays
        mTileProviders = new TileProvider[NUM_FLOORS];
        mTileOverlays = new TileOverlay[NUM_FLOORS];

        mMarkers = new HashMap<String, MarkerModel>();
        mMarkersFloor = new ArrayList<ArrayList<Marker>>();

        // initialise floor marker lists
        for (int i = 0; i < NUM_FLOORS; i++) {
            mMarkersFloor.add(i, new ArrayList<Marker>());
        }
    }

    private void setupMap(boolean resetCamera) {
        mInfoAdapter = new MapInfoWindowAdapter(getLayoutInflater(null), getResources(),
                mMarkers);
        mMap = getMap();

        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnCameraChangeListener(this);
        mMap.setInfoWindowAdapter(mInfoAdapter);

        if (resetCamera) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(
                    CAMERA_MOSCONE, CAMERA_ZOOM)));
        }

        mMap.setIndoorEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);

        if (MapUtils.getMyLocationEnabled(this.getActivity())) {
            mMyLocationManager = new MyLocationManager();
        }


        Bundle data = getArguments();
        if (data != null && data.containsKey(EXTRA_ROOM)) {
            mHighlightedRoom = data.getString(EXTRA_ROOM);
        }

        LOGD(TAG, "Map setup complete.");

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
        sp.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;

        getActivity().getContentResolver().unregisterContentObserver(mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);

    }

    private void showFloor(int floor) {
        if (mFloor != floor) {
            if (mFloor >= 0) {
                // hide old overlay
                mTileOverlays[mFloor].setVisible(false);
                mFloorButtons[mFloor].setBackgroundResource(R.drawable.map_floor_button_background);
                mFloorButtons[mFloor].setTextColor(getResources().getColor(
                        R.color.map_floorselect_inactive));
                // hide all markers
                for (Marker m : mMarkersFloor.get(mFloor)) {
                    m.setVisible(false);
                }
            }
            mFloor = floor;

            // show the floor overlay
            if (mTileOverlays[mFloor] != null) {
                mTileOverlays[mFloor].setVisible(true);
            }

            // show all markers
            for (Marker m : mMarkersFloor.get(mFloor)) {
                m.setVisible(mShowMarkers);
            }
            // mark button active
            mFloorButtons[mFloor]
                    .setBackgroundResource(R.drawable.map_floor_button_active_background);
            mFloorButtons[mFloor].setTextColor(getResources().getColor(
                    R.color.map_floorselect_active));
        }
    }

    /**
     * Enable floor controls and display map features when all loaders have
     * finished. This ensures that only complete data for the correct floor is
     * shown.
     */
    private void enableFloors() {
        if (mOverlaysLoaded && mMarkersLoaded && mTracksLoaded && mWidth > 0 && mHeight > 0) {
            mFloorControls.setVisibility(View.VISIBLE);

            // highlight a room if argument is set and exists, otherwise show the default floor
            if (mHighlightedRoom != null && mMarkers.containsKey(mHighlightedRoom)) {
                highlightRoom(mHighlightedRoom);
                mHighlightedRoom = null;
            } else {
                showFloor(INITIAL_FLOOR);
                mHighlightedRoom = null;
            }
        }
    }

    void addTileProvider(int floor, File f) {
        if (!f.exists()) {
            return;
        }

        TileProvider provider;
        try {
            provider = new SVGTileProvider(f, mDPI);
        } catch (IOException e) {
            LOGD(TAG, "Could not create Tile Provider.");
            e.printStackTrace();
            return;
        }
        TileOverlayOptions tileOverlay = new TileOverlayOptions()
                .tileProvider(provider).visible(false);
        mTileProviders[floor] = provider;
        mTileOverlays[floor] = mMap.addTileOverlay(tileOverlay);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final String snippet = marker.getSnippet();
        if (TYPE_SESSION.equals(snippet)) {
            final String roomId = marker.getTitle();
            EasyTracker.getTracker().sendEvent(
                    "Map", "infoclick", roomId, 0L);
            mCallbacks.onSessionRoomSelected(roomId, mMarkers.get(roomId).label);
            // ignore other markers
        } else if (TYPE_SANDBOX.equals(snippet)) {
            final String roomId = marker.getTitle();
            MarkerModel model = mMarkers.get(roomId);
            EasyTracker.getTracker().sendEvent(
                    "Map", "infoclick", roomId, 0L);
            mCallbacks.onSandboxRoomSelected(model.track, model.label);
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        final String snippet = marker.getSnippet();
        // get the room id
        String roomId = marker.getTitle();

        if (TYPE_SESSION.equals(snippet)) {
            // ignore other markers - sandbox is just another session type

            EasyTracker.getTracker().sendEvent(
                    "Map", "markerclick", roomId, 0L);
            final long time = UIUtils.getCurrentTime(getActivity());
            Uri uri = ScheduleContract.Sessions.buildSessionsInRoomAfterUri(roomId, time);
            final String order = ScheduleContract.Sessions.BLOCK_START + " ASC";

            mQueryHandler.startQuery(SessionAfterQuery._TOKEN, roomId, uri,
                    SessionAfterQuery.PROJECTION, null, null, order);
        } else if (TYPE_SANDBOX.equals(snippet)) {
            // get the room id
            EasyTracker.getTracker().sendEvent(
                    "Map", "markerclick", roomId, 0L);
            final long time = UIUtils.getCurrentTime(getActivity());
            String selection = ScheduleContract.Sandbox.AT_TIME_IN_ROOM_SELECTION;

            Uri uri = ScheduleContract.Sandbox.CONTENT_URI;
            String[] selectionArgs = ScheduleContract.Sandbox.buildAtTimeInRoomSelectionArgs(time, roomId);
            final String order = ScheduleContract.Sandbox.COMPANY_NAME + " ASC";

            mQueryHandler.startQuery(SandboxCompaniesAtQuery._TOKEN, roomId, uri,
                    SandboxCompaniesAtQuery.PROJECTION, selection, selectionArgs, order);
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
            showFloor(m.floor);

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
                    case SandboxCompaniesAtQuery._TOKEN: {
                        extractSandbox(cursor, model, time);
                    }
                }

                // update the displayed window
                model.marker.showInfoWindow();
            }

            private void extractSandbox(Cursor cursor, MarkerModel model, long time) {
                // get tracks data from cache: icon and color
                TrackModel track = mTracks.get(model.track);
                int color = (track != null) ? track.color : 0 ;
                int iconResId = 0;
                if(track != null){
                    iconResId = getResources().getIdentifier(
                            "track_" + ParserUtils.sanitizeId(track.name),
                            "drawable", getActivity().getPackageName());
                }

                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    final int maxCompaniesDisplay = getResources().getInteger(
                            R.integer.sandbox_company_list_max_display);
                    while (!cursor.isAfterLast() && count < maxCompaniesDisplay) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(cursor.getString(SandboxCompaniesAtQuery.COMPANY_NAME));
                        count++;
                        cursor.moveToNext();
                    }
                    if (count >= maxCompaniesDisplay && !cursor.isAfterLast()) {
                        // Additional sandbox companies to display
                        sb.append(", &hellip;");
                    }

                    mInfoAdapter.setSandbox(model.marker, model.label, color, iconResId,
                            sb.length() > 0 ? sb.toString() : null);

                }else{
                    // No active sandbox companies
                    mInfoAdapter.setSandbox(model.marker, model.label, color, iconResId, null);
                }

                model.marker.showInfoWindow();
            }

            private static final long SHOW_UPCOMING_TIME = 24 * 60 * 60 * 1000; // 24 hours

            private void extractSession(Cursor cursor, MarkerModel model, long time) {

                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    String currentTitle = null;
                    String nextTitle = null;
                    String nextTime = null;

                    final long blockStart = cursor.getLong(SessionAfterQuery.BLOCK_START);
                    final long blockEnd = cursor.getLong(SessionAfterQuery.BLOCK_END);
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
                        final long nextStart = cursor.getLong(SessionAfterQuery.BLOCK_START);

                        if (nextStart < time + SHOW_UPCOMING_TIME ) {
                            nextTitle = cursor.getString(SessionAfterQuery.SESSION_TITLE);
                            mBuffer.setLength(0);

                            boolean showWeekday = !DateUtils.isToday(blockStart)
                                    && !UIUtils.isSameDayDisplay(UIUtils.getCurrentTime(getActivity()), blockStart, getActivity());

                            nextTime = DateUtils.formatDateRange(getActivity(), mFormatter,
                                    blockStart, blockStart,
                                    DateUtils.FORMAT_SHOW_TIME | (showWeekday
                                            ? DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY
                                            : 0),
                                    PrefUtils.getDisplayTimeZone(getActivity()).getID()).toString();
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
                String id = cursor.getString(MarkerQuery.MARKER_ID);
                int floor = cursor.getInt(MarkerQuery.MARKER_FLOOR);
                float lat = cursor.getFloat(MarkerQuery.MARKER_LATITUDE);
                float lon = cursor.getFloat(MarkerQuery.MARKER_LONGITUDE);
                String type = cursor.getString(MarkerQuery.MARKER_TYPE);
                String label = cursor.getString(MarkerQuery.MARKER_LABEL);
                String track = cursor.getString(MarkerQuery.MARKER_TRACK);

                BitmapDescriptor icon = null;
                if (TYPE_SESSION.equals(type)) {
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_session);
                } else if (TYPE_SANDBOX.equals(type)) {
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_sandbox);
                } else if (TYPE_LABEL.equals(type)) {
                    Bitmap b = MapUtils.createTextLabel(label, mDPI);
                    if (b != null) {
                        icon = BitmapDescriptorFactory.fromBitmap(b);
                    }
                }

                // add marker to map
                if (icon != null) {
                    Marker m = mMap.addMarker(
                            new MarkerOptions().position(new LatLng(lat, lon)).title(id)
                                    .snippet(type).icon(icon)
                                    .visible(false));

                    MarkerModel model = new MarkerModel(id, floor, type, label, track, m);

                    mMarkersFloor.get(floor).add(m);
                    mMarkers.put(id, model);
                }

                cursor.moveToNext();
            }
            // no more markers to load
            mMarkersLoaded = true;
            enableFloors();
        }

    }

    private void onTracksLoaderComplete(Cursor cursor){
        if(cursor != null){
            mTracks = new HashMap<String, TrackModel>();
            cursor.moveToFirst();
            while(!cursor.isAfterLast()){
                final String name = cursor.getString(TracksQuery.TRACK_NAME);
                final String id = cursor.getString(TracksQuery.TRACK_ID);
                final int color = cursor.getInt(TracksQuery.TRACK_COLOR);

                mTracks.put(id,new TrackModel(id,name,color));
                cursor.moveToNext();
            }
            mTracksLoaded = true;
            enableFloors();
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
        enableFloors();
    }

    private interface MarkerQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                ScheduleContract.MapMarkers.MARKER_ID,
                ScheduleContract.MapMarkers.MARKER_FLOOR,
                ScheduleContract.MapMarkers.MARKER_LATITUDE,
                ScheduleContract.MapMarkers.MARKER_LONGITUDE,
                ScheduleContract.MapMarkers.MARKER_TYPE,
                ScheduleContract.MapMarkers.MARKER_LABEL,
                ScheduleContract.MapMarkers.MARKER_TRACK
        };

        int MARKER_ID = 0;
        int MARKER_FLOOR = 1;
        int MARKER_LATITUDE = 2;
        int MARKER_LONGITUDE = 3;
        int MARKER_TYPE = 4;
        int MARKER_LABEL = 5;
        int MARKER_TRACK = 6;
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
                ScheduleContract.Sessions.BLOCK_START, ScheduleContract.Sessions.BLOCK_END,
                ScheduleContract.Rooms.ROOM_NAME
        };

        int SESSION_TITLE = 0;
        int BLOCK_START = 1;
        int BLOCK_END = 2;
        int ROOM_NAME = 3;

    }

    private interface SandboxCompaniesAtQuery {
        int _TOKEN = 0x4;

        String[] PROJECTION = {
                ScheduleContract.Sandbox.COMPANY_NAME
        };

        int COMPANY_NAME = 0;
    }

    private interface TracksQuery {
        int _TOKEN = 0x6;
        String[] PROJECTION = {
                ScheduleContract.Tracks.TRACK_ID,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_COLOR
        };
                int TRACK_ID = 0;
        int TRACK_NAME = 1;
        int TRACK_COLOR = 2;
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
            case TracksQuery._TOKEN: {
                Uri uri = ScheduleContract.Tracks.CONTENT_URI;
                return new CursorLoader(getActivity(), uri,
                        TracksQuery.PROJECTION, null, null, null);
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
            case TracksQuery._TOKEN:
                onTracksLoaderComplete(cursor);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMyLocationManager != null) {
            mMyLocationManager.onDestroy();
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // ensure markers have been loaded and are being displayed
        if (mFloor < 0) {
            return;
        }

        mShowMarkers = cameraPosition.zoom >= 17;
        for (Marker m : mMarkersFloor.get(mFloor)) {
            m.setVisible(mShowMarkers);
        }
    }


    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
                    if(!isAdded()){
                        return;
                    }

                    boolean enableMyLocation = MapUtils.getMyLocationEnabled(MapFragment.this.getActivity());
                    //enable or disable location manager
                    if (enableMyLocation && mMyLocationManager == null) {
                        // enable location manager
                        mMyLocationManager = new MyLocationManager();
                    } else if (!enableMyLocation && mMyLocationManager != null) {
                        // disable location manager
                        mMyLocationManager.onDestroy();
                        mMyLocationManager = null;
                    }
                }
            };

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
            LoaderManager lm = getActivity().getSupportLoaderManager();
            mMarkersLoaded = false;
            mOverlaysLoaded = false;
            mTracksLoaded = false;

            Loader<Cursor> loader =
                    lm.getLoader(MarkerQuery._TOKEN);
            if (loader != null) {
                loader.forceLoad();
            }

            loader = lm.getLoader(OverlayQuery._TOKEN);
            if (loader != null) {
                loader.forceLoad();
            }

            loader = lm.getLoader(TracksQuery._TOKEN);
            if (loader != null) {
                loader.forceLoad();
            }

        }
    };

    /**
     * Manages the display of the "My Location" layer. Ensures that the layer is
     * only visible when the user is within 200m of Moscone Center.
     */
    private class MyLocationManager extends BroadcastReceiver {
        private static final String ACTION_PROXIMITY_ALERT
                = "com.google.android.apps.iosched.action.PROXIMITY_ALERT";
        private static final float DISTANCE = 200; // 200 metres.

        private final IntentFilter mIntentFilter = new IntentFilter(ACTION_PROXIMITY_ALERT);
        private final LocationManager mLocationManager;

        public MyLocationManager() {
            mLocationManager = (LocationManager) getActivity().getSystemService(
                    Context.LOCATION_SERVICE);

            Intent i = new Intent();
            i.setAction(ACTION_PROXIMITY_ALERT);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity()
                    .getApplicationContext(), 0, i, 0);
            mLocationManager.addProximityAlert(MOSCONE.latitude, MOSCONE.longitude, DISTANCE, -1,
                    pendingIntent);
            getActivity().registerReceiver(this, mIntentFilter);

            // The proximity alert is only fired if the user moves in/out of
            // range. Look at the current location to see if it is within range.
            checkCurrentLocation();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean inMoscone = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING,
                    false);
            mMap.setMyLocationEnabled(inMoscone);
        }

        public void checkCurrentLocation() {
            Criteria criteria = new Criteria();
            String provider = mLocationManager.getBestProvider(criteria, false);

            Location lastKnownLocation = mLocationManager.getLastKnownLocation(provider);
            if (lastKnownLocation == null) {
                return;
            }

            Location moscone = new Location(lastKnownLocation.getProvider());
            moscone.setLatitude(MOSCONE.latitude);
            moscone.setLongitude(MOSCONE.longitude);
            moscone.setAccuracy(1);

            if (moscone.distanceTo(lastKnownLocation) < DISTANCE) {
                mMap.setMyLocationEnabled(true);
            }
        }

        public void onDestroy() {
            getActivity().unregisterReceiver(this);
        }
    }

    /**
     * A structure to store information about a Marker.
     */
    public static class MarkerModel {
        String id;
        int floor;
        String type;
        String label;
        String track = null;
        Marker marker;

        public MarkerModel(String id, int floor, String type, String label, String track, Marker marker) {
            this.id = id;
            this.floor = floor;
            this.type = type;
            this.label = label;
            this.marker = marker;
            this.track = track;
        }
    }

    public static class TrackModel {
        String id;
        String name;
        int color;

        public TrackModel(String id, String name, int color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }
    }
}
