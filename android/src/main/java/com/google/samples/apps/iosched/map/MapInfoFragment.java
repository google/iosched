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
import com.google.samples.apps.iosched.map.util.SingleSessionLoader;
import com.google.samples.apps.iosched.map.util.OverviewSessionLoader;
import com.google.samples.apps.iosched.map.util.MarkerModel;
import com.google.samples.apps.iosched.map.util.SessionLoader;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.MapUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Displays information about the map.
 * This includes a list of sessions that are directly loaded by this fragment.
 */
public abstract class MapInfoFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {


    private static final int QUERY_TOKEN_SESSION_ROOM = 0x1;
    private static final int QUERY_TOKEN_SUBTITLE = 0x2;
    private static final String QUERY_ARG_ROOMID = "roomid";
    private static final String QUERY_ARG_ROOMTITLE = "roomtitle";
    private static final String QUERY_ARG_ROOMTYPE = "roomicon";

    protected TextView mTitle;
    protected TextView mSubtitle;
    protected ImageView mIcon;

    protected ListView mList;

    protected Callback mCallback = sDummyCallback;

    private static Callback sDummyCallback = new Callback() {
        @Override
        public void onInfoSizeChanged(int left, int top, int right, int bottom) {
        }

        @Override
        public void onSessionClicked(String id) {
        }
    };


    private AdapterView.OnItemClickListener mListClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SessionAdapter adapter = (SessionAdapter) parent.getAdapter();

            final String sessionId = adapter.getSessionIdAtPosition(position);
            mCallback.onSessionClicked(sessionId);
        }
    };


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callback)) {
            throw new ClassCastException("Activity must implement fragment's callback.");
        }

        mCallback = (Callback) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = sDummyCallback;
    }

    @Nullable
    @Override
    public abstract View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState);

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState, int layout) {
        View root = inflater.inflate(layout, container, false);

        mTitle = (TextView) root.findViewById(R.id.map_info_title);
        mSubtitle = (TextView) root.findViewById(R.id.map_info_subtitle);
        mIcon = (ImageView) root.findViewById(R.id.map_info_icon);
        mIcon.setColorFilter(getResources().getColor(R.color.my_schedule_icon_default));
        mList = (ListView) root.findViewById(R.id.map_info_list);
        mList.setOnItemClickListener(mListClickListener);

        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != QUERY_TOKEN_SESSION_ROOM && id != QUERY_TOKEN_SUBTITLE) {
            return null;
        }

        final long time = UIUtils.getCurrentTime(getActivity());
        final String roomId = args.getString(QUERY_ARG_ROOMID);
        final String roomTitle = args.getString(QUERY_ARG_ROOMTITLE);
        final int roomType = args.getInt(QUERY_ARG_ROOMTYPE);

        if (id == QUERY_TOKEN_SESSION_ROOM) {
            return new OverviewSessionLoader(getActivity(), roomId, roomTitle, roomType, time);
        } else if (id == QUERY_TOKEN_SUBTITLE) {
            return new SingleSessionLoader(getActivity(), roomId, roomTitle, roomType);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }
        switch (loader.getId()) {
            case QUERY_TOKEN_SESSION_ROOM: {
                SessionLoader sessionLoader = (SessionLoader) loader;
                showSessionList(sessionLoader.getRoomTitle(), sessionLoader.getRoomType(), cursor);
                break;
            }
            case QUERY_TOKEN_SUBTITLE: {
                SessionLoader sessionLoader = (SessionLoader) loader;
                showSessionSubtitle(sessionLoader.getRoomTitle(), sessionLoader.getRoomType(),
                        cursor);
            }
        }
    }

    /**
     * Creates a new instance depending of the form factor of the device.
     * For tablets, creates an {@link com.google.samples.apps.iosched.map.InlineInfoFragment},
     * for other form factors a {@link com.google.samples.apps.iosched.map.SlideableInfoFragment}.
     *
     * @see com.google.samples.apps.iosched.util.UIUtils#isTablet(android.content.Context)
     */
    public static MapInfoFragment newInstace(Context c) {
        if (UIUtils.isTablet(c)) {
            return InlineInfoFragment.newInstance();
        } else {
            return SlideableInfoFragment.newInstance();
        }
    }

    private void showSessionList(String roomTitle, int roomType, Cursor sessions) {
        if (sessions == null || sessions.isAfterLast()) {
            onSessionLoadingFailed(roomTitle, roomType);
            return;
        }

        onSessionsLoaded(roomTitle, roomType, sessions);
        mList.setAdapter(new SessionAdapter(getActivity(), sessions, 0,
                MapUtils.hasInfoSessionListIcons(roomType)));
    }

    private void showSessionSubtitle(String roomTitle, int roomType, Cursor sessions) {
        if (sessions == null || sessions.isAfterLast()) {
            onSessionLoadingFailed(roomTitle, roomType);
            return;
        }
        sessions.moveToFirst();

        final String title = roomTitle;
        final String subtitle = sessions.getString(SingleSessionLoader.Query.SESSION_ABSTRACT);

        setHeader(MapUtils.getRoomIcon(roomType), title, subtitle);
        mList.setVisibility(View.GONE);

        onRoomSubtitleLoaded(title, roomType, subtitle);
    }

    /**
     * Called when the subtitle has been loaded for a room.
     */
    protected void onRoomSubtitleLoaded(String roomTitle, int roomType, String subTitle){

    }
    /**
     * Called when the session list is about to be loaded for a new room.
     */
    protected void onSessionListLoading(String roomId, String roomTitle) {
        // No default behavior
    }

    /**
     * Prepares and starts a SessionLoader for the specified query token.
     */
    private void loadSessions(String roomId, String roomTitle, int roomType, int queryToken){
        setHeader(MapUtils.getRoomIcon(roomType), roomTitle, null);
        onSessionListLoading(roomId, roomTitle);

        // Load the following sessions for this room
        LoaderManager lm = getLoaderManager();
        Bundle args = new Bundle();
        args.putString(QUERY_ARG_ROOMID, roomId);
        args.putString(QUERY_ARG_ROOMTITLE, roomTitle);
        args.putInt(QUERY_ARG_ROOMTYPE, roomType);
        lm.restartLoader(queryToken, args, this);
    }

    /**
     * Called when the abstract of the first session in this room is to be used as the subtitle.
     */
    public void showFirstSessionTitle(String roomId, String roomTitle, int roomType) {
        loadSessions(roomId,roomTitle,roomType, QUERY_TOKEN_SUBTITLE);
    }

    /**
     * Called when a session list is to be displayed and has to be loaded.
     */
    public void showSessionList(String roomId, String roomTitle, int roomType) {
        loadSessions(roomId,roomTitle,roomType, QUERY_TOKEN_SESSION_ROOM);
    }

    protected void onSessionsLoaded(String roomTitle, int roomType, Cursor cursor) {
        setHeader(MapUtils.getRoomIcon(roomType), roomTitle, null);
        mList.setVisibility(View.VISIBLE);
    }

    protected void onSessionLoadingFailed(String roomTitle, int roomType) {
        setHeader(MapUtils.getRoomIcon(roomType), roomTitle, null);
        mList.setVisibility(View.GONE);
    }

    public void showMoscone() {
        setHeader(MapUtils.getRoomIcon(MarkerModel.TYPE_MOSCONE), R.string.map_moscone,
                R.string.map_moscone_address);
        mList.setVisibility(View.GONE);
    }

    protected void setHeader(int icon, int title, int subTitle) {
        mIcon.setImageResource(icon);

        if (title != 0) {
            mTitle.setText(title);
            mTitle.setVisibility(View.VISIBLE);
        } else {
            mTitle.setVisibility(View.GONE);
        }

        if (subTitle != 0) {
            mSubtitle.setText(subTitle);
            mSubtitle.setVisibility(View.VISIBLE);
        } else {
            mSubtitle.setVisibility(View.GONE);
        }

    }

    private void setHeader(int icon, String title, String subTitle) {
        mIcon.setImageResource(icon);

        if (title != null && !title.isEmpty()) {
            mTitle.setText(title);
            mTitle.setVisibility(View.VISIBLE);
        } else {
            mTitle.setVisibility(View.GONE);
        }

        if (subTitle != null && !subTitle.isEmpty()) {
            mSubtitle.setText(subTitle);
            mSubtitle.setVisibility(View.VISIBLE);
        } else {
            mSubtitle.setVisibility(View.GONE);
        }
    }

    public void showTitleOnly(int roomType, String title) {
        setHeader(MapUtils.getRoomIcon(roomType), title, null);
        mList.setVisibility(View.GONE);
    }

    public abstract void hide();

    public abstract boolean isExpanded();

    public abstract void minimize();

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    interface Callback {

        public void onInfoSizeChanged(int left, int top, int right, int bottom);

        public void onSessionClicked(String id);
    }


    /**
     * Adapter that displays a list of sessions.
     * This includes its title, time slot and icon.
     */
    class SessionAdapter extends CursorAdapter {

        private StringBuilder mStringBuilder = new StringBuilder();
        private final boolean mDisplayIcons;

        public SessionAdapter(Context context, Cursor c, int flags, boolean displayIcons) {
            super(context, c, flags);
            mDisplayIcons = displayIcons;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            // Initialise the row and set a holder for views within it
            final View view = ((LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.map_item_session, parent, false);
            final ItemHolder holder = initialiseHolder(view);
            view.setTag(holder);
            return view;
        }

        public String getSessionIdAtPosition(int position) {
            Cursor cursor = getCursor();
            if (cursor == null) {
                return null;
            }

            if (cursor.moveToPosition(position)) {
                return cursor
                        .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_ID));
            } else {
                return null;
            }

        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ItemHolder holder = (ItemHolder) view.getTag();
            if (holder == null) {
                holder = initialiseHolder(view);
                view.setTag(holder);
            }
            final String title = cursor
                    .getString(OverviewSessionLoader.Query.SESSION_TITLE);
            final String sessionId = cursor
                    .getString(OverviewSessionLoader.Query.SESSION_ID);
            final long blockStart = cursor
                    .getLong(OverviewSessionLoader.Query.SESSION_START);
            final long blockEnd = cursor
                    .getLong(OverviewSessionLoader.Query.SESSION_END);
            final String sessionTag = cursor
                    .getString(OverviewSessionLoader.Query.SESSION_TAGS);

            final int sessionType = ScheduleHelper.detectSessionType(sessionTag);
            final String text = UIUtils.formatIntervalTimeString(blockStart, blockEnd,
                    mStringBuilder, context);

            holder.title.setText(title);
            holder.title.setTag(sessionId);
            holder.text.setText(text);
            if (mDisplayIcons) {
                holder.image.setImageResource(UIUtils.getSessionIcon(sessionType));
            }
        }

        private ItemHolder initialiseHolder(View view) {
            ItemHolder holder = new ItemHolder();
            holder.title = (TextView) view.findViewById(R.id.map_item_title);
            holder.text = (TextView) view.findViewById(R.id.map_item_text);
            holder.image = (ImageView) view.findViewById(R.id.map_item_image);
            if (!mDisplayIcons) {
                holder.image.setVisibility(View.INVISIBLE);
            } else {
                holder.image.setVisibility(View.VISIBLE);
            }
            return holder;
        }

        class ItemHolder {

            TextView title;
            TextView text;
            ImageView image;
        }
    }
}
