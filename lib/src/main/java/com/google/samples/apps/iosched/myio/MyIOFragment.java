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
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import com.airbnb.lottie.LottieAnimationView;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.myio.MyIOAdapter.Callbacks;
import com.google.samples.apps.iosched.myio.MyIOContract.MyIoPresenter;
import com.google.samples.apps.iosched.myio.MyIOContract.MyIoView;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.schedule.DividerDecoration;
import com.google.samples.apps.iosched.schedule.ScheduleActivity;
import com.google.samples.apps.iosched.schedule.ScheduleModel;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.TimeUtils;

public class MyIOFragment extends Fragment implements MyIoView, Callbacks {

    private static final String TAG = LogUtils.makeLogTag(MyIOFragment.class);

    private static final long UI_REFRESH_DELAY = TimeUtils.MINUTE;

    private MyIoPresenter mPresenter;
    private ViewSwitcher mLoadingSwitcher;
    private RecyclerView mRecyclerView;
    private LottieAnimationView mLoadingView;
    private MyIOAdapter mAdapter;
    private Handler mHandler;

    private Runnable mUiRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPresenter != null) {
                mPresenter.refreshUI(getLoaderManager());
            }
            maybePostUiRefreshRunnable();
        }
    };

    /**
     * Asks {@link MyIOAdapter} to remove the post onboarding message card.
     */
    public void removePostOnboardingMessageCard() {
        mAdapter.removePostOnboardingMessageCard();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.myio_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLoadingSwitcher = (ViewSwitcher) view.findViewById(R.id.loading_switcher);
        mLoadingView = (LottieAnimationView) view.findViewById(R.id.loading_anim);
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.addItemDecoration(new DividerDecoration(getContext()));
        mAdapter = new MyIOAdapter(getContext(), this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPresenter = new MyIOPresenterImpl(getContext(), this);
        mPresenter.initModel(getLoaderManager());
        mHandler = new Handler();
    }

    @Override
    public void onStart() {
        super.onStart();
        maybePostUiRefreshRunnable();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mUiRefreshRunnable);
    }

    private void maybePostUiRefreshRunnable() {
        if (TimeUtils.isConferenceInProgress(getContext())) {
            mHandler.removeCallbacks(mUiRefreshRunnable);
            mHandler.postDelayed(mUiRefreshRunnable, UI_REFRESH_DELAY);
        }
    }

    // -- MyIoView callbacks

    @Override
    public void onScheduleLoaded(MyIOModel model) {
        showSchedule();
        mAdapter.setItems(model.getScheduleItems());
    }

    @Override
    public void onTagMetadataLoaded(MyIOModel model) {
        mAdapter.setTagMetadata(model.getTagMetadata());
    }

    // -- Adapter callbacks

    @Override
    public void onSessionClicked(String sessionId) {
        Bundle args = new Bundle();
        Uri sessionUri = Sessions.buildSessionUri(sessionId);
        args.putString(ScheduleModel.SESSION_URL_KEY, sessionUri.toString());
        startActivity(new Intent(Intent.ACTION_VIEW, sessionUri));
    }

    @Override
    public void onFeedbackClicked(String sessionId, String sessionTitle) {
        SessionFeedbackActivity.launchFeedback(getContext(), sessionId);
    }

    @Override
    public void onTagClicked(Tag tag) {
        ScheduleActivity.launchScheduleWithFilterTag(getContext(), tag);
    }

    @Override
    public boolean bookmarkingEnabled() {
        return false; // not supported
    }

    @Override
    public void onBookmarkClicked(String sessionId, boolean isInSchedule) {
        // not supported
    }

    @Override
    public boolean feedbackEnabled() {
        return true;
    }

    @Override
    public void onAddEventsClicked(int conferenceDay) {
        ScheduleActivity.launchScheduleForConferenceDay(getContext(), conferenceDay);
    }

    private void showSchedule() {
        mLoadingView.cancelAnimation();
        mLoadingSwitcher.setDisplayedChild(1);
    }
}
