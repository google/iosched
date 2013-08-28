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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.*;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.util.ImageLoader;
import com.google.android.apps.iosched.util.NetUtils;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.CommonGoogleClientRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Activity;
import com.google.api.services.plus.model.ActivityFeed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that renders Google+ search results for a given query, provided as the
 * {@link SocialStreamFragment#EXTRA_QUERY} extra in the fragment arguments. If no
 * search query is provided, the conference hashtag is used as the default query.
 */
public class SocialStreamFragment extends ListFragment implements
        AbsListView.OnScrollListener,
        LoaderManager.LoaderCallbacks<List<Activity>> {

    private static final String TAG = makeLogTag(SocialStreamFragment.class);

    public static final String EXTRA_QUERY = "com.google.android.apps.iosched.extra.QUERY";
    public static final String EXTRA_ADD_VERTICAL_MARGINS
            = "com.google.android.apps.iosched.extra.ADD_VERTICAL_MARGINS";

    private static final String STATE_POSITION = "position";
    private static final String STATE_TOP = "top";

    private static final long MAX_RESULTS_PER_REQUEST = 20;
    private static final String PLUS_RESULT_FIELDS =
            "nextPageToken,items(id,annotation,updated,url,verb,actor(displayName,image)," +
            "object(actor/displayName,attachments(displayName,image/url,objectType," +
            "thumbnails(image/url,url),url),content,plusoners/totalItems,replies/totalItems," +
            "resharers/totalItems))";
    private static final int STREAM_LOADER_ID = 0;

    private String mSearchString;

    private List<Activity> mStream = new ArrayList<Activity>();
    private StreamAdapter mStreamAdapter = new StreamAdapter();
    private int mListViewStatePosition;
    private int mListViewStateTop;

    private ImageLoader mImageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());

        // mSearchString can be populated before onCreate() by called refresh(String)
        if (TextUtils.isEmpty(mSearchString)) {
            mSearchString = intent.getStringExtra(EXTRA_QUERY);
        }
        if (TextUtils.isEmpty(mSearchString)) {
            mSearchString = UIUtils.CONFERENCE_HASHTAG;
        }

        if (!mSearchString.startsWith("#")) {
            mSearchString = "#" + mSearchString;
        }

        mImageLoader =
                PlusStreamRowViewBinder.getPlusStreamImageLoader(getActivity(), getResources());

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mListViewStatePosition = savedInstanceState.getInt(STATE_POSITION, -1);
            mListViewStateTop = savedInstanceState.getInt(STATE_TOP, 0);
        } else {
            mListViewStatePosition = -1;
            mListViewStateTop = 0;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.empty_social_stream));

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter
        // in the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(STREAM_LOADER_ID, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ListView listView = getListView();
        if (!UIUtils.isTablet(getActivity())) {
            view.setBackgroundColor(getResources().getColor(R.color.plus_stream_spacer_color));
        }

        if (getArguments() != null
                && getArguments().getBoolean(EXTRA_ADD_VERTICAL_MARGINS, false)) {
            int verticalMargin = getResources().getDimensionPixelSize(
                    R.dimen.plus_stream_padding_vertical);
            if (verticalMargin > 0) {
                listView.setClipToPadding(false);
                listView.setPadding(0, verticalMargin, 0, verticalMargin);
            }
        }

        listView.setOnScrollListener(this);
        listView.setDrawSelectorOnTop(true);
        listView.setDivider(getResources().getDrawable(android.R.color.transparent));
        listView.setDividerHeight(getResources()
                .getDimensionPixelSize(R.dimen.page_margin_width));

        TypedValue v = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.activatableItemBackground, v, true);
        listView.setSelector(v.resourceId);

        setListAdapter(mStreamAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.social_stream, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_compose:
                Intent intent = ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(mSearchString + "\n\n")
                        .getIntent();

                UIUtils.preferPackageForIntent(getActivity(), intent,
                        UIUtils.GOOGLE_PLUS_PACKAGE_NAME);

                startActivity(intent);

                EasyTracker.getTracker().sendEvent("Home Screen Dashboard", "Click", "Post to G+", 0L);
                LOGD("Tracker", "Home Screen Dashboard: Click, post to g+");

                return true;
        }
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    }

    @Override
    public void onDestroyOptionsMenu() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (isAdded()) {
            View v = getListView().getChildAt(0);
            int top = (v == null) ? 0 : v.getTop();
            outState.putInt(STATE_POSITION, getListView().getFirstVisiblePosition());
            outState.putInt(STATE_TOP, top);
        }
        super.onSaveInstanceState(outState);
    }

    public void refresh(String newQuery) {
        mSearchString = newQuery;
        refresh(true);
    }

    public void refresh() {
        refresh(false);
    }

    public void refresh(boolean forceRefresh) {
        if (isStreamLoading() && !forceRefresh) {
            return;
        }

        // clear current items
        mStream.clear();
        mStreamAdapter.notifyDataSetInvalidated();

        if (isAdded()) {
            Loader loader = getLoaderManager().getLoader(STREAM_LOADER_ID);
            ((StreamLoader) loader).init(mSearchString);
        }

        loadMoreResults();
    }

    public void loadMoreResults() {
        if (isAdded()) {
            Loader loader = getLoaderManager().getLoader(STREAM_LOADER_ID);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Activity activity = mStream.get(position);

        Intent postDetailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getUrl()));
        postDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        UIUtils.preferPackageForIntent(getActivity(), postDetailIntent,
                UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
        startActivity(postDetailIntent);
    }

    @Override
    public void onScrollStateChanged(AbsListView listView, int scrollState) {
        // Pause disk cache access to ensure smoother scrolling
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            mImageLoader.stopProcessingQueue();
        } else {
            mImageLoader.startProcessingQueue();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (!isStreamLoading()
                && streamHasMoreResults()
                && visibleItemCount != 0
                && firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
            loadMoreResults();
        }
    }

    @Override
    public Loader<List<Activity>> onCreateLoader(int id, Bundle args) {
        return new StreamLoader(getActivity(), mSearchString);

    }

    @Override
    public void onLoadFinished(Loader<List<Activity>> listLoader, List<Activity> activities) {
        if (activities != null) {
            mStream = activities;
        }
        mStreamAdapter.notifyDataSetChanged();
        if (mListViewStatePosition != -1 && isAdded()) {
            getListView().setSelectionFromTop(mListViewStatePosition, mListViewStateTop);
            mListViewStatePosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Activity>> listLoader) {
    }

    private boolean isStreamLoading() {
        if (isAdded()) {
            final Loader loader = getLoaderManager().getLoader(STREAM_LOADER_ID);
            if (loader != null) {
                return ((StreamLoader) loader).isLoading();
            }
        }
        return true;
    }

    private boolean streamHasMoreResults() {
        if (isAdded()) {
            final Loader loader = getLoaderManager().getLoader(STREAM_LOADER_ID);
            if (loader != null) {
                return ((StreamLoader) loader).hasMoreResults();
            }
        }
        return false;
    }

    private boolean streamHasError() {
        if (isAdded()) {
            final Loader loader = getLoaderManager().getLoader(STREAM_LOADER_ID);
            if (loader != null) {
                return ((StreamLoader) loader).hasError();
            }
        }
        return false;
    }

    private static class StreamLoader extends AsyncTaskLoader<List<Activity>> {
        List<Activity> mActivities;
        private String mSearchString;
        private String mNextPageToken;
        private boolean mIsLoading;
        private boolean mHasError;

        public StreamLoader(Context context, String searchString) {
            super(context);
            init(searchString);
        }

        private void init(String searchString) {
            mSearchString = searchString;
            mHasError = false;
            mNextPageToken = null;
            mIsLoading = true;
            mActivities = null;
        }

        @Override
        public List<Activity> loadInBackground() {
            mIsLoading = true;

            // Set up the HTTP transport and JSON factory
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = new AndroidJsonFactory();

            // Set up the main Google+ class
            Plus plus = new Plus.Builder(httpTransport, jsonFactory, null)
                    .setApplicationName(NetUtils.getUserAgent(getContext()))
                    .setGoogleClientRequestInitializer(
                            new CommonGoogleClientRequestInitializer(Config.API_KEY))
                    .build();

            ActivityFeed activities = null;
            try {
                activities = plus.activities().search(mSearchString)
                        .setPageToken(mNextPageToken)
                        .setOrderBy("recent")
                        .setMaxResults(MAX_RESULTS_PER_REQUEST)
                        .setFields(PLUS_RESULT_FIELDS)
                        .execute();

                mHasError = false;
                mNextPageToken = activities.getNextPageToken();

            } catch (IOException e) {
                e.printStackTrace();
                mHasError = true;
                mNextPageToken = null;
            }

            return (activities != null) ? activities.getItems() : null;
        }

        @Override
        public void deliverResult(List<Activity> activities) {
            mIsLoading = false;
            if (activities != null) {
                if (mActivities == null) {
                    mActivities = activities;
                } else {
                    mActivities.addAll(activities);
                }
            }
            if (isStarted()) {
                // Need to return new ArrayList for some reason or onLoadFinished() is not called
                super.deliverResult(mActivities == null ?
                        null : new ArrayList<Activity>(mActivities));
            }
        }

        @Override
        protected void onStartLoading() {
            if (mActivities != null) {
                // If we already have results and are starting up, deliver what we already have.
                deliverResult(null);
            } else {
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            mIsLoading = false;
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();
            mActivities = null;
        }

        public boolean isLoading() {
            return mIsLoading;
        }

        public boolean hasMoreResults() {
            return mNextPageToken != null;
        }

        public boolean hasError() {
            return mHasError;
        }

        public void setSearchString(String searchString) {
            mSearchString = searchString;
        }

        public void refresh() {
            reset();
            startLoading();
        }
    }

    private class StreamAdapter extends BaseAdapter {
        private static final int VIEW_TYPE_ACTIVITY = 0;
        private static final int VIEW_TYPE_LOADING = 1;

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) == VIEW_TYPE_ACTIVITY;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getCount() {
            return mStream.size() + (
                    // show the status list row if...
                    ((isStreamLoading() && mStream.size() == 0) // ...this is the first load
                            || streamHasMoreResults() // ...or there's another page
                            || streamHasError()) // ...or there's an error
                            ? 1 : 0);
        }

        @Override
        public int getItemViewType(int position) {
            return (position >= mStream.size())
                    ? VIEW_TYPE_LOADING
                    : VIEW_TYPE_ACTIVITY;
        }

        @Override
        public Object getItem(int position) {
            return (getItemViewType(position) == VIEW_TYPE_ACTIVITY)
                    ? mStream.get(position)
                    : null;
        }

        @Override
        public long getItemId(int position) {
            // TODO: better unique ID heuristic
            return (getItemViewType(position) == VIEW_TYPE_ACTIVITY)
                    ? mStream.get(position).getId().hashCode()
                    : -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (getItemViewType(position) == VIEW_TYPE_LOADING) {
                if (convertView == null) {
                    convertView = getLayoutInflater(null).inflate(
                            R.layout.list_item_stream_status, parent, false);
                }

                if (streamHasError()) {
                    convertView.findViewById(android.R.id.progress).setVisibility(View.GONE);
                    ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                            R.string.stream_error);
                } else {
                    convertView.findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
                    ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                            R.string.loading);
                }

                return convertView;

            } else {
                Activity activity = (Activity) getItem(position);
                if (convertView == null) {
                    convertView = getLayoutInflater(null).inflate(
                            R.layout.list_item_stream_activity, parent, false);
                }

                PlusStreamRowViewBinder.bindActivityView(convertView, activity, mImageLoader,
                        false);
                return convertView;
            }
        }
    }
}
