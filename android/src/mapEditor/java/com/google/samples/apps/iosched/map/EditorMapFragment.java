/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.samples.apps.iosched.map.util.MarkerModel;

import android.app.Activity;

/**
 * Extension of {@link MapFragment} that contains an option to make all markers
 * draggable and log some additional details through a callback interface.
 */
public class EditorMapFragment extends MapFragment {

    public interface Callbacks extends MapFragment.Callbacks {

        void onLogMessage(String message);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onLogMessage(String message) {
        }

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

    public static EditorMapFragment newInstance() {
        return new EditorMapFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException(
                    "Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);
        googleMap.setOnMarkerDragListener(mMarkerDragListener);
    }

    /**
     * Sets all markers on the map as draggable.
     */
    public void setElementsDraggable(boolean isDraggable) {
        //Set all markers as draggable
        for (MarkerModel markerModel : mMarkers.values()) {
            markerModel.marker.setDraggable(isDraggable);
        }
    }

    private GoogleMap.OnMarkerDragListener mMarkerDragListener
            = new GoogleMap.OnMarkerDragListener() {
        @Override
        public void onMarkerDragStart(Marker marker) {

        }

        @Override
        public void onMarkerDrag(Marker marker) {
            mCallbacks.onLogMessage("" + marker.getTitle() + ": " + marker.getPosition());
        }

        @Override
        public void onMarkerDragEnd(Marker marker) {
            mCallbacks
                    .onLogMessage("" + marker.getTitle() + " dropped at: " + marker.getPosition());
        }
    };
}
