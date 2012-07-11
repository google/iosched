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

import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.MapFragment;
import com.google.android.apps.iosched.ui.SimpleSinglePaneActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

/**
 * A single-pane activity that shows a {@link MapFragment}.
 */
public class MapActivity extends SimpleSinglePaneActivity implements
        MapFragment.Callbacks,
        LoaderManager.LoaderCallbacks<Cursor> {
    @Override
    protected Fragment onCreatePane() {
        return new MapFragment();
    }

    @Override
    public void onRoomSelected(String roomId) {
        Bundle loadRoomDataArgs = new Bundle();
        loadRoomDataArgs.putString("room_id", roomId);
        // force load (don't use previously used data)
        getSupportLoaderManager().restartLoader(0, loadRoomDataArgs, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(this,
                ScheduleContract.Rooms.buildRoomUri(data.getString("room_id")),
                RoomsQuery.PROJECTION,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) {
                return;
            }

            Intent roomIntent = new Intent(Intent.ACTION_VIEW,
                    ScheduleContract.Rooms.buildSessionsDirUri(
                            cursor.getString(RoomsQuery.ROOM_ID)));
            roomIntent.putExtra(Intent.EXTRA_TITLE, cursor.getString(RoomsQuery.ROOM_NAME));
            startActivity(roomIntent);

        } finally {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private interface RoomsQuery {
        String[] PROJECTION = {
                ScheduleContract.Rooms.ROOM_ID,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Rooms.ROOM_FLOOR,
        };

        int ROOM_ID = 0;
        int ROOM_NAME = 1;
        int ROOM_FLOOR = 2;
    }
}
