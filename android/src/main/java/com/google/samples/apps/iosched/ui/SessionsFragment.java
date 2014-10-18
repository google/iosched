/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.view.ViewCompat;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.ListPreloader;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.ui.widget.MessageCardView;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.PrefUtils;
import com.google.samples.apps.iosched.util.ThrottledContentObserver;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.util.WiFiUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGV;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;
import static com.google.samples.apps.iosched.util.UIUtils.buildStyledSnippet;

/**
 * A {@link ListFragment} showing a list of sessions. The fragment arguments
 * indicate what is the list of sessions to show. It may be a set of tag
 * filters or a search query.
 */
public class SessionsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, CollectionViewCallbacks {

    private static final String TAG = makeLogTag(SessionsFragment.class);

    // Disable track branding
    public static final String EXTRA_NO_TRACK_BRANDING =
            "com.google.android.iosched.extra.NO_TRACK_BRANDING";

    private static final String STATE_SESSION_QUERY_TOKEN = "session_query_token";
    private static final String STATE_ARGUMENTS = "arguments";

    /** The handler message for updating the search query. */
    private static final int MESSAGE_QUERY_UPDATE = 1;
    /** The delay before actual requerying in millisecs. */
    private static final int QUERY_UPDATE_DELAY_MILLIS = 100;
    /** The number of rows ahead to preload images for */
    private static final int ROWS_TO_PRELOAD = 2;

    private static final int ANIM_DURATION = 250;
    private static final int CARD_DISMISS_ACTION_DELAY = MessageCardView.ANIM_DURATION - 50;

    private Context mAppContext;

    // the cursor whose data we are currently displaying
    private int mSessionQueryToken;
    private Uri mCurrentUri = ScheduleContract.Sessions.CONTENT_URI;
    private Cursor mCursor;
    private boolean mIsSearchCursor;
    private boolean mNoTrackBranding;

    // this variable is relevant when we start the sessions loader, and indicates the desired
    // behavior when load finishes: if true, this is a full reload (for example, because filters
    // have been changed); if not, it's just a refresh because data has changed.
    private boolean mSessionDataIsFullReload = false;

    private ImageLoader mImageLoader;
    private int mDefaultSessionColor;

    private CollectionView mCollectionView;
    private TextView mEmptyView;
    private View mLoadingView;
    private TagMetadata mTagMetadata = null;

    private boolean mWasPaused = false;

    private static final int HERO_GROUP_ID = 123;

    private Bundle mArguments;

    private DateFormat mDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
    private DateFormat mTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

    private static final String CARD_ANSWER_ATTENDING_REMOTELY = "CARD_ANSWER_ATTENDING_REMOTELY";
    private static final String CARD_ANSWER_ATTENDING_IN_PERSON = "CARD_ANSWER_ATTENDING_IN_PERSON";
    private static final String CARD_ANSWER_YES = "CARD_ANSWER_YES";
    private static final String CARD_ANSWER_NO = "CARD_ANSWER_NO";

    private ThrottledContentObserver mSessionsObserver, mTagsObserver;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_QUERY_UPDATE) {
                String query = (String) msg.obj;
                reloadFromArguments(BaseActivity.intentToFragmentArguments(
                        new Intent(Intent.ACTION_SEARCH, ScheduleContract.Sessions.buildSearchUri(query))));
            }
        }

    };

    private Preloader mPreloader;

    public boolean canCollectionViewScrollUp() {
        return ViewCompat.canScrollVertically(mCollectionView, -1);
    }

    public void setContentTopClearance(int topClearance) {
        mCollectionView.setContentTopClearance(topClearance);
    }

    // Called when there is a change on sessions in the content provider
    private void onSessionsContentChanged() {
        LOGD(TAG, "ThrottledContentObserver fired (sessions). Content changed.");
        if (!isAdded()) {
            LOGD(TAG, "Ignoring ContentObserver event (Fragment not added).");
            return;
        }

        LOGD(TAG, "Requesting sessions cursor reload as a result of ContentObserver firing.");
        reloadSessionData(false);
    }

    // Called when there is a change in tag metadata in the content provider
    private void onTagsContentChanged() {
        LOGD(TAG, "ThrottledContentObserver fired (tags). Content changed.");
        if (!isAdded()) {
            LOGD(TAG, "Ignoring ContentObserver event (Fragment not added).");
            return;
        }

        LOGD(TAG, "Requesting tags cursor reload as a result of ContentObserver firing.");
        reloadTagMetadata();
    }

    private void reloadSessionData(boolean fullReload) {
        LOGD(TAG, "Reloading session data: " + (fullReload ? "FULL RELOAD" : "light refresh"));
        mSessionDataIsFullReload = fullReload;
        getLoaderManager().restartLoader(mSessionQueryToken, mArguments, SessionsFragment.this);
    }

    private void reloadTagMetadata() {
        getLoaderManager().restartLoader(TAG_METADATA_TOKEN, null, SessionsFragment.this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mWasPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWasPaused) {
            mWasPaused = false;
            LOGD(TAG, "Reloading data as a result of onResume()");
            mSessionsObserver.cancelPendingCallback();
            mTagsObserver.cancelPendingCallback();
            reloadSessionData(false);
        }
    }

    public interface Callbacks {
        public void onSessionSelected(String sessionId, View clickedView);
        public void onTagMetadataLoaded(TagMetadata metadata);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onSessionSelected(String sessionId, View clickedView) {}

        @Override
        public void onTagMetadataLoaded(TagMetadata metadata) {}
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(this.getActivity());
        }

        mDefaultSessionColor = getResources().getColor(R.color.default_session_color);

        final TimeZone tz = PrefUtils.getDisplayTimeZone(getActivity());
        mDateFormat.setTimeZone(tz);
        mTimeFormat.setTimeZone(tz);

        if (savedInstanceState != null) {
            mSessionQueryToken = savedInstanceState.getInt(STATE_SESSION_QUERY_TOKEN);
            mArguments = savedInstanceState.getParcelable(STATE_ARGUMENTS);
            if (mArguments != null) {
                mCurrentUri = mArguments.getParcelable("_uri");
                mNoTrackBranding = mArguments.getBoolean(EXTRA_NO_TRACK_BRANDING);
            }

            if (mSessionQueryToken > 0) {
                // Only if this is a config change should we initLoader(), to reconnect with an
                // existing loader. Otherwise, the loader will be init'd when reloadFromArguments
                // is called.
                getLoaderManager().initLoader(mSessionQueryToken, null, SessionsFragment.this);
            }
        }

        reloadTagMetadata();
    }

    private boolean useExpandedMode() {
        if (mCurrentUri != null && ScheduleContract.Sessions.CONTENT_URI.equals(mCurrentUri)) {
            // If showing all sessions (landing page) do not use expanded mode,
            // show info as condensed as possible
            return false;
        }
        return true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAppContext = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_sessions, container, false);
        mCollectionView = (CollectionView) root.findViewById(R.id.sessions_collection_view);
        mPreloader = new Preloader(ROWS_TO_PRELOAD);
        mCollectionView.setOnScrollListener(mPreloader);
        mEmptyView = (TextView) root.findViewById(R.id.empty_text);
        mLoadingView = root.findViewById(R.id.loading);
        return root;
    }

    void reloadFromArguments(Bundle arguments) {
        // Load new arguments
        if (arguments == null) {
            arguments = new Bundle();
        } else {
            // since we might make changes, don't meddle with caller's copy
            arguments = (Bundle) arguments.clone();
        }

        // save arguments so we can reuse it when reloading from content observer events
        mArguments = arguments;

        LOGD(TAG, "SessionsFragment reloading from arguments: " + arguments);
        mCurrentUri = arguments.getParcelable("_uri");
        if (mCurrentUri == null) {
            // if no URI, default to all sessions URI
            LOGD(TAG, "SessionsFragment did not get a URL, defaulting to all sessions.");
            arguments.putParcelable("_uri", ScheduleContract.Sessions.CONTENT_URI);
            mCurrentUri = ScheduleContract.Sessions.CONTENT_URI;
        }

        mNoTrackBranding = mArguments.getBoolean(EXTRA_NO_TRACK_BRANDING);

        if (ScheduleContract.Sessions.isSearchUri(mCurrentUri)) {
            mSessionQueryToken = SessionsQuery.SEARCH_TOKEN;
        } else {
            mSessionQueryToken = SessionsQuery.NORMAL_TOKEN;
        }

        LOGD(TAG, "SessionsFragment reloading, uri=" + mCurrentUri + ", expanded=" + useExpandedMode());

        reloadSessionData(true); // full reload
        if (mTagMetadata == null) {
            reloadTagMetadata();
        }
    }

    void requestQueryUpdate(String query) {
        mHandler.removeMessages(MESSAGE_QUERY_UPDATE);
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MESSAGE_QUERY_UPDATE, query),
                QUERY_UPDATE_DELAY_MILLIS);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException("Activity must implement fragment's callbacks.");
        }

        mAppContext = getActivity().getApplicationContext();
        mCallbacks = (Callbacks) activity;
        mSessionsObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                onSessionsContentChanged();
            }
        });
        mTagsObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                onTagsContentChanged();
            }
        });
        activity.getContentResolver().registerContentObserver(
                ScheduleContract.Sessions.CONTENT_URI, true, mSessionsObserver);
        activity.getContentResolver().registerContentObserver(
                ScheduleContract.Tags.CONTENT_URI, true, mTagsObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        sp.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
        getActivity().getContentResolver().unregisterContentObserver(mSessionsObserver);
        getActivity().getContentResolver().unregisterContentObserver(mTagsObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    public void animateReload() {
        //int curTop = mCollectionView.getTop();
        mCollectionView.setAlpha(0);
        //mCollectionView.setTop(getResources().getDimensionPixelSize(R.dimen.browse_sessions_anim_amount));
        //mCollectionView.animate().y(curTop).alpha(1).setDuration(ANIM_DURATION).setInterpolator(new DecelerateInterpolator());
        mCollectionView.animate().alpha(1).setDuration(ANIM_DURATION).setInterpolator(new DecelerateInterpolator());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SESSION_QUERY_TOKEN, mSessionQueryToken);
        outState.putParcelable(STATE_ARGUMENTS, mArguments);
    }

    // LoaderCallbacks interface
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        LOGD(TAG, "onCreateLoader, id=" + id + ", data=" + data);
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(data);
        Uri sessionsUri = intent.getData();
        if ((id == SessionsQuery.NORMAL_TOKEN || id == SessionsQuery.SEARCH_TOKEN) && sessionsUri == null) {
            LOGD(TAG, "intent.getData() is null, setting to default sessions search");
            sessionsUri = ScheduleContract.Sessions.CONTENT_URI;
        }
        Loader<Cursor> loader = null;
        String liveStreamedOnlySelection = UIUtils.shouldShowLiveSessionsOnly(getActivity())
                ? "IFNULL(" + ScheduleContract.Sessions.SESSION_LIVESTREAM_URL + ",'')!=''"
                : null;
        if (id == SessionsQuery.NORMAL_TOKEN) {
            LOGD(TAG, "Creating sessions loader for " + sessionsUri + ", selection " + liveStreamedOnlySelection);
            loader = new CursorLoader(getActivity(), sessionsUri, SessionsQuery.NORMAL_PROJECTION,
                    liveStreamedOnlySelection, null, ScheduleContract.Sessions.SORT_BY_TYPE_THEN_TIME);
        } else if (id == SessionsQuery.SEARCH_TOKEN) {
            LOGD(TAG, "Creating search loader for " + sessionsUri + ", selection " + liveStreamedOnlySelection);
            loader = new CursorLoader(getActivity(), sessionsUri, SessionsQuery.SEARCH_PROJECTION,
                    liveStreamedOnlySelection, null, ScheduleContract.Sessions.SORT_BY_TYPE_THEN_TIME);
        } else if (id == TAG_METADATA_TOKEN) {
            LOGD(TAG, "Creating metadata loader");
            loader = TagMetadata.createCursorLoader(getActivity());
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        int token = loader.getId();
        LOGD(TAG, "Loader finished: "  + (token == SessionsQuery.NORMAL_TOKEN ? "sessions" :
                token == SessionsQuery.SEARCH_TOKEN ? "search" : token == TAG_METADATA_TOKEN ? "tags" :
                        "unknown"));
        if (token == SessionsQuery.NORMAL_TOKEN || token == SessionsQuery.SEARCH_TOKEN) {
            if (mCursor != null && mCursor != cursor) {
                mCursor.close();
            }
            mCursor = cursor;
            mIsSearchCursor = token == SessionsQuery.SEARCH_TOKEN;
            LOGD(TAG, "Cursor has " + mCursor.getCount() + " items. Will now update collection view.");
            updateCollectionView();
        } else if (token == TAG_METADATA_TOKEN) {
            mTagMetadata = new TagMetadata(cursor);
            cursor.close();
            updateCollectionView();
            mCallbacks.onTagMetadataLoaded(mTagMetadata);
        } else {
            LOGD(TAG, "Query complete, Not Actionable: " + token);
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
            if (isAdded()) {
                if (PrefUtils.PREF_LOCAL_TIMES.equals(key)) {
                    updateCollectionView();
                } else if (PrefUtils.PREF_ATTENDEE_AT_VENUE.equals(key)) {
                    if (mCursor != null) {
                        reloadSessionData(true);
                    }
                }
            }
        }
    };

    private void updateCollectionView() {
        if (mCursor == null || mTagMetadata == null) {
            LOGD(TAG, "updateCollectionView: not ready yet... " + (mCursor == null ? "no cursor." :
                    "no tag metadata."));
            // not ready!
            return;
        }
        LOGD(TAG, "SessionsFragment updating CollectionView... " + (mSessionDataIsFullReload ?
                "(FULL RELOAD)" : "(light refresh)"));
        mCursor.moveToPosition(-1);
        int itemCount = mCursor.getCount();

        mMaxDataIndexAnimated = 0;

        CollectionView.Inventory inv;
        if (itemCount == 0) {
            showEmptyView();
            inv = new CollectionView.Inventory();
        } else {
            hideEmptyView();
            inv = prepareInventory();
        }

        Parcelable state = null;
        if (!mSessionDataIsFullReload) {
            // it's not a full reload, so we want to keep scroll position, etc
            state = mCollectionView.onSaveInstanceState();
        }
        LOGD(TAG, "Updating CollectionView with inventory, # groups = " + inv.getGroupCount()
                + " total items = " + inv.getTotalItemCount());
        mCollectionView.setCollectionAdapter(this);
        mCollectionView.updateInventory(inv, mSessionDataIsFullReload);
        if (state != null) {
            mCollectionView.onRestoreInstanceState(state);
        }
        mSessionDataIsFullReload = false;
    }

    private void hideEmptyView() {
        mEmptyView.setVisibility(View.GONE);
        mLoadingView.setVisibility(View.GONE);
    }

    private void showEmptyView() {
        final String searchQuery = ScheduleContract.Sessions.isSearchUri(mCurrentUri) ?
                ScheduleContract.Sessions.getSearchQuery(mCurrentUri) : null;

        if (mCurrentUri.equals(ScheduleContract.Sessions.CONTENT_URI)) {
            // if showing all sessions, the empty view should say "loading..." because
            // the only reason we would have no sessions at all is if we are currently
            // preparing the database from the bootstrap data, which should only take a few
            // seconds.
            mEmptyView.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.VISIBLE);
        } else if (ScheduleContract.Sessions.isUnscheduledSessionsInInterval(mCurrentUri)) {
            // Showing sessions in a given interval, so say "No sessions in this time slot."
            mEmptyView.setText(R.string.no_matching_sessions_in_interval);
            mEmptyView.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
        } else if (ScheduleContract.Sessions.isSearchUri(mCurrentUri)
                && (TextUtils.isEmpty(searchQuery) || "*".equals(searchQuery))) {
            // Empty search query (for example, user hasn't started to type the query yet),
            // so don't show an empty view.
            mEmptyView.setText("");
            mEmptyView.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
        } else {
            // Showing sessions as a result of search or filter, so say "No matching sessions."
            mEmptyView.setText(R.string.no_matching_sessions);
            mEmptyView.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
        }
    }


    // Creates the CollectionView groups based on the cursor data.
    private CollectionView.Inventory prepareInventory() {
        LOGD(TAG, "Preparing collection view inventory.");
        ArrayList<CollectionView.InventoryGroup> pastGroups =
                new ArrayList<CollectionView.InventoryGroup>();
        ArrayList<CollectionView.InventoryGroup> futureGroups =
                new ArrayList<CollectionView.InventoryGroup>();
        HashMap<String, CollectionView.InventoryGroup> pastGroupsByName =
                new HashMap<String, CollectionView.InventoryGroup>();
        HashMap<String, CollectionView.InventoryGroup> futureGroupsByName =
                new HashMap<String, CollectionView.InventoryGroup>();
        CollectionView.InventoryGroup heroGroup = null;

        mCursor.moveToPosition(-1);
        int nextGroupId = HERO_GROUP_ID + 1000; // to avoid conflict with the special hero group ID
        LOGD(TAG, "Total cursor data items: " + mCursor.getCount());
        int dataIndex = -1;
        final long now = UIUtils.getCurrentTime(mAppContext);
        final boolean conferenceEnded = TimeUtils.hasConferenceEnded(mAppContext);
        LOGD(TAG, "conferenceEnded=" + conferenceEnded);

        final boolean expandedMode = useExpandedMode();
        final int displayCols = getResources().getInteger(expandedMode ?
                R.integer.explore_2nd_level_grid_columns : R.integer.explore_1st_level_grid_columns);
        LOGD(TAG, "Using " + displayCols + " columns.");
        mPreloader.setDisplayCols(displayCols);

        while (mCursor.moveToNext()) {
            // For each data item, we decide what group it should appear under, then
            // we add it to that group (and create the group if it doesn't exist yet).

            long sessionEnd = mCursor.getLong(mCursor.getColumnIndex(
                    ScheduleContract.Sessions.SESSION_END));

            ++dataIndex;
            boolean showAsPast = !conferenceEnded && sessionEnd < now;
            String groupLabel;

            if (expandedMode) {
                String tags = mCursor.getString(mCursor.getColumnIndex(ScheduleContract.Sessions.SESSION_TAGS));
                TagMetadata.Tag groupTag = tags == null ? null
                        : mTagMetadata.getSessionGroupTag(tags.split(","));
                if (groupTag != null) {
                    groupLabel = groupTag.getName();
                } else {
                    groupLabel = getString(R.string.others);
                }
                groupLabel += (showAsPast ? " (" + getString(R.string.session_finished) + ")" : "");
            } else {
                groupLabel = showAsPast ? getString(R.string.ended_sessions) : "";
            }

            LOGV(TAG, "Data item #" + dataIndex + ", sessionEnd=" + sessionEnd + ", groupLabel="
                    + groupLabel + " showAsPast=" + showAsPast);

            CollectionView.InventoryGroup group;

            // should this item be the hero group?
            if (!useExpandedMode() && !showAsPast && heroGroup == null) {
                // yes, this item is the hero
                LOGV(TAG, "This item is the hero.");
                group = heroGroup = new CollectionView.InventoryGroup(HERO_GROUP_ID)
                        .setDisplayCols(1)  // hero item spans all columns
                        .setShowHeader(false).setHeaderLabel("");
            } else {
                // "list" and "map" are just shorthand variables pointing to the right list and map
                ArrayList<CollectionView.InventoryGroup> list = showAsPast ? pastGroups : futureGroups;
                HashMap<String, CollectionView.InventoryGroup> map = showAsPast ? pastGroupsByName :
                        futureGroupsByName;

                // Create group, if it doesn't exist yet
                if (!map.containsKey(groupLabel)) {
                    LOGV(TAG, "Creating new group: " + groupLabel);
                    group = new CollectionView.InventoryGroup(nextGroupId++)
                            .setDisplayCols(displayCols)
                            .setShowHeader(!TextUtils.isEmpty(groupLabel))
                            .setHeaderLabel(groupLabel);
                    map.put(groupLabel, group);
                    list.add(group);
                } else {
                    LOGV(TAG, "Adding to existing group: " + groupLabel);
                    group = map.get(groupLabel);
                }
            }

            // add this item to the group
            LOGV(TAG, "...adding to group '" + groupLabel + "' with custom data index " + dataIndex);
            group.addItemWithCustomDataIndex(dataIndex);
        }

        // prepare the final groups list
        ArrayList<CollectionView.InventoryGroup> groups = new ArrayList<CollectionView.InventoryGroup>();
        if (heroGroup != null) {
            groups.add(heroGroup); // start with the hero
        }
        groups.addAll(futureGroups); // then all future events
        groups.addAll(pastGroups); // then all past events
        LOGD(TAG, "Total: hero " + (heroGroup == null ? "absent" : "present")
                + " " + futureGroups.size() + " future groups, "
                + " " + pastGroups.size() + " past groups, total " + groups.size());

        // the first group doesn't need a header label, because it's the "default group"
        //if (groups.size() > 0) {
        //    groups.get(0).setHeaderLabel("").setShowHeader(false);
        //}

        // finally, assemble the inventory and we're done
        CollectionView.Inventory inventory = new CollectionView.Inventory();
        for (CollectionView.InventoryGroup g : groups) {
            inventory.addGroup(g);
        }
        return inventory;
    }

    @Override
    public View newCollectionHeaderView(Context context, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.list_item_explore_header, parent, false);
    }

    @Override
    public void bindCollectionHeaderView(Context context, View view, int groupId, String groupLabel) {
        TextView tv = (TextView) view.findViewById(android.R.id.text1);
        if (tv != null) {
            tv.setText(groupLabel);
        }
    }

    @Override
    public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        int layoutId;

        if (useExpandedMode()) {
            layoutId = R.layout.list_item_session;
        } else {
            // Group HERO_GROUP_ID is the hero -- use a larger layout
            layoutId = (groupId == HERO_GROUP_ID) ? R.layout.list_item_session_hero :
                    R.layout.list_item_session_summarized;
        }

        return inflater.inflate(layoutId, parent, false);
    }

    private StringBuilder mBuffer = new StringBuilder();

    private int mMaxDataIndexAnimated = 0;

    @Override
    public void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup, int dataIndex, Object tag) {
        if (mCursor == null || !mCursor.moveToPosition(dataIndex)) {
            LOGW(TAG, "Can't bind collection view item, dataIndex=" + dataIndex +
                    (mCursor == null ? ": cursor is null" : ": bad data index."));
            return;
        }

        final String sessionId = mCursor.getString(SessionsQuery.SESSION_ID);
        if (sessionId == null) {
            return;
        }

        // first, read session info from cursor and put it in convenience variables
        final String sessionTitle = mCursor.getString(SessionsQuery.TITLE);
        final String speakerNames = mCursor.getString(SessionsQuery.SPEAKER_NAMES);
        final String sessionAbstract = mCursor.getString(SessionsQuery.ABSTRACT);
        final long sessionStart = mCursor.getLong(SessionsQuery.SESSION_START);
        final long sessionEnd = mCursor.getLong(SessionsQuery.SESSION_END);
        final String roomName = mCursor.getString(SessionsQuery.ROOM_NAME);
        int sessionColor = mCursor.getInt(SessionsQuery.COLOR);
        sessionColor = sessionColor == 0 ? getResources().getColor(R.color.default_session_color)
                : sessionColor;
        int darkSessionColor = 0;
        final String snippet = mIsSearchCursor ? mCursor.getString(SessionsQuery.SNIPPET) : null;
        final Spannable styledSnippet = mIsSearchCursor ? buildStyledSnippet(snippet) : null;
        final boolean starred = mCursor.getInt(SessionsQuery.IN_MY_SCHEDULE) != 0;
        final String[] tags = mCursor.getString(SessionsQuery.TAGS).split(",");

        // now let's compute a few pieces of information from the data, which we will use
        // later to decide what to render where
        final boolean hasLivestream = !TextUtils.isEmpty(mCursor.getString(
                SessionsQuery.LIVESTREAM_URL));
        final long now = UIUtils.getCurrentTime(context);
        final boolean happeningNow = now >= sessionStart && now <= sessionEnd;

        // text that says "LIVE" if session is live, or empty if session is not live
        final String liveNowText = hasLivestream ? " " + UIUtils.getLiveBadgeText(context,
                sessionStart, sessionEnd) : "";

        // get reference to all the views in the layout we will need
        final TextView titleView = (TextView) view.findViewById(R.id.session_title);
        final TextView subtitleView = (TextView) view.findViewById(R.id.session_subtitle);
        final TextView shortSubtitleView = (TextView) view.findViewById(R.id.session_subtitle_short);
        final TextView snippetView = (TextView) view.findViewById(R.id.session_snippet);
        final TextView abstractView = (TextView) view.findViewById(R.id.session_abstract);
        final TextView categoryView = (TextView) view.findViewById(R.id.session_category);
        final View sessionTargetView = view.findViewById(R.id.session_target);

        if (sessionColor == 0) {
            // use default
            sessionColor = mDefaultSessionColor;
        }

        if (mNoTrackBranding) {
            sessionColor = getResources().getColor(R.color.no_track_branding_session_color);
        }

        darkSessionColor = UIUtils.scaleSessionColorToDefaultBG(sessionColor);

        ImageView photoView = (ImageView) view.findViewById(R.id.session_photo_colored);
        if (photoView != null) {
            if (!mPreloader.isDimensSet()) {
                final ImageView finalPhotoView = photoView;
                photoView.post(new Runnable() {
                    @Override
                    public void run() {
                        mPreloader.setDimens(finalPhotoView.getWidth(), finalPhotoView.getHeight());
                    }
                });
            }
            // colored
            photoView.setColorFilter(mNoTrackBranding
                    ? new PorterDuffColorFilter(
                    getResources().getColor(R.color.no_track_branding_session_tile_overlay),
                    PorterDuff.Mode.SRC_ATOP)
                    : UIUtils.makeSessionImageScrimColorFilter(darkSessionColor));
        } else {
            photoView = (ImageView) view.findViewById(R.id.session_photo);
        }
        ViewCompat.setTransitionName(photoView, "photo_" + sessionId);

        // when we load a photo, it will fade in from transparent so the
        // background of the container must be the session color to avoid a white flash
        ViewParent parent = photoView.getParent();
        if (parent != null && parent instanceof View) {
            ((View) parent).setBackgroundColor(darkSessionColor);
        } else {
            photoView.setBackgroundColor(darkSessionColor);
        }

        String photo = mCursor.getString(SessionsQuery.PHOTO_URL);
        if (!TextUtils.isEmpty(photo)) {
            mImageLoader.loadImage(photo, photoView, true /*crop*/);
        } else {
            // cleaning the (potentially) recycled photoView, in case this session has no photo:
            photoView.setImageDrawable(null);
        }

        // render title
        titleView.setText(sessionTitle == null ? "?" : sessionTitle);

        // render subtitle into either the subtitle view, or the short subtitle view, as available
        if (subtitleView != null) {
            subtitleView.setText(UIUtils.formatSessionSubtitle(
                    sessionStart, sessionEnd, roomName, mBuffer, context) + liveNowText);
        } else if (shortSubtitleView != null) {
            shortSubtitleView.setText(UIUtils.formatSessionSubtitle(
                    sessionStart, sessionEnd, roomName, mBuffer, context, true) + liveNowText);
        }

        // render category
        if (categoryView != null) {
            TagMetadata.Tag groupTag = mTagMetadata.getSessionGroupTag(tags);
            if (groupTag != null && !Config.Tags.SESSIONS.equals(groupTag.getId())) {
                categoryView.setText(groupTag.getName());
                categoryView.setVisibility(View.VISIBLE);
            } else {
                categoryView.setVisibility(View.GONE);
            }
        }

        // if a snippet view is available, render the session snippet there.
        if (snippetView != null) {
            if (mIsSearchCursor) {
                // render the search snippet into the snippet view
                snippetView.setText(styledSnippet);
            } else {
                // render speaker names and abstracts into the snippet view
                mBuffer.setLength(0);
                if (!TextUtils.isEmpty(speakerNames)) {
                    mBuffer.append(speakerNames).append(". ");
                }
                if (!TextUtils.isEmpty(sessionAbstract)) {
                    mBuffer.append(sessionAbstract);
                }
                snippetView.setText(mBuffer.toString());
            }
        }

        if (abstractView != null && !mIsSearchCursor) {
            // render speaker names and abstracts into the abstract view
            mBuffer.setLength(0);
            if (!TextUtils.isEmpty(speakerNames)) {
                mBuffer.append(speakerNames).append("\n\n");
            }
            if (!TextUtils.isEmpty(sessionAbstract)) {
                mBuffer.append(sessionAbstract);
            }
            abstractView.setText(mBuffer.toString());
        }

        // show or hide the "in my schedule" indicator
        view.findViewById(R.id.indicator_in_schedule).setVisibility(starred ? View.VISIBLE
                : View.INVISIBLE);

        // if we are in condensed mode and this card is the hero card (big card at the top
        // of the screen), set up the message card if necessary.
        if (!useExpandedMode() && groupId == HERO_GROUP_ID) {
            // this is the hero view, so we might want to show a message card
            final boolean cardShown = setupMessageCard(view);

            // if this is the wide hero layout, show or hide the card or the session abstract
            // view, as appropriate (they are mutually exclusive).
            final View cardContainer = view.findViewById(R.id.message_card_container_wide);
            final View abstractContainer = view.findViewById(R.id.session_abstract);
            if (cardContainer != null && abstractContainer != null) {
                cardContainer.setVisibility(cardShown ? View.VISIBLE : View.GONE);
                abstractContainer.setVisibility(cardShown ? View.GONE : View.VISIBLE);
                abstractContainer.setBackgroundColor(darkSessionColor);
            }
        }

        // if this session is live right now, display the "LIVE NOW" icon on top of it
        View liveNowBadge = view.findViewById(R.id.live_now_badge);
        if (liveNowBadge != null) {
            liveNowBadge.setVisibility(happeningNow && hasLivestream ? View.VISIBLE : View.GONE);
        }

        // if this view is clicked, open the session details view
        final View finalPhotoView = photoView;
        sessionTargetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.onSessionSelected(sessionId, finalPhotoView);
            }
        });

        // animate this card
        if (dataIndex > mMaxDataIndexAnimated) {
            mMaxDataIndexAnimated = dataIndex;
        }
    }

    private boolean setupMessageCard(View hero) {
        MessageCardView card = (MessageCardView) hero.findViewById(R.id.message_card);
        if (card == null) {
            LOGE(TAG, "Message card not found in UI (R.id.message_card).");
            return false;
        }
        if (!PrefUtils.hasAnsweredLocalOrRemote(getActivity()) &&
                !TimeUtils.hasConferenceEnded(getActivity())) {
            // show the "in person" vs "remote" card
            setupLocalOrRemoteCard(card);
            return true;
        } else if (WiFiUtils.shouldOfferToSetupWifi(getActivity(), true)) {
            // show wifi setup card
            setupWifiOfferCard(card);
            return true;
        } else if (PrefUtils.shouldOfferIOExtended(getActivity(), true)) {
            // show the I/O extended card
            setupIOExtendedCard(card);
            return true;
        } else {
            card.setVisibility(View.GONE);
            return false;
        }
    }

    private void setupLocalOrRemoteCard(final MessageCardView card) {
        card.setText(getString(R.string.question_local_or_remote));
        card.setButton(0, getString(R.string.attending_remotely), CARD_ANSWER_ATTENDING_REMOTELY,
                false, 0);
        card.setButton(1, getString(R.string.attending_in_person), CARD_ANSWER_ATTENDING_IN_PERSON,
                true, 0);
        final Context context = getActivity().getApplicationContext();
        final Activity activity = getActivity();
        card.setListener(new MessageCardView.OnMessageCardButtonClicked() {
            @Override
            public void onMessageCardButtonClicked(final String tag) {
                final boolean inPerson = CARD_ANSWER_ATTENDING_IN_PERSON.equals(tag);
                card.dismiss(true);

                if (activity != null) {
                    Toast.makeText(activity, inPerson ? R.string.explore_attending_in_person_toast
                            : R.string.explore_attending_remotely_toast, Toast.LENGTH_LONG).show();
                }

                // post delayed to give card time to animate
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PrefUtils.setAttendeeAtVenue(context, inPerson);
                        PrefUtils.markAnsweredLocalOrRemote(context);
                    }
                }, CARD_DISMISS_ACTION_DELAY);
            }
        });
        card.show();
    }

    private void setupWifiOfferCard(final MessageCardView card) {
        card.setText(getString(TimeUtils.hasConferenceStarted(getActivity()) ?
                R.string.question_setup_wifi_after_i_o_start :
                R.string.question_setup_wifi_before_i_o_start));
        card.setButton(0, getString(R.string.no_thanks), CARD_ANSWER_NO,
                false, 0);
        card.setButton(1, getString(R.string.setup_wifi_yes), CARD_ANSWER_YES,
                true, 0);
        final Context context = getActivity().getApplicationContext();
        card.setListener(new MessageCardView.OnMessageCardButtonClicked() {
            @Override
            public void onMessageCardButtonClicked(final String tag) {
                card.dismiss(true);

                // post delayed to give card time to animate
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (CARD_ANSWER_YES.equals(tag)) {
                            WiFiUtils.showWiFiDialog(SessionsFragment.this.getActivity());
                        } else {
                            PrefUtils.markDeclinedWifiSetup(context);
                        }
                    }
                }, CARD_DISMISS_ACTION_DELAY);
            }
        });
        card.show();
    }

    private void setupIOExtendedCard(final MessageCardView card) {
        card.setText(getString(R.string.question_i_o_extended));
        card.setButton(0, getString(R.string.no_thanks), CARD_ANSWER_NO,
                false, 0);
        card.setButton(1, getString(R.string.browse_events), CARD_ANSWER_YES,
                true, 0);
        card.setListener(new MessageCardView.OnMessageCardButtonClicked() {
            @Override
            public void onMessageCardButtonClicked(final String tag) {
                card.dismiss(true);

                // post delayed to give card time to animate
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (CARD_ANSWER_YES.equals(tag)) {
                            Intent intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(Config.IO_EXTENDED_LINK));
                            startActivity(intent);
                        }
                        PrefUtils.markDismissedIOExtendedCard(SessionsFragment.this.getActivity());
                    }
                }, CARD_DISMISS_ACTION_DELAY);
            }
        });
        card.show();
    }

    private void animateSessionAppear(final View view) {
    }

    private class Preloader extends ListPreloader<String> {

        private int[] photoDimens;
        private int displayCols;

        public Preloader(int maxPreload) {
            super(maxPreload);
        }

        public void setDisplayCols(int displayCols) {
            this.displayCols = displayCols;
        }

        public boolean isDimensSet() {
            return photoDimens != null;
        }

        public void setDimens(int width, int height) {
            if (photoDimens == null) {
                photoDimens = new int[] { width, height };
            }
        }

        @Override
        protected int[] getDimensions(String s) {
            return photoDimens;
        }

        @Override
        protected List<String> getItems(int start, int end) {
            // Our start and end are rows, we need to adjust them into data columns
            // The keynote is 1 row with 1 data item, so we need to adjust.
            int keynoteDataOffset = (displayCols - 1);
            int dataStart = start * displayCols - keynoteDataOffset;
            int dataEnd = end * displayCols - keynoteDataOffset;
            List<String> urls = new ArrayList<String>();
            if (mCursor != null) {
                for (int i = dataStart; i < dataEnd; i++) {
                    if (mCursor.moveToPosition(i)) {
                        urls.add(mCursor.getString(SessionsQuery.PHOTO_URL));
                    }
                }
            }
            return urls;
        }

        @Override
        protected GenericRequestBuilder getRequestBuilder(String url) {
            return mImageLoader.beginImageLoad(url, null, true /*crop*/);
        }
    }

    /**
     * {@link com.google.samples.apps.iosched.provider.ScheduleContract.Sessions}
     * query parameters.
     */
    private interface SessionsQuery {
        int NORMAL_TOKEN = 0x1;
        int SEARCH_TOKEN = 0x3;

        String[] NORMAL_PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Rooms.ROOM_ID,
                ScheduleContract.Sessions.SESSION_HASHTAG,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_URL,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_SPEAKER_NAMES,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_COLOR,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
        };

        String[] SEARCH_PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Rooms.ROOM_ID,
                ScheduleContract.Sessions.SESSION_HASHTAG,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_URL,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_SPEAKER_NAMES,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_COLOR,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SEARCH_SNIPPET,
        };


        int _ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int IN_MY_SCHEDULE = 3;
        int SESSION_START = 4;
        int SESSION_END = 5;
        int ROOM_NAME = 6;
        int ROOM_ID = 7;
        int HASHTAGS = 8;
        int URL = 9;
        int LIVESTREAM_URL = 10;
        int TAGS = 11;
        int SPEAKER_NAMES = 12;
        int ABSTRACT = 13;
        int COLOR = 14;
        int PHOTO_URL = 15;
        int SNIPPET = 16;
    }

    private static final int TAG_METADATA_TOKEN = 0x4;
}
