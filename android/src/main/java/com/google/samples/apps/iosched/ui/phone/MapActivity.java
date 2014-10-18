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

package com.google.samples.apps.iosched.ui.phone;

import android.app.FragmentManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.ui.BaseMapActivity;
import com.google.samples.apps.iosched.ui.MapFragment;
import com.google.samples.apps.iosched.ui.NearbyFragment;
import com.google.samples.apps.iosched.ui.PartnersFragment;
import com.google.samples.apps.iosched.util.AnalyticsManager;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class MapActivity extends BaseMapActivity implements MapFragment.Callbacks {

    private static final String TAG = makeLogTag(MapActivity.class);

    private static final String SCREEN_LABEL = "Map";
    private static final String PARTNERS_FRAGMENT_TAG = "partners";

    private boolean mPopupVisible = false; // Nearby or Partners
    private boolean mFirstPopupAnimate = true;
    private View mPopupContainerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_map);
        mPopupContainerView = findViewById(R.id.fragment_container_popup);

        /* [ANALYTICS:SCREEN]
         * TRIGGER:   View the Map screen on a phone.
         * LABEL:     "Map"
         * [/ANALYTICS]
         */
        AnalyticsManager.sendScreenView(SCREEN_LABEL);
        LOGD("Tracker", SCREEN_LABEL);

        overridePendingTransition(0, 0);

        getFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        mPopupVisible = (getFragmentManager().getBackStackEntryCount() == 1);
                        updatePopup();
                    }
                }
        );

        updatePopup();
    }

    private void updatePopup() {
        View mapContainerView = findViewById(R.id.fragment_container_map);
        if (mFirstPopupAnimate) {
            if (mPopupVisible) {
                mPopupContainerView.setTranslationY(mapContainerView.getHeight());
                mPopupContainerView.setVisibility(View.VISIBLE);
                mFirstPopupAnimate = false;
            } else {
                return;
            }
        }
        mPopupContainerView.animate()
                .translationY(mPopupVisible ? 0 : mapContainerView.getHeight())
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(250);
    }

    @Override
    public void onInsetsChanged(Rect insets) {
        super.onInsetsChanged(insets);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)
                mPopupContainerView.getLayoutParams();
        lp.topMargin= insets.top;
        mPopupContainerView.setLayoutParams(lp);
    }

    @Override
    public void onBackPressed() {
        // Force checking the native fragment manager for a backstack rather than
        // the support lib fragment manager.
        if (!getFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onSessionRoomSelected(String roomId, String roomTitle) {
        // we no longer have a screen that shows sessions on a given room
    }

    @Override
    public void onShowPartners() {
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container_popup, PartnersFragment.newInstance(true),
                        PARTNERS_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    // Show whichever Fragment has been provided by NearbyActivity.
    @Override
    protected void showNearbyFragment(String tag) {
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container_popup, NearbyFragment.newInstance(true), tag)
                .addToBackStack(null)
                .commit();
    }
}
