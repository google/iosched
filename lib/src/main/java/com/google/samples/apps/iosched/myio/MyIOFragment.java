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
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;

import io.doist.recyclerviewext.sticky_headers.StickyHeadersLinearLayoutManager;

public class MyIOFragment extends Fragment implements MyIoView, Callbacks {

    private static final long UI_REFRESH_DELAY = TimeUtils.MINUTE;

    private MyIoPresenter mPresenter;
    private ViewSwitcher mLoadingSwitcher;
    private RecyclerView mRecyclerView;
    private LottieAnimationView mLoadingView;
    private MyIOAdapter mAdapter;
    private Handler mHandler;
    private boolean mScrolled = false;

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
     * Asks {@link MyIOAdapter} to remove the post on-boarding message card.
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
        mRecyclerView.setLayoutManager(
                new StickyHeadersLinearLayoutManager<MyIOAdapter>(getContext()));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mScrolled = true;
                recyclerView.removeOnScrollListener(this);
            }
        });
        View header = view.findViewById(R.id.header_anim);
        if (header instanceof ImageView) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) ContextCompat.getDrawable(
                    getContext(), R.drawable.avd_header_my_io);
            ((ImageView) header).setImageDrawable(avd);
            avd.start();
        }
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
        moveToCurrentTimeSlot();
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
        AnalyticsHelper.sendEvent("My I/O", "Feedback", sessionTitle);
        SessionFeedbackActivity.launchFeedback(getContext(), sessionId);
    }

    @Override
    public void onTagClicked(Tag tag) {
        AnalyticsHelper.sendEvent("My I/O", "Tag", tag.getName().toString());
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
        AnalyticsHelper.sendEvent("My I/O", "Add Events", String.valueOf(conferenceDay));
        ScheduleActivity.launchScheduleForConferenceDay(getContext(), conferenceDay);
        getActivity().finish();
    }

    private void showSchedule() {
        mLoadingView.cancelAnimation();
        mLoadingSwitcher.setDisplayedChild(1);
    }

    private void moveToCurrentTimeSlot() {
        // don't auto-scroll to current time outside of the conf or if user has manually scrolled
        if (mScrolled || !TimeUtils.isConferenceInProgress(getContext())) return;

        int nowPos = mAdapter.findPositionForTime(TimeUtils.getCurrentTime(getContext()));
        if (nowPos > 0) {
            LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            lm.scrollToPositionWithOffset(nowPos,
                    getResources().getDimensionPixelOffset(R.dimen.spacing_normal));
        }
    }
}
