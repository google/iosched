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

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.ExploreSessionsActivity;
import com.google.samples.apps.iosched.framework.QueryEnum;
import com.google.samples.apps.iosched.framework.UpdatableView;
import com.google.samples.apps.iosched.framework.UserActionEnum;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailUserActionEnum;
import com.google.samples.apps.iosched.ui.widget.CheckableFloatingActionButton;
import com.google.samples.apps.iosched.ui.widget.MessageCardView;
import com.google.samples.apps.iosched.ui.widget.ObservableScrollView;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.LUtils;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.samples.apps.iosched.util.YouTubeUtils;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
public class SessionDetailFragment extends Fragment
        implements ObservableScrollView.Callbacks, UpdatableView<SessionDetailModel> {

    private static final String TAG = LogUtils.makeLogTag(SessionDetailFragment.class);

    /**
     * Stores the session IDs for which the user has dismissed the "give feedback" card. This
     * information is kept for the duration of the app's execution so that if they say "No,
     * thanks", we don't show the card again for that session while the app is still executing.
     */
    private static HashSet<String> sDismissedFeedbackCard = new HashSet<>();

    private static final float PHOTO_ASPECT_RATIO = 1.7777777f;

    private View mAddScheduleButtonContainer;
    private CheckableFloatingActionButton mAddScheduleButton;

    private int mAddScheduleButtonContainerHeightPixels;

    private View mScrollViewChild;

    private TextView mTitle;

    private TextView mSubtitle;

    private ObservableScrollView mScrollView;

    private TextView mAbstract;

    private ImageView mPlusOneIcon;

    private ImageView mTwitterIcon;

    private TextView mLiveStreamVideocamIconAndText;

    private TextView mLiveStreamPlayIconAndText;

    private LinearLayout mTags;

    private ViewGroup mTagsContainer;

    private TextView mRequirements;

    private View mHeaderBox;

    private View mDetailsContainer;

    private int mPhotoHeightPixels;

    private int mHeaderHeightPixels;

    private boolean mHasPhoto;

    private View mPhotoViewContainer;

    private ImageView mPhotoView;

    private float mMaxHeaderElevation;

    private float mFABElevation;

    private ImageLoader mSpeakersImageLoader, mNoPlaceholderImageLoader;

    private Runnable mTimeHintUpdaterRunnable = null;

    private List<Runnable> mDeferredUiOperations = new ArrayList<>();

    private LUtils mLUtils;

    private Handler mHandler;

    private boolean mAnalyticsScreenViewHasFired;

    List<UserActionListener> mListeners = new ArrayList<>();

    @Override
    public void addListener(UserActionListener toAdd) {
        mListeners.add(toAdd);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAnalyticsScreenViewHasFired = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.session_detail_frag, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLUtils = LUtils.getInstance((AppCompatActivity) getActivity());
        mHandler = new Handler();
        initViews();
        initViewListeners();
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
    public void onDestroy() {
        super.onDestroy();
        if (mScrollView == null) {
            return;
        }

        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.removeGlobalOnLayoutListener(mGlobalLayoutListener);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.session_detail, menu);
        tryExecuteDeferredUiOperations();
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
        for (UserActionListener l : mListeners) {
            l.onUserAction(action, args);
        }
    }

    private void initViews() {
        mFABElevation = getResources().getDimensionPixelSize(R.dimen.fab_elevation);
        mMaxHeaderElevation = getResources().getDimensionPixelSize(
                R.dimen.session_detail_max_header_elevation);

        mScrollView = (ObservableScrollView) getActivity().findViewById(R.id.scroll_view);
        mScrollView.addCallbacks(this);
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }

        mScrollViewChild = getActivity().findViewById(R.id.scroll_view_child);
        mScrollViewChild.setVisibility(View.INVISIBLE);

        mDetailsContainer = getActivity().findViewById(R.id.details_container);
        mHeaderBox = getActivity().findViewById(R.id.header_session);
        mTitle = (TextView) getActivity().findViewById(R.id.session_title);
        mSubtitle = (TextView) getActivity().findViewById(R.id.session_subtitle);
        mPhotoViewContainer = getActivity().findViewById(R.id.session_photo_container);
        mPhotoView = (ImageView) getActivity().findViewById(R.id.session_photo);

        mAbstract = (TextView) getActivity().findViewById(R.id.session_abstract);

        mPlusOneIcon = (ImageView) getActivity().findViewById(R.id.gplus_icon_box);
        mTwitterIcon = (ImageView) getActivity().findViewById(R.id.twitter_icon_box);

        //Find view that shows a Videocam icon if the session is being live streamed.
        mLiveStreamVideocamIconAndText = (TextView) getActivity().findViewById(
                R.id.live_stream_videocam_icon_and_text);

        // Find view that shows a play button and some text for the user to watch the session live stream.
        mLiveStreamPlayIconAndText = (TextView) getActivity().findViewById(
                R.id.live_stream_play_icon_and_text);

        mRequirements = (TextView) getActivity().findViewById(R.id.session_requirements);
        mTags = (LinearLayout) getActivity().findViewById(R.id.session_tags);
        mTagsContainer = (ViewGroup) getActivity().findViewById(R.id.session_tags_container);

        ViewCompat.setTransitionName(mPhotoView, SessionDetailConstants.TRANSITION_NAME_PHOTO);

        mAddScheduleButtonContainer = getActivity()
                .findViewById(R.id.add_schedule_button_container);
        mAddScheduleButton = (CheckableFloatingActionButton) getActivity()
                .findViewById(R.id.add_schedule_button);

        mNoPlaceholderImageLoader = new ImageLoader(getContext());
        mSpeakersImageLoader = new ImageLoader(getContext(), R.drawable.person_image_empty);
    }

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mAddScheduleButtonContainerHeightPixels = mAddScheduleButtonContainer.getHeight();
            recomputePhotoAndScrollingMetrics();
        }
    };

    private void recomputePhotoAndScrollingMetrics() {
        mHeaderHeightPixels = mHeaderBox.getHeight();

        mPhotoHeightPixels = 0;
        if (mHasPhoto) {
            mPhotoHeightPixels = (int) (mPhotoView.getWidth() / PHOTO_ASPECT_RATIO);
            mPhotoHeightPixels = Math.min(mPhotoHeightPixels, mScrollView.getHeight() * 2 / 3);
        }

        ViewGroup.LayoutParams lp;
        lp = mPhotoViewContainer.getLayoutParams();
        if (lp.height != mPhotoHeightPixels) {
            lp.height = mPhotoHeightPixels;
            mPhotoViewContainer.setLayoutParams(lp);
        }

        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                mDetailsContainer.getLayoutParams();
        if (mlp.topMargin != mHeaderHeightPixels + mPhotoHeightPixels) {
            mlp.topMargin = mHeaderHeightPixels + mPhotoHeightPixels;
            mDetailsContainer.setLayoutParams(mlp);
        }

        onScrollChanged(0, 0); // trigger scroll handling
    }

    @Override
    public void displayData(SessionDetailModel data, QueryEnum query) {
        if (SessionDetailQueryEnum.SESSIONS == query) {
            displaySessionData(data);
        } else if (SessionDetailQueryEnum.FEEDBACK == query) {
            displayFeedbackData(data);
        } else if (SessionDetailQueryEnum.SPEAKERS == query) {
            displaySpeakersData(data);
        } else if (SessionDetailQueryEnum.TAG_METADATA == query) {
            displayTags(data);
        }
    }

    private void initViewListeners() {
        mAddScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean starred = !((CheckableFloatingActionButton) view).isChecked();
                showStarred(starred, true);
                if (starred) {
                    sendUserAction(SessionDetailUserActionEnum.STAR, null);
                } else {
                    sendUserAction(SessionDetailUserActionEnum.UNSTAR, null);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mAddScheduleButton.announceForAccessibility(starred ?
                            getString(R.string.session_details_a11y_session_added) :
                            getString(R.string.session_details_a11y_session_removed));
                }
            }
        });
    }


    private void showStarred(boolean starred, boolean allowAnimate) {
        mAddScheduleButton.setChecked(starred, allowAnimate);

        mLUtils.setOrAnimatePlusCheckIcon(mAddScheduleButton, starred, allowAnimate);
        mAddScheduleButton.setContentDescription(getString(starred
                ? R.string.remove_from_schedule_desc
                : R.string.add_to_schedule_desc));
    }

    @Override
    public void displayErrorMessage(QueryEnum query) {
    }

    @Override
    public Uri getDataUri(QueryEnum query) {
        if (SessionDetailQueryEnum.SESSIONS == query) {
            return ((SessionDetailActivity) getActivity()).getSessionUri();
        } else {
            return null;
        }
    }


    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        // Reposition the header bar -- it's normally anchored to the top of the content,
        // but locks to the top of the screen on scroll
        int scrollY = mScrollView.getScrollY();

        float newTop = Math.max(mPhotoHeightPixels, scrollY);
        mHeaderBox.setTranslationY(newTop);
        mAddScheduleButtonContainer.setTranslationY(newTop + mHeaderHeightPixels
                - mAddScheduleButtonContainerHeightPixels / 2);

        float gapFillProgress = 1;
        if (mPhotoHeightPixels != 0) {
            gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                    0,
                    mPhotoHeightPixels), 0), 1);
        }

        ViewCompat.setElevation(mHeaderBox, gapFillProgress * mMaxHeaderElevation);
        ViewCompat.setElevation(mAddScheduleButtonContainer, gapFillProgress * mMaxHeaderElevation
                + mFABElevation);
        ViewCompat.setElevation(mAddScheduleButton, gapFillProgress * mMaxHeaderElevation
                + mFABElevation);

        // Move background photo (parallax effect)
        mPhotoViewContainer.setTranslationY(scrollY * 0.5f);
    }

    private void displaySessionData(final SessionDetailModel data) {
        mTitle.setText(data.getSessionTitle());
        mSubtitle.setText(data.getSessionSubtitle());

        mPhotoViewContainer
                .setBackgroundColor(UIUtils.scaleSessionColorToDefaultBG(data.getSessionColor()));

        if (data.hasPhotoUrl()) {
            mHasPhoto = true;
            mNoPlaceholderImageLoader.loadImage(data.getPhotoUrl(), mPhotoView, new RequestListener<String, Bitmap>() {
                @Override
                public boolean onException(Exception e, String model, Target<Bitmap> target,
                                           boolean isFirstResource) {
                    mHasPhoto = false;
                    recomputePhotoAndScrollingMetrics();
                    return false;
                }

                @Override
                public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target,
                                               boolean isFromMemoryCache, boolean isFirstResource) {
                    // Trigger image transition
                    recomputePhotoAndScrollingMetrics();
                    return false;
                }
            });
            recomputePhotoAndScrollingMetrics();
        } else {
            mHasPhoto = false;
            recomputePhotoAndScrollingMetrics();
        }

        tryExecuteDeferredUiOperations();

        // Handle Keynote as a special case, where the user cannot remove it
        // from the schedule (it is auto added to schedule on sync)
        mAddScheduleButton.setVisibility(
                (AccountUtils.hasActiveAccount(getContext()) && !data.isKeynote())
                        ? View.VISIBLE : View.INVISIBLE);

        displayTags(data);

        if (!data.isKeynote()) {
            showStarredDeferred(data.isInSchedule(), false);
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
                    R.color.video_scrim_watched));
            mLiveStreamPlayIconAndText.setText(getString(R.string.session_replay));
        }

        if (data.hasLiveStream()) {
            mLiveStreamPlayIconAndText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String videoId = YouTubeUtils.getVideoIdFromSessionData(data.getYouTubeUrl(),
                            data.getLiveStreamId());
                    YouTubeUtils.showYouTubeVideo(videoId, getActivity());
                }
            });
        }

        fireAnalyticsScreenView(data.getSessionTitle());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onScrollChanged(0, 0); // trigger scroll handling
                mScrollViewChild.setVisibility(View.VISIBLE);
                //mAbstract.setTextIsSelectable(true);
            }
        });

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
    }

    /**
     * Sends a screen view to Google Analytics, if a screenview hasn't already been sent
     * since the fragment was loaded.  This prevents background syncs from causing superflous
     * screen views.
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

            if (!TextUtils.isEmpty(speaker.getImageUrl()) && mSpeakersImageLoader != null) {
                mSpeakersImageLoader.loadImage(speaker.getImageUrl(), speakerImageView);
            }

            speakerHeaderView.setText(speakerHeader);
            speakerImageView.setContentDescription(
                    getString(R.string.speaker_googleplus_profile, speakerHeader));
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
                        startActivity(speakerProfileIntent);
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
        // Show "Live streamed" for all live-streamed sessions that aren't currently going on.
        mLiveStreamVideocamIconAndText.setVisibility(data.hasLiveStream() && !data.isSessionOngoing() ?
                View.VISIBLE : View.GONE);

        if (data.hasLiveStream() && data.hasSessionStarted()) {
            // Show the play button and text only once the session starts.
            mLiveStreamVideocamIconAndText.setVisibility(View.VISIBLE);

            if (data.isSessionOngoing()) {
                mLiveStreamPlayIconAndText.setText(getString(R.string.session_watch_live));
            } else {
                mLiveStreamPlayIconAndText.setText(getString(R.string.session_watch));
                // TODO: implement Replay.
            }
        } else {
            mLiveStreamPlayIconAndText.setVisibility(View.GONE);
        }

        // If the session is done, hide the FAB, and show the "Give feedback" card.
        if (data.isSessionReadyForFeedback()) {
            mAddScheduleButton.setVisibility(View.INVISIBLE);
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
                chipView.setContentDescription(
                        getString(R.string.talkback_button, tag.getName()));
                chipView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(), ExploreSessionsActivity.class)
                                .putExtra(ExploreSessionsActivity.EXTRA_FILTER_TAG, tag.getId())
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
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
                if ("GIVE_FEEDBACK".equals(tag)) {
                    // ANALYTICS EVENT: Click on the "send feedback" action in Session Details.
                    // Contains: The session title.
                    AnalyticsHelper.sendEvent("Session", "Feedback", data.getSessionTitle());
                    Intent intent = data.getFeedbackIntent();
                    startActivity(intent);
                } else {
                    sDismissedFeedbackCard.add(data.getSessionId());
                    messageCardView.dismiss();
                }
            }
        });
    }

    private void showStarredDeferred(final boolean starred, final boolean allowAnimate) {
        mDeferredUiOperations.add(new Runnable() {
            @Override
            public void run() {
                showStarred(starred, allowAnimate);
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
}
