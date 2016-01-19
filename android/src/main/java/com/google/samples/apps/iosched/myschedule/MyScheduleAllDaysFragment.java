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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.util.TimeUtils;

/**
 * This is used by the wide layout in {@link MyScheduleActivity}. It uses a
 * {@link MyScheduleSingleDayNoScrollView} for each day of the conference.
 */
public class MyScheduleAllDaysFragment extends Fragment {

    private MyScheduleSingleDayNoScrollView[] mMyScheduleSingleDayNoScrollViewWide
            = new MyScheduleSingleDayNoScrollView[2];

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.my_schedule_alldays_frag, container, false);
        mMyScheduleSingleDayNoScrollViewWide[0] = (MyScheduleSingleDayNoScrollView) root
                .findViewById(R.id.my_schedule_first_day);
        mMyScheduleSingleDayNoScrollViewWide[1] = (MyScheduleSingleDayNoScrollView) root
                .findViewById(R.id.my_schedule_second_day);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MyScheduleActivity activity = (MyScheduleActivity) getActivity();
        setData(activity.getConferenceData(), activity.getPreConferenceData());
    }

    private void setData(MyScheduleDayAdapter[] conferenceData, MyScheduleDayAdapter preConferenceData) {
        mMyScheduleSingleDayNoScrollViewWide[0].setAdapter(conferenceData[0]);
        mMyScheduleSingleDayNoScrollViewWide[1].setAdapter(conferenceData[1]);

        TextView firstDayHeaderView = (TextView) getActivity()
                .findViewById(R.id.day_label_first_day);
        TextView secondDayHeaderView = (TextView) getActivity()
                .findViewById(R.id.day_label_second_day);
        if (firstDayHeaderView != null) {
            firstDayHeaderView.setText(TimeUtils.getDayName(getContext(), 0));
        }
        if (secondDayHeaderView != null) {
            secondDayHeaderView.setText(TimeUtils.getDayName(getContext(), 1));
        }

        TextView zerothDayHeaderView = (TextView) getActivity()
                .findViewById(R.id.day_label_zeroth_day);
        MyScheduleSingleDayNoScrollView dayZeroView = (MyScheduleSingleDayNoScrollView)
                getActivity().findViewById(R.id.my_schedule_zeroth_day);
        if (preConferenceData != null) {
            dayZeroView.setAdapter(preConferenceData);
            dayZeroView.setVisibility(View.VISIBLE);
            zerothDayHeaderView.setText(TimeUtils.getDayName(getContext(), -1));
            zerothDayHeaderView.setVisibility(View.VISIBLE);
        } else {
            dayZeroView.setVisibility(View.GONE);
            zerothDayHeaderView.setVisibility(View.GONE);
        }
    }
}
