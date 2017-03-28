/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.myio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.myio.MyIOContract.MyIoPresenter;
import com.google.samples.apps.iosched.myio.MyIOContract.MyIoView;
import com.google.samples.apps.iosched.myschedule.MyScheduleActivity;
import com.google.samples.apps.iosched.myschedule.MyScheduleDayAdapter;
import com.google.samples.apps.iosched.myschedule.MyScheduleDayAdapter.ScheduleAdapterListener;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel;

public class MyIOFragment extends Fragment implements MyIoView, ScheduleAdapterListener {

    private MyIoPresenter mPresenter;
    private RecyclerView mRecyclerView;
    private MyScheduleDayAdapter mAdapter; // TODO make new adapter

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.myio_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mAdapter = new MyScheduleDayAdapter(this, null, false);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPresenter = new MyIOPresenterImpl(getContext(), this);
        mPresenter.initModel(getLoaderManager());
    }

    @Override
    public void onScheduleLoaded(MyIOModel model) {
        mAdapter.updateItems(model.getScheduleItems());
    }

    // -- ScheduleAdapterListener callbacks

    @Override
    public void onSessionClicked(Uri sessionUri) {
        Bundle args = new Bundle();
        args.putString(MyScheduleModel.SESSION_URL_KEY, sessionUri.toString());
        startActivity(new Intent(Intent.ACTION_VIEW, sessionUri));
    }

    @Override
    public void onFeedbackClicked(String sessionId, String sessionTitle) {
        SessionFeedbackActivity.launchFeedback(getContext(), sessionId);
    }

    @Override
    public void onTagClicked(Tag tag) {
        MyScheduleActivity.launchScheduleWithFilterTag(getContext(), tag);
    }

    @Override
    public void onBookmarkClicked(String sessionId, boolean isInSchedule) {
        // not supported
    }
}
