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
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.archframework.UserActionEnum;
import com.google.samples.apps.iosched.explore.ExploreSessionsActivity;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.map.MapActivity;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailUserActionEnum;
import com.google.samples.apps.iosched.ui.widget.CheckableFloatingActionButton;
import com.google.samples.apps.iosched.ui.widget.MessageCardView;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.util.YouTubeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

/**
 * Displays the details about a session. The user can add/remove a session from the schedule, watch
 * a live stream if available, watch the session on YouTube, view the map, share the session, and
 * submit feedback.
 */
public class SessionDetailFragment extends Fragment implements
        UpdatableView<SessionDetailModel, SessionDetailQueryEnum, SessionDetailUserActionEnum> {

    private static final String TAG = LogUtils.makeLogTag(SessionDetailFragment.class);

    /**
     * Stores the session IDs for which the user has dismissed the "give feedback" card. This
     * information is kept for the duration of the app's execution so that if they say "No, thanks",
     * we don't show the card again for that session while the app is still executing.
     */
    private static HashSet<String> sDismissedFeedbackCard = new HashSet<>();

    private CheckableFloatingActionButton mAddScheduleFab;

    private AppBarLayout mAppBar;

    private CollapsingToolbarLayout mCollapsingToolbar;

    private Toolbar mToolbar;

    private TextView mTitle;

    private TextView mSubtitle;

    private TextView mAbstract;

    private TextView mLiveStreamedIndicator;

    private Button mWatchVideo;

    private LinearLayout mTags;

    private TextView mExtended;

    private ViewGroup mTagsContainer;

    private TextView mRequirements;

    private View mHeaderBox;

    private View mPhotoViewContainer;

    private ImageView mPhotoView;

    private ImageLoader mImageLoader;

    private Runnable mTimeHintUpdaterRunnable = null;

    private List<Runnable> mDeferredUiOperations = new ArrayList<>();

    private Handler mHandler;

    private boolean mAnalyticsScreenViewHasFired;

    private UserActionListener mListener;

    private boolean mShowFab = false;

    private boolean mHasEnterTransition = false;

    private String mExtendedSessionUrl = null;

    private GoogleApiClient mClient;

    @Override
    public void addListener(UserActionListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAnalyticsScreenViewHasFired = false;
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(AppIndex.API)
                .enableAutoManage((SessionDetailActivity) getActivity(), null)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.session_detail_frag, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHandler = new Handler();
        initPresenter();
        initViews();
        initViewListeners();
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
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
        if (!BuildConfig.ENABLE_MAP_IN_NAVIGATION) {
            MenuItem map = menu.findItem(R.id.menu_map_room);
            map.setVisible(false);
        }
        tryExecuteDeferredUiOperations();
    }


    public void fireExtendedSessionIntent() {
        if (mExtendedSessionUrl != null) {
            Intent extendedSessionIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(
                    mExtendedSessionUrl));
            startActivity(extendedSessionIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_map_room:
                sendUserAction(SessionDetailUserActionEnum.SHOW_MAP, null);
                return true;
            case R.id.menu_share:
                sendUserAction(SessionDetailUserActionEnum.SHOW_SHARE, null);
                return true;
        }
        return false;
    }

    private void sendUserAction(UserActionEnum action, Bundle args) {
        mListener.onUserAction(action, args);
    }

    private void initPresenter() {
        SessionDetailModel model = ModelProvider.provideSessionDetailModel(
                ((SessionDetailActivity) getActivity()).getSessionUri(), getContext(),
                new SessionsHelper(getActivity()), getLoaderManager());
        PresenterImpl presenter = new PresenterImpl(model, this,
                SessionDetailUserActionEnum.values(), SessionDetailQueryEnum.values());
        presenter.loadInitialQueries();
    }

    private void initViews() {
        final ViewGroup root = (ViewGroup) getActivity().findViewById(R.id.session_detail_frag);
        mAppBar = (AppBarLayout) root.findViewById(R.id.appbar);
        mCollapsingToolbar =
                (CollapsingToolbarLayout) mAppBar.findViewById(R.id.collapsing_toolbar);
        mHeaderBox = mAppBar.findViewById(R.id.header_session);
        mToolbar = (Toolbar) mHeaderBox.findViewById(R.id.toolbar);
        mTitle = (TextView) mHeaderBox.findViewById(R.id.session_title);
        mSubtitle = (TextView) mHeaderBox.findViewById(R.id.session_subtitle);
        mPhotoViewContainer = mCollapsingToolbar.findViewById(R.id.session_photo_container);
        mPhotoView = (ImageView) mPhotoViewContainer.findViewById(R.id.session_photo);
        mWatchVideo = (Button) mCollapsingToolbar.findViewById(R.id.watch);
        final ViewGroup details = (ViewGroup) root.findViewById(R.id.details_container);
        mAbstract = (TextView) details.findViewById(R.id.session_abstract);
        mLiveStreamedIndicator =
                (TextView) details.findViewById(R.id.live_streamed_indicator);
        mRequirements = (TextView) details.findViewById(R.id.session_requirements);
        mTags = (LinearLayout) details.findViewById(R.id.session_tags);
        mExtended = (TextView) details.findViewById(R.id.extended_session_button);
        mTagsContainer = (ViewGroup) details.findViewById(R.id.session_tags_container);
        mAddScheduleFab =
                (CheckableFloatingActionButton) root.findViewById(R.id.add_schedule_button);
        mImageLoader = new ImageLoader(getContext());
    }

    @Override
    public void displayData(SessionDetailModel data, SessionDetailQueryEnum query) {
        switch (query) {
            case SESSIONS:
                displaySessionData(data);
                displayTrackColor(data);
                break;
            case FEEDBACK:
                displayFeedbackData(data);
                break;
            case SPEAKERS:
                displaySpeakersData(data);
                break;
            case TAG_METADATA:
                displayTags(data);
                displayTrackColor(data);
                break;
            default:
                break;
        }
    }

    private void initViewListeners() {
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mAddScheduleFab.announceForAccessibility(isInSchedule ?
                            getString(R.string.session_details_a11y_session_added) :
                            getString(R.string.session_details_a11y_session_removed));
                }
            }
        });
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
                Intent intentShowMap =
                        new Intent(getActivity().getApplicationContext(), MapActivity.class);
                intentShowMap.putExtra(MapActivity.EXTRA_ROOM, data.getRoomId());
                intentShowMap.putExtra(MapActivity.EXTRA_DETACHED_MODE, true);
                getActivity().startActivity(intentShowMap);
                break;
            case SHOW_SHARE:
                ShareCompat.IntentBuilder builder =
                        ShareCompat.IntentBuilder.from(getActivity()).setType(
                                "text/plain").setText(getActivity()
                                .getString(R.string.share_template, data.getSessionTitle(),
                                        BuildConfig.CONFERENCE_HASHTAG, data.getSessionUrl()));
                Intent intentShare = Intent.createChooser(
                        builder.getIntent(),
                        getString(R.string.title_share));
                intentShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intentShare);

                break;
            case EXTENDED:
                fireExtendedSessionIntent();
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
        mTitle.setText(data.getSessionTitle());
        mSubtitle.setText(data.getSessionSubtitle());
        try {
            AppIndex.AppIndexApi.start(mClient, getActionForTitle(data.getSessionTitle()));
        } catch (Throwable e) {
            // Nothing to do if indexing fails.
        }

        if (data.shouldShowHeaderImage()) {
            mImageLoader.loadImage(data.getPhotoUrl(), mPhotoView);
        } else {
            mPhotoViewContainer.setVisibility(View.GONE);
            ViewCompat.setFitsSystemWindows(mAppBar, false);
            // This is hacky but the collapsing toolbar requires a minimum height to enable
            // the status bar scrim feature; set 1px. When there is no image, this would leave
            // a 1px gap so we offset with a negative margin.
            ((ViewGroup.MarginLayoutParams) mCollapsingToolbar.getLayoutParams()).topMargin = -1;
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

        // Build requirements section
        final View requirementsBlock = getActivity().findViewById(R.id.session_requirements_block);
        final String sessionRequirements = data.getRequirements();
        if (!TextUtils.isEmpty(sessionRequirements)) {
            UIUtils.setTextMaybeHtml(mRequirements, sessionRequirements);
            requirementsBlock.setVisibility(View.VISIBLE);
        } else {
            requirementsBlock.setVisibility(View.GONE);
        }

        final ViewGroup relatedVideosBlock = (ViewGroup) getActivity().findViewById(
                R.id.related_videos_block);
        relatedVideosBlock.setVisibility(View.GONE);

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


        if (BuildConfig.ENABLE_EXTENDED_SESSION_URL && data.shouldShowExtendedSessionLink()) {
            mExtendedSessionUrl = data.getExtendedSessionUrl();
            if (!TextUtils.isEmpty(mExtendedSessionUrl)) {
                mExtended.setText(R.string.description_extended);
                mExtended.setVisibility(View.VISIBLE);

                mExtended.setClickable(true);
                mExtended.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        sendUserAction(SessionDetailUserActionEnum.EXTENDED, null);
                    }
                });
            }
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

            final Drawable background = mHeaderBox.getBackground();
            if (background instanceof ColorDrawable
                    && ((ColorDrawable) background).getColor() == trackColor) {
                return;
            }

            // Animate the color change to make the transition smoother
            final ObjectAnimator color =
                    ObjectAnimator.ofInt(mHeaderBox, UIUtils.BACKGROUND_COLOR, trackColor);
            color.setEvaluator(new ArgbEvaluator());
            if (mHasEnterTransition) {
                color.setStartDelay(200L);
            }
            color.setDuration(300L);
            color.start();

            if (mCollapsingToolbar.getFitsSystemWindows()
                    && mPhotoViewContainer.getVisibility() == View.VISIBLE) { // immersive+photo
                mCollapsingToolbar.setStatusBarScrimColor(trackColor);
            } else {
                UIUtils.adjustAndSetStatusBarColor(getActivity(), trackColor);
            }

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
        final ObjectAnimator color = ObjectAnimator.ofInt(mHeaderBox, UIUtils.BACKGROUND_COLOR,
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

    private void displayFeedbackData(SessionDetailModel data) {
        if (data.hasFeedback()) {
            final MessageCardView giveFeedbackCardView =
                    (MessageCardView) getActivity().findViewById(R.id.give_feedback_card);
            if (giveFeedbackCardView != null) {
                giveFeedbackCardView.setVisibility(View.GONE);
            }
        }
        LOGD(TAG, "User " + (data.hasFeedback() ? "already gave" : "has not given")
                + " feedback for session.");
    }

    private void displaySpeakersData(SessionDetailModel data) {
        final ViewGroup speakersGroup = (ViewGroup) getActivity()
                .findViewById(R.id.session_speakers_block);

        // Remove all existing speakers (everything but first child, which is the header)
        for (int i = speakersGroup.getChildCount() - 1; i >= 1; i--) {
            speakersGroup.removeViewAt(i);
        }

        final LayoutInflater inflater = getActivity().getLayoutInflater();

        boolean hasSpeakers = false;

        List<SessionDetailModel.Speaker> speakers = data.getSpeakers();

        for (final SessionDetailModel.Speaker speaker : speakers) {

            String speakerHeader = speaker.getName();
            if (!TextUtils.isEmpty(speaker.getCompany())) {
                speakerHeader += ", " + speaker.getCompany();
            }

            final View speakerView = inflater
                    .inflate(R.layout.speaker_detail, speakersGroup, false);
            final TextView speakerHeaderView = (TextView) speakerView
                    .findViewById(R.id.speaker_header);
            final ImageView speakerImageView = (ImageView) speakerView
                    .findViewById(R.id.speaker_image);
            final TextView speakerAbstractView = (TextView) speakerView
                    .findViewById(R.id.speaker_abstract);
            final ImageView plusOneIcon = (ImageView) speakerView.findViewById(R.id.gplus_icon_box);
            final ImageView twitterIcon = (ImageView) speakerView.findViewById(
                    R.id.twitter_icon_box);

            setUpSpeakerSocialIcon(speaker, twitterIcon, speaker.getTwitterUrl(),
                    UIUtils.TWITTER_COMMON_NAME, UIUtils.TWITTER_PACKAGE_NAME);

            setUpSpeakerSocialIcon(speaker, plusOneIcon, speaker.getPlusoneUrl(),
                    UIUtils.GOOGLE_PLUS_COMMON_NAME, UIUtils.GOOGLE_PLUS_PACKAGE_NAME);

            // A speaker may have both a Twitter and GPlus page, only a Twitter page or only a
            // GPlus page, or neither. By default, align the Twitter icon to the right and the GPlus
            // icon to its left. If only a single icon is displayed, align it to the right.
            determineSocialIconPlacement(plusOneIcon, twitterIcon);

            if (!TextUtils.isEmpty(speaker.getImageUrl()) && mImageLoader != null) {
                mImageLoader.loadImage(speaker.getImageUrl(), speakerImageView);
            }

            speakerHeaderView.setText(speakerHeader);
            speakerImageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            UIUtils.setTextMaybeHtml(speakerAbstractView, speaker.getAbstract());

            if (!TextUtils.isEmpty(speaker.getUrl())) {
                speakerImageView.setEnabled(true);
                speakerImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent speakerProfileIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(speaker.getUrl()));
                        speakerProfileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        UIUtils.preferPackageForIntent(getActivity(),
                                speakerProfileIntent,
                                UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
                        getActivity().startActivity(speakerProfileIntent);
                    }
                });
            } else {
                speakerImageView.setEnabled(false);
                speakerImageView.setOnClickListener(null);
            }

            speakersGroup.addView(speakerView);
            hasSpeakers = true;
        }

        speakersGroup.setVisibility(hasSpeakers ? View.VISIBLE : View.GONE);
        updateEmptyView(data);
    }

    /**
     * Determines visibility of a social icon, sets up a click listener to allow the user to
     * navigate to the social network associated with the icon, and sets up a content description
     * for the icon.
     */
    private void setUpSpeakerSocialIcon(final SessionDetailModel.Speaker speaker,
            ImageView socialIcon, final String socialUrl,
            String socialNetworkName, final String packageName) {
        if (socialUrl == null || socialUrl.isEmpty()) {
            socialIcon.setVisibility(View.GONE);
        } else {
            socialIcon.setContentDescription(getString(
                            R.string.speaker_social_page,
                            socialNetworkName,
                            speaker.getName())
            );
            socialIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIUtils.fireSocialIntent(
                            getActivity(),
                            Uri.parse(socialUrl),
                            packageName
                    );
                }
            });
        }
    }

    /**
     * Aligns the Twitter icon the parent bottom right. Aligns the G+ icon to the left of the
     * Twitter icon if it is present. Otherwise, aligns the G+ icon to the parent bottom right.
     */
    private void determineSocialIconPlacement(ImageView plusOneIcon, ImageView twitterIcon) {
        if (plusOneIcon.getVisibility() == View.VISIBLE) {
            // Set the dimensions of the G+ button.
            int socialIconDimension = getResources().getDimensionPixelSize(
                    R.dimen.social_icon_box_size);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    socialIconDimension, socialIconDimension);
            params.addRule(RelativeLayout.BELOW, R.id.speaker_abstract);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

            if (twitterIcon.getVisibility() == View.VISIBLE) {
                params.addRule(RelativeLayout.LEFT_OF, R.id.twitter_icon_box);
            } else {
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            }
            plusOneIcon.setLayoutParams(params);
        }
    }

    private void updateEmptyView(SessionDetailModel data) {
        getActivity().findViewById(android.R.id.empty).setVisibility(
                (data.getSessionTitle() != null && data.getSpeakers().size() == 0
                        && !data.hasSummaryContent())
                        ? View.VISIBLE
                        : View.GONE);
    }

    private void updateTimeBasedUi(SessionDetailModel data) {
        // Show "Live streamed" label for all live-streamed sessions unless it has ended
        mLiveStreamedIndicator.setVisibility(
                (data.hasLiveStream() && !data.hasSessionEnded()) ? View.VISIBLE : View.GONE);

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

        // If the session is done, hide the FAB, and show the "Give feedback" card.
        if (data.isSessionReadyForFeedback()) {
            mShowFab = false;
            mAddScheduleFab.setVisibility(View.GONE);
            if (!data.hasFeedback() && data.isInScheduleWhenSessionFirstLoaded() &&
                    !sDismissedFeedbackCard.contains(data.getSessionId())) {
                showGiveFeedbackCard(data);
            }
        }

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
                    timeHint = getString(R.string.time_hint_about_to_start_shortly,
                            minutesUntilStart);
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
        if (data.getTagMetadata() == null || data.getTagsString() == null) {
            mTagsContainer.setVisibility(View.GONE);
            return;
        }

        if (TextUtils.isEmpty(data.getTagsString())) {
            mTagsContainer.setVisibility(View.GONE);
        } else {
            mTagsContainer.setVisibility(View.VISIBLE);
            mTags.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            String[] tagIds = data.getTagsString().split(",");

            List<TagMetadata.Tag> tags = new ArrayList<TagMetadata.Tag>();
            for (String tagId : tagIds) {
                if (Config.Tags.SESSIONS.equals(tagId) ||
                        Config.Tags.SPECIAL_KEYNOTE.equals(tagId)) {
                    continue;
                }

                TagMetadata.Tag tag = data.getTagMetadata().getTag(tagId);
                if (tag == null) {
                    continue;
                }

                tags.add(tag);
            }

            if (tags.size() == 0) {
                mTagsContainer.setVisibility(View.GONE);
                return;
            }

            Collections.sort(tags, TagMetadata.TAG_DISPLAY_ORDER_COMPARATOR);

            for (final TagMetadata.Tag tag : tags) {
                TextView chipView = (TextView) inflater.inflate(
                        R.layout.include_session_tag_chip, mTags, false);
                chipView.setText(tag.getName());
                chipView.setContentDescription(tag.getName());
                chipView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(), ExploreSessionsActivity.class)
                                .putExtra(ExploreSessionsActivity.EXTRA_FILTER_TAG, tag.getId())
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        getActivity().startActivity(intent);
                    }
                });

                mTags.addView(chipView);
            }
        }
    }

    private void showGiveFeedbackCard(final SessionDetailModel data) {
        final MessageCardView messageCardView = (MessageCardView) getActivity().findViewById(
                R.id.give_feedback_card);
        messageCardView.show();
        messageCardView.setListener(new MessageCardView.OnMessageCardButtonClicked() {
            @Override
            public void onMessageCardButtonClicked(String tag) {
                if (getResources().getString(R.string.tag_give_feedback).equals(tag)) {
                    sendUserAction(SessionDetailUserActionEnum.GIVE_FEEDBACK, null);
                    Intent intent = data.getFeedbackIntent();
                    getActivity().startActivity(intent);
                } else {
                    sDismissedFeedbackCard.add(data.getSessionId());
                    messageCardView.dismiss();
                }
            }
        });
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

    /*
         * Event structure:
         * Category -> "Session Details"
         * Action -> Link Text
         * Label -> Session's Title
         * Value -> 0.
         */
    private void fireLinkEvent(int actionId, SessionDetailModel data) {
        // ANALYTICS EVENT:  Click on a link in the Session Details page.
        // Contains: The link's name and the session title.
        AnalyticsHelper.sendEvent("Session", getString(actionId), data.getSessionTitle());
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
}
