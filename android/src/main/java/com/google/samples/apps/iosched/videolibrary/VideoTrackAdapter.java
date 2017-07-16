/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.videolibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.ui.widget.VideoThumbnail;
import com.google.samples.apps.iosched.ui.widget.recyclerview.UpdatableAdapter;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.videolibrary.data.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A {@link RecyclerView.Adapter} for a list of {@link Video}. This adapter runs in two
 * <i>modes</i>: compact and detail.
 * <p/>
 * Compact mode is created via the {@link #createHorizontal(Activity, List, ImageLoader, List)}
 * factory method & shows a smaller presentation of a video. It is used by {@link
 * VideoLibraryFragment}.
 * <p/>
 * The detail mode is created via the {@link #createVerticalGrid(Activity, List, ImageLoader, List,
 * int)} factory method and adds headers dividing videos by year and shows a larger presentation of
 * a video. It is used by {@link VideoLibraryFilteredFragment}.
 */
public class VideoTrackAdapter extends UpdatableAdapter<List<Video>, RecyclerView.ViewHolder> {

    // Constants
    private static final int TYPE_VIDEO = 0;

    private static final int TYPE_YEAR_HEADER = 1;

    private static final String VIDEO_LIBRARY_ANALYTICS_CATEGORY = "Video Library";

    // Immutable state
    private final Activity mHost;

    private final LayoutInflater mInflater;

    private final ImageLoader mImageLoader;

    private final List<UpdatableView.UserActionListener> mListeners;

    private final boolean mCompactMode;

    private final int mColumns;

    private final ColorDrawable[] mBackgroundColors;

    // State
    private List mItems;

    // private constructor, see the more meaningful static factory methods
    private VideoTrackAdapter(@NonNull Activity activity,
            @NonNull List<Video> videos,
            @NonNull ImageLoader imageLoader,
            @NonNull List<UpdatableView.UserActionListener> listeners,
            boolean compactMode,
            int columns) {
        mHost = activity;
        mInflater = LayoutInflater.from(activity);
        mImageLoader = imageLoader;
        mListeners = listeners;
        mCompactMode = compactMode;
        mColumns = columns;
        // load the background colors
        final int[] colors = mHost.getResources().getIntArray(R.array.session_tile_backgrounds);
        mBackgroundColors = new ColorDrawable[colors.length];
        for (int i = 0; i < colors.length; i++) {
            mBackgroundColors[i] = new ColorDrawable(colors[i]);
        }
        mItems = processData(videos);
    }

    public static VideoTrackAdapter createHorizontal(@NonNull Activity activity,
            @NonNull List<Video> videos,
            @NonNull ImageLoader imageLoader,
            @NonNull List<UpdatableView.UserActionListener> listeners) {
        return new VideoTrackAdapter(activity, videos, imageLoader, listeners, true, -1);
    }

    public static VideoTrackAdapter createVerticalGrid(@NonNull Activity activity,
            @NonNull List<Video> videos,
            @NonNull ImageLoader imageLoader,
            @NonNull List<UpdatableView.UserActionListener> listeners,
            int columns) {
        return new VideoTrackAdapter(activity, videos, imageLoader, listeners, false, columns);
    }

    @Override
    public void update(@NonNull final List<Video> updatedData) {
        // Attempt to update data in place i.e. only if it has changed so as not to lose scroll
        // position etc when an item updates e.g. when a video is marked as watched
        final List newItems = processData(updatedData);
        if (newItems.size() != mItems.size()) {
            mItems = newItems;
            notifyDataSetChanged();
            return;
        }
        for (int i = 0; i < newItems.size(); i++) {
            final Object oldItem = mItems.get(i);
            final Object newItem = newItems.get(i);
            // Because we update the Video object directly from the model, we need to check if it
            // has been updated rather than using equals
            if (!oldItem.equals(newItem) ||
                    (newItem instanceof Video && ((Video) newItem).dataUpdated())) {
                mItems.set(i, newItem);
                notifyItemChanged(i);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case TYPE_VIDEO:
                return createVideoViewHolder(parent);
            case TYPE_YEAR_HEADER:
                return createYearHeaderViewHolder(parent);
            default:
                throw new IllegalArgumentException("Unknown view type");
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        switch (getItemViewType(position)) {
            case TYPE_VIDEO:
                bindVideoViewHolder((VideoViewHolder) holder,
                        (Video) mItems.get(position), position);
                break;
            case TYPE_YEAR_HEADER:
                bindYearHeaderViewHolder((HeaderViewHolder) holder,
                        (YearHeader) mItems.get(position));
                break;
        }
    }

    @Override
    public int getItemViewType(final int position) {
        if (mCompactMode) {
            return TYPE_VIDEO;
        }
        Object item = mItems.get(position);
        if (item instanceof Video) {
            return TYPE_VIDEO;
        } else if (item instanceof YearHeader) {
            return TYPE_YEAR_HEADER;
        }
        throw new IllegalArgumentException("Unknown item type");
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public int getSpanCount(final int position) {
        if (mCompactMode || getItemViewType(position) == TYPE_VIDEO) {
            return 1;
        } else {
            return mColumns;
        }
    }

    /**
     * Process the given {@code vidoe} to create the list of items to be displayed by the {@link
     * RecyclerView}. In detail mode, this means inserting date header objects to separate videos by
     * year.
     */
    private List processData(final List<Video> videos) {
        List data = new ArrayList(videos.size());

        if (mCompactMode) {
            data.addAll(videos);
        } else {
            int currentYear = Integer.MAX_VALUE;
            for (final Video video : videos) {
                if (video.getYear() < currentYear) {
                    currentYear = video.getYear();
                    data.add(new YearHeader(String.valueOf(currentYear)));
                }
                data.add(video);
            }
        }
        return data;
    }

    private
    @NonNull
    RecyclerView.ViewHolder createVideoViewHolder(final ViewGroup parent) {
        final VideoViewHolder holder = new VideoViewHolder(
                mInflater.inflate(mCompactMode ? R.layout.video_item_list_tile
                        : R.layout.video_item_grid_tile, parent, false));
        if (mCompactMode) {
            ViewCompat.setImportantForAccessibility(holder.itemView,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
            ViewCompat.setImportantForAccessibility(holder.title,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        return holder;
    }

    private
    @NonNull
    RecyclerView.ViewHolder createYearHeaderViewHolder(final ViewGroup parent) {
        return new HeaderViewHolder(mInflater.inflate(
                R.layout.grid_header_major, parent, false));
    }

    private void bindVideoViewHolder(final VideoViewHolder holder,
            final Video video,
            final int position) {
        holder.itemView.setBackgroundDrawable(
                mBackgroundColors[position % mBackgroundColors.length]);
        holder.itemView.setOnClickListener(mVideoClick);
        mImageLoader.loadImage(video.getThumbnailUrl(), holder.thumbnail);
        holder.title.setText(video.getTitle());
        holder.thumbnail.setPlayed(video.getAlreadyPlayed());
    }

    private final View.OnClickListener mVideoClick = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (!(lp instanceof RecyclerView.LayoutParams)) {
                return;
            }
            final int position = ((RecyclerView.LayoutParams) lp).getViewAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            final Video video = (Video) mItems.get(position);
            final String videoId = video.getId();
            final String youtubeLink = TextUtils.isEmpty(videoId) ? "" :
                    videoId.contains("://") ? videoId :
                            String.format(Locale.US, Config.VIDEO_LIBRARY_URL_FMT, videoId);
            if (!TextUtils.isEmpty(youtubeLink)) {
                // ANALYTICS EVENT: Click on a video on the Video Library screen
                // Contains: video's YouTube URL, http://www.youtube.com/...
                AnalyticsHelper.sendEvent(VIDEO_LIBRARY_ANALYTICS_CATEGORY, "selectvideo",
                        youtubeLink);
                // Start playing the video on Youtube.
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeLink));
                UIUtils.preferPackageForIntent(mHost, i,
                        UIUtils.YOUTUBE_PACKAGE_NAME);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                mHost.startActivity(i);
                // Mark the video as played.
                fireVideoPlayedEvent(video);
            }
        }
    };

    private void bindYearHeaderViewHolder(final HeaderViewHolder holder,
            final YearHeader yearHeader) {
        holder.header.setText(yearHeader.year);
    }

    /**
     * Let all UserActionListener know that the given Video has been played.
     */
    private void fireVideoPlayedEvent(Video video) {
        for (UpdatableView.UserActionListener h1 : mListeners) {
            Bundle args = new Bundle();
            args.putString(VideoLibraryModel.KEY_VIDEO_ID, video.getId());
            h1.onUserAction(VideoLibraryModel.VideoLibraryUserActionEnum.VIDEO_PLAYED, args);
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {

        final VideoThumbnail thumbnail;
        final TextView title;

        public VideoViewHolder(final View itemView) {
            super(itemView);
            thumbnail = (VideoThumbnail) itemView.findViewById(R.id.thumbnail);
            title = (TextView) itemView.findViewById(R.id.title);
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        final TextView header;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            header = (TextView) itemView;
        }
    }

    private static class YearHeader {

        private final String year;

        public YearHeader(String year) {
            this.year = year;
        }
    }
}
