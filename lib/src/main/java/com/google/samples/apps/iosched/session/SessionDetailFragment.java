/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.session;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.NestedScrollView.OnScrollChangeListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.map.MapActivity;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.myschedule.MyScheduleActivity;
import com.google.samples.apps.iosched.myschedule.MyScheduleDayAdapter;
import com.google.samples.apps.iosched.myschedule.MyScheduleDayAdapter.ScheduleAdapterListener;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailUserActionEnum;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.ui.widget.CheckableFloatingActionButton;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.util.YouTubeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the details about a session. The user can add/remove a session from the schedule, watch
 * a live stream if available, watch the session on YouTube, view the map, share the session, and
 * submit feedback.
 */
public class SessionDetailFragment extends Fragment implements
        UpdatableView<SessionDetailModel, SessionDetailQueryEnum, SessionDetailUserActionEnum>,
        ScheduleAdapterListener {

    private static final String TAG = LogUtils.makeLogTag(SessionDetailFragment.class);

    private CheckableFloatingActionButton mAddScheduleFab;

    private CoordinatorLayout mCoordinatorLayout;

    private AppBarLayout mAppBar;

    private CollapsingToolbarLayout mCollapsingToolbar;

    private Toolbar mToolbar;

    private TextView mToolbarTitle;

    private NestedScrollView mScrollView;

    private TextView mTitle;

    private TextView mSubtitle;

    private TextView mAbstract;

    private Button mWatchVideo;

    private LinearLayout mTags;

    private ViewGroup mTagsContainer;

    private Button mFeedbackButton;

    private View mPhotoViewContainer;

    private ImageView mPhotoView;

    private View mMapImage;

    private TextView mRelatedSessionsLabel;

    private RecyclerView mRelatedSessions;

    private MyScheduleDayAdapter mRelatedSessionsAdapter;

    private ImageLoader mImageLoader;

    private Runnable mTimeHintUpdaterRunnable = null;

    private List<Runnable> mDeferredUiOperations = new ArrayList<>();

    private Handler mHandler;

    private boolean mAnalyticsScreenViewHasFired;

    private UserActionListener<SessionDetailUserActionEnum> mListener;

    private boolean mShowFab = false;

    private boolean mHasEnterTransition = false;

    private GoogleApiClient mClient;

    private boolean mIsFloatingWindow;

    private float mToolbarTitleAlpha;

    @Override
    public void addListener(UserActionListener<SessionDetailUserActionEnum> listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAnalyticsScreenViewHasFired = false;
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(AppIndex.API)
                .enableAutoManage(getActivity(), null)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.session_detail_frag, container, false);
        if (mIsFloatingWindow) {
            // Window background is transparent, so set the background of the root view
            root.setBackgroundColor(ContextCompat.getColor(root.getContext(), R.color.background));
        }
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mCoordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.root_container);
        mCoordinatorLayout.setStatusBarBackground(null);

        mAppBar = (AppBarLayout) view.findViewById(R.id.appbar);
        mCollapsingToolbar =
                (CollapsingToolbarLayout) mAppBar.findViewById(R.id.collapsing_toolbar);
        mCollapsingToolbar.setStatusBarScrim(null);
        mToolbar = (Toolbar) mCollapsingToolbar.findViewById(R.id.toolbar);
        mToolbarTitle = (TextView) mToolbar.findViewById(R.id.toolbar_title);
        mToolbarTitleAlpha = mToolbarTitle.getAlpha();
        mPhotoViewContainer = mCollapsingToolbar.findViewById(R.id.session_photo_container);
        mPhotoView = (ImageView) mPhotoViewContainer.findViewById(R.id.session_photo);
        mWatchVideo = (Button) mCollapsingToolbar.findViewById(R.id.watch);

        mScrollView = (NestedScrollView) view.findViewById(R.id.scroll_view);
        mScrollView.setOnScrollChangeListener(new OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX,
                    int oldScrollY) {
                if (scrollY > mTitle.getBottom()) {
                    fadeInToolbarTitle();
                } else {
                    fadeOutToolbarTitle();
                }
            }
        });
        final ViewGroup details = (ViewGroup) view.findViewById(R.id.details_container);
        mTitle = (TextView) details.findViewById(R.id.session_title);
        mSubtitle = (TextView) details.findViewById(R.id.session_subtitle);
        mAbstract = (TextView) details.findViewById(R.id.session_abstract);
        mTags = (LinearLayout) details.findViewById(R.id.session_tags);
        mTagsContainer = (ViewGroup) details.findViewById(R.id.session_tags_container);
        mFeedbackButton = (Button) details.findViewById(R.id.give_feedback_button);

        final ViewGroup mapContainer = (ViewGroup) details.findViewById(R.id.map_container);
        mMapImage = mapContainer.findViewById(R.id.map_image);
        mapContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserAction(SessionDetailUserActionEnum.SHOW_MAP, null);
            }
        });

        mRelatedSessionsLabel = (TextView) details.findViewById(R.id.related_sessions_label);
        mRelatedSessions = (RecyclerView) details.findViewById(R.id.related_sessions_list);
        mRelatedSessionsAdapter = new MyScheduleDayAdapter(this, null, false);
        mRelatedSessions.setAdapter(mRelatedSessionsAdapter);

        mAddScheduleFab =
                (CheckableFloatingActionButton) view.findViewById(R.id.add_schedule_button);
        mAddScheduleFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isInSchedule = !((CheckableFloatingActionButton) view).isChecked();
                showInSchedule(isInSchedule);
                if (isInSchedule) {
                    sendUserAction(SessionDetailUserActionEnum.STAR, null);
                } else {
                    sendUserAction(SessionDetailUserActionEnum.UNSTAR, null);
                }

                mAddScheduleFab.announceForAccessibility(isInSchedule
                        ? getString(R.string.session_details_a11y_session_added)
                        : getString(R.string.session_details_a11y_session_removed));
            }
        });

        mImageLoader = new ImageLoader(getContext());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHandler = new Handler();

        // init presenter
        SessionDetailModel model = ModelProvider.provideSessionDetailModel(
                ((SessionDetailActivity) getActivity()).getSessionUri(), getContext(),
                new SessionsHelper(getActivity()), getLoaderManager());
        PresenterImpl<SessionDetailModel, SessionDetailQueryEnum, SessionDetailUserActionEnum>
                presenter = new PresenterImpl<>(model, this, SessionDetailUserActionEnum.values(),
                SessionDetailQueryEnum.values());
        presenter.loadInitialQueries();
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (activity instanceof BaseActivity) {
            mIsFloatingWindow = ((BaseActivity) activity).shouldBeFloatingWindow();
        }

        final Transition sharedElementEnterTransition =
                activity.getWindow().getSharedElementEnterTransition();
        if (sharedElementEnterTransition != null) {
            mHasEnterTransition = true;
            sharedElementEnterTransition.addListener(new UIUtils.TransitionListenerAdapter() {
                @Override
                public void onTransitionStart(final Transition transition) {
                    enterTransitionStarted();
                }

                @Override
                public void onTransitionEnd(final Transition transition) {
                    enterTransitionFinished();
                }
            });
        }
        final Transition sharedElementReturnTransition =
                activity.getWindow().getSharedElementReturnTransition();
        if (sharedElementReturnTransition != null) {
            sharedElementReturnTransition.addListener(new UIUtils.TransitionListenerAdapter() {
                @Override
                public void onTransitionStart(final Transition transition) {
                    returnTransitionStarted();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTimeHintUpdaterRunnable != null) {
            mHandler.postDelayed(mTimeHintUpdaterRunnable,
                    SessionDetailConstants.TIME_HINT_UPDATE_INTERVAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.session_detail, menu);
        tryExecuteDeferredUiOperations();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_share) {
            sendUserAction(SessionDetailUserActionEnum.SHOW_SHARE, null);
            return true;
        }
        return false;
    }

    private void sendUserAction(SessionDetailUserActionEnum action, Bundle args) {
        mListener.onUserAction(action, args);
    }

    @Override
    public void displayData(SessionDetailModel data, SessionDetailQueryEnum query) {
        switch (query) {
            case SESSIONS:
                displaySessionData(data);
                displayTrackColor(data);
                break;
            case FEEDBACK:
                updateFeedbackButton(data);
                break;
            case SPEAKERS:
                displaySpeakersData(data);
                break;
            case TAG_METADATA:
                displayTags(data);
                displayTrackColor(data);
                break;
            case RELATED:
                displayRelatedSessions(data);
            default:
                break;
        }
    }

    private void showInSchedule(boolean isInSchedule) {
        mAddScheduleFab.setChecked(isInSchedule);
        if (isInSchedule) {
            AnimatedVectorDrawableCompat addToSchedule = AnimatedVectorDrawableCompat
                    .create(getContext(), R.drawable.avd_add_to_schedule);
            mAddScheduleFab.setImageDrawable(addToSchedule);
            addToSchedule.start();
        } else {
            AnimatedVectorDrawableCompat removeFromSchedule = AnimatedVectorDrawableCompat
                    .create(getContext(), R.drawable.avd_remove_from_schedule);
            mAddScheduleFab.setImageDrawable(removeFromSchedule);
            removeFromSchedule.start();
        }

        mAddScheduleFab.setContentDescription(getString(isInSchedule
                ? R.string.remove_from_schedule
                : R.string.add_to_schedule));
    }

    @Override
    public void displayErrorMessage(SessionDetailQueryEnum query) {
        // Not showing any error
    }

    @Override
    public void displayUserActionResult(SessionDetailModel data,
            SessionDetailUserActionEnum userAction,
            boolean success) {
        switch (userAction) {
            case SHOW_MAP:
                Intent intentShowMap = new Intent(getActivity(), MapActivity.class);
                intentShowMap.putExtra(MapActivity.EXTRA_ROOM, data.getRoomId());
                intentShowMap.putExtra(MapActivity.EXTRA_DETACHED_MODE, true);
                getActivity().startActivity(intentShowMap);
                break;
            case SHOW_SHARE:
                ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getString(R.string.share_template, data.getSessionTitle(),
                                BuildConfig.CONFERENCE_HASHTAG, data.getSessionUrl()));
                Intent intentShare = Intent.createChooser(
                        builder.getIntent(),
                        getString(R.string.title_share));
                intentShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intentShare);

                break;
            default:
                // Other user actions are completely handled in model
                break;
        }
    }

    @Override
    public Uri getDataUri(SessionDetailQueryEnum query) {
        switch (query) {
            case SESSIONS:
                return ((SessionDetailActivity) getActivity()).getSessionUri();
            default:
                return null;
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    private void displaySessionData(final SessionDetailModel data) {
        mToolbarTitle.setText(data.getSessionTitle());

        mTitle.setText(data.getSessionTitle());
        mSubtitle.setText(data.getSessionSubtitle());
        try {
            AppIndex.AppIndexApi.start(mClient, getActionForTitle(data.getSessionTitle()));
        } catch (Throwable e) {
            // Nothing to do if indexing fails.
        }

        if (data.shouldShowHeaderImage()) {
            mImageLoader.loadImage(data.getPhotoUrl(), mPhotoView);
        }

        tryExecuteDeferredUiOperations();

        // Handle Keynote as a special case, where the user cannot remove it
        // from the schedule (it is auto added to schedule on sync)
        mShowFab =  (AccountUtils.hasActiveAccount(getContext()) && !data.isKeynote());
        mAddScheduleFab.setVisibility(mShowFab ? View.VISIBLE : View.INVISIBLE);

        displayTags(data);

        if (!data.isKeynote()) {
            showInScheduleDeferred(data.isInSchedule());
        }

        if (!TextUtils.isEmpty(data.getSessionAbstract())) {
            UIUtils.setTextMaybeHtml(mAbstract, data.getSessionAbstract());
            mAbstract.setVisibility(View.VISIBLE);
        } else {
            mAbstract.setVisibility(View.GONE);
        }

        updateEmptyView(data);

        updateTimeBasedUi(data);

        if (data.getLiveStreamVideoWatched()) {
            mPhotoView.setColorFilter(getContext().getResources().getColor(
                    R.color.played_video_tint));
            mWatchVideo.setText(getString(R.string.session_replay));
        }

        if (data.hasLiveStream()) {
            mWatchVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String videoId = YouTubeUtils.getVideoIdFromSessionData(data.getYouTubeUrl(),
                            data.getLiveStreamId());
                    YouTubeUtils.showYouTubeVideo(videoId, getActivity());
                }
            });
        }

        fireAnalyticsScreenView(data.getSessionTitle());

        mTimeHintUpdaterRunnable = new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) {
                    // Do not post a delayed message if the activity is detached.
                    return;
                }
                updateTimeBasedUi(data);
                mHandler.postDelayed(mTimeHintUpdaterRunnable,
                        SessionDetailConstants.TIME_HINT_UPDATE_INTERVAL);
            }
        };
        mHandler.postDelayed(mTimeHintUpdaterRunnable,
                SessionDetailConstants.TIME_HINT_UPDATE_INTERVAL);

        if (!mHasEnterTransition) {
            // No enter transition so update UI manually
            enterTransitionFinished();
        }
    }

    /**
     * Update the header box background color & status bar color depending upon which track this
     * session belongs to.
     * <p>
     * Note this requires both the {@link SessionDetailQueryEnum#SESSIONS} &
     * {@link SessionDetailQueryEnum#TAG_METADATA) queries to have returned.
     */
    private void displayTrackColor(SessionDetailModel data) {
        if (data.isSessionTrackColorAvailable()) {
            int trackColor = data.getSessionTrackColor();
            if (trackColor == Color.TRANSPARENT) {
                trackColor = UIUtils.getThemeColor(getContext(), R.attr.colorPrimary,
                        R.color.theme_primary);
            }

            final Drawable background = mAppBar.getBackground();
            if (background instanceof ColorDrawable
                    && ((ColorDrawable) background).getColor() == trackColor) {
                return;
            }

            mCollapsingToolbar.setContentScrimColor(trackColor);

            // Animate the color change to make the transition smoother
            final ObjectAnimator color =
                    ObjectAnimator.ofInt(mAppBar, UIUtils.BACKGROUND_COLOR, trackColor);
            color.setEvaluator(new ArgbEvaluator());
            if (mHasEnterTransition) {
                color.setStartDelay(200L);
            }
            color.setDuration(300L);
            color.start();
        }
    }

    private void enterTransitionStarted() {
        mAddScheduleFab.setVisibility(View.INVISIBLE);
        mToolbar.setAlpha(0f);
    }

    /**
     * Finish any UI setup that should be deferred until the enter transition has completed.
     */
    private void enterTransitionFinished() {
        if (mShowFab) {
            mAddScheduleFab.show();
        }
        if (mToolbar.getAlpha() != 1f) {
            mToolbar.animate()
                    .alpha(1f)
                    .setDuration(200L)
                    .setInterpolator(new LinearOutSlowInInterpolator())
                    .start();
        }
    }

    private void returnTransitionStarted() {
        // Fade the header bar for a smoother transition.
        final ObjectAnimator color = ObjectAnimator.ofInt(mAppBar, UIUtils.BACKGROUND_COLOR,
                ContextCompat.getColor(getContext(), R.color.background));
        color.setEvaluator(new ArgbEvaluator());
        color.setDuration(200L);
        color.start();
        // Also fade out the toolbar and FAB
        mToolbar.animate()
                  .alpha(0f)
                  .setDuration(200L)
                  .start();
        mAddScheduleFab.hide();
    }

    /**
     * Sends a screen view to Google Analytics, if a screenview hasn't already been sent since the
     * fragment was loaded.  This prevents background syncs from causing superflous screen views.
     *
     * @param sessionTitle The name of the session being tracked.
     */
    private void fireAnalyticsScreenView(String sessionTitle) {
        if (!mAnalyticsScreenViewHasFired) {
            // ANALYTICS SCREEN: View the Session Details page for a specific session.
            // Contains: The session title.
            AnalyticsHelper.sendScreenView("Session: " + sessionTitle);
            mAnalyticsScreenViewHasFired = true;
        }
    }

    private void displaySpeakersData(SessionDetailModel data) {
        final ViewGroup speakersGroup = (ViewGroup) getActivity()
                .findViewById(R.id.session_speakers_block);
        speakersGroup.removeAllViews();

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        List<SessionDetailModel.Speaker> speakers = data.getSpeakers();
        for (SessionDetailModel.Speaker speaker : speakers) {
            View speakerView = inflater.inflate(R.layout.speaker_detail, speakersGroup, false);
            ImageView speakerImage = (ImageView) speakerView.findViewById(R.id.speaker_image);
            TextView speakerName = (TextView) speakerView.findViewById(R.id.speaker_name);
            TextView speakerCompany = (TextView) speakerView.findViewById(R.id.speaker_company);

            speakerName.setText(speaker.getName());
            if (TextUtils.isEmpty(speaker.getCompany())) {
                speakerCompany.setVisibility(View.GONE);
            } else {
                speakerCompany.setText(speaker.getCompany());
            }
            if (!TextUtils.isEmpty(speaker.getImageUrl()) && mImageLoader != null) {
                mImageLoader.loadImage(speaker.getImageUrl(), speakerImage);
            }

            speakersGroup.addView(speakerView);
        }

        speakersGroup.setVisibility(speakersGroup.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        updateEmptyView(data);
    }

    private void displayRelatedSessions(SessionDetailModel data) {
        mRelatedSessionsAdapter.updateItems(data.getRelatedSessions());
        int visibility = mRelatedSessionsAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE;
        mRelatedSessions.setVisibility(visibility);
        mRelatedSessionsLabel.setVisibility(visibility);
    }

    private void updateEmptyView(SessionDetailModel data) {
        getActivity().findViewById(android.R.id.empty).setVisibility(
                (data.getSessionTitle() != null && data.getSpeakers().size() == 0
                        && !data.hasSummaryContent())
                        ? View.VISIBLE
                        : View.GONE);
    }

    private void updateTimeBasedUi(SessionDetailModel data) {
        if (data.showLiveStream()) {
            // Show the play button and text only once the session is about to start.
            mWatchVideo.setVisibility(View.VISIBLE);

            if (data.hasSessionEnded()) {
                mWatchVideo.setText(getString(R.string.session_watch));
                // TODO: implement Replay.
            } else {
                mWatchVideo.setText(getString(R.string.session_watch_live));
            }
        } else {
            mWatchVideo.setVisibility(View.GONE);
        }

        // If the session is done, hide the FAB, and show the feedback button.
        mShowFab = !data.isSessionReadyForFeedback();
        if (mShowFab) {
            mAddScheduleFab.show();
        } else {
            mAddScheduleFab.hide();
        }
        updateFeedbackButton(data);

        String timeHint = "";

        if (TimeUtils.hasConferenceEnded(getContext())) {
            // No time hint to display.
            timeHint = "";
        } else if (data.hasSessionEnded()) {
            timeHint = getString(R.string.time_hint_session_ended);
        } else if (data.isSessionOngoing()) {
            long minutesAgo = data.minutesSinceSessionStarted();
            if (minutesAgo > 1) {
                timeHint = getString(R.string.time_hint_started_min, minutesAgo);
            } else {
                timeHint = getString(R.string.time_hint_started_just);
            }
        } else {
            long minutesUntilStart = data.minutesUntilSessionStarts();
            if (minutesUntilStart > 0
                    && minutesUntilStart <= SessionDetailConstants.HINT_TIME_BEFORE_SESSION_MIN) {
                if (minutesUntilStart > 1) {
                    timeHint = getString(R.string.time_hint_about_to_start_min, minutesUntilStart);
                } else {
                    timeHint = getString(R.string.time_hint_about_to_start_shortly);
                }
            }
        }

        final TextView timeHintView = (TextView) getActivity().findViewById(R.id.time_hint);

        if (!TextUtils.isEmpty(timeHint)) {
            timeHintView.setVisibility(View.VISIBLE);
            timeHintView.setText(timeHint);
        } else {
            timeHintView.setVisibility(View.GONE);
        }
    }

    private void displayTags(SessionDetailModel data) {
        mRelatedSessionsAdapter.setTagMetadata(data.getTagMetadata());

        // TODO determine how to handle tags that aren't filterable (b/36001587)
        // For now just do the main tag
        mTags.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mTags.getContext());
        if (data.getTagMetadata() != null) {
            final Tag mainTag = data.getTagMetadata().getTag(data.getMainTag());
            if (mainTag != null) {
                TextView tagView = (TextView) inflater.inflate(R.layout.include_schedule_tag, mTags,
                        false);
                tagView.setText(mainTag.getName());
                tagView.setBackgroundTintList(ColorStateList.valueOf(mainTag.getColor()));
                tagView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MyScheduleActivity.launchScheduleWithFilterTag(getContext(), mainTag);
                    }
                });
                mTags.addView(tagView);
            }
        }

        if (data.isKeynote() || data.hasLiveStream()) {
            if (mTags.getChildCount() > 0) {
                // Insert the spacer first
                inflater.inflate(R.layout.include_schedule_live_spacer, mTags);
            }
            inflater.inflate(R.layout.include_schedule_live, mTags);
        }

        mTagsContainer.setVisibility(mTags.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateFeedbackButton(final SessionDetailModel data) {
        mFeedbackButton.setVisibility(data.hasFeedback() ? View.GONE : View.VISIBLE);
        if (!data.hasFeedback() && data.isInScheduleWhenSessionFirstLoaded()) {
            mFeedbackButton.setVisibility(View.VISIBLE);
            mFeedbackButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendUserAction(SessionDetailUserActionEnum.GIVE_FEEDBACK, null);
                    Intent intent = data.getFeedbackIntent();
                    startActivity(intent);
                }
            });
            LOGD(TAG, "User has not given feedback for session.");
        } else {
            mFeedbackButton.setVisibility(View.GONE);
            mFeedbackButton.setOnClickListener(null);
            LOGD(TAG, "User already gave feedback for session.");
        }
    }

    private void showInScheduleDeferred(final boolean isInSchedule) {
        mDeferredUiOperations.add(new Runnable() {
            @Override
            public void run() {
                if (mAddScheduleFab.isChecked() != isInSchedule) {
                    mAddScheduleFab.setChecked(isInSchedule);
                    mAddScheduleFab.setImageResource(isInSchedule ?
                            R.drawable.ic_session_in_schedule : R.drawable.ic_add_to_schedule);
                    mAddScheduleFab.setContentDescription(getString(isInSchedule ?
                            R.string.remove_from_schedule_desc : R.string.add_to_schedule_desc));
                }
            }
        });
        tryExecuteDeferredUiOperations();
    }

    private void tryExecuteDeferredUiOperations() {
        for (Runnable r : mDeferredUiOperations) {
            r.run();
            mDeferredUiOperations.clear();
        }
    }

    private Action getActionForTitle(String title) {
        Uri sessionUri = ((SessionDetailActivity) getActivity()).getSessionUri();
        String uuid = sessionUri.toString().substring(sessionUri.toString().lastIndexOf("/") + 1);
        Uri uri = new Uri.Builder()
                .scheme(Config.HTTPS)
                .authority(BuildConfig.PRODUCTION_WEBSITE_HOST_NAME)
                .path(BuildConfig.WEB_URL_SCHEDULE_PATH)
                .appendQueryParameter(Config.SESSION_ID_URL_QUERY_KEY, uuid)
                .build();
        // Build a schema.org Thing that represents the session details currently displayed. Its
        // name is the session's title, and its URL is a deep link back to this
        // SessionDetailFragment.
        Thing session = new Thing.Builder()
                .setName(title)
                .setUrl(uri)
                .build();
        // Build a schema.org Action that represents a user viewing this session screen. This Action
        // is then ready to be passed to the App Indexing API. Read more about the API here:
        // https://developers.google.com/app-indexing/introduction#android.
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(session)
                .build();
    }

    private void fadeInToolbarTitle() {
        if (mToolbarTitleAlpha < 1f) {
            mToolbarTitleAlpha = 1f;
            mToolbarTitle.animate().alpha(mToolbarTitleAlpha).start();
        }
    }

    private void fadeOutToolbarTitle() {
        if (mToolbarTitleAlpha > 0f) {
            mToolbarTitleAlpha = 0f;
            mToolbarTitle.animate().alpha(mToolbarTitleAlpha).start();
        }
    }

    @Override
    public void onSessionClicked(Uri sessionUri) {
        startActivity(new Intent(Intent.ACTION_VIEW, sessionUri));
    }

    @Override
    public void onBookmarkClicked(String sessionId, boolean isInSchedule) {
        Bundle args = new Bundle();
        args.putString(Sessions.SESSION_ID, sessionId);
        SessionDetailUserActionEnum action = isInSchedule
                ? SessionDetailUserActionEnum.UNSTAR_RELATED
                : SessionDetailUserActionEnum.STAR_RELATED;
        sendUserAction(action, args);
    }

    @Override
    public void onFeedbackClicked(String sessionId, String sessionTitle) {
        SessionFeedbackActivity.launchFeedback(getContext(), sessionId);
    }

    @Override
    public void onTagClicked(Tag tag) {
        MyScheduleActivity.launchScheduleWithFilterTag(getContext(), tag);
    }
}
