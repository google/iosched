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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.BitmapCache;
import com.google.android.apps.iosched.util.UIUtils;

/**
 * A {@link android.widget.CursorAdapter} that renders a {@link TracksQuery}.
 */
public class TracksAdapter extends CursorAdapter {
    private static final int ALL_ITEM_ID = Integer.MAX_VALUE;
    private static final int LEVEL_2_SECTION_HEADER_ITEM_ID = ALL_ITEM_ID - 1;

    private Activity mActivity;
    private boolean mHasAllItem;
    private int mFirstLevel2CursorPosition = -1;
    private BitmapCache mBitmapCache;
    private boolean mIsDropDown;

    public TracksAdapter(FragmentActivity activity, boolean isDropDown) {
        super(activity, null, 0);
        mActivity = activity;
        mIsDropDown = isDropDown;

        // Fetch track icon size in pixels.
        int trackIconSize =
                activity.getResources().getDimensionPixelSize(R.dimen.track_icon_source_size);

        // Cache size is total pixels by 4 bytes (as format is ARGB_8888) by 20 (max icons to hold
        // in the cache) converted to KB.
        int cacheSize = trackIconSize * trackIconSize * 4 * 20 / 1024;

        // Create a BitmapCache to hold the track icons.
        mBitmapCache = BitmapCache.getInstance(activity.getSupportFragmentManager(),
                UIUtils.TRACK_ICONS_TAG, cacheSize);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        updateSpecialItemPositions(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        updateSpecialItemPositions(newCursor);
        return super.swapCursor(newCursor);
    }

    public void setHasAllItem(boolean hasAllItem) {
        mHasAllItem = hasAllItem;
        updateSpecialItemPositions(getCursor());
    }

    private void updateSpecialItemPositions(Cursor cursor) {
        mFirstLevel2CursorPosition = -1;
        if (cursor != null && !cursor.isClosed()) {
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                if (cursor.getInt(TracksQuery.TRACK_LEVEL) == 2) {
                    mFirstLevel2CursorPosition = cursor.getPosition();
                    break;
                }
            }
        }
    }

    public boolean isAllTracksItem(int position) {
        return mHasAllItem && position == 0;
    }

    public boolean isLevel2Header(int position) {
        return mFirstLevel2CursorPosition >= 0
                && position - (mHasAllItem ? 1 : 0) == mFirstLevel2CursorPosition;
    }

    public int adapterPositionToCursorPosition(int position) {
        position -= (mHasAllItem ? 1 : 0);
        if (mFirstLevel2CursorPosition >= 0 && position > mFirstLevel2CursorPosition) {
            --position;
        }
        return position;
    }

    @Override
    public int getCount() {
        int superCount = super.getCount();
        if (superCount == 0) {
            return  0;
        }

        return superCount
                + (mFirstLevel2CursorPosition >= 0 ? 1 : 0)
                + (mHasAllItem ? 1 : 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (isAllTracksItem(position)) {
            if (convertView == null) {
                convertView = mActivity.getLayoutInflater().inflate(
                        R.layout.list_item_track, parent, false);
            }

            // Custom binding for the first item
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                    "(" + mActivity.getResources().getString(R.string.all_tracks) + ")");
            convertView.findViewById(android.R.id.icon1).setVisibility(View.INVISIBLE);

            return convertView;

        } else if (isLevel2Header(position)) {
            TextView view = (TextView) convertView;
            if (view == null) {
                view = (TextView) mActivity.getLayoutInflater().inflate(
                        R.layout.list_item_track_header, parent, false);
                if (mIsDropDown) {
                    Rect r = new Rect(view.getPaddingLeft(), view.getPaddingTop(),
                            view.getPaddingRight(), view.getPaddingBottom());
                    view.setBackgroundResource(R.drawable.track_header_bottom_border);
                    view.setPadding(r.left, r.top, r.right, r.bottom);
                }
            }
            view.setText(R.string.other_tracks);
            return view;
        }
        return super.getView(adapterPositionToCursorPosition(position), convertView, parent);
    }

    @Override
    public Object getItem(int position) {
        if (isAllTracksItem(position) || isLevel2Header(position)) {
            return null;
        }
        return super.getItem(adapterPositionToCursorPosition(position));
    }

    @Override
    public long getItemId(int position) {
        if (isAllTracksItem(position)) {
            return ALL_ITEM_ID;
        } else if (isLevel2Header(position)) {
            return LEVEL_2_SECTION_HEADER_ITEM_ID;
        }
        return super.getItemId(adapterPositionToCursorPosition(position));
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return position < (mHasAllItem ? 1 : 0)
                || !isLevel2Header(position)
                && super.isEnabled(adapterPositionToCursorPosition(position));
    }

    @Override
    public int getViewTypeCount() {
        // Add an item type for the "All" item and section header.
        return super.getViewTypeCount() + 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (isAllTracksItem(position)) {
            return getViewTypeCount() - 1;
        } else if (isLevel2Header(position)) {
            return getViewTypeCount() - 2;
        }
        return super.getItemViewType(adapterPositionToCursorPosition(position));
    }

    /** {@inheritDoc} */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mActivity.getLayoutInflater().inflate(R.layout.list_item_track, parent,
                false);
    }

    /** {@inheritDoc} */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String trackName = cursor.getString(TracksQuery.TRACK_NAME);

        final TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(trackName);

        // Assign track color and icon to visible block
        final ImageView iconView = (ImageView) view.findViewById(android.R.id.icon1);

        new UIUtils.TrackIconViewAsyncTask(iconView, trackName,
                cursor.getInt(TracksQuery.TRACK_COLOR), mBitmapCache).execute(context);
    }

    /** {@link com.google.android.apps.iosched.provider.ScheduleContract.Tracks} query parameters. */
    public interface TracksQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Tracks.TRACK_ID,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_ABSTRACT,
                ScheduleContract.Tracks.TRACK_COLOR,
                ScheduleContract.Tracks.TRACK_LEVEL,
                ScheduleContract.Tracks.TRACK_META,
        };

        String[] PROJECTION_WITH_SESSIONS_COUNT = {
                BaseColumns._ID,
                ScheduleContract.Tracks.TRACK_ID,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_ABSTRACT,
                ScheduleContract.Tracks.TRACK_COLOR,
                ScheduleContract.Tracks.TRACK_LEVEL,
                ScheduleContract.Tracks.TRACK_META,
                ScheduleContract.Tracks.SESSIONS_COUNT,
        };

        String[] PROJECTION_WITH_OFFICE_HOURS_COUNT = {
                BaseColumns._ID,
                ScheduleContract.Tracks.TRACK_ID,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_ABSTRACT,
                ScheduleContract.Tracks.TRACK_COLOR,
                ScheduleContract.Tracks.TRACK_LEVEL,
                ScheduleContract.Tracks.TRACK_META,
                ScheduleContract.Tracks.OFFICE_HOURS_COUNT,
        };

        String[] PROJECTION_WITH_SANDBOX_COUNT = {
                BaseColumns._ID,
                ScheduleContract.Tracks.TRACK_ID,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_ABSTRACT,
                ScheduleContract.Tracks.TRACK_COLOR,
                ScheduleContract.Tracks.TRACK_LEVEL,
                ScheduleContract.Tracks.TRACK_META,
                ScheduleContract.Tracks.SANDBOX_COUNT,
        };

        int _ID = 0;
        int TRACK_ID = 1;
        int TRACK_NAME = 2;
        int TRACK_ABSTRACT = 3;
        int TRACK_COLOR = 4;
        int TRACK_LEVEL = 5;
        int TRACK_META = 6;
    }
}
