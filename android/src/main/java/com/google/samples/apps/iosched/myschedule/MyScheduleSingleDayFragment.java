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

package com.google.samples.apps.iosched.myschedule;

import android.app.Activity;
import android.app.ListFragment;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.R;

/**
 * This is used by the {@link android.support.v4.view.ViewPager} used by the narrow layout in
 * {@link MyScheduleActivity}. It is a {@link ListFragment} that shows schedule items for a day,
 * using {@link MyScheduleDayAdapter} as its data source.
 */
public class MyScheduleSingleDayFragment extends ListFragment {

    private String mContentDescription = null;

    private View mRoot = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.my_schedule_singleday_frag, container, false);
        if (mContentDescription != null) {
            mRoot.setContentDescription(mContentDescription);
        }
        return mRoot;
    }

    public void setContentDescription(String desc) {
        mContentDescription = desc;
        if (mRoot != null) {
            mRoot.setContentDescription(mContentDescription);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MyScheduleActivity activity = (MyScheduleActivity) getActivity();
        setData(activity.getConferenceData(), activity.getPreConferenceData());
    }

    private void setData(MyScheduleDayAdapter[] conferenceData, MyScheduleDayAdapter preConferenceData) {
        getListView().addHeaderView(getActivity().getLayoutInflater()
                .inflate(R.layout.reserve_action_bar_space_header_view, null));
        int dayIndex = getArguments().getInt(MyScheduleActivity.ARG_CONFERENCE_DAY_INDEX, 0);

        // Set id to list view, so it can be referred to from tests
        TypedArray ids = getResources().obtainTypedArray(R.array.myschedule_listview_ids);
        int listViewId = ids.getResourceId(dayIndex+1,0);
        getListView().setId(listViewId);

        if (dayIndex < 0) {
            setListAdapter(preConferenceData);
            getListView().setRecyclerListener(preConferenceData);
        } else {
            setListAdapter(conferenceData[dayIndex]);
            getListView().setRecyclerListener(conferenceData[dayIndex]);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Listener) getActivity()).onSingleDayFragmentAttached(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((Listener) getActivity()).onSingleDayFragmentDetached(this);
    }

    interface Listener {
        void onSingleDayFragmentAttached(MyScheduleSingleDayFragment fragment);

        void onSingleDayFragmentDetached(MyScheduleSingleDayFragment fragment);
    }
}
