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

package com.google.android.apps.iosched.ui;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;

import com.actionbarsherlock.app.SherlockFragment;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

/**
 * A retained, non-UI helper fragment that loads track information such as name, color, etc. and
 * returns this information via {@link Callbacks#onTrackInfoAvailable(String, String, int)}.
 */
public class TrackInfoHelperFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The track URI for which to load data.
     */
    public static final String ARG_TRACK = "com.google.android.iosched.extra.TRACK";

    private Uri mTrackUri;

    // To be loaded
    private String mTrackId;
    private String mTrackName;
    private int mTrackColor;

    private Handler mHandler = new Handler();

    public interface Callbacks {
        public void onTrackInfoAvailable(String trackId, String trackName, int trackColor);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onTrackInfoAvailable(String trackId, String trackName, int trackColor) {
        }
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    public static TrackInfoHelperFragment newFromSessionUri(Uri sessionUri) {
        return newFromTrackUri(ScheduleContract.Sessions.buildTracksDirUri(
                ScheduleContract.Sessions.getSessionId(sessionUri)));
    }

    public static TrackInfoHelperFragment newFromTrackUri(Uri trackUri) {
        TrackInfoHelperFragment f = new TrackInfoHelperFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TRACK, trackUri);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mTrackUri = getArguments().getParcelable(ARG_TRACK);

        if (ScheduleContract.Tracks.ALL_TRACK_ID.equals(
                ScheduleContract.Tracks.getTrackId(mTrackUri))) {

            mTrackId = ScheduleContract.Tracks.ALL_TRACK_ID;
            mTrackName = getString(R.string.all_tracks);
            mTrackColor = getResources().getColor(android.R.color.white);

            mCallbacks.onTrackInfoAvailable(mTrackId, mTrackName, mTrackColor);

        } else {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;

        if (mTrackId != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallbacks.onTrackInfoAvailable(mTrackId, mTrackName, mTrackColor);
                }
            });
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(), mTrackUri, TracksQuery.PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) {
                return;
            }

            mTrackId = cursor.getString(TracksQuery.TRACK_ID);
            mTrackName = cursor.getString(TracksQuery.TRACK_NAME);
            mTrackColor = cursor.getInt(TracksQuery.TRACK_COLOR);

            // Wrapping in a Handler.post allows users of this helper to commit fragment
            // transactions in the callback.
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    mCallbacks.onTrackInfoAvailable(mTrackId, mTrackName, mTrackColor);
                }
            });

        } finally {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Tracks} query parameters.
     */
    private interface TracksQuery {
        String[] PROJECTION = {
                ScheduleContract.Tracks.TRACK_ID,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_COLOR,
        };

        int TRACK_ID = 0;
        int TRACK_NAME = 1;
        int TRACK_COLOR = 2;
    }
}
