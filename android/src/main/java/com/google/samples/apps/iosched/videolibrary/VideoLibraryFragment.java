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

package com.google.samples.apps.iosched.videolibrary;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.framework.PresenterFragmentImpl;
import com.google.samples.apps.iosched.framework.QueryEnum;
import com.google.samples.apps.iosched.framework.UpdatableView;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This Fragment displays all the videos of past Google I/O sessions in the form of a card for each
 * topics and a card for new videos of the current year.
 */
public class VideoLibraryFragment extends Fragment implements UpdatableView<VideoLibraryModel>,
        CollectionViewCallbacks.GroupCollectionViewCallbacks {

    private static final String TAG = makeLogTag(VideoLibraryFragment.class);

    private static final String VIDEO_LIBRARY_ANALYTICS_CATEGORY = "Video Library";

    private static final int GROUP_ID_NEW = 0;

    private static final int GROUP_ID_KEYNOTES = 1;

    private static final int GROUP_ID_TOPIC = 2;

    private ImageLoader mImageLoader;

    private CollectionView mCollectionView = null;

    private View mEmptyView = null;

    private List<UserActionListener> mListeners = new ArrayList<>();

    @Override
    public void displayData(VideoLibraryModel model, QueryEnum query) {
        if ((VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS == query
                || VideoLibraryModel.VideoLibraryQueryEnum.MY_VIEWED_VIDEOS == query)
                && model.getVideos() != null) {
            updateCollectionView(model.getVideos());
        }
    }

    @Override
    public void displayErrorMessage(QueryEnum query) {
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void addListener(UserActionListener toAdd) {
        mListeners.add(toAdd);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.video_library_frag, container, false);
        mCollectionView = (CollectionView) root.findViewById(R.id.videos_collection_view);
        mEmptyView = root.findViewById(android.R.id.empty);
        getActivity().overridePendingTransition(0, 0);

        // Reload the content so that new random Videos are shown.
        fireReloadEvent();

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mImageLoader = new ImageLoader(getActivity(), android.R.color.transparent);
    }

    private void setContentTopClearance(int clearance) {
        if (mCollectionView != null) {
            mCollectionView.setContentTopClearance(clearance);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();

        // configure video fragment's top clearance to take our overlaid controls (Action Bar
        // and spinner box) into account.
        int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }
        setContentTopClearance(actionBarSize);
    }

    /**
     * Returns a {@link CollectionView.InventoryGroup} containing {@code numRandomVideos} number of
     * videos randomly selected in the given {@code videos} list.
     */
    private CollectionView.InventoryGroup makeRandomCollectionViewInventoryGroup(
            List<VideoLibraryModel.Video> videos, int numRandomVideos, String groupHeaderLabel,
            int groupId) {

        // Get the number of display columns for each groups.
        int normalColumns = getResources().getInteger(R.integer.video_library_columns);

        // Randomly select the requested number of items fro the list.
        videos = new ArrayList<>(videos);
        Collections.shuffle(videos);
        videos = videos.subList(0, Math.min(videos.size(), numRandomVideos));

        // Add these videos to the group.
        CollectionView.InventoryGroup lastYearGroup =
                new CollectionView.InventoryGroup(groupId)
                        .setDataIndexStart(0)
                        .setHeaderLabel(groupHeaderLabel)
                        .setShowHeader(true)
                        .setDisplayCols(normalColumns);
        for (VideoLibraryModel.Video video : videos) {
            lastYearGroup.addItemWithTag(video);
        }
        return lastYearGroup;
    }

    /**
     * Returns the current year. We use it to display a special card for new videos.
     */
    private static int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * Updates the CollectionView with the given list of {@code videos}.
     */
    private void updateCollectionView(List<VideoLibraryModel.Video> videos) {
        LOGD(TAG, "Updating video library collection view.");
        CollectionView.Inventory inventory = new CollectionView.Inventory();
        int shownVideos = getResources().getInteger(R.integer.shown_videos);

        // Find out what's the current year.
        int currentYear = getCurrentYear();

        // Get all the videos for the current year. They go into a special section for "new" videos.
        // This means this section will contain no videos between 31st of december and the next
        // Google IO which typically happens in May/June. So in effect Videos of more than 6 month
        // are not considered "New" anymore.
        List<VideoLibraryModel.Video> latestYearVideos = new ArrayList<>();
        for (int dataIndex = 0; dataIndex < videos.size(); ++dataIndex) {
            VideoLibraryModel.Video video = videos.get(dataIndex);
            if (currentYear == video.getYear()) {
                latestYearVideos.add(video);
            }
        }

        if (latestYearVideos.size() > 0) {
            CollectionView.InventoryGroup lastYearGroup = makeRandomCollectionViewInventoryGroup(
                    latestYearVideos, shownVideos,
                    getString(R.string.new_videos_title, currentYear), GROUP_ID_NEW);
            inventory.addGroup(lastYearGroup);
        }

        // Adding keynotes on top.
        List<VideoLibraryModel.Video> keynotes = new ArrayList<>();
        for (int dataIndex = 0; dataIndex < videos.size(); ++dataIndex) {
            VideoLibraryModel.Video video = videos.get(dataIndex);
            String curTopic = video.getTopic();

            // We ignore the video if it;s not a keynote.
            if (!VideoLibraryModel.KEYNOTES_TOPIC.equals(curTopic)) {
                continue;
            }

            keynotes.add(video);
        }
        CollectionView.InventoryGroup curGroup = makeRandomCollectionViewInventoryGroup(
                keynotes, shownVideos, VideoLibraryModel.KEYNOTES_TOPIC, GROUP_ID_KEYNOTES);
        inventory.addGroup(curGroup);

        // Go through all videos and organize them into groups for each topic. We assume they are
        // already ordered by topics already.
        List<VideoLibraryModel.Video> curGroupVideos = new ArrayList<>();
        for (int dataIndex = 0; dataIndex < videos.size(); ++dataIndex) {
            VideoLibraryModel.Video video = videos.get(dataIndex);
            String curTopic = video.getTopic();

            // We ignore Keynotes because they have already been added.
            if (VideoLibraryModel.KEYNOTES_TOPIC.equals(curTopic)) {
                continue;
            }

            // Skip some potentially problematic videos that have null topics.
            if(curTopic == null) {
                LOGW(TAG, "Video with title '" + video.getTitle() + "' has a null topic so it "
                        + "won't be displayed in the video library.");
                continue;
            }
            curGroupVideos.add(video);

            // If we've added all the videos with the same topic (i.e. the next video has a
            // different topic) then we create the InventoryGroup and add it to the Inventory.
            if (dataIndex == videos.size() - 1 ||
                    !videos.get(dataIndex + 1).getTopic().equals(curTopic)) {
                curGroup = makeRandomCollectionViewInventoryGroup(
                        curGroupVideos, shownVideos, curTopic, GROUP_ID_TOPIC);
                inventory.addGroup(curGroup);
                curGroupVideos = new ArrayList<>();
            }
        }

        mCollectionView.setCollectionAdapter(this);
        mCollectionView.updateInventory(inventory);

        mEmptyView.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public ViewGroup newCollectionGroupView(Context context, int groupId,
                                            CollectionView.InventoryGroup group, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        return (ViewGroup) inflater.inflate(R.layout.video_lib_card_container, parent, false);
    }

    @Override
    public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.card_header_with_button, parent, false);
    }

    @Override
    public void bindCollectionHeaderView(Context context, View view, final int groupId,
                                         final String headerLabel, Object headerTag) {
        ((TextView) view.findViewById(android.R.id.title)).setText(headerLabel);
        view.setContentDescription(getString(R.string.more_items_button_desc_with_label_a11y,
                headerLabel));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Clicking More button on VideoLib category: " + headerLabel);

                // ANALYTICS EVENT: Click on the "More" button of a card in the Video Library
                // Contains: The clicked header's label
                AnalyticsHelper.sendEvent(VIDEO_LIBRARY_ANALYTICS_CATEGORY, "morebutton",
                        headerLabel);
                // Start the Filtered Video Library intent.
                Intent i = new Intent(getContext(), VideoLibraryFilteredActivity.class);
                if (groupId == GROUP_ID_KEYNOTES) {
                    i.putExtra(VideoLibraryFilteredActivity.KEY_FILTER_TOPIC,
                            VideoLibraryModel.KEYNOTES_TOPIC);
                } else if (groupId == GROUP_ID_NEW) {
                    i.putExtra(VideoLibraryFilteredActivity.KEY_FILTER_YEAR, getCurrentYear());
                } else if (groupId == GROUP_ID_TOPIC) {
                    i.putExtra(VideoLibraryFilteredActivity.KEY_FILTER_TOPIC, headerLabel);
                }
                startActivity(i);
            }
        });
    }

    /**
     * Holds pointers to View's children.
     */
    static class CollectionItemViewHolder {
        ImageView thumbnailView;
        TextView titleView;
        TextView speakersView;
        TextView descriptionView;
    }

    @Override
    public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.video_library_item, parent, false);
        CollectionItemViewHolder viewHolder = new CollectionItemViewHolder();
        viewHolder.thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
        viewHolder.titleView = (TextView) view.findViewById(R.id.title);
        viewHolder.speakersView = (TextView) view.findViewById(R.id.speakers);
        viewHolder.descriptionView = (TextView) view.findViewById(R.id.description);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindCollectionItemView(Context context, View view, int groupId,
            int indexInGroup, int dataIndex, Object tag) {
        final VideoLibraryModel.Video video = (VideoLibraryModel.Video) tag;
        if (video == null) {
            return;
        }
        CollectionItemViewHolder viewHolder = (CollectionItemViewHolder) view.getTag();
        viewHolder.titleView.setText(video.getTitle());
        viewHolder.speakersView.setText(video.getSpeakers());
        viewHolder.speakersView.setVisibility(
                TextUtils.isEmpty(video.getSpeakers()) ? View.GONE : View.VISIBLE);
        viewHolder.descriptionView.setText(video.getDesc());
        viewHolder.descriptionView.setVisibility(
                TextUtils.isEmpty(video.getDesc()) || video.getTitle().equals(video.getDesc()) ?
                        View.GONE : View.VISIBLE);

        String thumbUrl = video.getThumbnailUrl();
        if (TextUtils.isEmpty(thumbUrl)) {
            viewHolder.thumbnailView.setImageResource(android.R.color.transparent);
        } else {
            mImageLoader.loadImage(thumbUrl, viewHolder.thumbnailView);
        }

        final String videoId = video.getId();
        final String youtubeLink = TextUtils.isEmpty(videoId) ? "" :
                videoId.contains("://") ? videoId :
                        String.format(Locale.US, Config.VIDEO_LIBRARY_URL_FMT, videoId);

        // Display the overlay if the video has already been played.
        if (video.getAlreadyPlayed()) {
            styleVideoAsViewed(view);
        } else {
            viewHolder.thumbnailView.setColorFilter(getContext().getResources().getColor(
                    R.color.light_content_scrim));
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(youtubeLink)) {
                    LOGD(TAG, "Launching Youtube video: " + youtubeLink);

                    // ANALYTICS EVENT: Click on a video on the Video Library screen
                    // Contains: video's YouTube URL, http://www.youtube.com/...
                    AnalyticsHelper.sendEvent(VIDEO_LIBRARY_ANALYTICS_CATEGORY, "selectvideo",
                            youtubeLink);
                    // Start playing the video on Youtube.
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeLink));
                    UIUtils.preferPackageForIntent(getActivity(), i,
                            UIUtils.YOUTUBE_PACKAGE_NAME);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(i);
                    // Mark the video as played.
                    fireVideoPlayedEvent(video);
                    // Display the overlay for videos that has already been played.
                    styleVideoAsViewed(view);
                }
            }
        });
    }

    /**
     * Show the video as Viewed. We display a semi-transparent grey overlay over the video
     * thumbnail.
     */
    private void styleVideoAsViewed(View videoItemView) {
        ImageView thumbnailView = (ImageView) videoItemView.findViewById(R.id.thumbnail);
        thumbnailView.setColorFilter(getContext().getResources().getColor(
                R.color.video_scrim_watched));
    }

    /**
     * Let all UserActionListener know that the video list has been reloaded and that therefore we
     * need to display another random set of videos.
     */
    private void fireReloadEvent() {
        for (UserActionListener h1 : mListeners) {
            Bundle args = new Bundle();
            args.putInt(PresenterFragmentImpl.KEY_RUN_QUERY_ID,
                    VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS.getId());
            h1.onUserAction(VideoLibraryModel.VideoLibraryUserActionEnum.RELOAD, args);
        }
    }

    /**
     * Let all UserActionListener know that the given Video has been played.
     */
    private void fireVideoPlayedEvent(VideoLibraryModel.Video video) {
        for (UserActionListener h1 : mListeners) {
            Bundle args = new Bundle();
            args.putString(VideoLibraryModel.KEY_VIDEO_ID, video.getId());
            h1.onUserAction(VideoLibraryModel.VideoLibraryUserActionEnum.VIDEO_PLAYED, args);
        }
    }

    @Override
    public Uri getDataUri(QueryEnum query) {
        if (query == VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS) {
            return ScheduleContract.Videos.CONTENT_URI;
        } else if (query == VideoLibraryModel.VideoLibraryQueryEnum.MY_VIEWED_VIDEOS) {
            return ScheduleContract.MyViewedVideos.CONTENT_URI;
        }
        return Uri.EMPTY;
    }
}
