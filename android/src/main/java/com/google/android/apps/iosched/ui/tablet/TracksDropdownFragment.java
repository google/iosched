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

package com.google.android.apps.iosched.ui.tablet;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.TracksAdapter;
import com.google.android.apps.iosched.util.ParserUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * A tablet-specific fragment that is a giant {@link android.widget.Spinner}
 * -like widget. It shows a {@link ListPopupWindow} containing a list of tracks,
 * using {@link TracksAdapter}. Requires API level 11 or later since
 * {@link ListPopupWindow} is API level 11+.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TracksDropdownFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener,
        PopupWindow.OnDismissListener {

    public static final int VIEW_TYPE_SESSIONS = 0;
    public static final int VIEW_TYPE_OFFICE_HOURS = 1;
    public static final int VIEW_TYPE_SANDBOX = 2;

    private static final String STATE_VIEW_TYPE = "viewType";
    private static final String STATE_SELECTED_TRACK_ID = "selectedTrackId";

    private TracksAdapter mAdapter;
    private int mViewType;

    private Handler mHandler = new Handler();

    private ListPopupWindow mListPopupWindow;
    private ViewGroup mRootView;
    private ImageView mIcon;
    private TextView mTitle;
    private TextView mAbstract;
    private String mTrackId;

    public interface Callbacks {
        public void onTrackSelected(String trackId);
        public void onTrackNameAvailable(String trackId, String trackName);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onTrackSelected(String trackId) {
        }
        
        @Override
        public void onTrackNameAvailable(String trackId, String trackName) {}
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new TracksAdapter(getActivity(), true);

        if (savedInstanceState != null) {
            // Since this fragment doesn't rely on fragment arguments, we must
            // handle state restores and saves ourselves.
            mViewType = savedInstanceState.getInt(STATE_VIEW_TYPE);
            mTrackId = savedInstanceState.getString(STATE_SELECTED_TRACK_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_VIEW_TYPE, mViewType);
        outState.putString(STATE_SELECTED_TRACK_ID, mTrackId);
    }

    public String getSelectedTrackId() {
        return mTrackId;
    }

    public void selectTrack(String trackId) {
        loadTrackList(mViewType, trackId);
    }

    public void loadTrackList(int viewType) {
        loadTrackList(viewType, mTrackId);
    }

    public void loadTrackList(int viewType, String selectTrackId) {
        // Teardown from previous arguments
        if (mListPopupWindow != null) {
            mListPopupWindow.setAdapter(null);
        }

        mViewType = viewType;
        mTrackId = selectTrackId;

        // Start background query to load tracks
        getLoaderManager().restartLoader(TracksAdapter.TracksQuery._TOKEN, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_tracks_dropdown, null);
        mIcon = (ImageView) mRootView.findViewById(R.id.track_icon);
        mTitle = (TextView) mRootView.findViewById(R.id.track_title);
        mAbstract = (TextView) mRootView.findViewById(R.id.track_abstract);

        mRootView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mListPopupWindow = new ListPopupWindow(getActivity());
                mListPopupWindow.setAdapter(mAdapter);
                mListPopupWindow.setModal(true);
                mListPopupWindow.setContentWidth(
                        getResources().getDimensionPixelSize(R.dimen.track_dropdown_width));
                mListPopupWindow.setAnchorView(mRootView);
                mListPopupWindow.setOnItemClickListener(TracksDropdownFragment.this);
                mListPopupWindow.show();
                mListPopupWindow.setOnDismissListener(TracksDropdownFragment.this);
            }
        });
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
        if (mListPopupWindow != null) {
            mListPopupWindow.dismiss();
        }
    }

    /** {@inheritDoc} */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        loadTrack(cursor, true);

        if (mListPopupWindow != null) {
            mListPopupWindow.dismiss();
        }
    }

    public String getTrackName() {
        return (String) mTitle.getText();
    }
    
    private void loadTrack(Cursor cursor, boolean triggerCallback) {
        final int trackColor;
        final Resources res = getResources();

        if (cursor != null) {
            trackColor = cursor.getInt(TracksAdapter.TracksQuery.TRACK_COLOR);

            mTrackId = cursor.getString(TracksAdapter.TracksQuery.TRACK_ID);

            String trackName = cursor.getString(TracksAdapter.TracksQuery.TRACK_NAME);

            mTitle.setText(trackName);
            mAbstract.setText(cursor.getString(TracksAdapter.TracksQuery.TRACK_ABSTRACT));

            int iconResId = res.getIdentifier(
                    "track_" + ParserUtils.sanitizeId(trackName),
                    "drawable", getActivity().getPackageName());
            if (iconResId != 0) {
                BitmapDrawable sourceIconDrawable = (BitmapDrawable) res.getDrawable(iconResId);
                Bitmap icon = Bitmap.createBitmap(sourceIconDrawable.getIntrinsicWidth(),
                        sourceIconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(icon);
                sourceIconDrawable.setBounds(0, 0, icon.getWidth(), icon.getHeight());
                sourceIconDrawable.draw(canvas);
                BitmapDrawable iconDrawable = new BitmapDrawable(res, icon);
                mIcon.setImageDrawable(iconDrawable);
            } else {
                mIcon.setImageDrawable(null);
            }
        } else {
            trackColor = res.getColor(R.color.all_track_color);
            mTrackId = ScheduleContract.Tracks.ALL_TRACK_ID;

            mIcon.setImageDrawable(null);
            switch (mViewType) {
                case VIEW_TYPE_SESSIONS:
                    mTitle.setText(R.string.all_tracks_sessions);
                    mAbstract.setText(R.string.all_tracks_subtitle_sessions);
                    break;
                case VIEW_TYPE_OFFICE_HOURS:
                    mTitle.setText(R.string.all_tracks_office_hours);
                    mAbstract.setText(R.string.all_tracks_subtitle_office_hours);
                    break;
                case VIEW_TYPE_SANDBOX:
                    mTitle.setText(R.string.all_tracks_sandbox);
                    mAbstract.setText(R.string.all_tracks_subtitle_sandbox);
                    break;
            }
        }

        mRootView.setBackgroundColor(trackColor);
        mCallbacks.onTrackNameAvailable(mTrackId, mTitle.getText().toString());

        if (triggerCallback) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallbacks.onTrackSelected(mTrackId);
                }
            });
        }
    }

    public void onDismiss() {
        mListPopupWindow = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        // Filter our tracks query to only include those with valid results
        String[] projection = TracksAdapter.TracksQuery.PROJECTION;
        String selection = null;

        switch (mViewType) {
            case VIEW_TYPE_SESSIONS:
                // Only show tracks with at least one session
                projection = TracksAdapter.TracksQuery.PROJECTION_WITH_SESSIONS_COUNT;
                selection = ScheduleContract.Tracks.SESSIONS_COUNT + ">0";
                break;

            case VIEW_TYPE_OFFICE_HOURS:
                // Only show tracks with at least one office hours
                projection = TracksAdapter.TracksQuery.PROJECTION_WITH_OFFICE_HOURS_COUNT;
                selection = ScheduleContract.Tracks.OFFICE_HOURS_COUNT + ">0";
                break;

            case VIEW_TYPE_SANDBOX:
                // Only show tracks with at least one company
                projection = TracksAdapter.TracksQuery.PROJECTION_WITH_SANDBOX_COUNT;
                selection = ScheduleContract.Tracks.SANDBOX_COUNT + ">0";
                break;
        }
        return new CursorLoader(getActivity(), ScheduleContract.Tracks.CONTENT_URI,
                projection, selection, null, ScheduleContract.Tracks.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null || cursor == null) {
            return;
        }

        boolean trackLoaded = false;

        if (mTrackId != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                if (mTrackId.equals(cursor.getString(TracksAdapter.TracksQuery.TRACK_ID))) {
                    loadTrack(cursor, false);
                    trackLoaded = true;
                    break;
                }
                cursor.moveToNext();
            }
        }

        if (!trackLoaded) {
            loadTrack(null, false);
        }

        mAdapter.setHasAllItem(true);
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cusor) {
    }
}
