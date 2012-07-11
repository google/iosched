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
import com.google.android.apps.iosched.ui.tablet.SessionsVendorsMultiPaneActivity;
import com.google.android.apps.iosched.ui.widget.SimpleSectionedListAdapter;
import com.google.android.apps.iosched.util.ParserUtils;
import com.google.android.apps.iosched.util.SessionsHelper;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.android.apps.iosched.util.actionmodecompat.ActionMode;

import com.actionbarsherlock.app.SherlockListFragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.apps.iosched.util.LogUtils.LOGW;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that shows the user's customized schedule, including sessions that she has chosen,
 * and common conference items such as keynote sessions, lunch breaks, after hours party, etc.
 */
public class MyScheduleFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ActionMode.Callback {

    private static final String TAG = makeLogTag(MyScheduleFragment.class);

    private SimpleSectionedListAdapter mAdapter;
    private MyScheduleAdapter mScheduleAdapter;
    private SparseArray<String> mLongClickedItemData;
    private View mLongClickedView;
    private ActionMode mActionMode;
    private boolean mScrollToNow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The MyScheduleAdapter is wrapped in a SimpleSectionedListAdapter so that
        // we can show list headers separating out the different days of the conference
        // (Wednesday/Thursday/Friday).
        mScheduleAdapter = new MyScheduleAdapter(getActivity());
        mAdapter = new SimpleSectionedListAdapter(getActivity(),
                R.layout.list_item_schedule_header, mScheduleAdapter);
        setListAdapter(mAdapter);

        if (savedInstanceState == null) {
            mScrollToNow = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.fragment_list_with_empty_container, container, false);
        inflater.inflate(R.layout.empty_waiting_for_sync,
                (ViewGroup) root.findViewById(android.R.id.empty), true);
        root.setBackgroundColor(Color.WHITE);
        ListView listView = (ListView) root.findViewById(android.R.id.list);
        listView.setItemsCanFocus(true);
        listView.setCacheColorHint(Color.WHITE);
        listView.setSelector(android.R.color.transparent);
        //listView.setEmptyView(root.findViewById(android.R.id.empty));
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a
        // FragmentPagerAdapter in the fragment's onCreate may cause the same LoaderManager to be
        // dealt to multiple fragments because their mIndex is -1 (haven't been added to the
        // activity yet). Thus, we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (getActivity() == null) {
                return;
            }

            Loader<Cursor> loader = getLoaderManager().getLoader(0);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.getContentResolver().registerContentObserver(
                ScheduleContract.Sessions.CONTENT_URI, true, mObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    // LoaderCallbacks
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(),
                ScheduleContract.Blocks.CONTENT_URI,
                BlocksQuery.PROJECTION,
                null,
                null,
                ScheduleContract.Blocks.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        long currentTime = UIUtils.getCurrentTime(getActivity());
        int firstNowPosition = ListView.INVALID_POSITION;

        List<SimpleSectionedListAdapter.Section> sections =
                new ArrayList<SimpleSectionedListAdapter.Section>();
        cursor.moveToFirst();
        long previousBlockStart = -1;
        long blockStart, blockEnd;
        while (!cursor.isAfterLast()) {
            blockStart = cursor.getLong(BlocksQuery.BLOCK_START);
            blockEnd = cursor.getLong(BlocksQuery.BLOCK_END);
            if (!UIUtils.isSameDay(previousBlockStart, blockStart)) {
                sections.add(new SimpleSectionedListAdapter.Section(cursor.getPosition(),
                        DateUtils.formatDateTime(getActivity(), blockStart,
                                DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE
                                        | DateUtils.FORMAT_SHOW_WEEKDAY)));
            }
            if (mScrollToNow && firstNowPosition == ListView.INVALID_POSITION
                    // if we're currently in this block, or we're not in a block
                    // and this
                    // block is in the future, then this is the scroll position
                    && ((blockStart < currentTime && currentTime < blockEnd)
                    || blockStart > currentTime)) {
                firstNowPosition = cursor.getPosition();
            }
            previousBlockStart = blockStart;
            cursor.moveToNext();
        }

        mScheduleAdapter.changeCursor(cursor);

        SimpleSectionedListAdapter.Section[] dummy =
                new SimpleSectionedListAdapter.Section[sections.size()];
        mAdapter.setSections(sections.toArray(dummy));

        if (mScrollToNow && firstNowPosition != ListView.INVALID_POSITION) {
            firstNowPosition = mAdapter.positionToSectionedPosition(firstNowPosition);
            getListView().setSelectionFromTop(firstNowPosition,
                    getResources().getDimensionPixelSize(R.dimen.list_scroll_top_offset));
            mScrollToNow = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        SessionsHelper helper = new SessionsHelper(getActivity());
        String title = mLongClickedItemData.get(BlocksQuery.STARRED_SESSION_TITLE);
        String hashtags = mLongClickedItemData.get(BlocksQuery.STARRED_SESSION_HASHTAGS);
        String url = mLongClickedItemData.get(BlocksQuery.STARRED_SESSION_URL);
        boolean handled = false;
        switch (item.getItemId()) {
            case R.id.menu_map:
                String roomId = mLongClickedItemData.get(BlocksQuery.STARRED_SESSION_ROOM_ID);
                helper.startMapActivity(roomId);
                handled = true;
                break;
            case R.id.menu_star:
                String sessionId = mLongClickedItemData.get(BlocksQuery.STARRED_SESSION_ID);
                Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(sessionId);
                helper.setSessionStarred(sessionUri, false, title);
                handled = true;
                break;
            case R.id.menu_share:
                // On ICS+ devices, we normally won't reach this as ShareActionProvider will handle
                // sharing.
                helper.shareSession(getActivity(), R.string.share_template, title, hashtags, url);
                handled = true;
                break;
            case R.id.menu_social_stream:
                helper.startSocialStream(hashtags);
                handled = true;
                break;
            default:
                LOGW(TAG, "Unknown action taken");
        }
        mActionMode.finish();
        return handled;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.sessions_context, menu);
        MenuItem starMenuItem = menu.findItem(R.id.menu_star);
        starMenuItem.setTitle(R.string.description_remove_schedule);
        starMenuItem.setIcon(R.drawable.ic_action_remove_schedule);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        if (mLongClickedView != null) {
            UIUtils.setActivatedCompat(mLongClickedView, false);
        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * A list adapter that shows schedule blocks as list items. It handles a number of different
     * cases, such as empty blocks where the user has not chosen a session, blocks with conflicts
     * (i.e. multiple sessions chosen), non-session blocks, etc.
     */
    private class MyScheduleAdapter extends CursorAdapter {

        public MyScheduleAdapter(Context context) {
            super(context, null, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_schedule_block,
                    parent, false);
        }

        @Override
        public void bindView(View view, Context context, final Cursor cursor) {
            final String type = cursor.getString(BlocksQuery.BLOCK_TYPE);

            final String blockId = cursor.getString(BlocksQuery.BLOCK_ID);
            final String blockTitle = cursor.getString(BlocksQuery.BLOCK_TITLE);
            final long blockStart = cursor.getLong(BlocksQuery.BLOCK_START);
            final long blockEnd = cursor.getLong(BlocksQuery.BLOCK_END);
            final String blockMeta = cursor.getString(BlocksQuery.BLOCK_META);
            final String blockTimeString = UIUtils.formatBlockTimeString(blockStart, blockEnd,
                    context);

            final TextView timeView = (TextView) view.findViewById(R.id.block_time);
            final TextView titleView = (TextView) view.findViewById(R.id.block_title);
            final TextView subtitleView = (TextView) view.findViewById(R.id.block_subtitle);
            final ImageButton extraButton = (ImageButton) view.findViewById(R.id.extra_button);
            final View primaryTouchTargetView = view.findViewById(R.id.list_item_middle_container);

            final Resources res = getResources();

            String subtitle;

            boolean isLiveStreamed = false;
            primaryTouchTargetView.setOnLongClickListener(null);
            UIUtils.setActivatedCompat(primaryTouchTargetView, false);

            if (ParserUtils.BLOCK_TYPE_SESSION.equals(type)
                    || ParserUtils.BLOCK_TYPE_CODE_LAB.equals(type)) {
                final int numStarredSessions = cursor.getInt(BlocksQuery.NUM_STARRED_SESSIONS);
                final String starredSessionId = cursor.getString(BlocksQuery.STARRED_SESSION_ID);
                View.OnClickListener allSessionsListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final Uri sessionsUri = ScheduleContract.Blocks.buildSessionsUri(blockId);
                        final Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
                        intent.putExtra(Intent.EXTRA_TITLE, blockTimeString);
                        startActivity(intent);
                    }
                };

                if (numStarredSessions == 0) {
                    // 0 sessions starred
                    titleView.setText(getString(R.string.schedule_empty_slot_title_template,
                            TextUtils.isEmpty(blockTitle)
                                    ? ""
                                    : (" " + blockTitle.toLowerCase())));
                    titleView.setTextColor(res.getColorStateList(R.color.body_text_1_positive));
                    subtitle = getString(R.string.schedule_empty_slot_subtitle);
                    extraButton.setVisibility(View.GONE);

                    primaryTouchTargetView.setOnClickListener(allSessionsListener);

                } else if (numStarredSessions == 1) {
                    // exactly 1 session starred
                    final String starredSessionTitle =
                            cursor.getString(BlocksQuery.STARRED_SESSION_TITLE);
                    titleView.setText(starredSessionTitle);
                    titleView.setTextColor(res.getColorStateList(R.color.body_text_1));
                    subtitle = cursor.getString(BlocksQuery.STARRED_SESSION_ROOM_NAME);
                    if (subtitle == null) {
                        // TODO: remove this WAR for API not returning rooms for code labs
                        subtitle = getString(
                                starredSessionTitle.contains("Code Lab")
                                        ? R.string.codelab_room
                                        : R.string.unknown_room);
                    }
                    isLiveStreamed = !TextUtils.isEmpty(
                            cursor.getString(BlocksQuery.STARRED_SESSION_LIVESTREAM_URL));
                    extraButton.setVisibility(View.VISIBLE);
                    extraButton.setOnClickListener(allSessionsListener);
                    if (mLongClickedItemData != null && mActionMode != null
                            && mLongClickedItemData.get(BlocksQuery.STARRED_SESSION_ID, "").equals(
                                    starredSessionId)) {
                        UIUtils.setActivatedCompat(primaryTouchTargetView, true);
                        mLongClickedView = primaryTouchTargetView;
                    }

                    primaryTouchTargetView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(
                                    starredSessionId);
                            final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
                            intent.putExtra(SessionsVendorsMultiPaneActivity.EXTRA_MASTER_URI,
                                    ScheduleContract.Blocks.buildSessionsUri(blockId));
                            intent.putExtra(Intent.EXTRA_TITLE, blockTimeString);
                            startActivity(intent);
                        }
                    });

                    primaryTouchTargetView.setOnLongClickListener(new OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (mActionMode != null) {
                                // CAB already displayed, ignore
                                return true;
                            }
                            mLongClickedView = primaryTouchTargetView;
                            String hashtags = cursor.getString(BlocksQuery.STARRED_SESSION_HASHTAGS);
                            String url = cursor.getString(BlocksQuery.STARRED_SESSION_URL);
                            String roomId = cursor.getString(BlocksQuery.STARRED_SESSION_ROOM_ID);
                            mLongClickedItemData = new SparseArray<String>();
                            mLongClickedItemData.put(BlocksQuery.STARRED_SESSION_ID,
                                    starredSessionId);
                            mLongClickedItemData.put(BlocksQuery.STARRED_SESSION_TITLE,
                                    starredSessionTitle);
                            mLongClickedItemData.put(BlocksQuery.STARRED_SESSION_HASHTAGS, hashtags);
                            mLongClickedItemData.put(BlocksQuery.STARRED_SESSION_URL, url);
                            mLongClickedItemData.put(BlocksQuery.STARRED_SESSION_ROOM_ID, roomId);
                            mActionMode = ActionMode.start(getActivity(), MyScheduleFragment.this);
                            UIUtils.setActivatedCompat(primaryTouchTargetView, true);
                            return true;
                        }
                    });

                } else {
                    // 2 or more sessions starred
                    titleView.setText(getString(R.string.schedule_conflict_title,
                            numStarredSessions));
                    titleView.setTextColor(res.getColorStateList(R.color.body_text_1));
                    subtitle = getString(R.string.schedule_conflict_subtitle);
                    extraButton.setVisibility(View.VISIBLE);
                    extraButton.setOnClickListener(allSessionsListener);

                    primaryTouchTargetView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final Uri sessionsUri = ScheduleContract.Blocks
                                    .buildStarredSessionsUri(
                                    blockId);
                            final Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
                            intent.putExtra(Intent.EXTRA_TITLE, blockTimeString);
                            startActivity(intent);
                        }
                    });
                }

                subtitleView.setTextColor(res.getColorStateList(R.color.body_text_2));
                primaryTouchTargetView.setEnabled(true);

            } else if (ParserUtils.BLOCK_TYPE_KEYNOTE.equals(type)) {
                final String starredSessionId = cursor.getString(BlocksQuery.STARRED_SESSION_ID);
                final String starredSessionTitle =
                        cursor.getString(BlocksQuery.STARRED_SESSION_TITLE);

                long currentTimeMillis = UIUtils.getCurrentTime(context);
                boolean past = (currentTimeMillis > blockEnd
                        && currentTimeMillis < UIUtils.CONFERENCE_END_MILLIS);
                boolean present = !past && (currentTimeMillis >= blockStart);
                boolean canViewStream = present && UIUtils.hasHoneycomb();

                isLiveStreamed = true;
                titleView.setTextColor(canViewStream
                        ? res.getColorStateList(R.color.body_text_1)
                        : res.getColorStateList(R.color.body_text_disabled));
                subtitleView.setTextColor(canViewStream
                        ? res.getColorStateList(R.color.body_text_2)
                        : res.getColorStateList(R.color.body_text_disabled));
                subtitle = getString(R.string.keynote_room);

                titleView.setText(starredSessionTitle);
                extraButton.setVisibility(View.GONE);
                primaryTouchTargetView.setEnabled(canViewStream);
                primaryTouchTargetView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(
                                starredSessionId);
                        Intent livestreamIntent = new Intent(Intent.ACTION_VIEW, sessionUri);
                        livestreamIntent.setClass(getActivity(), SessionLivestreamActivity.class);
                        startActivity(livestreamIntent);
                    }
                });

            } else {
                titleView.setTextColor(res.getColorStateList(R.color.body_text_disabled));
                subtitleView.setTextColor(res.getColorStateList(R.color.body_text_disabled));
                subtitle = blockMeta;
                titleView.setText(blockTitle);
                extraButton.setVisibility(View.GONE);
                primaryTouchTargetView.setEnabled(false);
                primaryTouchTargetView.setOnClickListener(null);
            }

            timeView.setText(DateUtils.formatDateTime(context, blockStart,
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_12HOUR));

            // Show past/present/future and livestream status for this block.
            UIUtils.updateTimeAndLivestreamBlockUI(context,
                    blockStart, blockEnd, isLiveStreamed,
                    view, titleView, subtitleView, subtitle);
        }
    }

    private interface BlocksQuery {

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Blocks.BLOCK_ID,
                ScheduleContract.Blocks.BLOCK_TITLE,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
                ScheduleContract.Blocks.BLOCK_TYPE,
                ScheduleContract.Blocks.BLOCK_META,
                ScheduleContract.Blocks.SESSIONS_COUNT,
                ScheduleContract.Blocks.NUM_STARRED_SESSIONS,
                ScheduleContract.Blocks.STARRED_SESSION_ID,
                ScheduleContract.Blocks.STARRED_SESSION_TITLE,
                ScheduleContract.Blocks.STARRED_SESSION_ROOM_NAME,
                ScheduleContract.Blocks.STARRED_SESSION_ROOM_ID,
                ScheduleContract.Blocks.STARRED_SESSION_HASHTAGS,
                ScheduleContract.Blocks.STARRED_SESSION_URL,
                ScheduleContract.Blocks.STARRED_SESSION_LIVESTREAM_URL,
        };

        int _ID = 0;
        int BLOCK_ID = 1;
        int BLOCK_TITLE = 2;
        int BLOCK_START = 3;
        int BLOCK_END = 4;
        int BLOCK_TYPE = 5;
        int BLOCK_META = 6;
        int SESSIONS_COUNT = 7;
        int NUM_STARRED_SESSIONS = 8;
        int STARRED_SESSION_ID = 9;
        int STARRED_SESSION_TITLE = 10;
        int STARRED_SESSION_ROOM_NAME = 11;
        int STARRED_SESSION_ROOM_ID = 12;
        int STARRED_SESSION_HASHTAGS = 13;
        int STARRED_SESSION_URL = 14;
        int STARRED_SESSION_LIVESTREAM_URL = 15;
    }
}
