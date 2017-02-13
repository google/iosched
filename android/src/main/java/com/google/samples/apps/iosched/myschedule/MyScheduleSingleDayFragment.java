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
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleQueryEnum;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleUserActionEnum;


/**
 * This is used by the {@link android.support.v4.view.ViewPager} used by the narrow layout in {@link
 * MyScheduleActivity}. It is a {@link ListFragment} that shows schedule items for a day, using
 * {@link MyScheduleDayAdapter} as its data source.
 */
public class MyScheduleSingleDayFragment extends ListFragment
        implements UpdatableView<MyScheduleModel, MyScheduleQueryEnum, MyScheduleUserActionEnum>,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int TAG_METADATA_TOKEN = 0x8;

    private String mContentDescription = null;
    private TagMetadata mTagMetadata;

    private View mRoot = null;

    /**
     * This is 1 for the first day of the conference, 2 for the second, and so on, and {@link
     * MyScheduleModel#PRE_CONFERENCE_DAY_ID} for the preconference day
     */
    private int mDayId = 1;

    private MyScheduleDayAdapter mViewAdapter;

    private UserActionListener<MyScheduleUserActionEnum> mListener;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(TAG_METADATA_TOKEN, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.my_schedule_singleday_frag, container, false);
        if (mContentDescription != null) {
            mRoot.setContentDescription(mContentDescription);
        }
        setRetainInstance(true);
        return mRoot;
    }

    public void setContentDescription(String desc) {
        mContentDescription = desc;
        if (mRoot != null) {
            mRoot.setContentDescription(mContentDescription);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.onUserAction(MyScheduleUserActionEnum.RELOAD_DATA, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initViews();
    }

    private void initViews() {
        mDayId = getArguments().getInt(MyScheduleActivity.ARG_CONFERENCE_DAY_INDEX, 0);

        // Set id to list view, so it can be referred to from tests
        TypedArray ids = getResources().obtainTypedArray(R.array.myschedule_listview_ids);
        int listViewId = ids.getResourceId(mDayId, 0);
        ids.recycle();
        getListView().setId(listViewId);

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
    public void displayUserActionResult(MyScheduleModel model, MyScheduleUserActionEnum userAction,
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
            if (mViewAdapter == null) {
                mViewAdapter = new MyScheduleDayAdapter(getActivity(), mListener, mTagMetadata);
            }
            mViewAdapter.updateItems(model.getConferenceDataForDay(mDayId));
            if (getListAdapter() == null) {
                setListAdapter(mViewAdapter);
                getListView().setRecyclerListener(mViewAdapter);
            }
        } else {
            /**
             * Ignore the updated model. The data will be request when the Fragment becomes visible
             * again (in {@link #onResume()}.
             */
        }
    }

    @Override
    public Uri getDataUri(MyScheduleQueryEnum query) {
        // Not used by the model
        return null;
    }

    @Override
    public void addListener(UserActionListener<MyScheduleUserActionEnum> listener) {
        mListener = listener;
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    interface Listener {
        void onSingleDayFragmentAttached(MyScheduleSingleDayFragment fragment);

        void onSingleDayFragmentDetached(MyScheduleSingleDayFragment fragment);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case TAG_METADATA_TOKEN:
                return TagMetadata.createCursorLoader(getActivity());
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case TAG_METADATA_TOKEN:
                mTagMetadata = new TagMetadata(cursor);
                if (mViewAdapter != null) {
                    mViewAdapter.setTagMetadata(mTagMetadata);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
    }
}
