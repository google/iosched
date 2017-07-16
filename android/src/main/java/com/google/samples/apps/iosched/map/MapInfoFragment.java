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
import com.google.samples.apps.iosched.util.MapUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    protected RecyclerView mList;

    protected Callback mCallback = sDummyCallback;

    private static Callback sDummyCallback = new Callback() {
        @Override
        public void onInfoSizeChanged(int left, int top, int right, int bottom) {
        }

        @Override
        public void onSessionClicked(String id) {
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
        mList = (RecyclerView) root.findViewById(R.id.map_info_list);
        final Context context = mList.getContext();
        mList.addItemDecoration(new DividerDecoration(context));
        mList.setLayoutManager(new LinearLayoutManager(context));
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != QUERY_TOKEN_SESSION_ROOM && id != QUERY_TOKEN_SUBTITLE) {
            return null;
        }

        final long time = TimeUtils.getCurrentTime(getActivity());
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
        mList.setAdapter(new SessionAdapter(getActivity(), sessions,
                MapUtils.hasInfoSessionListIcons(roomType), mOnClickListener));
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

    public void showVenue() {
        setHeader(MapUtils.getRoomIcon(MarkerModel.TYPE_VENUE), R.string.map_venue_name,
                R.string.map_venue_address);
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

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            String sessionId = (String) v.getTag(R.id.tag_session_id);
            if (sessionId != null) {
                mCallback.onSessionClicked(sessionId);
            }
        }

    };

    private static class DividerDecoration extends RecyclerView.ItemDecoration {

        private final Paint mPaint = new Paint();
        private final int mHeight;

        public DividerDecoration(Context context) {
            final Resources resources = context.getResources();
            mPaint.setColor(ResourcesCompat.getColor(resources, R.color.divider,
                    context.getTheme()));
            mHeight = resources.getDimensionPixelSize(R.dimen.divider_height);
        }

        @Override
        public void getItemOffsets(final Rect outRect, final View view, final RecyclerView parent,
                final RecyclerView.State state) {
            outRect.set(0, 0, 0, mHeight);
        }

        @Override
        public void onDraw(final Canvas c, final RecyclerView parent,
                final RecyclerView.State state) {
            int width = parent.getWidth();
            for (int i = 0, count = parent.getChildCount(); i < count; i++) {
                View child = parent.getChildAt(i);
                int bottom = child.getBottom();
                c.drawRect(0, bottom, width, bottom + mHeight, mPaint);
            }
        }
    }

    /**
     * Adapter that displays a list of sessions.
     * This includes its title, time slot and icon.
     */
    private static class SessionAdapter extends RecyclerView.Adapter<ItemHolder> {

        private final StringBuilder mStringBuilder = new StringBuilder();

        private final Context mContext;

        private final Cursor mCursor;

        private final boolean mDisplayIcons;

        private final View.OnClickListener mListener;

        public SessionAdapter(Context context, Cursor cursor, boolean displayIcons,
                View.OnClickListener listener) {
            mContext = context;
            mCursor = cursor;
            mDisplayIcons = displayIcons;
            mListener = listener;
        }

        public String getSessionIdAtPosition(int position) {
            if (mCursor.moveToPosition(position)) {
                return  mCursor.getString(OverviewSessionLoader.Query.SESSION_ID);
            } else {
                return null;
            }
        }

        @Override
        public ItemHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final ItemHolder holder = new ItemHolder(LayoutInflater.from(mContext), parent);
            holder.image.setVisibility(mDisplayIcons ? View.VISIBLE : View.INVISIBLE);
            holder.itemView.setOnClickListener(mListener);
            return holder;
        }

        @Override
        public void onBindViewHolder(final ItemHolder holder, final int position) {
            mCursor.moveToPosition(position);
            final String title = mCursor.getString(OverviewSessionLoader.Query.SESSION_TITLE);
            final String sessionId = mCursor.getString(OverviewSessionLoader.Query.SESSION_ID);
            final long blockStart = mCursor.getLong(OverviewSessionLoader.Query.SESSION_START);
            final long blockEnd = mCursor.getLong(OverviewSessionLoader.Query.SESSION_END);
            final String sessionTag = mCursor.getString(OverviewSessionLoader.Query.SESSION_TAGS);
            final int sessionType = ScheduleHelper.detectSessionType(sessionTag);
            final String text = UIUtils.formatIntervalTimeString(blockStart, blockEnd,
                    mStringBuilder, mContext);

            holder.itemView.setTag(R.id.tag_session_id, sessionId);
            holder.title.setText(title);
            holder.text.setText(text);
            if (mDisplayIcons) {
                holder.image.setImageResource(UIUtils.getSessionIcon(sessionType));
            }
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

    }

    private static class ItemHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView text;
        ImageView image;

        public ItemHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.map_item_session, parent, false));
            title = (TextView) itemView.findViewById(R.id.map_item_title);
            text = (TextView) itemView.findViewById(R.id.map_item_text);
            image = (ImageView) itemView.findViewById(R.id.map_item_image);
        }

    }

}
