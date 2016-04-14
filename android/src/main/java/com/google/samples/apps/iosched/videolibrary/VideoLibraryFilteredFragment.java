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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.ModelWithLoaderManager;
import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.videolibrary.VideoLibraryModel.VideoLibraryQueryEnum;
import com.google.samples.apps.iosched.videolibrary.VideoLibraryModel.VideoLibraryUserActionEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;


/**
 * This Fragment displays all the videos of past Google I/O sessions. You can also filter them per
 * year and/or topics.
 */
public class VideoLibraryFilteredFragment extends Fragment implements
        UpdatableView<VideoLibraryModel, VideoLibraryQueryEnum, VideoLibraryUserActionEnum> {

    private static final String TAG = makeLogTag(VideoLibraryFilteredFragment.class);
    private DrawerLayout mDrawerLayout = null;
    private View mEmptyView = null;
    private ImageLoader mImageLoader;
    private List<UserActionListener> mListeners = new ArrayList<>();
    private VideoLibraryFilteredContainer mParent;
    private RadioGroup mTopicsFilterRadioGroup = null;
    private RecyclerView mVideoList = null;
    private RadioGroup mYearsFilterRadioGroup = null;

    @Override
    public void addListener(UserActionListener toAdd) {
        mListeners.add(toAdd);
    }

    @Override
    public void displayData(final VideoLibraryModel model,
            final VideoLibraryQueryEnum query) {
        if ((VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS == query
                || VideoLibraryModel.VideoLibraryQueryEnum.MY_VIEWED_VIDEOS == query)
                && model.getVideos() != null) {
            displayVideos(model);
        }
        if (VideoLibraryModel.VideoLibraryQueryEnum.FILTERS == query) {
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

    @Override
    public void displayErrorMessage(final VideoLibraryQueryEnum query) {
        // No UI changes upon query error
    }

    @Override
    public void displayUserActionResult(final VideoLibraryModel model,
            final VideoLibraryUserActionEnum userAction, final boolean success) {
        switch (userAction) {
            case CHANGE_FILTER:
                displayVideos(model);
                break;
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public Uri getDataUri(final VideoLibraryQueryEnum query) {
        switch (query) {
            case VIDEOS:
            case FILTERS:
                return ScheduleContract.Videos.CONTENT_URI;
            case MY_VIEWED_VIDEOS:
                return ScheduleContract.MyViewedVideos.CONTENT_URI;
            default:
                return Uri.EMPTY;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mImageLoader = new ImageLoader(getActivity(), android.R.color.transparent);
        mYearsFilterRadioGroup = (RadioGroup) getActivity().findViewById(R.id.years_radio_group);
        mTopicsFilterRadioGroup = (RadioGroup) getActivity().findViewById(R.id.topics_radio_group);
        mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_flipped, GravityCompat.END);
        initPresenter();
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (activity instanceof VideoLibraryFilteredContainer) {
            mParent = (VideoLibraryFilteredContainer) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.video_library_filtered_frag, container, false);
        mVideoList = (RecyclerView) root.findViewById(R.id.videos_list);
        mEmptyView = root.findViewById(android.R.id.empty);
        getActivity().overridePendingTransition(0, 0);
        return root;
    }

    @Override
    public void onDetach() {
        mParent = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();

        final DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
        if (drawShadowFrameLayout != null) {
            // configure video fragment's top clearance to take our overlaid Toolbar into account.
            drawShadowFrameLayout.setShadowTopOffset(UIUtils.calculateActionBarSize(getActivity()));
        }
    }

    private void displayVideos(VideoLibraryModel model) {
        if (!model.hasVideos()) {
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        final GridLayoutManager glm = (GridLayoutManager) mVideoList.getLayoutManager();
        final VideoTrackAdapter adapter =
                VideoTrackAdapter.createVerticalGrid(getActivity(), model.getAllVideos(),
                        mImageLoader, mListeners, glm.getSpanCount());
        mVideoList.setAdapter(adapter);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int position) {
                return adapter.getSpanCount(position);
            }
        });
        updateParent(model.getSelectedTopic(), model.getSelectedTopicImageUrl(),
                model.getSelectedTopicColor(), model.getSelectedYear());
        mEmptyView.setVisibility(View.GONE);
    }

    private void initPresenter() {
        VideoLibraryModel model = ModelProvider
                .provideVideoLibraryModel(getDataUri(VideoLibraryQueryEnum.VIDEOS),
                        getDataUri(VideoLibraryQueryEnum.MY_VIEWED_VIDEOS),
                        getDataUri(VideoLibraryQueryEnum.FILTERS), getActivity(),
                        getLoaderManager());

        // Instantiate a new model with initial filter values from the intent call.
        String topicIdFilter = VideoLibraryModel.ALL_TOPICS;
        int yearFilter = VideoLibraryModel.ALL_YEARS;
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            topicIdFilter = extras.getString(VideoLibraryFilteredActivity.KEY_FILTER_TOPIC,
                    VideoLibraryModel.ALL_TOPICS);
            yearFilter = extras.getInt(VideoLibraryFilteredActivity.KEY_FILTER_YEAR,
                    VideoLibraryModel.ALL_YEARS);
        }
        model.setSelectedTopic(topicIdFilter);
        model.setSelectedYear(yearFilter);

        PresenterImpl presenter =
                new PresenterImpl(model, this, VideoLibraryUserActionEnum.values(),
                        VideoLibraryQueryEnum.values());
        presenter.loadInitialQueries();
    }

    /**
     * Called when the user has selected a new filter for videos.
     */
    private void onVideoFilterChanged(Object filter) {
        for (UserActionListener listener : mListeners) {
            Bundle args = new Bundle();
            args.putInt(ModelWithLoaderManager.KEY_RUN_QUERY_ID,
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

    /**
     * Sets the title of the activity depending on the year and topic filters.
     */
    private void updateParent(@Nullable String trackName,
            @Nullable String trackImageUrl, @ColorInt int trackColor, int year) {
        if (mParent != null) {
            String title;
            if (!trackName.equals(VideoLibraryModel.ALL_TOPICS)
                    && year != VideoLibraryModel.ALL_YEARS) {
                title = getString(R.string.title_year_and_topic_filtered_video_library, year,
                        trackName);
            } else if (!trackName.equals(VideoLibraryModel.ALL_TOPICS)
                    && trackName.equals(VideoLibraryModel.KEYNOTES_TOPIC)) {
                title = getString(R.string.keynote_group_title);
            } else if (!trackName.equals(VideoLibraryModel.ALL_TOPICS)) {
                title = getString(R.string.title_topic_filtered_video_library, trackName);
            } else if (year != VideoLibraryModel.ALL_YEARS) {
                title = getString(R.string.title_year_filtered_video_library, year);
            } else {
                title = getString(R.string.title_video_library);
            }
            mParent.filtersUpdated(title, trackImageUrl, trackColor);
        }
    }

    /**
     * Generates RadioButton for each item of the {@code values} list and adds them to the {@code
     * radioGroup}. The item equals to {@code selectedValue} will be checked initially. Items with
     * special Labels can be added using {@code specialValues}. They will be added on top and in
     * uppercase characters.
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
        radioGroup.clearCheck();
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

    public interface VideoLibraryFilteredContainer {
        void filtersUpdated(@NonNull String title, @Nullable String selectionImage,
                @ColorInt int trackColor);
    }

}