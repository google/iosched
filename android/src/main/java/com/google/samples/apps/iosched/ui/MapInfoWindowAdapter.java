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

import android.content.res.Resources;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.ui.MapFragment.MarkerModel;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;

class MapInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    // Common parameters
    private String roomTitle;
    private Marker mMarker;

    //Session
    private String titleCurrent, titleNext, timeNext;
    private boolean inProgress;

    //Partner
    private String partnerName;

    // Inflated views
    private View mViewPartner = null;
    private View mViewSession = null;
    private View mViewTitleOnly = null;

    private LayoutInflater mInflater;
    private Resources mResources;

    private HashMap<String, MarkerModel> mMarkers;


    public MapInfoWindowAdapter(LayoutInflater inflater, Resources resources,
            HashMap<String, MarkerModel> markers) {
        this.mInflater = inflater;
        this.mResources = resources;
        mMarkers = markers;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        final String snippet = marker.getSnippet();
        if (MapFragment.TYPE_PARTNER.equals(snippet)) {
            return renderPartner(marker);

        } else if (MapFragment.TYPE_SESSION.equals(snippet)
                && mMarker != null && mMarker.getTitle().equals(marker.getTitle())) {
            // Ensure data has been set for a session marker first
            return renderSession(marker);

        } else if (MapFragment.TYPE_PLAIN_SESSION.equals(snippet)) {
            // Show the label only for plain session markers
            final MarkerModel model = mMarkers.get(marker.getTitle());
            if (model != null) {
                return renderTitleOnly(model.label);
            }
        }
        return null;
    }

    private View renderPartner(Marker marker) {
        if (mViewPartner == null) {
            mViewPartner = mInflater.inflate(R.layout.map_info_partner, null);
        }

        TextView title = (TextView) mViewPartner.findViewById(R.id.map_info_title);
        title.setText(mMarkers.get(marker.getTitle()).label);

        return mViewPartner;
    }

    private View renderTitleOnly(String title) {
        if (mViewTitleOnly == null) {
            mViewTitleOnly = mInflater.inflate(R.layout.map_info_titleonly, null);
        }

        TextView titleView = (TextView) mViewTitleOnly.findViewById(R.id.map_info_title);
        titleView.setText(title);

        return mViewTitleOnly;
    }

    private View renderSession(Marker marker) {
        if (mViewSession == null) {
            mViewSession = mInflater.inflate(R.layout.map_info_session, null);
        }
        TextView roomName = (TextView) mViewSession.findViewById(R.id.map_info_roomtitle);
        roomName.setText(roomTitle);

        TextView first = (TextView) mViewSession.findViewById(R.id.map_info_session_now);
        TextView second = (TextView) mViewSession.findViewById(R.id.map_info_session_next);

        // default visibility
        first.setVisibility(View.GONE);
        second.setVisibility(View.GONE);

        if (inProgress) {
            // A session is in progress, show its title
            first.setText(Html.fromHtml(mResources.getString(R.string.map_now_playing,
                    titleCurrent)));
            first.setVisibility(View.VISIBLE);
        }

        // show the next session if there is one
        if (titleNext != null) {
            second.setText(Html.fromHtml(mResources.getString(R.string.map_at, timeNext, titleNext)));
            second.setVisibility(View.VISIBLE);
        }

        if (!inProgress && titleNext == null) {
            // No session in progress or coming up
            second.setText(Html.fromHtml(mResources.getString(R.string.map_now_playing,
                    mResources.getString(R.string.map_infowindow_text_empty))));
            second.setVisibility(View.VISIBLE);
        }

        return mViewSession;
    }

    public void clearData() {
        this.titleCurrent = null;
        this.titleNext = null;
        this.inProgress = false;
        this.mMarker = null;
    }

    public void setSessionData(Marker marker, String roomTitle, String titleCurrent,
            String titleNext,
            String timeNext,
            boolean inProgress) {
        clearData();
        this.titleCurrent = titleCurrent;
        this.titleNext = titleNext;
        this.timeNext = timeNext;
        this.inProgress = inProgress;
        this.mMarker = marker;
        this.roomTitle = roomTitle;
    }

    public void setMarker(Marker marker, String roomTitle) {
        clearData();
        this.mMarker = marker;
        this.roomTitle = roomTitle;
    }

}
