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
import android.support.v4.app.Fragment;

import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.MapFragment;
import com.google.android.apps.iosched.ui.SimpleSinglePaneActivity;

public class MapActivity extends SimpleSinglePaneActivity implements
        MapFragment.Callbacks {
    @Override
    protected Fragment onCreatePane() {
        return new MapFragment();
    }

    @Override
    public void onSessionRoomSelected(String roomId, String roomTitle) {
        Intent roomIntent = new Intent(Intent.ACTION_VIEW,
                ScheduleContract.Rooms.buildSessionsDirUri(roomId));
        roomIntent.putExtra(Intent.EXTRA_TITLE, roomTitle);
        startActivity(roomIntent);
    }

    @Override
    public void onSandboxRoomSelected(String trackId, String roomTitle) {
        Intent intent = new Intent(this,TrackDetailActivity.class);
        intent.setData( ScheduleContract.Tracks.buildSandboxUri(trackId));
        intent.putExtra(Intent.EXTRA_TITLE, roomTitle);
        startActivity(intent);
    }

}
