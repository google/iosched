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

import android.animation.ObjectAnimator;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Map info fragment that displays its content within in.
 *
 * It resizes based on the available space of its container. The list of sessions is automatically
 * marked as scrollable if required.
 * It is designed to be displayed at the left of the screen with a fixed width that is the only
 * value that is returned to
 * {@link com.google.samples.apps.iosched.map.MapInfoFragment.Callback#onInfoSizeChanged(int, int,
 * int, int)}.
 */
public class InlineInfoFragment extends MapInfoFragment {

    private View mLayout;

    private int mWidth = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mLayout = super
                .onCreateView(inflater, container, savedInstanceState, R.layout.map_info_inline);

        mLayout.addOnLayoutChangeListener(mLayoutChangeListener);

        return mLayout;
    }

    private View.OnLayoutChangeListener mLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                int oldTop, int oldRight, int oldBottom) {
            mWidth = right;
            mLayout.removeOnLayoutChangeListener(this);
        }
    };

    @Override
    protected void onSessionListLoading(String roomId, String roomTitle) {
        // Do not update the UI while the list is loading to prevent flickering when list is set.
    }

    @Override
    protected void onSessionsLoaded(String roomTitle, int roomType, Cursor cursor) {
        super.onSessionsLoaded(roomTitle, roomType, cursor);
        show();
    }

    @Override
    protected void onRoomSubtitleLoaded(String roomTitle, int roomType, String subTitle) {
        super.onRoomSubtitleLoaded(roomTitle, roomType, subTitle);
        show();
    }

    @Override
    protected void onSessionLoadingFailed(String roomTitle, int roomType) {
        super.onSessionLoadingFailed(roomTitle, roomType);
        show();
    }

    @Override
    public void showVenue() {
        super.showVenue();
        show();
    }

    @Override
    public void showTitleOnly(int icon, String roomTitle) {
        super.showTitleOnly(icon, roomTitle);
        show();
    }

    public boolean isExpanded() {
        return mLayout.getVisibility() == View.VISIBLE;
    }

    @Override
    public void minimize() {
        hide();
    }

    private void show() {
        mLayout.setVisibility(View.VISIBLE);
        mCallback.onInfoSizeChanged(0, 0, mWidth, 0);
    }

    @Override
    public void hide() {
        mLayout.setVisibility(View.GONE);
        mCallback.onInfoSizeChanged(0, 0, 0, 0);
    }

    public static MapInfoFragment newInstance() {
        return new InlineInfoFragment();
    }
}
