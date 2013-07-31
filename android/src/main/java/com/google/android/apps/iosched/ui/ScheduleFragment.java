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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.view.*;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.tablet.SessionsSandboxMultiPaneActivity;
import com.google.android.apps.iosched.util.PrefUtils;
import com.google.android.apps.iosched.util.SessionsHelper;
import com.google.android.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import static com.google.android.apps.iosched.util.LogUtils.LOGW;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

public class ScheduleFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ActionMode.Callback {

    private static final String TAG = makeLogTag(ScheduleFragment.class);

    private static final String STATE_ACTION_MODE = "actionMode";

    private SimpleSectionedListAdapter mAdapter;
    private MyScheduleAdapter mScheduleAdapter;
    private SparseArray<String> mSelectedItemData;
    private View mLongClickedView;
    private ActionMode mActionMode;
    private boolean mScrollToNow;
    private boolean mActionModeStarted = false;
    private Bundle mViewDestroyedInstanceState;

    private StringBuilder mBuffer = new StringBuilder();
    private Formatter mFormatter = new Formatter(mBuffer, Locale.getDefault());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mScheduleAdapter = new MyScheduleAdapter(getActivity());
        mAdapter = new SimpleSectionedListAdapter(getActivity(),
                R.layout.list_item_schedule_header, mScheduleAdapter);
        setListAdapter(mAdapter);

        if (savedInstanceState == null) {
            mScrollToNow = true;
        }
    }

    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        persistActionModeState(outState);
	}

    private void persistActionModeState(Bundle outState) {
        if (outState != null && mActionModeStarted && mSelectedItemData != null) {
            outState.putStringArray(STATE_ACTION_MODE, new String[]{
                    mSelectedItemData.get(BlocksQuery.STARRED_SESSION_ID),
                    mSelectedItemData.get(BlocksQuery.STARRED_SESSION_TITLE),
                    mSelectedItemData.get(BlocksQuery.STARRED_SESSION_HASHTAGS),
                    mSelectedItemData.get(BlocksQuery.STARRED_SESSION_URL),
                    mSelectedItemData.get(BlocksQuery.STARRED_SESSION_ROOM_ID),
            });
        }
    }

    private void loadActionModeState(Bundle state) {
        if (state != null && state.containsKey(STATE_ACTION_MODE)) {
            mActionModeStarted = true;
            mActionMode = ((ActionBarActivity) getActivity()).startSupportActionMode(this);
            String[] data = state.getStringArray(STATE_ACTION_MODE);
            if (data != null) {
                mSelectedItemData = new SparseArray<String>();
                mSelectedItemData.put(BlocksQuery.STARRED_SESSION_ID, data[0]);
                mSelectedItemData.put(BlocksQuery.STARRED_SESSION_TITLE, data[1]);
                mSelectedItemData.put(BlocksQuery.STARRED_SESSION_HASHTAGS, data[2]);
                mSelectedItemData.put(BlocksQuery.STARRED_SESSION_URL, data[3]);
                mSelectedItemData.put(BlocksQuery.STARRED_SESSION_ROOM_ID, data[4]);
            }
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        // Hide the action mode when the fragment becomes invisible
        if (!menuVisible) {
            if (mActionModeStarted && mActionMode != null && mSelectedItemData != null) {
                mViewDestroyedInstanceState = new Bundle();
                persistActionModeState(mViewDestroyedInstanceState);
                mActionMode.finish();
            }

        } else if (mViewDestroyedInstanceState != null) {
            loadActionModeState(mViewDestroyedInstanceState);
            mViewDestroyedInstanceState = null;
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

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadActionModeState(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
            if (isAdded() && mAdapter != null) {
                if (PrefUtils.PREF_LOCAL_TIMES.equals(key)) {
                    PrefUtils.isUsingLocalTime(getActivity(), true); // force update
                    mAdapter.notifyDataSetInvalidated();
                } else if (PrefUtils.PREF_ATTENDEE_AT_VENUE.equals(key)) {
                    PrefUtils.isAttendeeAtVenue(getActivity(), true); // force update
                    getLoaderManager().restartLoader(0, null, ScheduleFragment.this);
                }
            }
        }
    };

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (!isAdded()) {
                return;
            }

            getLoaderManager().restartLoader(0, null, ScheduleFragment.this);
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.getContentResolver().registerContentObserver(
                ScheduleContract.Sessions.CONTENT_URI, true, mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        sp.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    // LoaderCallbacks
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        String liveStreamedOnlyBlocksSelection = "("
                + (UIUtils.shouldShowLiveSessionsOnly(getActivity())
                ? ScheduleContract.Blocks.BLOCK_TYPE + " NOT IN ('"
                + ScheduleContract.Blocks.BLOCK_TYPE_SESSION + "','"
                + ScheduleContract.Blocks.BLOCK_TYPE_CODELAB + "','"
                + ScheduleContract.Blocks.BLOCK_TYPE_OFFICE_HOURS + "','"
                + ScheduleContract.Blocks.BLOCK_TYPE_FOOD + "')"
                + " OR " + ScheduleContract.Blocks.NUM_LIVESTREAMED_SESSIONS + ">1 "
                : "1==1") + ")";
        String onlyStarredOfficeHoursSelection = "("
                + ScheduleContract.Blocks.BLOCK_TYPE + " != '"
                + ScheduleContract.Blocks.BLOCK_TYPE_OFFICE_HOURS
                + "' OR " + ScheduleContract.Blocks.NUM_STARRED_SESSIONS + ">0)";
        String excludeSandbox = "("+ScheduleContract.Blocks.BLOCK_TYPE + " != '"
                + ScheduleContract.Blocks.BLOCK_TYPE_SANDBOX +"')";
        
        return new CursorLoader(getActivity(),
                ScheduleContract.Blocks.CONTENT_URI,
                BlocksQuery.PROJECTION,
                liveStreamedOnlyBlocksSelection + " AND " + onlyStarredOfficeHoursSelection + " AND " + excludeSandbox,
                null,
                ScheduleContract.Blocks.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (!isAdded()) {
            return;
        }

        Context context = getActivity();
        long currentTime = UIUtils.getCurrentTime(getActivity());
        int firstNowPosition = ListView.INVALID_POSITION;

        String displayTimeZone = PrefUtils.getDisplayTimeZone(context).getID();

        List<SimpleSectionedListAdapter.Section> sections =
                new ArrayList<SimpleSectionedListAdapter.Section>();
        cursor.moveToFirst();
        long previousBlockStart = -1;
        long blockStart, blockEnd;
        while (!cursor.isAfterLast()) {
            blockStart = cursor.getLong(BlocksQuery.BLOCK_START);
            blockEnd = cursor.getLong(BlocksQuery.BLOCK_END);
            if (!UIUtils.isSameDayDisplay(previousBlockStart, blockStart, context)) {
                mBuffer.setLength(0);
                sections.add(new SimpleSectionedListAdapter.Section(cursor.getPosition(),
                        DateUtils.formatDateRange(
                                context, mFormatter,
                                blockStart, blockStart,
                                DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE
                                        | DateUtils.FORMAT_SHOW_WEEKDAY,
                                displayTimeZone).toString()));
            }
            if (mScrollToNow && firstNowPosition == ListView.INVALID_POSITION
                    // if we're currently in this block, or we're not in a block
                    // and this block is in the future, then this is the scroll position
                    && ((blockStart < currentTime && currentTime < blockEnd)
                    || blockStart > currentTime)) {
                firstNowPosition = cursor.getPosition();
            }
            previousBlockStart = blockStart;
            cursor.moveToNext();
        }

        mScheduleAdapter.swapCursor(cursor);

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

    private class MyScheduleAdapter extends CursorAdapter {
        public MyScheduleAdapter(Context context) {
            super(context, null, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.list_item_schedule_block,
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
                    mBuffer, context);

            final TextView timeView = (TextView) view.findViewById(R.id.block_time);
            final TextView endtimeView = (TextView) view.findViewById(R.id.block_endtime);
            final TextView titleView = (TextView) view.findViewById(R.id.block_title);
            final TextView subtitleView = (TextView) view.findViewById(R.id.block_subtitle);
            final ImageButton extraButton = (ImageButton) view.findViewById(R.id.extra_button);
            final View primaryTouchTargetView = view.findViewById(R.id.list_item_middle_container);

            final Resources res = getResources();

            String subtitle;

            boolean isLiveStreamed = false;
            primaryTouchTargetView.setOnLongClickListener(null);
            primaryTouchTargetView.setSelected(false);

            endtimeView.setText(null);

            titleView.setTextColor(res.getColorStateList(R.color.body_text_1_stateful));
            subtitleView.setTextColor(res.getColorStateList(R.color.body_text_2_stateful));

            if (ScheduleContract.Blocks.BLOCK_TYPE_SESSION.equals(type)
                    || ScheduleContract.Blocks.BLOCK_TYPE_CODELAB.equals(type)
                    || ScheduleContract.Blocks.BLOCK_TYPE_OFFICE_HOURS.equals(type)) {
                final int numStarredSessions = cursor.getInt(BlocksQuery.NUM_STARRED_SESSIONS);
                final String starredSessionId = cursor.getString(BlocksQuery.STARRED_SESSION_ID);
                View.OnClickListener allSessionsListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mActionModeStarted) {
                            return;
                        }

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
                    titleView.setTextColor(res.getColorStateList(
                            R.color.body_text_1_positive_stateful));
                    subtitle = getString(R.string.schedule_empty_slot_subtitle);
                    extraButton.setVisibility(View.GONE);

                    primaryTouchTargetView.setOnClickListener(allSessionsListener);
                    primaryTouchTargetView.setEnabled(!mActionModeStarted);

                } else if (numStarredSessions == 1) {
                    // exactly 1 session starred
                    final String starredSessionTitle =
                            cursor.getString(BlocksQuery.STARRED_SESSION_TITLE);
                    final String starredSessionHashtags = cursor.getString(BlocksQuery.STARRED_SESSION_HASHTAGS);
                    final String starredSessionUrl = cursor.getString(BlocksQuery.STARRED_SESSION_URL);
                    final String starredSessionRoomId = cursor.getString(BlocksQuery.STARRED_SESSION_ROOM_ID);
                    titleView.setText(starredSessionTitle);
                    subtitle = cursor.getString(BlocksQuery.STARRED_SESSION_ROOM_NAME);
                    if (subtitle == null) {
                        subtitle = getString(R.string.unknown_room);
                    }

                    // Determine if the session is in the past
                    long currentTimeMillis = UIUtils.getCurrentTime(context);
                    boolean conferenceEnded = currentTimeMillis > UIUtils.CONFERENCE_END_MILLIS;
                    boolean blockEnded = currentTimeMillis > blockEnd;
                    if (blockEnded && !conferenceEnded) {
                        subtitle = getString(R.string.session_finished);
                    }

                    isLiveStreamed = !TextUtils.isEmpty(
                            cursor.getString(BlocksQuery.STARRED_SESSION_LIVESTREAM_URL));
                    extraButton.setVisibility(View.VISIBLE);
                    extraButton.setOnClickListener(allSessionsListener);
                    extraButton.setEnabled(!mActionModeStarted);
                    if (mSelectedItemData != null && mActionModeStarted
                            && mSelectedItemData.get(BlocksQuery.STARRED_SESSION_ID, "").equals(
                                    starredSessionId)) {
                        primaryTouchTargetView.setSelected(true);
                        mLongClickedView = primaryTouchTargetView;
                    }

                    final Runnable restartActionMode = new Runnable() {
                        @Override
                        public void run() {
                            boolean currentlySelected = false;

                            if (mActionModeStarted
                                    && mSelectedItemData != null
                                    && starredSessionId.equals(mSelectedItemData.get(
                                    BlocksQuery.STARRED_SESSION_ID))) {
                                currentlySelected = true;
                            }

                            if (mActionMode != null) {
                                mActionMode.finish();
                                if (currentlySelected) {
                                    return;
                                }
                            }

                            mLongClickedView = primaryTouchTargetView;
                            mSelectedItemData = new SparseArray<String>();
                            mSelectedItemData.put(BlocksQuery.STARRED_SESSION_ID,
                                    starredSessionId);
                            mSelectedItemData.put(BlocksQuery.STARRED_SESSION_TITLE,
                                    starredSessionTitle);
                            mSelectedItemData.put(BlocksQuery.STARRED_SESSION_HASHTAGS,
                                    starredSessionHashtags);
                            mSelectedItemData.put(BlocksQuery.STARRED_SESSION_URL,
                                    starredSessionUrl);
                            mSelectedItemData.put(BlocksQuery.STARRED_SESSION_ROOM_ID,
                                    starredSessionRoomId);
                            mActionMode = ((ActionBarActivity) getActivity())
                                    .startSupportActionMode(ScheduleFragment.this);
                            primaryTouchTargetView.setSelected(true);
                        }
                    };

                    primaryTouchTargetView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (mActionModeStarted) {
                                restartActionMode.run();
                                return;
                            }

                            final Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(
                                    starredSessionId);
                            final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
                            intent.putExtra(SessionsSandboxMultiPaneActivity.EXTRA_MASTER_URI,
                                    ScheduleContract.Blocks.buildSessionsUri(blockId));
                            intent.putExtra(Intent.EXTRA_TITLE, blockTimeString);
                            startActivity(intent);
                        }
                    });

                    primaryTouchTargetView.setOnLongClickListener(new OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            restartActionMode.run();
                            return true;
                        }
                    });

                    primaryTouchTargetView.setEnabled(true);

                } else {
                    // 2 or more sessions starred
                    titleView.setText(getString(R.string.schedule_conflict_title,
                            numStarredSessions));
                    subtitle = getString(R.string.schedule_conflict_subtitle);
                    extraButton.setVisibility(View.VISIBLE);
                    extraButton.setOnClickListener(allSessionsListener);
                    extraButton.setEnabled(!mActionModeStarted);

                    primaryTouchTargetView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (mActionModeStarted) {
                                return;
                            }

                            final Uri sessionsUri = ScheduleContract.Blocks
                                    .buildStarredSessionsUri(
                                    blockId);
                            final Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
                            intent.putExtra(Intent.EXTRA_TITLE, blockTimeString);
                            startActivity(intent);
                        }
                    });

                    primaryTouchTargetView.setEnabled(!mActionModeStarted);
                }

            } else if (ScheduleContract.Blocks.BLOCK_TYPE_KEYNOTE.equals(type)) {
                final String starredSessionId = cursor.getString(BlocksQuery.STARRED_SESSION_ID);
                final String starredSessionTitle =
                        cursor.getString(BlocksQuery.STARRED_SESSION_TITLE);

                long currentTimeMillis = UIUtils.getCurrentTime(context);
                boolean past = (currentTimeMillis > blockEnd
                        && currentTimeMillis < UIUtils.CONFERENCE_END_MILLIS);
                boolean present = !past && (currentTimeMillis >= blockStart);
                boolean canViewStream = present && UIUtils.hasHoneycomb();

                boolean enabled = canViewStream && !mActionModeStarted;

                isLiveStreamed = true;
                subtitle = getString(R.string.keynote_room);

                titleView.setText(starredSessionTitle);
                extraButton.setVisibility(View.GONE);
                primaryTouchTargetView.setEnabled(enabled);
                primaryTouchTargetView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mActionModeStarted) {
                            return;
                        }

                        final Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(
                                starredSessionId);
                        Intent livestreamIntent = new Intent(Intent.ACTION_VIEW, sessionUri);
                        livestreamIntent.setClass(getActivity(), SessionLivestreamActivity.class);
                        startActivity(livestreamIntent);
                    }
                });

            } else {
                subtitle = blockMeta;
                titleView.setText(blockTitle);
                extraButton.setVisibility(View.GONE);
                primaryTouchTargetView.setEnabled(false);
                primaryTouchTargetView.setOnClickListener(null);

                mBuffer.setLength(0);
                endtimeView.setText(DateUtils.formatDateRange(context, mFormatter,
                        blockEnd, blockEnd,
                        DateUtils.FORMAT_SHOW_TIME,
                        PrefUtils.getDisplayTimeZone(context).getID()).toString());
            }

            mBuffer.setLength(0);
            timeView.setText(DateUtils.formatDateRange(context, mFormatter,
                    blockStart, blockStart,
                    DateUtils.FORMAT_SHOW_TIME,
                    PrefUtils.getDisplayTimeZone(context).getID()).toString());

            // Show past/present/future and livestream status for this block.
            UIUtils.updateTimeAndLivestreamBlockUI(context,
                    blockStart, blockEnd, isLiveStreamed,
                    titleView, subtitleView, subtitle);
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
                ScheduleContract.Blocks.NUM_LIVESTREAMED_SESSIONS,
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
        int NUM_LIVESTREAMED_SESSIONS = 9;
        int STARRED_SESSION_ID = 10;
        int STARRED_SESSION_TITLE = 11;
        int STARRED_SESSION_ROOM_NAME = 12;
        int STARRED_SESSION_ROOM_ID = 13;
        int STARRED_SESSION_HASHTAGS = 14;
        int STARRED_SESSION_URL = 15;
        int STARRED_SESSION_LIVESTREAM_URL = 16;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        SessionsHelper helper = new SessionsHelper(getActivity());
        String title = mSelectedItemData.get(BlocksQuery.STARRED_SESSION_TITLE);
        String hashtags = mSelectedItemData.get(BlocksQuery.STARRED_SESSION_HASHTAGS);
        String url = mSelectedItemData.get(BlocksQuery.STARRED_SESSION_URL);
        boolean handled = false;
        switch (item.getItemId()) {
            case R.id.menu_map:
                String roomId = mSelectedItemData.get(BlocksQuery.STARRED_SESSION_ROOM_ID);
                helper.startMapActivity(roomId);
                handled = true;
                break;
            case R.id.menu_star:
                String sessionId = mSelectedItemData.get(BlocksQuery.STARRED_SESSION_ID);
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
        mAdapter.notifyDataSetChanged();
        mActionModeStarted = true;
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        if (mLongClickedView != null) {
            mLongClickedView.setSelected(false);
        }
        mActionModeStarted = false;
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }
}
