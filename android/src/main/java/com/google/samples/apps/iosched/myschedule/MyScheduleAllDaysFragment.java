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

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleQueryEnum;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleUserActionEnum;
import com.google.samples.apps.iosched.util.TimeUtils;

/**
 * This is used by the wide layout in {@link MyScheduleActivity}. It uses a {@link
 * MyScheduleSingleDayNoScrollView} for each day of the conference.
 */
public class MyScheduleAllDaysFragment extends Fragment
        implements UpdatableView<MyScheduleModel, MyScheduleQueryEnum, MyScheduleUserActionEnum> {

    private MyScheduleSingleDayNoScrollView mPreConferenceDayView;

    // TODO - this layout assumes the conference lasts exactly 3 days, make it more flexible in the
    // way it is built
    private MyScheduleSingleDayNoScrollView[] mMyScheduleSingleDayViews
            = new MyScheduleSingleDayNoScrollView[3];

    private UserActionListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.my_schedule_alldays_frag, container, false);
        mMyScheduleSingleDayViews[0] = (MyScheduleSingleDayNoScrollView) root
                .findViewById(R.id.my_schedule_first_day);
        mMyScheduleSingleDayViews[1] = (MyScheduleSingleDayNoScrollView) root
                .findViewById(R.id.my_schedule_second_day);
        mMyScheduleSingleDayViews[2] = (MyScheduleSingleDayNoScrollView) root
                .findViewById(R.id.my_schedule_third_day);
        setRetainInstance(false);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initViews();
    }

    private void initViews() {
        TextView firstDayHeaderView = (TextView) getActivity()
                .findViewById(R.id.day_label_first_day);
        TextView secondDayHeaderView = (TextView) getActivity()
                .findViewById(R.id.day_label_second_day);
        TextView thirdDayHeaderView = (TextView) getActivity()
                .findViewById(R.id.day_label_third_day);
        if (firstDayHeaderView != null) {
            firstDayHeaderView.setText(TimeUtils.getDayName(getContext(), 0));
        }
        if (secondDayHeaderView != null) {
            secondDayHeaderView.setText(TimeUtils.getDayName(getContext(), 1));
        }
        if (thirdDayHeaderView != null) {
            thirdDayHeaderView.setText(TimeUtils.getDayName(getContext(), 2));
        }

        mPreConferenceDayView = (MyScheduleSingleDayNoScrollView)
                getActivity().findViewById(R.id.my_schedule_zeroth_day);
        TextView zerothDayHeaderView = (TextView) getActivity()
                .findViewById(R.id.day_label_zeroth_day);
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
            default:
                break;
        }
    }

    @Override
    public void displayErrorMessage(MyScheduleQueryEnum query) {
        // Not showing any error
    }

    @Override
    public void displayUserActionResult(MyScheduleModel model
            , MyScheduleUserActionEnum userAction,
            boolean success) {
        switch (userAction) {
            case RELOAD_DATA:
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
                if (mPreConferenceDayView.getAdapter() == null) {
                    mPreConferenceDayView.setAdapter(new MyScheduleDayAdapter(getActivity(),
                            ((MyScheduleActivity) getActivity()).getLUtils(), mListener));
                }
                mPreConferenceDayView.getAdapter().updateItems(model.getConferenceDataForDay(
                        MyScheduleModel.PRE_CONFERENCE_DAY_ID));
            }
            for (int i = 0; i < mMyScheduleSingleDayViews.length; i++) {
                if (mMyScheduleSingleDayViews[i].getAdapter() == null) {
                    mMyScheduleSingleDayViews[i].setAdapter(new MyScheduleDayAdapter(getActivity(),
                            ((MyScheduleActivity) getActivity()).getLUtils(), mListener));

                }
                mMyScheduleSingleDayViews[i].getAdapter().updateItems(model.getConferenceDataForDay(
                                i + 1)
                ); // Day 1 of conference has id 1
            }
        }
    }

    @Override
    public Uri getDataUri(MyScheduleQueryEnum query) {
        return null;
    }

    @Override
    public void addListener(UserActionListener listener) {
        mListener = listener;
    }

    @Override
    public Context getContext() {
        return getActivity();
    }
}
