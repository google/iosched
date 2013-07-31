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

package com.google.android.apps.iosched.ui.phone;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.*;
import com.google.android.apps.iosched.ui.SandboxDetailFragment;
import com.google.android.apps.iosched.util.ImageLoader;
import com.google.android.apps.iosched.util.UIUtils;

public class SandboxDetailActivity extends SimpleSinglePaneActivity implements
        SandboxDetailFragment.Callbacks,
        TrackInfoHelperFragment.Callbacks,
        ImageLoader.ImageLoaderProvider {

    private String mTrackId = null;
    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageLoader = new ImageLoader(this, R.drawable.sandbox_logo_empty)
                .setMaxImageSize(getResources().getDimensionPixelSize(
                        R.dimen.sandbox_company_image_size))
                .setFadeInImage(UIUtils.hasHoneycombMR1());
    }

    @Override
    protected Fragment onCreatePane() {
        return new SandboxDetailFragment();
    }

    @Override
    public Intent getParentActivityIntent() {
        // Up to this company's track details, or Home if no track is available
        if (mTrackId != null) {
            return new Intent(Intent.ACTION_VIEW, ScheduleContract.Tracks.buildTrackUri(mTrackId));
        } else {
            return new Intent(this, HomeActivity.class);
        }
    }

    @Override
    public void onTrackInfoAvailable(String trackId, TrackInfo track) {
        mTrackId = trackId;
        setTitle(track.name);
        setActionBarTrackIcon(track.name, track.color);
    }

    @Override
    public void onTrackIdAvailable(final String trackId) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.findFragmentByTag("track_info") == null) {
                    fm.beginTransaction()
                            .add(TrackInfoHelperFragment.newFromTrackUri(
                                    ScheduleContract.Tracks.buildTrackUri(trackId)),
                                    "track_info")
                            .commit();
                }
            }
        });
    }

    @Override
    public ImageLoader getImageLoaderInstance() {
        return mImageLoader;
    }
}
