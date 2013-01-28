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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.util.ImageFetcher;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.api.client.googleapis.services.GoogleKeyInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Activity;
import com.google.api.services.plus.model.ActivityFeed;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A {@link WebView}-based fragment that shows Google+ public search results for a given query,
 * provided as the {@link SocialStreamFragment#EXTRA_QUERY} extra in the fragment arguments. If no
 * search query is provided, the conference hashtag is used as the default query.
 *
 * <p>WARNING! This fragment uses the Google+ API, and is subject to quotas. If you expect to
 * write a wildly popular app based on this code, please check the
 * at <a href="https://developers.google.com/+/">Google+ Platform documentation</a> on latest
 * best practices and quota details. You can check your current quota at the
 * <a href="https://code.google.com/apis/console">APIs console</a>.
 */
public class SocialStreamFragment extends SherlockListFragment implements
        AbsListView.OnScrollListener,
        LoaderManager.LoaderCallbacks<List<Activity>> {

    private static final String TAG = makeLogTag(SocialStreamFragment.class);

    public static final String EXTRA_QUERY = "com.google.android.iosched.extra.QUERY";
    private static final String STATE_POSITION = "position";
    private static final String STATE_TOP = "top";

    private static final long MAX_RESULTS_PER_REQUEST = 20;
    private static final int STREAM_LOADER_ID = 0;

    private String mSearchString;

    private List<Activity> mStream = new ArrayList<Activity>();
    private StreamAdapter mStreamAdapter = new StreamAdapter();
    private int mListViewStatePosition;
    private int mListViewStateTop;

    private ImageFetcher mImageFetcher;

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

        mImageFetcher = UIUtils.getImageFetcher(getActivity());
        setListAdapter(mStreamAdapter);
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
        view.setBackgroundColor(Color.WHITE);

        final ListView listView = getListView();
        listView.setCacheColorHint(Color.WHITE);
        listView.setOnScrollListener(this);
        listView.setDrawSelectorOnTop(true);
        TypedValue v = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.activatableItemBackground, v, true);
        listView.setSelector(v.resourceId);
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.social_stream, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
                
                EasyTracker.getTracker().trackEvent("Home Screen Dashboard", "Click",
                        "Post to Google+", 0L);
                LOGD("Tracker", "Home Screen Dashboard: Click, post to Google+");

                return true;
        }
        return super.onOptionsItemSelected(item);
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
        UIUtils.safeOpenLink(getActivity(), postDetailIntent);
    }

    @Override
    public void onScrollStateChanged(AbsListView listView, int scrollState) {
        // Pause disk cache access to ensure smoother scrolling
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING ||
                scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            mImageFetcher.setPauseWork(true);
        } else {
            mImageFetcher.setPauseWork(false);
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        // Simple implementation of the infinite scrolling UI pattern; loads more Google+
        // search results as the user scrolls to the end of the list.
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

    /**
     * An {@link AsyncTaskLoader} that loads activities from the public Google+ stream for
     * a given search query. The loader maintains a page state with the Google+ API and thus
     * supports pagination.
     */
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
            JsonFactory jsonFactory = new JacksonFactory();

            JsonHttpRequestInitializer initializer = new GoogleKeyInitializer(
                    Config.API_KEY);

            // Set up the main Google+ class
            Plus plus = Plus.builder(httpTransport, jsonFactory)
                    .setApplicationName(Config.APP_NAME)
                    .setJsonHttpRequestInitializer(initializer)
                    .build();

            ActivityFeed activities = null;
            try {
                activities = plus.activities().search(mSearchString)
                        .setPageToken(mNextPageToken)
                        .setMaxResults(MAX_RESULTS_PER_REQUEST)
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

        public void refresh(String searchString) {
            setSearchString(searchString);
            refresh();
        }

        public void refresh() {
            reset();
            startLoading();
        }
    }

    /**
     * A list adapter that shows individual Google+ activities as list items.
     * If another page is available, the last item is a "loading" view to support the
     * infinite scrolling UI pattern.
     */
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

                StreamRowViewBinder.bindActivityView(convertView, activity, mImageFetcher);
                return convertView;
            }
        }
    }

    /**
     * A helper class to bind data from a Google+ {@link Activity} to the list item view.
     */
    private static class StreamRowViewBinder {
        private static class ViewHolder {
            private TextView[] detail;
            private ImageView[] detailIcon;
            private ImageView[] media;
            private ImageView[] mediaOverlay;
            private TextView originalAuthor;
            private View reshareLine;
            private View reshareSpacer;
            private ImageView userImage;
            private TextView userName;
            private TextView content;
            private View plusOneIcon;
            private TextView plusOneCount;
            private View commentIcon;
            private TextView commentCount;
        }

        private static void bindActivityView(
                final View rootView, Activity activity, ImageFetcher imageFetcher) {
            // Prepare view holder.
            ViewHolder temp = (ViewHolder) rootView.getTag();
            final ViewHolder views;
            if (temp != null) {
                views = temp;
            } else {
                views = new ViewHolder();
                rootView.setTag(views);
                views.detail = new TextView[] {
                        (TextView) rootView.findViewById(R.id.stream_detail_text)
                };
                views.detailIcon = new ImageView[] {
                        (ImageView) rootView.findViewById(R.id.stream_detail_media_small)
                };
                views.media = new ImageView[] {
                        (ImageView) rootView.findViewById(R.id.stream_media_1_1),
                        (ImageView) rootView.findViewById(R.id.stream_media_1_2),
                };
                views.mediaOverlay = new ImageView[] {
                        (ImageView) rootView.findViewById(R.id.stream_media_overlay_1_1),
                        (ImageView) rootView.findViewById(R.id.stream_media_overlay_1_2),
                };
                views.originalAuthor = (TextView) rootView.findViewById(R.id.stream_original_author);
                views.reshareLine = rootView.findViewById(R.id.stream_reshare_line);
                views.reshareSpacer = rootView.findViewById(R.id.stream_reshare_spacer);
                views.userImage = (ImageView) rootView.findViewById(R.id.stream_user_image);
                views.userName = (TextView) rootView.findViewById(R.id.stream_user_name);
                views.content = (TextView) rootView.findViewById(R.id.stream_content);
                views.plusOneIcon = rootView.findViewById(R.id.stream_plus_one_icon);
                views.plusOneCount = (TextView) rootView.findViewById(R.id.stream_plus_one_count);
                views.commentIcon = rootView.findViewById(R.id.stream_comment_icon);
                views.commentCount = (TextView) rootView.findViewById(R.id.stream_comment_count);
            }

            final Resources res = rootView.getContext().getResources();

            // Hide all the array items.
            int detailIndex = 0;
            int mediaIndex = 0;

            for (View v : views.detail) {
                v.setVisibility(View.GONE);
            }

            for (View v : views.detailIcon) {
                v.setVisibility(View.GONE);
            }

            for (View v : views.media) {
                v.setVisibility(View.GONE);
            }

            for (View v : views.mediaOverlay) {
                v.setVisibility(View.GONE);
            }

            // Determine if this is a reshare (affects how activity fields are to be
            // interpreted).
            boolean isReshare = (activity.getObject().getActor() != null);

            // Set user name.
            views.userName.setText(activity.getActor().getDisplayName());

            if (activity.getActor().getImage() != null) {
                imageFetcher.loadThumbnailImage(activity.getActor().getImage().getUrl(), views.userImage,
                        R.drawable.person_image_empty);
            } else {
                views.userImage.setImageResource(R.drawable.person_image_empty);
            }

            // Set +1 and comment counts.
            final int plusOneCount = (activity.getObject().getPlusoners() != null)
                    ? activity.getObject().getPlusoners().getTotalItems().intValue() : 0;
            if (plusOneCount == 0) {
                views.plusOneIcon.setVisibility(View.GONE);
                views.plusOneCount.setVisibility(View.GONE);
            } else {
                views.plusOneIcon.setVisibility(View.VISIBLE);
                views.plusOneCount.setVisibility(View.VISIBLE);
                views.plusOneCount.setText(Integer.toString(plusOneCount));
            }

            final int commentCount = (activity.getObject().getReplies() != null)
                    ? activity.getObject().getReplies().getTotalItems().intValue() : 0;
            if (commentCount == 0) {
                views.commentIcon.setVisibility(View.GONE);
                views.commentCount.setVisibility(View.GONE);
            } else {
                views.commentIcon.setVisibility(View.VISIBLE);
                views.commentCount.setVisibility(View.VISIBLE);
                views.commentCount.setText(Integer.toString(commentCount));
            }

            // Set content.
            String selfContent = isReshare
                    ? activity.getAnnotation()
                    : activity.getObject().getContent();

            if (!TextUtils.isEmpty(selfContent)) {
                views.content.setVisibility(View.VISIBLE);
                views.content.setText(Html.fromHtml(selfContent));
            } else {
                views.content.setVisibility(View.GONE);
            }

            // Set original author.
            if (activity.getObject().getActor() != null) {
                views.originalAuthor.setVisibility(View.VISIBLE);
                views.originalAuthor.setText(res.getString(R.string.stream_originally_shared,
                        activity.getObject().getActor().getDisplayName()));

                views.reshareLine.setVisibility(View.VISIBLE);
                views.reshareSpacer.setVisibility(View.INVISIBLE);

            } else {
                views.originalAuthor.setVisibility(View.GONE);
                views.reshareLine.setVisibility(View.GONE);
                views.reshareSpacer.setVisibility(View.GONE);
            }

            // Set document content.
            if (isReshare && !TextUtils.isEmpty(activity.getObject().getContent())
                    && detailIndex < views.detail.length) {
                views.detail[detailIndex].setVisibility(View.VISIBLE);
                views.detail[detailIndex].setTextColor(res.getColor(R.color.stream_content_color));
                views.detail[detailIndex].setText(Html.fromHtml(activity.getObject().getContent()));
                ++detailIndex;
            }

            // Set location.
            String location = activity.getPlaceName();
            if (!TextUtils.isEmpty(location)) {
                location = activity.getAddress();
            }
            if (!TextUtils.isEmpty(location)) {
                location = activity.getGeocode();
            }

            if (!TextUtils.isEmpty(location) && detailIndex < views.detail.length) {
                views.detail[detailIndex].setVisibility(View.VISIBLE);
                views.detail[detailIndex].setTextColor(res.getColor(R.color.stream_link_color));
                views.detailIcon[detailIndex].setVisibility(View.VISIBLE);
                views.detail[detailIndex].setText(location);

                if ("checkin".equals(activity.getVerb())) {
                    views.detailIcon[detailIndex].setImageResource(R.drawable.stream_ic_checkin);
                } else {
                    views.detailIcon[detailIndex].setImageResource(R.drawable.stream_ic_location);
                }

                ++detailIndex;
            }

            // Set media content.
            if (activity.getObject().getAttachments() != null) {
                for (Activity.PlusObject.Attachments attachment : activity.getObject().getAttachments()) {
                    String objectType = attachment.getObjectType();
                    if (("photo".equals(objectType) || "video".equals(objectType)) &&
                            mediaIndex < views.media.length) {
                        if (attachment.getImage() == null) {
                            continue;
                        }

                        final ImageView mediaView = views.media[mediaIndex];
                        mediaView.setVisibility(View.VISIBLE);
                        imageFetcher.loadThumbnailImage(attachment.getImage().getUrl(), mediaView);

                        if ("video".equals(objectType)) {
                            views.mediaOverlay[mediaIndex].setVisibility(View.VISIBLE);
                        }

                        ++mediaIndex;

                    } else if (("article".equals(objectType)) &&
                            detailIndex < views.detail.length) {

                        try {
                            String faviconUrl = "http://www.google.com/s2/favicons?domain=" +
                                    Uri.parse(attachment.getUrl()).getHost();

                            final ImageView iconView = views.detailIcon[detailIndex];
                            iconView.setVisibility(View.VISIBLE);
                            imageFetcher.loadThumbnailImage(faviconUrl, iconView);
                        } catch (ParseException ignored) {}

                        views.detail[detailIndex].setVisibility(View.VISIBLE);
                        views.detail[detailIndex].setTextColor(
                                res.getColor(R.color.stream_link_color));
                        views.detail[detailIndex].setText(attachment.getDisplayName());

                        ++detailIndex;
                    }
                }
            }
        }
    }
}
