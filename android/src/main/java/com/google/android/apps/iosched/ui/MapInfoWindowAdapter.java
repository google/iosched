/*
 * Copyright 2013 Google Inc.
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

import android.content.res.Resources;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.ui.MapFragment.MarkerModel;
import com.google.android.apps.iosched.ui.widget.EllipsizedTextView;
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

    //Sandbox
    private int sandboxColor;
    private int companyIcon;
    private String companyList;

    // Inflated views
    private View mViewSandbox = null;
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

        // render fallback if incorrect data is set or any other type
        // except for session or sandbox are rendered

        if (mMarker != null && !mMarker.getTitle().equals(marker.getTitle()) &&
                (MapFragment.TYPE_SESSION.equals(marker.getSnippet()) ||
                        MapFragment.TYPE_SANDBOX.equals(marker.getSnippet()))) {
            // View will be rendered in getInfoWindow, need to return null
            return null;
        } else {
            return renderTitleOnly(marker);

        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        if (mMarker != null && mMarker.getTitle().equals(marker.getTitle())) {
            final String snippet = marker.getSnippet();

            if (MapFragment.TYPE_SESSION.equals(snippet)) {
                return renderSession(marker);
            } else if (MapFragment.TYPE_SANDBOX.equals(snippet)) {
                return renderSandbox(marker);
            }
        }
        return null;

    }

    private View renderTitleOnly(Marker marker) {
        if (mViewTitleOnly == null) {
            mViewTitleOnly = mInflater.inflate(R.layout.map_info_titleonly, null);
        }

        TextView title = (TextView) mViewTitleOnly.findViewById(R.id.map_info_title);
        title.setText(mMarkers.get(marker.getTitle()).label);

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
        View spacer = mViewSession.findViewById(R.id.map_info_session_spacer);

        // default visibility
        first.setVisibility(View.GONE);
        second.setVisibility(View.GONE);
        spacer.setVisibility(View.GONE);

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

        if(!inProgress && titleNext == null){
            // No session in progress or coming up
            second.setText(Html.fromHtml(mResources.getString(R.string.map_now_playing,
                    mResources.getString(R.string.map_infowindow_text_empty))));
            second.setVisibility(View.VISIBLE);
        }else if(inProgress && titleNext != null){
            // Both lines are displayed, add extra padding
            spacer.setVisibility(View.VISIBLE);
        }

        return mViewSession;
    }

    private View renderSandbox(Marker marker) {
        if (mViewSandbox == null) {
            mViewSandbox = mInflater.inflate(R.layout.map_info_sandbox, null);
        }

        TextView titleView = (TextView) mViewSandbox.findViewById(R.id.map_info_roomtitle);
        titleView.setText(roomTitle);
        ImageView iconView = (ImageView) mViewSandbox.findViewById(R.id.map_info_icon);
        iconView.setImageResource(companyIcon);

        View rootLayout = mViewSandbox.findViewById(R.id.map_info_top);
        rootLayout.setBackgroundColor(this.sandboxColor);

        // Views
        EllipsizedTextView companyListView = (EllipsizedTextView) mViewSandbox.findViewById(R.id.map_info_sandbox_now);

        if (this.companyList != null) {
            companyListView.setText(Html.fromHtml(mResources.getString(R.string.map_now_sandbox,
                    companyList)));
            //TODO: fix missing ellipsize
        } else {
            // No active companies
            companyListView.setText(Html.fromHtml(mResources.getString(R.string.map_now_sandbox,
                    mResources.getString(R.string.map_infowindow_text_empty))));
        }

        return mViewSandbox;
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

    public void setSandbox(Marker marker, String label, int color, int iconId, String companies) {
        clearData();

        mMarker = marker;
        this.companyList = companies;
        this.roomTitle = label;
        this.sandboxColor = color;
        this.companyIcon = iconId;
    }
}
