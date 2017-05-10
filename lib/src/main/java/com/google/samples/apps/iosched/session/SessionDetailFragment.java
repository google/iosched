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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.AppBarLayout.OnOffsetChangedListener;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.NestedScrollView.OnScrollChangeListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.map.MapActivity;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.schedule.ScheduleActivity;
import com.google.samples.apps.iosched.schedule.ScheduleDayAdapter;
import com.google.samples.apps.iosched.schedule.SessionItemViewHolder.Callbacks;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailUserActionEnum;
import com.google.samples.apps.iosched.ui.widget.CheckableFloatingActionButton;
import com.google.samples.apps.iosched.ui.widget.ReserveButton;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.RegistrationUtils;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.util.YouTubeUtils;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.*;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.samples.apps.iosched.Config.Tags.CATEGORY_TRACK;
import static com.google.samples.apps.iosched.model.ScheduleItem.SESSION_TYPE_SESSION;
import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

/**
 * Displays the details about a session. The user can add/remove a session from the schedule, watch
 * a live stream if available, watch the session on YouTube, view the map, share the session, and
 * submit feedback.
 */
public class SessionDetailFragment extends Fragment implements
        UpdatableView<SessionDetailModel, SessionDetailQueryEnum, SessionDetailUserActionEnum>,
        Callbacks {

    private static final String TAG = LogUtils.makeLogTag(SessionDetailFragment.class);
    private static final long HEADER_FADE_DURATION = 300L;

    private SessionDetailPresenter mPresenter;

    private CheckableFloatingActionButton mAddScheduleFab;

    private CoordinatorLayout mCoordinatorLayout;

    private AppBarLayout mAppBar;

    private CollapsingToolbarLayout mCollapsingToolbar;

    private ViewGroup mToolbar;

    private TextView mToolbarTitle;

    private ImageView mBackButton;

    private ImageView mShareButton;

    private ImageView mMapButton;

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

    private TextView mRelatedSessionsLabel;

    private RecyclerView mRelatedSessions;

    private ScheduleDayAdapter mRelatedSessionsAdapter;

    private View mEmptyView;

    private ImageLoader mImageLoader;

    private Runnable mTimeHintUpdaterRunnable = null;

    private List<Runnable> mDeferredUiOperations = new ArrayList<>();

    private Handler mHandler;

    private boolean mAnalyticsScreenViewHasFired;

    private UserActionListener<SessionDetailUserActionEnum> mListener;

    private boolean mShowFab = false;

    private boolean mHasEnterTransition = false;

    private GoogleApiClient mClient;

    private float mToolbarTitleAlpha;

    private float mHeaderImageAlpha;

    private boolean mHasHeaderImage;

    private ColorStateList mIconTintNormal;

    private ColorStateList mIconTintCollapsing;

    private long mHeaderAnimDuration;

    private ReserveButton mReserve;

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
        return inflater.inflate(R.layout.session_detail_frag, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mCoordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.root_container);
        mCoordinatorLayout.setStatusBarBackground(null);

        mAppBar = (AppBarLayout) view.findViewById(R.id.appbar);
        mReserve = (ReserveButton) view.findViewById(R.id.reserve);
        mCollapsingToolbar =
                (CollapsingToolbarLayout) mAppBar.findViewById(R.id.collapsing_toolbar);
        mCollapsingToolbar.setStatusBarScrim(null);
        mToolbar = (ViewGroup) mCollapsingToolbar.findViewById(R.id.session_detail_toolbar);
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
        mRelatedSessionsLabel = (TextView) details.findViewById(R.id.related_sessions_label);
        mRelatedSessions = (RecyclerView) details.findViewById(R.id.related_sessions_list);
        mRelatedSessionsAdapter = new ScheduleDayAdapter(getContext(), this, null, false);
        mRelatedSessions.setAdapter(mRelatedSessionsAdapter);

        mEmptyView = details.findViewById(android.R.id.empty);

        mAddScheduleFab =
                (CheckableFloatingActionButton) view.findViewById(R.id.add_schedule_button);
        mAddScheduleFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isInSchedule = !((CheckableFloatingActionButton) view).isChecked();
                showInSchedule(isInSchedule, true);
                if (isInSchedule) {
                    sendUserAction(SessionDetailUserActionEnum.STAR, null);
                } else {
                    sendUserAction(SessionDetailUserActionEnum.UNSTAR, null);
                }

                SessionsHelper.showBookmarkClickedHint(mAddScheduleFab, isInSchedule);
                mAddScheduleFab.announceForAccessibility(isInSchedule
                        ? getString(R.string.session_details_a11y_session_added)
                        : getString(R.string.session_details_a11y_session_removed));
            }
        });

        // Set up the fake toolbar
        Context toolbarContext = mToolbar.getContext();
        mIconTintCollapsing = AppCompatResources.getColorStateList(toolbarContext,
                R.color.session_detail_toolbar_icon_tint_collapsing);
        mIconTintNormal = AppCompatResources.getColorStateList(toolbarContext,
                R.color.session_detail_toolbar_icon_tint_normal);

        mBackButton = (ImageView) mToolbar.findViewById(R.id.back);
        mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onNavigateUp();
            }
        });
        mMapButton = (ImageView) mToolbar.findViewById(R.id.map);
        mMapButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserAction(SessionDetailUserActionEnum.SHOW_MAP, null);
            }
        });
        mShareButton = (ImageView) mToolbar.findViewById(R.id.share);
        mShareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserAction(SessionDetailUserActionEnum.SHOW_SHARE, null);
            }
        });
        mAppBar.addOnOffsetChangedListener(new OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    fadeOutHeaderImage();
                } else {
                    fadeInHeaderImage();
                }
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
        mPresenter =
                new SessionDetailPresenter(model, this, SessionDetailUserActionEnum.values(),
                        SessionDetailQueryEnum.values());
        mPresenter.loadInitialQueries();
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        final Transition sharedElementEnterTransition =
                getActivity().getWindow().getSharedElementEnterTransition();
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
                getActivity().getWindow().getSharedElementReturnTransition();
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
        mPresenter.initListeners();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
        mPresenter.cleanUpListeners();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.session_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_map_room) {
            sendUserAction(SessionDetailUserActionEnum.SHOW_MAP, null);
            return true;
        } else if (itemId == R.id.menu_share) {
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
                break;
            case RESERVATION_STATUS:
                updateReservationStatusAndSeatAvailability(data);
                break;
            case RESERVATION_RESULT:
                updateReservationResult(data);
                break;
            case RESERVATION_PENDING:
                updateReservationPending(data);
                break;
            case RESERVATION_FAILED:
                showRequestFailed();
                updateReservationStatusAndSeatAvailability(data);
                break;
            case RESERVATION_SEAT_AVAILABILITY:
                updateReservationStatusAndSeatAvailability(data);
                break;
            case AUTH_REGISTRATION:
                updateAuthRegistration(data);
                break;
            default:
                break;
        }
    }

    private void showInSchedule(boolean isInSchedule, boolean animate) {
        mAddScheduleFab.setChecked(isInSchedule);
        mAddScheduleFab.setContentDescription(getString(isInSchedule
                ? R.string.remove_from_schedule
                : R.string.add_to_schedule));
        if (!animate) return;

        AnimatedVectorDrawable avd = (AnimatedVectorDrawable) ContextCompat.getDrawable(
                getContext(), isInSchedule ? R.drawable.avd_bookmark : R.drawable.avd_unbookmark);
        mAddScheduleFab.setImageDrawable(avd);
        ObjectAnimator backgroundColor = ObjectAnimator.ofArgb(
                mAddScheduleFab,
                UIUtils.BACKGROUND_TINT,
                isInSchedule ? Color.WHITE
                        : ContextCompat.getColor(getContext(), R.color.lightish_blue));
        backgroundColor.setDuration(400L);
        backgroundColor.setInterpolator(AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.fast_out_slow_in));
        backgroundColor.start();
        avd.start();
    }

    @Override
    public void displayErrorMessage(SessionDetailQueryEnum query) {
        // Not showing any error
    }

    @Override
    public void displayUserActionResult(SessionDetailModel data,
            SessionDetailUserActionEnum userAction, boolean success) {
        switch (userAction) {
            case SHOW_MAP:
                Intent mapIntent = new Intent(getActivity(), MapActivity.class);
                mapIntent.putExtra(MapActivity.EXTRA_ROOM, data.getRoomId());
                getActivity().startActivity(mapIntent);
                break;
            case SHOW_SHARE:
                ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getString(R.string.share_template, data.getSessionTitle(),
                                BuildConfig.CONFERENCE_HASHTAG, data.getSessionUrl()));
                Intent shareIntent = Intent.createChooser(
                        builder.getIntent(),
                        getString(R.string.title_share));
                getActivity().startActivity(shareIntent);
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
        try {
            AppIndex.AppIndexApi.start(mClient, getActionForTitle(data.getSessionTitle()));
        } catch (Throwable e) {
            // Nothing to do if indexing fails.
        }

        mToolbarTitle.setText(data.getSessionTitle());
        mTitle.setText(data.getSessionTitle());
        mSubtitle.setText(data.getSessionSubtitle());
        if (!TextUtils.isEmpty(data.getSessionAbstract())) {
            UIUtils.setTextMaybeHtml(mAbstract, data.getSessionAbstract());
            mAbstract.setVisibility(VISIBLE);
        } else {
            mAbstract.setVisibility(GONE);
        }

        // Handle Keynote as a special case, where the user cannot remove it
        // from the schedule (it is auto added to schedule on sync)

        showInSchedule(!data.isKeynote() && data.isInSchedule(), false);

        displayTags(data);
        updateTimeBasedUi(data);
        updateEmptyView(data);

        if (data.shouldShowHeaderImage()) {
            setToolbarTint(mIconTintCollapsing);
            mImageLoader.loadImage(data.getPhotoUrl(), mPhotoView);
            mPhotoViewContainer.setVisibility(VISIBLE);
        } else {
            setToolbarTint(mIconTintNormal);
            mPhotoViewContainer.setVisibility(GONE);
        }

        if (data.getLiveStreamVideoWatched()) {
            mPhotoView.setColorFilter(getContext().getResources().getColor(
                    R.color.played_video_tint));
            mWatchVideo.setText(getString(R.string.session_replay));
        }

        if (data.hasLiveStream()) {
            mWatchVideo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String videoId = YouTubeUtils.getVideoIdFromSessionData(data.getYouTubeUrl(),
                            data.getLiveStreamId());
                    if (videoId != null) {
                        YouTubeUtils.showYouTubeVideo(videoId, getActivity());
                        AnalyticsHelper.sendEvent("Session", "Youtube Video", mTitle.toString());
                    }
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

        updateReservationStatusFromServer(data);
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
                trackColor = ContextCompat.getColor(getContext(), R.color.neon_blue);
            }

            final Drawable background = mAppBar.getBackground();
            if (background instanceof ColorDrawable
                    && ((ColorDrawable) background).getColor() == trackColor) {
                return;
            }

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

    private void setToolbarTint(ColorStateList tintList) {
        mBackButton.setImageTintList(tintList);
        mMapButton.setImageTintList(tintList);
        mShareButton.setImageTintList(tintList);
    }

    private void enterTransitionStarted() {
        mAddScheduleFab.setVisibility(INVISIBLE);
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
                    .alpha(1f).setDuration(200L)
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
            // Contains: The session title and session ID.
            AnalyticsHelper.sendScreenView("Session: " + sessionTitle, this.getActivity());
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
            TextView speakerAbstract = (TextView) speakerView.findViewById(R.id.speaker_abstract);

            speakerName.setText(speaker.getName());
            if (TextUtils.isEmpty(speaker.getCompany())) {
                speakerCompany.setVisibility(GONE);
            } else {
                speakerCompany.setText(speaker.getCompany());
            }
            if (!TextUtils.isEmpty(speaker.getImageUrl()) && mImageLoader != null) {
                mImageLoader.loadImage(speaker.getImageUrl(), speakerImage);
            }
            UIUtils.setTextMaybeHtml(speakerAbstract, speaker.getAbstract());

            speakersGroup.addView(speakerView);
        }

        speakersGroup.setVisibility(speakersGroup.getChildCount() > 0 ? VISIBLE : GONE);
        updateEmptyView(data);
    }

    private void displayRelatedSessions(SessionDetailModel data) {
        mRelatedSessionsAdapter.updateItems(data.getRelatedSessions());
        int visibility = mRelatedSessionsAdapter.getItemCount() > 0 ? VISIBLE : GONE;
        mRelatedSessions.setVisibility(visibility);
        mRelatedSessionsLabel.setVisibility(visibility);
    }

    private void updateEmptyView(SessionDetailModel data) {
        mEmptyView.setVisibility(data.hasSummaryContent() ? GONE : VISIBLE);
    }

    private void updateTimeBasedUi(SessionDetailModel data) {
        if (data.showLiveStream()) {
            // Show the play button and text only once the session is about to start.
            mWatchVideo.setVisibility(VISIBLE);

            if (data.hasSessionEnded()) {
                mWatchVideo.setText(getString(R.string.session_watch));
                // TODO: implement Replay.
            } else {
                mWatchVideo.setText(getString(R.string.session_watch_live));
            }
        } else {
            mWatchVideo.setVisibility(GONE);
        }

        // If the session is done, hide the FAB, and show the feedback button.
        mShowFab = !data.isKeynote();
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
            timeHintView.setVisibility(VISIBLE);
            timeHintView.setText(timeHint);
        } else {
            timeHintView.setVisibility(GONE);
        }
    }

    private void displayTags(SessionDetailModel data) {
        mRelatedSessionsAdapter.setTagMetadata(data.getTagMetadata());

        // TODO determine how to handle tags that aren't filterable (b/36001587)
        // For now just do the main tag
        mTags.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mTags.getContext());
        TagMetadata tagMetadata = data.getTagMetadata();
        if (tagMetadata != null) {
            List<Tag> tags = new ArrayList<>();
            final TagMetadata.Tag mainTag = tagMetadata.getTag(data.getMainTag());
            if (mainTag != null) {
                tags.add(mainTag);
            }
            if (data.getTags() != null) {
                for (String tagId : data.getTags()) {
                    if (!tagId.startsWith(CATEGORY_TRACK)) {
                        continue;
                    }
                    Tag tag = tagMetadata.getTagById(tagId);
                    if (tag == null || tag.equals(mainTag)) {
                        continue;
                    }
                    tags.add(tag);
                }
            }
            for (Tag tag : tags) {
                TextView tagView = (TextView) inflater.inflate(
                        R.layout.include_schedule_tag, mTags, false);
                tagView.setText(tag.getName());
                tagView.setBackgroundTintList(ColorStateList.valueOf(tag.getColor()));
                tagView.setTag(R.id.key_session_tag, tag);
                tagView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ScheduleActivity.launchScheduleWithFilterTag(getContext(), mainTag);
                    }
                });
                mTags.addView(tagView);
            }
        }

        if (data.isKeynote() || data.hasLiveStream()) {
            if (mTags.getChildCount() > 0) {
                // Insert the spacer first
                inflater.inflate(R.layout.spacer, mTags);
            }
            inflater.inflate(R.layout.include_schedule_live, mTags);
        }

        mTagsContainer.setVisibility(mTags.getChildCount() > 0 ? VISIBLE : GONE);
    }

    private void updateFeedbackButton(final SessionDetailModel data) {
        if (!data.hasFeedback() && data.hasSessionEnded()) {
            mFeedbackButton.setVisibility(VISIBLE);
            mFeedbackButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendUserAction(SessionDetailUserActionEnum.GIVE_FEEDBACK, null);
                    Intent intent = data.getFeedbackIntent();
                    startActivity(intent);
                }
            });
        } else {
            mFeedbackButton.setVisibility(GONE);
            mFeedbackButton.setOnClickListener(null);
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

    private void fadeInHeaderImage() {
        if (mHeaderImageAlpha < 1f) {
            mHeaderImageAlpha = 1f;
            mPhotoViewContainer.animate()
                    .setDuration(HEADER_FADE_DURATION)
                    .alpha(mHeaderImageAlpha)
                    .start();
        }
    }

    private void fadeOutHeaderImage() {
        if (mHeaderImageAlpha > 0f) {
            mHeaderImageAlpha = 0f;
            mPhotoViewContainer.animate()
                    .setDuration(HEADER_FADE_DURATION)
                    .alpha(mHeaderImageAlpha)
                    .start();
        }
    }

    // -- Adapter callbacks (for related sessions)

    @Override
    public void onSessionClicked(String sessionId) {
        startActivity(new Intent(Intent.ACTION_VIEW, Sessions.buildSessionUri(sessionId)));
    }

    @Override
    public boolean bookmarkingEnabled() {
        return true;
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
    public boolean feedbackEnabled() {
        return false;
    }

    @Override
    public void onFeedbackClicked(String sessionId, String sessionTitle) {
        SessionFeedbackActivity.launchFeedback(getContext(), sessionId);
    }

    @Override
    public void onTagClicked(Tag tag) {
        ScheduleActivity.launchScheduleWithFilterTag(getContext(), tag);
        AnalyticsHelper.sendEvent("Session: " + mTitle.getText(), "Tag", tag.getName().toString());
    }

    /**
     * Update UI to reflect reservation status known to ContentProvider.
     */
    public void updateReservationStatusFromServer(SessionDetailModel sessionDetailModel) {
        int reservationStatus = sessionDetailModel.getServerReservationStatus();
        switch (reservationStatus) {
            case ScheduleContract.MyReservations.RESERVATION_STATUS_RESERVED:
                showAlreadyReserved();
                break;
            case ScheduleContract.MyReservations.RESERVATION_STATUS_UNRESERVED:
                updateReservationStatusAndSeatAvailability(sessionDetailModel);
                break;
            case ScheduleContract.MyReservations.RESERVATION_STATUS_WAITLISTED:
                showWaitlisted();
                break;
        }
    }

    /**
     * Update UI to reflect reservation status and seat availability known to Firebase
     * (which is the ultimate source of truth).
     */
    public void updateReservationStatusAndSeatAvailability(SessionDetailModel sessionDetailModel) {
        if (isAdded()) {
            boolean isKeynote = sessionDetailModel.isKeynote();
            int sessionType = sessionDetailModel.getSessionType();
            if (isKeynote || sessionType != SESSION_TYPE_SESSION) {
                hideReservationButton();
            } else {
                showReservationButton();
                String reservationStatus = sessionDetailModel.getReservationStatus();
                LOGD(TAG, "reservationStatus == " + reservationStatus);
                if (reservationStatus == null) {
                    updateSeatsAvailability(sessionDetailModel);
                } else {
                    switch (reservationStatus) {
                        case SessionDetailConstants.RESERVE_STATUS_GRANTED:
                            showAlreadyReserved();
                            break;
                        case SessionDetailConstants.RESERVE_STATUS_RETURNED:
                            updateSeatsAvailability(sessionDetailModel);
                            break;
                        case SessionDetailConstants.RESERVE_STATUS_WAITING:
                            showWaitlisted();
                            break;
                    }
                }
            }
        }
    }

    /**
     * Update UI based on reservation request result. Usually this means presenting a dialog
     * and/or updating the reservation button UI.
     */
    public void updateReservationResult(SessionDetailModel sessionDetailModel) {
        String reservationResult = sessionDetailModel.getReservationResult();
        if (reservationResult != null && isAdded()) {
            switch (reservationResult) {
                case SessionDetailConstants.RESERVE_DENIED_CUTOFF:
                    showReservationDeniedCutoff();
                    break;
                case SessionDetailConstants.RESERVE_DENIED_CLASH:
                    showReservationDeniedClash();
                    break;
                case SessionDetailConstants.RESERVE_DENIED_FAILED:
                    showRequestFailed();
                    break;
                case SessionDetailConstants.RESERVE_DENIED_SPACE:
                    showReservationDeniedSpace();
                    break;
                case SessionDetailConstants.RESERVED:
                    showReservationSuccessful();
                    break;
                case SessionDetailConstants.RETURN_DENIED_CUTOFF:
                    showReturnDeniedCutoff();
                    break;
                case SessionDetailConstants.RETURN_DENIED_FAILED:
                    showRequestFailed();
                    break;
                case SessionDetailConstants.RETURNED:
                    break;
            }
            updateReservationStatusAndSeatAvailability(sessionDetailModel);
        }
    }

    /**
     * Update the reservation button based on whether the user is logged in and/or is
     * registered for the event.
     */
    public void updateAuthRegistration(SessionDetailModel sessionDetailModel) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null ||
                RegistrationUtils.isRegisteredAttendee(getContext()) != RegistrationUtils.REGSTATUS_REGISTERED) {
            hideReservationButton();
        } else {
            showReservationButton();
        }
    }

    /**
     * Update UI based on whether there is space left in the event.
     */
    public void updateSeatsAvailability(SessionDetailModel sessionDetailModel) {
        if (isAdded()) {
            if (sessionDetailModel.getSeatsAvailability()) {
                showReservationEnabled();
            } else {
                showWaitlistEnabled();
            }
            updateAuthRegistration(sessionDetailModel);
        }
    }

    /**
     * Update the button UI while a reservation request is pending.
     */
    public void updateReservationPending(SessionDetailModel sessionDetailModel) {
        if (isAdded()) {
            if (sessionDetailModel.isReservationPending() ||
                    sessionDetailModel.isReturnPending()) {
                showReservationInQueue();
            }
        }
    }

    /**
     * Update reservation button UI to convey that the session is reservable.
     */
    public void showReservationEnabled() {
        mReserve.setStatus(ReserveButton.ReservationStatus.RESERVABLE);
        mReserve.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserAction(SessionDetailUserActionEnum.RESERVE, null);
                AnalyticsHelper.sendEvent("Session: " + mTitle.getText(), "Reservation", "reservation attempted");
            }
        });
    }

    /**
     * Update reservation button UI to convey that the reserve request is being procesed.
     */
    public void showReservationInQueue() {
        mReserve.setStatus(ReserveButton.ReservationStatus.RESERVATION_PENDING);
        mReserve.setOnClickListener(null);
    }

    /**
     * Update reservation button UI to convey that the user has already reserved the session.
     */
    public void showAlreadyReserved() {
        mReserve.setStatus(ReserveButton.ReservationStatus.RESERVED);
        mReserve.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.my_schedule_reservation_cancel_confirm_message)
                        .setTitle(R.string.my_schedule_reservation_cancel_confirm_title)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendUserAction(SessionDetailUserActionEnum.RETURN, null);
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    public void showReservationSuccessful() {
        // No excessive UI update when reservation successful.
    }

    public void showReservationDeniedSpace() {
        // No excessive UI update when added to waitlist.
    }

    /**
     * Update reservation button UI to convey that the user is on the waitlist for the session.
     */
    public void showWaitlisted() {
        mReserve.setStatus(ReserveButton.ReservationStatus.WAITLISTED);
        mReserve.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.my_schedule_waitlist_cancel_confirm_message)
                        .setTitle(R.string.my_schedule_waitlist_cancel_confirm_title)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendUserAction(SessionDetailUserActionEnum.RETURN, null);
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    /**
     * Update reservation button UI to convey that the session is full and can only be waitlisted.
     */
    public void showWaitlistEnabled() {
        mReserve.setStatus(ReserveButton.ReservationStatus.WAITLIST_AVAILABLE);
        mReserve.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserAction(SessionDetailUserActionEnum.RESERVE, null);
            }
        });
    }

    /**
     * Update reservation button UI to convey that the time window to reserve the session is over.
     */
    public void showReservationDeniedCutoff() {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.my_schedule_reservation_window_closed_info)
                .setTitle(R.string.my_schedule_reservation_window_closed)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    /**
     * Alert the user that their reservation request failed due to a time conflict.
     */
    public void showReservationDeniedClash() {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.my_schedule_reservation_clash_message)
                .setTitle(R.string.my_schedule_reservation_clash_title)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    /**
     * Alert the user that their reservation cancellation request failed because the reservation
     * cutoff has been reached.
     */
    public void showReturnDeniedCutoff() {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.my_schedule_return_denied_cutoff_message)
                .setTitle(R.string.my_schedule_reservation_window_closed)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    /**
     * Alert the user that their request failed due to an unknown reason (blame the server...).
     */
    public void showRequestFailed() {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.my_schedule_reservation_request_failed_message)
                .setTitle(R.string.my_schedule_request_failed_title)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    private void hideReservationButton() {
        mReserve.setVisibility(GONE);
    }

    private void showReservationButton() {
        mReserve.setVisibility(VISIBLE);
    }
}
