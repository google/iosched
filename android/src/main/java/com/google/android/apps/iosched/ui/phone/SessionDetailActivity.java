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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.TaskStackBuilder;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.*;
import com.google.android.apps.iosched.util.BeamUtils;
import com.google.android.apps.iosched.util.ImageLoader;
import com.google.android.apps.iosched.util.UIUtils;

public class SessionDetailActivity extends SimpleSinglePaneActivity implements
        TrackInfoHelperFragment.Callbacks,
        ImageLoader.ImageLoaderProvider {

    private String mTrackId = null;
    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UIUtils.tryTranslateHttpIntent(this);
        BeamUtils.tryUpdateIntentFromBeam(this);
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Uri sessionUri = getIntent().getData();
            BeamUtils.setBeamSessionUri(this, sessionUri);
            getSupportFragmentManager().beginTransaction()
                    .add(TrackInfoHelperFragment.newFromSessionUri(sessionUri),
                            "track_info")
                    .commit();
        }

        mImageLoader = new ImageLoader(this, R.drawable.person_image_empty)
                .setMaxImageSize(getResources().getDimensionPixelSize(R.dimen.speaker_image_size))
                .setFadeInImage(UIUtils.hasHoneycombMR1());
    }

    @Override
    protected Fragment onCreatePane() {
        return new SessionDetailFragment();
    }

    @Override
    public Intent getParentActivityIntent() {
        // Up to this session's track details, or Home if no track is available
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
    public ImageLoader getImageLoaderInstance() {
        return mImageLoader;
    }
}
