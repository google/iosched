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

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;


/**
 * This Fragment displays all the videos of past Google I/O sessions. You can also filter them per
 * year and/or topics.
 */
public class VideoLibraryFilteredFragment extends Fragment implements UpdatableView<VideoLibraryModel>,
        CollectionViewCallbacks {

    private static final String TAG = makeLogTag(VideoLibraryFilteredFragment.class);

    private static final String FILTERED_VIDEO_LIBRARY_ANALYTICS_CATEGORY =
            "Filtered Video Library";

    private ImageLoader mImageLoader;

    private CollectionView mCollectionView = null;

    private View mEmptyView = null;

    private RadioGroup mYearsFilterRadioGroup = null;

    private RadioGroup mTopicsFilterRadioGroup = null;

    private DrawerLayout mDrawerLayout = null;

    private List<UserActionListener> mListeners = new ArrayList<>();

    @Override
    public void displayData(VideoLibraryModel model, QueryEnum query) {
        if ((VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS == query
                || VideoLibraryModel.VideoLibraryQueryEnum.MY_VIEWED_VIDEOS == query)
                && model.getVideos() != null) {
            updateCollectionView(model.getVideos());
            setActivityTitle(model.getSelectedYear(), model.getSelectedTopic());
        } if (VideoLibraryModel.VideoLibraryQueryEnum.FILTERS == query) {
            Map<Integer, String> specialYearEntries = new HashMap<>();
            specialYearEntries.put(VideoLibraryModel.ALL_YEARS, getString(R.string.all));
            updateRadioGroup(mYearsFilterRadioGroup, model.getYears(), model.getSelectedYear(),
                    specialYearEntries);
            Map<String, String> specialTopicEntries = new HashMap<>();
            specialTopicEntries.put(VideoLibraryModel.ALL_TOPICS, getString(R.string.all));
            specialTopicEntries.put(VideoLibraryModel.KEYNOTES_TOPIC,
                    VideoLibraryModel.KEYNOTES_TOPIC);
            List<String> topics = model.getTopics();
            topics.remove(VideoLibraryModel.KEYNOTES_TOPIC);
            updateRadioGroup(mTopicsFilterRadioGroup, model.getTopics(), model.getSelectedTopic(),
                    specialTopicEntries);
        }
    }

    /**
     * Sets the title of the activity depending on the year and topic filters.
     */
    private void setActivityTitle(int yearFilter, String topicFilter) {
        if (!topicFilter.equals(VideoLibraryModel.ALL_TOPICS)
                && yearFilter != VideoLibraryModel.ALL_YEARS) {
            getActivity().setTitle(getString(R.string.title_year_and_topic_filtered_video_library,
                    yearFilter, topicFilter));
        } else if (!topicFilter.equals(VideoLibraryModel.ALL_TOPICS)
                && topicFilter.equals(VideoLibraryModel.KEYNOTES_TOPIC)) {
            getActivity().setTitle(R.string.keynote_group_title);
        } else if (!topicFilter.equals(VideoLibraryModel.ALL_TOPICS)) {
            getActivity().setTitle(getString(R.string.title_topic_filtered_video_library,
                    topicFilter));
        } else if (yearFilter != VideoLibraryModel.ALL_YEARS) {
            getActivity().setTitle(getString(R.string.title_year_filtered_video_library,
                    yearFilter));
        } else {
            getActivity().setTitle(R.string.title_video_library);
        }
    }

    /**
     * Generates RadioButton for each item of the {@code values} list and adds them to the
     * {@code radioGroup}. The item equals to {@code selectedValue} will be checked initially. Items
     * with special Labels can be added using {@code specialValues}. They will be added on top and
     * in uppercase characters.
     */
    private <T extends Comparable> void updateRadioGroup(final RadioGroup radioGroup,
            List<T> values, T selectedValue, Map<T, String> specialValues) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        if (radioGroup == null) {
            return;
        }

        // Add special Values to the list
        List<T> specialItemsList = new ArrayList<>(specialValues.keySet());
        Collections.sort(specialItemsList);
        for (T keys : specialItemsList) {
            values.add(0, keys);
        }

        radioGroup.removeAllViews();
        int idCounter = 0;
        for (final T value : values) {
            View buttonLayout = inflater.inflate(
                    R.layout.video_library_filter_radio_button, radioGroup, false);
            final RadioButton button = (RadioButton) buttonLayout.findViewById(R.id.button);
            radioGroup.addView(buttonLayout);

            // Set the Label of the Radio Button.
            TextView text = (TextView) buttonLayout.findViewById(R.id.text);
            text.setText(specialValues.get(value) == null ?
                    value.toString() : specialValues.get(value));

            // We have to give different IDs to all the RadioButtons inside the RadioGroup so that
            // only one can be checked at a time.
            button.setId(idCounter);
            idCounter++;

            // Trigger a RadioButton click when clicking the Text.
            text.setOnClickListener(new View.OnClickListener() {
                @Override
                @TargetApi(15)
                public void onClick(View v) {
                    button.callOnClick();
                }
            });

            // When Clicking the RadioButton filter when re-filter the videos.
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    radioGroup.check(button.getId());
                    onVideoFilterChanged(value);
                }
            });

            if (selectedValue.equals(value)) {
                radioGroup.check(button.getId());
            }
        }
    }

    /**
     * Called when the user has selected a new filter for videos.
     */
    private void onVideoFilterChanged(Object filter) {
        for (UserActionListener listener : mListeners) {
            Bundle args = new Bundle();
            args.putInt(PresenterFragmentImpl.KEY_RUN_QUERY_ID,
                    VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS.getId());
            if (filter instanceof Integer) {
                args.putInt(VideoLibraryModel.KEY_YEAR, (Integer) filter);
            } else if (filter instanceof String) {
                args.putString(VideoLibraryModel.KEY_TOPIC, (String) filter);
            }
            listener.onUserAction(VideoLibraryModel.VideoLibraryUserActionEnum.CHANGE_FILTER, args);
        }
        mDrawerLayout.closeDrawer(GravityCompat.END);
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

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mImageLoader = new ImageLoader(getActivity(), android.R.color.transparent);
        mYearsFilterRadioGroup = (RadioGroup) getActivity().findViewById(R.id.years_radio_group);
        mTopicsFilterRadioGroup = (RadioGroup) getActivity().findViewById(R.id.topics_radio_group);
        mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_flipped, GravityCompat.END);
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
        setContentTopClearance(actionBarSize
                + getResources().getDimensionPixelSize(R.dimen.explore_grid_padding));
    }

    /**
     * Updates the CollectionView with the given list of {@code videos}.
     */
    private void updateCollectionView(List<VideoLibraryModel.Video> videos) {
        LOGD(TAG, "Updating filtered video library collection view.");
        CollectionView.Inventory inventory = new CollectionView.Inventory();
        int normalColumns = getResources().getInteger(R.integer.video_library_columns);

        // Go through all videos and organize them into groups for each topic. We assume they are
        // already ordered by topics.
        CollectionView.InventoryGroup curGroup = new CollectionView.InventoryGroup(0)
                        .setDataIndexStart(0)
                        .setShowHeader(false)
                        .setDisplayCols(normalColumns);
        for (int dataIndex = 0; dataIndex < videos.size(); ++dataIndex) {
            curGroup.addItemWithTag(videos.get(dataIndex));
        }

        if (curGroup.getRowCount() > 0) {
            inventory.addGroup(curGroup);
        }

        mCollectionView.setCollectionAdapter(this);
        mCollectionView.updateInventory(inventory);

        mEmptyView.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.list_item_explore_header, parent, false);
    }

    @Override
    public void bindCollectionHeaderView(Context context, View view, int groupId,
                                         String headerLabel, Object headerTag) {
        ((TextView) view.findViewById(android.R.id.text1)).setText(headerLabel);
    }

    @Override
    public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.video_library_item, parent, false);
    }

    @Override
    public void bindCollectionItemView(Context context, View view, int groupId,
                int indexInGroup, int dataIndex, Object tag) {
        final VideoLibraryModel.Video video = (VideoLibraryModel.Video) tag;
        if (video == null) {
            return;
        }
        ImageView thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        TextView speakersView = (TextView) view.findViewById(R.id.speakers);
        TextView descriptionView = (TextView) view.findViewById(R.id.description);
        titleView.setText(video.getTitle());
        speakersView.setText(video.getSpeakers());
        speakersView.setVisibility(
                TextUtils.isEmpty(video.getSpeakers()) ? View.GONE : View.VISIBLE);
        descriptionView.setText(video.getDesc());
        descriptionView.setVisibility(
                TextUtils.isEmpty(video.getDesc()) || video.getTitle().equals(video.getDesc()) ?
                        View.GONE : View.VISIBLE);

        String thumbUrl = video.getThumbnailUrl();
        if (TextUtils.isEmpty(thumbUrl)) {
            thumbnailView.setImageResource(android.R.color.transparent);
        } else {
            mImageLoader.loadImage(thumbUrl, thumbnailView);
        }

        // Display the overlay if the video has already been played.
        if (video.getAlreadyPlayed()) {
            styleVideoAsViewed(view);
        }

        final String videoId = video.getId();
        final String youtubeLink = TextUtils.isEmpty(videoId) ? "" :
                videoId.contains("://") ? videoId :
                String.format(Locale.US, Config.VIDEO_LIBRARY_URL_FMT, videoId);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(youtubeLink)) {
                    LOGD(TAG, "Launching Youtube video: " + youtubeLink);

                    // ANALYTICS EVENT: Click on a video on the Filtered Video Library screen
                    // Contains: video's YouTube URL, http://www.youtube.com/...
                    AnalyticsHelper.sendEvent(FILTERED_VIDEO_LIBRARY_ANALYTICS_CATEGORY,
                            "selectvideo", youtubeLink);
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
        if (query == VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS
                || query == VideoLibraryModel.VideoLibraryQueryEnum.FILTERS) {
            return ScheduleContract.Videos.CONTENT_URI;
        } else if (query == VideoLibraryModel.VideoLibraryQueryEnum.MY_VIEWED_VIDEOS) {
            return ScheduleContract.MyViewedVideos.CONTENT_URI;
        }
        return Uri.EMPTY;
    }
}
