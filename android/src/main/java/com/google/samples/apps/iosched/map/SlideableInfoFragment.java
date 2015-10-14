/*
 * Copyright 2015 Google Inc. All rights reserved.
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

import com.google.samples.apps.iosched.R;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Map info fragment that uses a {@link com.sothree.slidinguppanel.SlidingUpPanelLayout} to display
 * its contents.
 * It is designed to be displayed at the bottom of the screen and handles resizing, scrolling and
 * expanding of content itself.
 * Minimised panel heights need to be predefined (see
 * <code>@dimen/map_slideableinfo_height_titleonly</code>) and are automatically applied depending
 * on its state.
 */
public class SlideableInfoFragment extends MapInfoFragment {

    private View mSlideableView;

    private SlidingUpPanelLayout mLayout;

    /**
     * View that is used by the SlidingUpPanel as its main content. It does not contain anything
     * and is only used as a transparent overlay to intercept touch events and to provide a dummy
     * container for the panel layout.
     */
    private View mPanelContent;


    private int mHeightTitleOnly;
    private int mHeightMoscone;
    private int mHeightSession;

    /**
     * Progress of panel sliding after which the padding returned through the #Callback is fixed
     * at 0. Below this factor it returns the actual height of the bottom panel.
     */
    public static final float MAX_PANEL_PADDING_FACTOR = 0.6f;


    protected static SlideableInfoFragment newInstance() {
        return new SlideableInfoFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load heights
        final Resources resources = getResources();
        mHeightTitleOnly = resources
                .getDimensionPixelOffset(R.dimen.map_slideableinfo_height_titleonly);
        mHeightMoscone = resources
                .getDimensionPixelOffset(R.dimen.map_slideableinfo_height_moscone);
        mHeightSession = resources
                .getDimensionPixelOffset(R.dimen.map_slideableinfo_height_session);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = super
                .onCreateView(inflater, container, savedInstanceState, R.layout.map_info_bottom);
        mLayout = (SlidingUpPanelLayout) root.findViewById(R.id.map_bottomsheet);
        mSlideableView = mLayout.findViewById(R.id.map_bottomsheet_slideable);

        mLayout.setPanelSlideListener(mPanelSlideListener);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mLayout.setEnableDragViewTouchEvents(false);

        // Collapse the panel when the dummy content view is touched
        mPanelContent = root.findViewById(R.id.map_bottomsheet_dummycontent);
        mPanelContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
        mPanelContent.setClickable(false);

        return root;
    }

    @Override
    public void showTitleOnly(int roomType, String title) {
        super.showTitleOnly(roomType, title);
        setCollapsedOnly();
    }

    @Override
    protected void onSessionListLoading(String roomId, String roomTitle) {

        // Update the title and hide the list if displayed.
        // We don't want to uneccessarily resize the panel.
        mTitle.setText(roomTitle);
        mList.setVisibility(mList.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.GONE);

    }


    private void setCollapsedOnly() {
        // Set up panel: collapsed only with title height and icon
        mLayout.setPanelHeight(mHeightTitleOnly);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mLayout.setTouchEnabled(false);
    }

    public void showMoscone() {
        // Set up panel: collapsed with moscone height
        mLayout.setPanelHeight(mHeightMoscone);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mLayout.setTouchEnabled(false);

        super.showMoscone();
    }

    @Override
    protected void onSessionLoadingFailed(String roomTitle, int roomType) {
        // Do not display the list but permanently hide it
        super.onSessionLoadingFailed(roomTitle, roomType);
        setCollapsedOnly();
    }


    @Override
    protected void onSessionsLoaded(String roomTitle, int roomType, Cursor cursor) {
        super.onSessionsLoaded(roomTitle, roomType, cursor);
        // Set up panel: expandable with session height
        mLayout.setPanelHeight(mHeightSession);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mLayout.setTouchEnabled(true);

    }

    @Override
    protected void onRoomSubtitleLoaded(String roomTitle, int roomType, String subTitle) {
        super.onRoomSubtitleLoaded(roomTitle, roomType, subTitle);

        // Set up panel: Same height as Moscone, but collapsible
        mLayout.setPanelHeight(mHeightMoscone);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mLayout.setTouchEnabled(true);
    }

    public void hide() {
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mLayout.setPanelHeight(0);
    }

    @Override
    public boolean isExpanded() {
        return mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED;
    }

    @Override
    public void minimize() {
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    private SlidingUpPanelLayout.PanelSlideListener mPanelSlideListener
            = new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View view, float v) {
            // Visible size of panel. The bottom position is therefore the height of the layout,
            // not the bottom of the expandable view.
            mCallback.onInfoSizeChanged(mSlideableView.getLeft(), mSlideableView.getTop(),
                    mSlideableView.getRight(), mLayout.getHeight());
            mPanelContent.setClickable(false);
        }

        @Override
        public void onPanelCollapsed(View view) {
            mList.setScrollContainer(false);
            mList.setEnabled(false);
            mList.setSelection(0);
            mPanelContent.setClickable(false);

        }

        @Override
        public void onPanelExpanded(View view) {
            mList.setScrollContainer(true);
            mList.setEnabled(true);
            mPanelContent.setClickable(true);
        }

        @Override
        public void onPanelAnchored(View view) {
        }

        @Override
        public void onPanelHidden(View view) {
            mCallback.onInfoSizeChanged(mSlideableView.getLeft(), mSlideableView.getTop(),
                    mSlideableView.getRight(), mLayout.getHeight());
        }
    };

}
