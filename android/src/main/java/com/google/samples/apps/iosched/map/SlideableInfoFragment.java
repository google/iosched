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

import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
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

    private View mBottomSheet;

    private CoordinatorLayout mCoordinator;

    private BottomSheetBehavior mBehavior;

    private int mHeightTitleOnly;
    private int mHeightVenue;
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
        mHeightVenue = resources
                .getDimensionPixelOffset(R.dimen.map_slideableinfo_height_venue);
        mHeightSession = resources
                .getDimensionPixelOffset(R.dimen.map_slideableinfo_height_session);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState,
                R.layout.map_info_bottom);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCoordinator = (CoordinatorLayout) view.findViewById(R.id.map_coordinator);
        mBottomSheet = mCoordinator.findViewById(R.id.map_bottomsheet);
        mBehavior = BottomSheetBehavior.from(mBottomSheet);
        mBehavior.setBottomSheetCallback(mBottomSheetCallback);
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
        mBehavior.setPeekHeight(mHeightTitleOnly);
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void showVenue() {
        // Set up panel: collapsed with venue height
        mBehavior.setPeekHeight(mHeightVenue);
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        super.showVenue();
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
        mBehavior.setPeekHeight(mHeightSession);
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    protected void onRoomSubtitleLoaded(String roomTitle, int roomType, String subTitle) {
        super.onRoomSubtitleLoaded(roomTitle, roomType, subTitle);

        // Set up panel: Same height as venue, but collapsible
        mBehavior.setPeekHeight(mHeightVenue);
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void hide() {
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public boolean isExpanded() {
        return mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    @Override
    public void minimize() {
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetCallback
            = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull final View bottomSheet, final int newState) {
            switch (newState) {
                case BottomSheetBehavior.STATE_COLLAPSED:
                    mList.setScrollContainer(false);
                    mList.setEnabled(false);
                    mList.getLayoutManager().scrollToPosition(0);
                    mCoordinator.requestLayout();
                    break;
                case BottomSheetBehavior.STATE_EXPANDED:
                    mList.setScrollContainer(true);
                    mList.setEnabled(true);
                    break;
                case BottomSheetBehavior.STATE_HIDDEN:
                    mCallback.onInfoSizeChanged(mBottomSheet.getLeft(), mBottomSheet.getTop(),
                            mBottomSheet.getRight(), mCoordinator.getHeight());
                    break;
            }
        }

        @Override
        public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {
            mCallback.onInfoSizeChanged(mBottomSheet.getLeft(), mBottomSheet.getTop(),
                    mBottomSheet.getRight(), mCoordinator.getHeight());
        }

    };

}
