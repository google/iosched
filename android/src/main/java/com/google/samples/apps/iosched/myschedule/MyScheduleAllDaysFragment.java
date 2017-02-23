/*
 * Copyright 2016 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.myschedule;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleQueryEnum;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleUserActionEnum;
import com.google.samples.apps.iosched.util.TimeUtils;

/**
 * This is used by the wide layout in {@link MyScheduleActivity}. It uses a
 * {@link RecyclerView} for each day of the conference.
 */
public class MyScheduleAllDaysFragment extends Fragment
        implements UpdatableView<MyScheduleModel, MyScheduleQueryEnum, MyScheduleUserActionEnum>,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int TAG_METADATA_TOKEN = 0x8;

    private RecyclerView mPreConferenceDayView;

    // TODO - this layout assumes the conference lasts exactly 3 days, make it more flexible in the
    // way it is built
    private final RecyclerView[] mMyScheduleSingleDayViews = new RecyclerView[3];

    private UserActionListener<MyScheduleUserActionEnum> mListener;

    private TagMetadata mTagMetadata;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
        getLoaderManager().initLoader(TAG_METADATA_TOKEN, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.my_schedule_alldays_frag, container, false);
        mMyScheduleSingleDayViews[0] = (RecyclerView) v.findViewById(R.id.my_schedule_first_day);
        mMyScheduleSingleDayViews[1] = (RecyclerView) v.findViewById(R.id.my_schedule_second_day);
        mMyScheduleSingleDayViews[2] = (RecyclerView) v.findViewById(R.id.my_schedule_third_day);
        return v;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        TextView firstDayHeaderView = (TextView) view.findViewById(R.id.day_label_first_day);
        TextView secondDayHeaderView = (TextView) view.findViewById(R.id.day_label_second_day);
        TextView thirdDayHeaderView = (TextView) view.findViewById(R.id.day_label_third_day);
        if (firstDayHeaderView != null) {
            firstDayHeaderView.setText(TimeUtils.getDayName(getContext(), 0));
        }
        if (secondDayHeaderView != null) {
            secondDayHeaderView.setText(TimeUtils.getDayName(getContext(), 1));
        }
        if (thirdDayHeaderView != null) {
            thirdDayHeaderView.setText(TimeUtils.getDayName(getContext(), 2));
        }

        for (int i = 0; i < mMyScheduleSingleDayViews.length; i++) {
            setupRecyclerView(mMyScheduleSingleDayViews[i]);
        }

        mPreConferenceDayView = (RecyclerView) view.findViewById(R.id.my_schedule_zeroth_day);
        setupRecyclerView(mPreConferenceDayView);

        TextView zerothDayHeaderView = (TextView) view.findViewById(R.id.day_label_zeroth_day);
        if (MyScheduleModel.showPreConferenceData(getContext())) {
            mPreConferenceDayView.setVisibility(View.VISIBLE);
            zerothDayHeaderView.setText(TimeUtils.getDayName(getContext(), -1));
            zerothDayHeaderView.setVisibility(View.VISIBLE);
        } else {
            mPreConferenceDayView.setVisibility(View.GONE);
            zerothDayHeaderView.setVisibility(View.GONE);
        }
    }

    @Override
    public void displayData(MyScheduleModel model, MyScheduleQueryEnum query) {
        switch (query) {
            case SCHEDULE:
                updateSchedule(model);
                break;
        }
    }

    @Override
    public void displayErrorMessage(MyScheduleQueryEnum query) {
        // Not showing any error
    }

    @Override
    public void displayUserActionResult(MyScheduleModel model, MyScheduleUserActionEnum userAction,
            boolean success) {
        switch (userAction) {
            case RELOAD_DATA:
            case SESSION_STAR:
            case SESSION_UNSTAR:
                updateSchedule(model);
                break;
            case SESSION_SLOT:
                break;
            case FEEDBACK:
                break;
            default:
                break;
        }
    }

    private void updateSchedule(MyScheduleModel model) {
        if (isVisible()) {
            if (MyScheduleModel.showPreConferenceData(getContext())) {
                MyScheduleDayAdapter adapter =
                        (MyScheduleDayAdapter) mPreConferenceDayView.getAdapter();
                if (adapter == null) {
                    adapter = new MyScheduleDayAdapter(getContext(), mListener, mTagMetadata);
                    mPreConferenceDayView.setAdapter(adapter);
                }
                adapter.updateItems(
                        model.getConferenceDataForDay(MyScheduleModel.PRE_CONFERENCE_DAY_ID));
            }
            for (int i = 0; i < mMyScheduleSingleDayViews.length; i++) {
                RecyclerView view = mMyScheduleSingleDayViews[i];
                MyScheduleDayAdapter adapter = (MyScheduleDayAdapter) view.getAdapter();
                if (adapter == null) {
                    adapter = new MyScheduleDayAdapter(getContext(), mListener, mTagMetadata);
                    view.setAdapter(adapter);
                }
                // Day 1 of conference has id 1
                adapter.updateItems(model.getConferenceDataForDay(i + 1));
            }
        }
    }

    @Override
    public Uri getDataUri(MyScheduleQueryEnum query) {
        return null;
    }

    @Override
    public void addListener(UserActionListener<MyScheduleUserActionEnum> listener) {
        mListener = listener;
    }

    private void setupRecyclerView(@NonNull final RecyclerView view) {
        view.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case TAG_METADATA_TOKEN:
                return TagMetadata.createCursorLoader(getContext());
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case TAG_METADATA_TOKEN:
                mTagMetadata = new TagMetadata(cursor);
                for (int i = 0; i < mMyScheduleSingleDayViews.length; i++) {
                    MyScheduleDayAdapter adapter =
                            (MyScheduleDayAdapter) mMyScheduleSingleDayViews[i].getAdapter();
                    if (adapter != null) {
                        adapter.setTagMetadata(mTagMetadata);
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
    }
}
