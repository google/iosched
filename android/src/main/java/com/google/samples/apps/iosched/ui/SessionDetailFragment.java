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

package com.google.samples.apps.iosched.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Pair;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.plus.PlusOneButton;
import com.google.android.youtube.player.YouTubeIntents;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.service.SessionAlarmService;
import com.google.samples.apps.iosched.service.SessionCalendarService;
import com.google.samples.apps.iosched.ui.widget.CheckableFrameLayout;
import com.google.samples.apps.iosched.ui.widget.MessageCardView;
import com.google.samples.apps.iosched.ui.widget.ObservableScrollView;
import com.google.samples.apps.iosched.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.bumptech.glide.request.bitmap.RequestListener;
import com.bumptech.glide.request.target.Target;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that shows detail information for a session, including session title, abstract,
 * time information, speaker photos and bios, etc.
 */
public class SessionDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ObservableScrollView.Callbacks {

    private static final String TAG = makeLogTag(SessionDetailFragment.class);

    private static final int[] SECTION_HEADER_RES_IDS = {
            R.id.session_links_header,
            R.id.session_speakers_header,
            R.id.session_requirements_header,
            R.id.related_videos_header,
    };
    private static final float PHOTO_ASPECT_RATIO = 1.7777777f;

    public static final String VIEW_NAME_PHOTO = "photo";

    private Handler mHandler = new Handler();
    private static final int TIME_HINT_UPDATE_INTERVAL = 10000; // 10 sec

    private TagMetadata mTagMetadata;

    private String mSessionId;
    private Uri mSessionUri;

    private long mSessionStart;
    private long mSessionEnd;
    private String mTitleString;
    private String mHashTag;
    private String mUrl;
    private String mRoomId;
    private String mRoomName;
    private String mTagsString;

    // A comma-separated list of speakers to be passed to Android Wear
    private String mSpeakers;

    private boolean mStarred;
    private boolean mInitStarred;
    private boolean mDismissedWatchLivestreamCard = false;
    private boolean mHasLivestream = false;
    private MenuItem mSocialStreamMenuItem;
    private MenuItem mShareMenuItem;

    private ViewGroup mRootView;
    private View mScrollViewChild;
    private TextView mTitle;
    private TextView mSubtitle;
    private PlusOneButton mPlusOneButton;

    private ObservableScrollView mScrollView;
    private CheckableFrameLayout mAddScheduleButton;

    private TextView mAbstract;
    private LinearLayout mTags;
    private ViewGroup mTagsContainer;
    private TextView mRequirements;
    private View mHeaderBox;
    private View mHeaderContentBox;
    private View mHeaderBackgroundBox;
    private View mHeaderShadow;
    private View mDetailsContainer;

    private boolean mSessionCursor = false;
    private boolean mSpeakersCursor = false;
    private boolean mHasSummaryContent = false;

    private ImageLoader mSpeakersImageLoader, mNoPlaceholderImageLoader;
    private List<Runnable> mDeferredUiOperations = new ArrayList<Runnable>();

    private StringBuilder mBuffer = new StringBuilder();

    private int mHeaderTopClearance;
    private int mPhotoHeightPixels;
    private int mHeaderHeightPixels;
    private int mAddScheduleButtonHeightPixels;

    private boolean mHasPhoto;
    private View mPhotoViewContainer;
    private ImageView mPhotoView;
    boolean mGapFillShown;
    private int mSessionColor;
    private String mLivestreamUrl;

    private static final float GAP_FILL_DISTANCE_MULTIPLIER = 1.5f;

    private Runnable mTimeHintUpdaterRunnable = null;

    private boolean mAlreadyGaveFeedback = false;
    private boolean mIsKeynote = false;

    // this set stores the session IDs for which the user has dismissed the
    // "give feedback" card. This information is kept for the duration of the app's execution
    // so that if they say "No, thanks", we don't show the card again for that session while
    // the app is still executing.
    private static HashSet<String> sDismissedFeedbackCard = new HashSet<String>();

    private TextView mSubmitFeedbackView;
    private float mMaxHeaderElevation;
    private float mFABElevation;

    private int mTagColorDotSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mSessionUri = intent.getData();

        if (mSessionUri == null) {
            return;
        }

        mSessionId = ScheduleContract.Sessions.getSessionId(mSessionUri);

        setHasOptionsMenu(true);

        mFABElevation = getResources().getDimensionPixelSize(R.dimen.fab_elevation);
        mMaxHeaderElevation = getResources().getDimensionPixelSize(
                R.dimen.session_detail_max_header_elevation);

        mTagColorDotSize = getResources().getDimensionPixelSize(R.dimen.tag_color_dot_size);

        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_session_detail, container, false);
        mScrollViewChild = mRootView.findViewById(R.id.scroll_view_child);
        mScrollViewChild.setVisibility(View.INVISIBLE);

        mDetailsContainer = mRootView.findViewById(R.id.details_container);
        mHeaderBox = mRootView.findViewById(R.id.header_session);
        mHeaderContentBox = mRootView.findViewById(R.id.header_session_contents);
        mHeaderBackgroundBox = mRootView.findViewById(R.id.header_background);
        mHeaderShadow = mRootView.findViewById(R.id.header_shadow);
        mTitle = (TextView) mRootView.findViewById(R.id.session_title);
        mSubtitle = (TextView) mRootView.findViewById(R.id.session_subtitle);
        mPhotoViewContainer = mRootView.findViewById(R.id.session_photo_container);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.session_photo);

        mPlusOneButton = (PlusOneButton) mRootView.findViewById(R.id.plus_one_button);
        mAbstract = (TextView) mRootView.findViewById(R.id.session_abstract);
        mRequirements = (TextView) mRootView.findViewById(R.id.session_requirements);
        mTags = (LinearLayout) mRootView.findViewById(R.id.session_tags);
        mTagsContainer = (ViewGroup) mRootView.findViewById(R.id.session_tags_container);

        mAddScheduleButton = (CheckableFrameLayout)
                mRootView.findViewById(R.id.add_schedule_button);
        mAddScheduleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean starred = !mStarred;
                SessionsHelper helper = new SessionsHelper(getActivity());
                showStarred(starred, true);
                helper.setSessionStarred(mSessionUri, starred, mTitleString);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mRootView.announceForAccessibility(starred ?
                            getString(R.string.session_details_a11y_session_added) :
                            getString(R.string.session_details_a11y_session_removed));
                }

                /* [ANALYTICS:EVENT]
                 * TRIGGER:   Add or remove a session from My Schedule.
                 * CATEGORY:  'Session'
                 * ACTION:    'Starred' or 'Unstarred'
                 * LABEL:     Session title/subtitle.
                 * [/ANALYTICS]
                 */
                AnalyticsManager.sendEvent(
                        "Session", starred ? "Starred" : "Unstarred", mTitleString, 0L);
            }
        });

        ((BaseActivity) getActivity()).getLPreviewUtils().setViewName(mPhotoView, VIEW_NAME_PHOTO);

        setupCustomScrolling(mRootView);
        return mRootView;
    }

    private void recomputePhotoAndScrollingMetrics() {
        final int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
        mHeaderTopClearance = actionBarSize - mHeaderContentBox.getPaddingTop();
        mHeaderHeightPixels = mHeaderContentBox.getHeight();

        mPhotoHeightPixels = mHeaderTopClearance;
        if (mHasPhoto) {
            mPhotoHeightPixels = (int) (mPhotoView.getWidth() / PHOTO_ASPECT_RATIO);
            mPhotoHeightPixels = Math.min(mPhotoHeightPixels, mRootView.getHeight() * 2 / 3);
        }

        ViewGroup.LayoutParams lp;
        lp = mPhotoViewContainer.getLayoutParams();
        if (lp.height != mPhotoHeightPixels) {
            lp.height = mPhotoHeightPixels;
            mPhotoViewContainer.setLayoutParams(lp);
        }

        lp = mHeaderBackgroundBox.getLayoutParams();
        if (lp.height != mHeaderHeightPixels) {
            lp.height = mHeaderHeightPixels;
            mHeaderBackgroundBox.setLayoutParams(lp);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mSpeakersImageLoader == null) {
            mSpeakersImageLoader = new ImageLoader(this.getActivity(), R.drawable.person_image_empty);
        }
        if (mNoPlaceholderImageLoader == null) {
            mNoPlaceholderImageLoader = new ImageLoader(this.getActivity());
        }
    }

    private void setupCustomScrolling(View rootView) {
        mScrollView = (ObservableScrollView) rootView.findViewById(R.id.scroll_view);
        mScrollView.addCallbacks(this);
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mScrollView == null) {
            return;
        }

        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.removeGlobalOnLayoutListener(mGlobalLayoutListener);
        }
    }

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mAddScheduleButtonHeightPixels = mAddScheduleButton.getHeight();
            recomputePhotoAndScrollingMetrics();
        }
    };

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        final BaseActivity activity = (BaseActivity) getActivity();
        if (activity == null) {
            return;
        }

        // Reposition the header bar -- it's normally anchored to the top of the content,
        // but locks to the top of the screen on scroll
        int scrollY = mScrollView.getScrollY();

        float newTop = Math.max(mPhotoHeightPixels, scrollY + mHeaderTopClearance);
        mHeaderBox.setTranslationY(newTop);
        mAddScheduleButton.setTranslationY(newTop + mHeaderHeightPixels
                - mAddScheduleButtonHeightPixels / 2);

        mHeaderBackgroundBox.setPivotY(mHeaderHeightPixels);
        int gapFillDistance = (int) (mHeaderTopClearance * GAP_FILL_DISTANCE_MULTIPLIER);
        boolean showGapFill = !mHasPhoto || (scrollY > (mPhotoHeightPixels - gapFillDistance));
        float desiredHeaderScaleY = showGapFill ?
                ((mHeaderHeightPixels + gapFillDistance + 1) * 1f / mHeaderHeightPixels)
                : 1f;
        if (!mHasPhoto) {
            mHeaderBackgroundBox.setScaleY(desiredHeaderScaleY);
        } else if (mGapFillShown != showGapFill) {
            mHeaderBackgroundBox.animate()
                    .scaleY(desiredHeaderScaleY)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .setDuration(250)
                    .start();
        }
        mGapFillShown = showGapFill;

        LPreviewUtilsBase lpu = activity.getLPreviewUtils();

        mHeaderShadow.setVisibility(lpu.hasLPreviewAPIs() ? View.GONE : View.VISIBLE);

        if (mHeaderTopClearance != 0) {
            // Fill the gap between status bar and header bar with color
            float gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                    mPhotoHeightPixels - mHeaderTopClearance * 2,
                    mPhotoHeightPixels - mHeaderTopClearance), 0), 1);
            lpu.setViewElevation(mHeaderBackgroundBox, gapFillProgress * mMaxHeaderElevation);
            lpu.setViewElevation(mHeaderContentBox, gapFillProgress * mMaxHeaderElevation + 0.1f);
            lpu.setViewElevation(mAddScheduleButton, gapFillProgress * mMaxHeaderElevation
                    + mFABElevation);
            if (!lpu.hasLPreviewAPIs()) {
                mHeaderShadow.setAlpha(gapFillProgress);
            }
        }

        // Move background photo (parallax effect)
        mPhotoViewContainer.setTranslationY(scrollY * 0.5f);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePlusOneButton();
        if (mTimeHintUpdaterRunnable != null) {
            mHandler.postDelayed(mTimeHintUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
        }

        // Refresh whether or not feedback has been submitted
        getLoaderManager().restartLoader(FeedbackQuery._TOKEN, null, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mInitStarred != mStarred) {
            if (UIUtils.getCurrentTime(getActivity()) < mSessionStart) {
                // Update Calendar event through the Calendar API on Android 4.0 or new versions.
                Intent intent = null;
                if (mStarred) {
                    // Set up intent to add session to Calendar, if it doesn't exist already.
                    intent = new Intent(SessionCalendarService.ACTION_ADD_SESSION_CALENDAR,
                            mSessionUri);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_START,
                            mSessionStart);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_END,
                            mSessionEnd);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_ROOM, mRoomName);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_TITLE, mTitleString);
                } else {
                    // Set up intent to remove session from Calendar, if exists.
                    intent = new Intent(SessionCalendarService.ACTION_REMOVE_SESSION_CALENDAR,
                            mSessionUri);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_START,
                            mSessionStart);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_END,
                            mSessionEnd);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_TITLE, mTitleString);
                }
                intent.setClass(getActivity(), SessionCalendarService.class);
                getActivity().startService(intent);

                if (mStarred) {
                    setupNotification();
                }
            }
        }
    }

    private void setupNotification() {
        Intent scheduleIntent;
        final Context context = getActivity();

        // Schedule session notification
        if (UIUtils.getCurrentTime(context) < mSessionStart) {
            LOGD(TAG, "Scheduling notification about session start.");
            scheduleIntent = new Intent(
                    SessionAlarmService.ACTION_SCHEDULE_STARRED_BLOCK,
                    null, context, SessionAlarmService.class);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, mSessionStart);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, mSessionEnd);
            context.startService(scheduleIntent);
        } else {
            LOGD(TAG, "Not scheduling notification about session start, too late.");
        }

        // Schedule feedback notification
        if (UIUtils.getCurrentTime(context) < mSessionEnd) {
            LOGD(TAG, "Scheduling notification about session feedback.");
            scheduleIntent = new Intent(
                    SessionAlarmService.ACTION_SCHEDULE_FEEDBACK_NOTIFICATION,
                    null, context, SessionAlarmService.class);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ID, mSessionId);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, mSessionStart);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, mSessionEnd);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_TITLE, mTitleString);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ROOM, mRoomName);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_SPEAKERS, mSpeakers);
            context.startService(scheduleIntent);
        } else {
            LOGD(TAG, "Not scheduling feedback notification, too late.");
        }
    }

    private void updateTimeBasedUi() {
        final Context context = mRootView.getContext();
        long currentTimeMillis = UIUtils.getCurrentTime(context);
        boolean canShowLivestream = mHasLivestream;

        if (canShowLivestream && !mDismissedWatchLivestreamCard
                && currentTimeMillis > mSessionStart
                && currentTimeMillis <= mSessionEnd) {
            // show the "watch now" card
            showWatchNowCard();
        } else if (!mAlreadyGaveFeedback && mInitStarred && currentTimeMillis >= (mSessionEnd -
                Config.FEEDBACK_MILLIS_BEFORE_SESSION_END)
                && !sDismissedFeedbackCard.contains(mSessionId)) {
            // show the "give feedback" card
            showGiveFeedbackCard();
        }

        String timeHint = "";
        long countdownMillis = mSessionStart - currentTimeMillis;

        if (TimeUtils.hasConferenceEnded(context)) {
            // no time hint to display
            timeHint = "";
        } else if (currentTimeMillis >= mSessionEnd) {
            timeHint = context.getString(R.string.time_hint_session_ended);
        } else if (currentTimeMillis >= mSessionStart) {
            long minutesAgo = (currentTimeMillis - mSessionStart) / 60000;
            if (minutesAgo > 1) {
                timeHint = context.getString(R.string.time_hint_started_min, minutesAgo);
            } else {
                timeHint = context.getString(R.string.time_hint_started_just);
            }
        } else if (countdownMillis > 0 && countdownMillis < Config.HINT_TIME_BEFORE_SESSION) {
            long millisUntil = mSessionStart - currentTimeMillis;
            long minutesUntil = millisUntil / 60000 + (millisUntil % 1000 > 0 ? 1 : 0);
            if (minutesUntil > 1) {
                timeHint = context.getString(R.string.time_hint_about_to_start_min, minutesUntil);
            } else {
                timeHint = context.getString(R.string.time_hint_about_to_start_shortly, minutesUntil);
            }
        }

        final TextView timeHintView = (TextView) mRootView.findViewById(R.id.time_hint);

        if (!TextUtils.isEmpty(timeHint)) {
            timeHintView.setVisibility(View.VISIBLE);
            timeHintView.setText(timeHint);
        } else {
            timeHintView.setVisibility(View.GONE);
        }
    }

    private void setTextSelectable(TextView tv) {
        if (tv != null && !tv.isTextSelectable()) {
            tv.setTextIsSelectable(true);
        }
    }

    private void onFeedbackQueryComplete(Cursor cursor) {
        // Views have not been set up yet -- continue loading the rest of data
        if (mSubmitFeedbackView == null) {
            LoaderManager manager = getLoaderManager();
            manager.restartLoader(SessionsQuery._TOKEN, null, this);
            manager.restartLoader(SpeakersQuery._TOKEN, null, this);
            manager.restartLoader(TAG_METADATA_TOKEN, null, this);
        }

        // Is there existing feedback for this session?
        mAlreadyGaveFeedback = cursor.getCount() > 0;

        if (mAlreadyGaveFeedback) {
            final MessageCardView giveFeedbackCardView = (MessageCardView) mRootView.findViewById(
                    R.id.give_feedback_card);
            if (giveFeedbackCardView != null) {
                giveFeedbackCardView.setVisibility(View.GONE);
            }
            if (mSubmitFeedbackView != null) {
                mSubmitFeedbackView.setVisibility(View.GONE);
            }
        }
        LOGD(TAG, "User " + (mAlreadyGaveFeedback ? "already gave" : "has not given") + " feedback for session.");
        cursor.close();
    }

    /**
     * Handle {@link SessionsQuery} {@link Cursor}.
     */
    private void onSessionQueryComplete(Cursor cursor) {
        mSessionCursor = true;
        if (!cursor.moveToFirst()) {
            if (isAdded()) {
                // TODO: Remove this in favor of a callbacks interface that the activity
                // can implement.
                getActivity().finish();
            }
            return;
        }

        mTitleString = cursor.getString(SessionsQuery.TITLE);
        mSessionColor = cursor.getInt(SessionsQuery.COLOR);

        if (mSessionColor == 0) {
            // no color -- use default
            mSessionColor = getResources().getColor(R.color.default_session_color);
        } else {
            // make sure it's opaque
            mSessionColor = UIUtils.setColorAlpha(mSessionColor, 255);
        }

        mHeaderBackgroundBox.setBackgroundColor(mSessionColor);
        ((BaseActivity) getActivity()).getLPreviewUtils().setStatusBarColor(
                UIUtils.scaleColor(mSessionColor, 0.8f, false));

        mLivestreamUrl = cursor.getString(SessionsQuery.LIVESTREAM_URL);
        mHasLivestream = !TextUtils.isEmpty(mLivestreamUrl);

        // Format the time this session occupies
        mSessionStart = cursor.getLong(SessionsQuery.START);
        mSessionEnd = cursor.getLong(SessionsQuery.END);
        mRoomName = cursor.getString(SessionsQuery.ROOM_NAME);
        mSpeakers = cursor.getString(SessionsQuery.SPEAKER_NAMES);
        String subtitle = UIUtils.formatSessionSubtitle(
                mSessionStart, mSessionEnd, mRoomName, mBuffer, getActivity());
        if (mHasLivestream) {
            subtitle += " " + UIUtils.getLiveBadgeText(getActivity(), mSessionStart, mSessionEnd);
        }

        mTitle.setText(mTitleString);
        mSubtitle.setText(subtitle);

        for (int resId : SECTION_HEADER_RES_IDS) {
            ((TextView) mRootView.findViewById(resId)).setTextColor(mSessionColor);
        }

        mPhotoViewContainer.setBackgroundColor(UIUtils.scaleSessionColorToDefaultBG(mSessionColor));

        String photo = cursor.getString(SessionsQuery.PHOTO_URL);
        if (!TextUtils.isEmpty(photo)) {
            mHasPhoto = true;
            mNoPlaceholderImageLoader.loadImage(photo, mPhotoView, new RequestListener<String>() {
                @Override
                public void onException(Exception e, String url, Target target) {
                    mHasPhoto = false;
                    recomputePhotoAndScrollingMetrics();
                }

                @Override
                public void onImageReady(String url, Target target, boolean b, boolean b2) {
                    // Trigger image transition
                    recomputePhotoAndScrollingMetrics();
                }
            });
            recomputePhotoAndScrollingMetrics();
        } else {
            mHasPhoto = false;
            recomputePhotoAndScrollingMetrics();
        }

        mUrl = cursor.getString(SessionsQuery.URL);
        if (TextUtils.isEmpty(mUrl)) {
            mUrl = "";
        }

        mHashTag = cursor.getString(SessionsQuery.HASHTAG);
        if (!TextUtils.isEmpty(mHashTag)) {
            enableSocialStreamMenuItemDeferred();
        }

        mRoomId = cursor.getString(SessionsQuery.ROOM_ID);

        final boolean inMySchedule = cursor.getInt(SessionsQuery.IN_MY_SCHEDULE) != 0;

        setupShareMenuItemDeferred();

        // Handle Keynote as a special case, where the user cannot remove it
        // from the schedule (it is auto added to schedule on sync)
        mTagsString = cursor.getString(SessionsQuery.TAGS);
        mIsKeynote = mTagsString.contains(Config.Tags.SPECIAL_KEYNOTE);
        mAddScheduleButton.setVisibility(
                (AccountUtils.hasActiveAccount(getActivity()) && !mIsKeynote)
                        ? View.VISIBLE : View.INVISIBLE);

        tryRenderTags();

        if (!mIsKeynote) {
            showStarredDeferred(mInitStarred = inMySchedule, false);
        }

        final String sessionAbstract = cursor.getString(SessionsQuery.ABSTRACT);
        if (!TextUtils.isEmpty(sessionAbstract)) {
            UIUtils.setTextMaybeHtml(mAbstract, sessionAbstract);
            mAbstract.setVisibility(View.VISIBLE);
            mHasSummaryContent = true;
        } else {
            mAbstract.setVisibility(View.GONE);
        }

        updatePlusOneButton();

        // Build requirements section
        final View requirementsBlock = mRootView.findViewById(R.id.session_requirements_block);
        final String sessionRequirements = cursor.getString(SessionsQuery.REQUIREMENTS);
        if (!TextUtils.isEmpty(sessionRequirements)) {
            UIUtils.setTextMaybeHtml(mRequirements, sessionRequirements);
            requirementsBlock.setVisibility(View.VISIBLE);
            mHasSummaryContent = true;
        } else {
            requirementsBlock.setVisibility(View.GONE);
        }

        // Build related videos section
        final ViewGroup relatedVideosBlock = (ViewGroup) mRootView.findViewById(R.id.related_videos_block);
        relatedVideosBlock.setVisibility(View.GONE);


        // Build links section
        buildLinksSection(cursor);

        // Show empty message when all data is loaded, and nothing to show
        if (mSpeakersCursor && !mHasSummaryContent) {
            mRootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }

        updateTimeBasedUi();
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
                if (isAdded()) {
                    updateTimeBasedUi();
                    mHandler.postDelayed(mTimeHintUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
                }
            }
        };
        mHandler.postDelayed(mTimeHintUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
    }

    private void tryRenderTags() {
        if (mTagMetadata == null || mTagsString == null || !isAdded()) {
            return;
        }

        if (TextUtils.isEmpty(mTagsString)) {
            mTagsContainer.setVisibility(View.GONE);
        } else {
            mTagsContainer.setVisibility(View.VISIBLE);
            mTags.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            String[] tagIds = mTagsString.split(",");

            List<TagMetadata.Tag> tags = new ArrayList<TagMetadata.Tag>();
            for (String tagId : tagIds) {
                if (Config.Tags.SESSIONS.equals(tagId) ||
                        Config.Tags.SPECIAL_KEYNOTE.equals(tagId)) {
                    continue;
                }

                TagMetadata.Tag tag = mTagMetadata.getTag(tagId);
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

                if (Config.Tags.CATEGORY_TOPIC.equals(tag.getCategory())) {
                    ShapeDrawable colorDrawable = new ShapeDrawable(new OvalShape());
                    colorDrawable.setIntrinsicWidth(mTagColorDotSize);
                    colorDrawable.setIntrinsicHeight(mTagColorDotSize);
                    colorDrawable.getPaint().setStyle(Paint.Style.FILL);
                    chipView.setCompoundDrawablesWithIntrinsicBounds(colorDrawable,
                            null, null, null);
                    colorDrawable.getPaint().setColor(tag.getColor());
                }

                chipView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getActivity().finish(); // TODO: better encapsulation
                        Intent intent = new Intent(getActivity(), BrowseSessionsActivity.class)
                                .putExtra(BrowseSessionsActivity.EXTRA_FILTER_TAG, tag.getId())
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });

                mTags.addView(chipView);
            }
        }
    }

    private void buildLinksSection(Cursor cursor) {
        final Context context = mRootView.getContext();

        // Compile list of links (I/O live link, submit feedback, and normal links)
        ViewGroup linkContainer = (ViewGroup) mRootView.findViewById(R.id.links_container);
        linkContainer.removeAllViews();


        // Build links section
        // the Object can be either a string URL or an Intent
        List<Pair<Integer, Object>> links = new ArrayList<Pair<Integer, Object>>();

        long currentTimeMillis = UIUtils.getCurrentTime(context);
        if (mHasLivestream
                && currentTimeMillis > mSessionStart
                && currentTimeMillis <= mSessionEnd) {
            links.add(new Pair<Integer, Object>(
                    R.string.session_link_livestream,
                    getWatchLiveIntent(context)));
        }

        // Add session feedback link, if appropriate
        if (!mAlreadyGaveFeedback && currentTimeMillis > mSessionEnd
                - Config.FEEDBACK_MILLIS_BEFORE_SESSION_END) {
            links.add(new Pair<Integer, Object>(
                    R.string.session_feedback_submitlink,
                    getFeedbackIntent()
            ));
        }

        for (int i = 0; i < SessionsQuery.LINKS_INDICES.length; i++) {
            final String linkUrl = cursor.getString(SessionsQuery.LINKS_INDICES[i]);
            if (TextUtils.isEmpty(linkUrl)) {
                continue;
            }

            links.add(new Pair<Integer, Object>(
                    SessionsQuery.LINKS_TITLES[i],
                    new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            ));
        }

        // Render links
        if (links.size() > 0) {
            LayoutInflater inflater = LayoutInflater.from(context);
            int columns = context.getResources().getInteger(R.integer.links_columns);

            LinearLayout currentLinkRowView = null;
            for (int i = 0; i < links.size(); i++) {
                final Pair<Integer, Object> link = links.get(i);

                // Create link view
                TextView linkView = (TextView) inflater.inflate(R.layout.list_item_session_link,
                        linkContainer, false);
                if (link.first == R.string.session_feedback_submitlink) {
                    mSubmitFeedbackView = linkView;
                }
                linkView.setText(getString(link.first));
                linkView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        fireLinkEvent(link.first);
                        Intent intent=null;
                        if (link.second instanceof Intent) {
                            intent = (Intent) link.second;
                        } else if (link.second instanceof String) {
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) link.second))
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        }
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException ignored) {
                        }
                    }
                });

                // Place it inside a container
                if (columns == 1) {
                    linkContainer.addView(linkView);
                } else {
                    // create a new link row
                    if (i % columns == 0) {
                        currentLinkRowView = (LinearLayout) inflater.inflate(
                                R.layout.include_link_row, linkContainer, false);
                        currentLinkRowView.setWeightSum(columns);
                        linkContainer.addView(currentLinkRowView);
                    }

                    ((LinearLayout.LayoutParams) linkView.getLayoutParams()).width = 0;
                    ((LinearLayout.LayoutParams) linkView.getLayoutParams()).weight = 1;
                    currentLinkRowView.addView(linkView);
                }
            }

            mRootView.findViewById(R.id.session_links_header).setVisibility(View.VISIBLE);
            mRootView.findViewById(R.id.links_container).setVisibility(View.VISIBLE);

        } else {
            mRootView.findViewById(R.id.session_links_header).setVisibility(View.GONE);
            mRootView.findViewById(R.id.links_container).setVisibility(View.GONE);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTimeHintUpdaterRunnable != null) {
            mHandler.removeCallbacks(mTimeHintUpdaterRunnable);
        }
    }

    private Intent getWatchLiveIntent(Context context) {
        LPreviewUtilsBase lpu = ((BaseActivity) getActivity()).getLPreviewUtils();
        if (lpu.hasLPreviewAPIs() && YouTubeIntents.canResolvePlayVideoIntent(context)) {
            String youtubeVideoId = SessionLivestreamActivity.getVideoIdFromUrl(mLivestreamUrl);
            return YouTubeIntents.createPlayVideoIntentWithOptions(
                    context, youtubeVideoId, true, false);
        }
        return new Intent(Intent.ACTION_VIEW, mSessionUri).setClass(context,
                SessionLivestreamActivity.class);
    }

    private void updatePlusOneButton() {
        if (mPlusOneButton == null) {
            return;
        }

        if (!TextUtils.isEmpty(mUrl) && !mIsKeynote) {
            mPlusOneButton.initialize(mUrl, 0);
            mPlusOneButton.setVisibility(View.VISIBLE);
        } else {
            mPlusOneButton.setVisibility(View.GONE);
        }
    }

    private void showWatchNowCard() {
        final MessageCardView messageCardView = (MessageCardView) mRootView.findViewById(
                R.id.live_now_card);
        messageCardView.show();
        messageCardView.setListener(new MessageCardView.OnMessageCardButtonClicked() {
            @Override
            public void onMessageCardButtonClicked(String tag) {
                if ("WATCH_NOW".equals(tag)) {
                    Intent intent = getWatchLiveIntent(getActivity());
                    startActivity(intent);
                } else {
                    mDismissedWatchLivestreamCard = true;
                    messageCardView.dismiss();
                }
            }
        });
    }

    private void showGiveFeedbackCard() {
        final MessageCardView messageCardView = (MessageCardView) mRootView.findViewById(
                R.id.give_feedback_card);
        messageCardView.show();
        messageCardView.setListener(new MessageCardView.OnMessageCardButtonClicked() {
            @Override
            public void onMessageCardButtonClicked(String tag) {
                if ("GIVE_FEEDBACK".equals(tag)) {
                    /* [ANALYTICS:EVENT]
                     * TRIGGER:   Click on the Send Feedback action on the Session Details page.
                     * CATEGORY:  'Session'
                     * ACTION:    'Feedback'
                     * LABEL:     session title/subtitle
                     * [/ANALYTICS]
                     */
                    AnalyticsManager.sendEvent("Session", "Feedback", mTitleString, 0L);
                    Intent intent = getFeedbackIntent();
                    startActivity(intent);
                } else {
                    sDismissedFeedbackCard.add(mSessionId);
                    messageCardView.dismiss();
                }
            }
        });
    }

    private Intent getFeedbackIntent() {
        return new Intent(Intent.ACTION_VIEW, mSessionUri, getActivity(),
                SessionFeedbackActivity.class);
    }

    private void enableSocialStreamMenuItemDeferred() {
        mDeferredUiOperations.add(new Runnable() {
            @Override
            public void run() {
                mSocialStreamMenuItem.setVisible(true);
            }
        });
        tryExecuteDeferredUiOperations();
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

    private void showStarred(boolean starred, boolean allowAnimate) {
        mStarred = starred;

        mAddScheduleButton.setChecked(mStarred, allowAnimate);

        ImageView iconView = (ImageView) mAddScheduleButton.findViewById(R.id.add_schedule_icon);
        ((BaseActivity) getActivity()).getLPreviewUtils().setOrAnimatePlusCheckIcon(
                iconView, starred, allowAnimate);
        mAddScheduleButton.setContentDescription(getString(starred
                ? R.string.remove_from_schedule_desc
                : R.string.add_to_schedule_desc));
    }

    private void setupShareMenuItemDeferred() {
        mDeferredUiOperations.add(new Runnable() {
            @Override
            public void run() {
                new SessionsHelper(getActivity()).tryConfigureShareMenuItem(mShareMenuItem,
                        R.string.share_template, mTitleString, mHashTag, mUrl);
            }
        });
        tryExecuteDeferredUiOperations();
    }

    private void tryExecuteDeferredUiOperations() {
        if (mSocialStreamMenuItem != null) {
            for (Runnable r : mDeferredUiOperations) {
                r.run();
            }
            mDeferredUiOperations.clear();
        }
    }

    private void onSpeakersQueryComplete(Cursor cursor) {
        mSpeakersCursor = true;
        final ViewGroup speakersGroup = (ViewGroup)
                mRootView.findViewById(R.id.session_speakers_block);

        // Remove all existing speakers (everything but first child, which is the header)
        for (int i = speakersGroup.getChildCount() - 1; i >= 1; i--) {
            speakersGroup.removeViewAt(i);
        }

        final LayoutInflater inflater = getActivity().getLayoutInflater();

        boolean hasSpeakers = false;

        while (cursor.moveToNext()) {
            final String speakerName = cursor.getString(SpeakersQuery.SPEAKER_NAME);
            if (TextUtils.isEmpty(speakerName)) {
                continue;
            }

            final String speakerImageUrl = cursor.getString(SpeakersQuery.SPEAKER_IMAGE_URL);
            final String speakerCompany = cursor.getString(SpeakersQuery.SPEAKER_COMPANY);
            final String speakerUrl = cursor.getString(SpeakersQuery.SPEAKER_URL);
            final String speakerAbstract = cursor.getString(SpeakersQuery.SPEAKER_ABSTRACT);

            String speakerHeader = speakerName;
            if (!TextUtils.isEmpty(speakerCompany)) {
                speakerHeader += ", " + speakerCompany;
            }

            final View speakerView = inflater
                    .inflate(R.layout.speaker_detail, speakersGroup, false);
            final TextView speakerHeaderView = (TextView) speakerView
                    .findViewById(R.id.speaker_header);
            final ImageView speakerImageView = (ImageView) speakerView
                    .findViewById(R.id.speaker_image);
            final TextView speakerAbstractView = (TextView) speakerView
                    .findViewById(R.id.speaker_abstract);

            if (!TextUtils.isEmpty(speakerImageUrl) && mSpeakersImageLoader != null) {
                mSpeakersImageLoader.loadImage(speakerImageUrl, speakerImageView);
            }

            speakerHeaderView.setText(speakerHeader);
            speakerImageView.setContentDescription(
                    getString(R.string.speaker_googleplus_profile, speakerHeader));
            UIUtils.setTextMaybeHtml(speakerAbstractView, speakerAbstract);

            if (!TextUtils.isEmpty(speakerUrl)) {
                speakerImageView.setEnabled(true);
                speakerImageView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent speakerProfileIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(speakerUrl));
                        speakerProfileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        UIUtils.preferPackageForIntent(getActivity(), speakerProfileIntent,
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
            mHasSummaryContent = true;
        }

        speakersGroup.setVisibility(hasSpeakers ? View.VISIBLE : View.GONE);

        // Show empty message when all data is loaded, and nothing to show
        if (mSessionCursor && !mHasSummaryContent) {
            mRootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.session_detail, menu);
        mSocialStreamMenuItem = menu.findItem(R.id.menu_social_stream);
        mShareMenuItem = menu.findItem(R.id.menu_share);
        tryExecuteDeferredUiOperations();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SessionsHelper helper = new SessionsHelper(getActivity());
        switch (item.getItemId()) {
            case R.id.menu_map_room:
                /* [ANALYTICS:EVENT]
                 * TRIGGER:   Click on the Map action on the Session Details page.
                 * CATEGORY:  'Session'
                 * ACTION:    'Map'
                 * LABEL:     session title/subtitle
                 * [/ANALYTICS]
                 */
                AnalyticsManager.sendEvent("Session", "Map", mTitleString, 0L);
                helper.startMapActivity(mRoomId);
                return true;

            case R.id.menu_share:
                // On ICS+ devices, we normally won't reach this as ShareActionProvider will handle
                // sharing.
                helper.shareSession(getActivity(), R.string.share_template, mTitleString,
                        mHashTag, mUrl);
                return true;

            case R.id.menu_social_stream:
                if (!TextUtils.isEmpty(mHashTag)) {
                    /* [ANALYTICS:EVENT]
                     * TRIGGER:   Click on the Social Stream action on the Session Details page.
                     * CATEGORY:  'Session'
                     * ACTION:    'Stream'
                     * LABEL:     session title/subtitle
                     * [/ANALYTICS]
                     */
                    AnalyticsManager.sendEvent("Session", "Stream", mTitleString, 0L);
                    UIUtils.showHashtagStream(getActivity(), mHashTag);
                }
                return true;
        }
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    }

    @Override
    public void onDestroyOptionsMenu() {
    }

    /*
     * Event structure:
     * Category -> "Session Details"
     * Action -> Link Text
     * Label -> Session's Title
     * Value -> 0.
     */
    void fireLinkEvent(int actionId) {
        /* [ANALYTICS:EVENT]
         * TRIGGER:   Click on a link on the Session Details page.
         * CATEGORY:  'Session'
         * ACTION:    The link's name ("Watch Live", "Follow us on Google+", etc)
         * LABEL:     The session's title/subtitle.
         * [/ANALYTICS]
         */
        AnalyticsManager.sendEvent("Session", getString(actionId), mTitleString, 0L);
    }

    /**
     * {@link com.google.samples.apps.iosched.provider.ScheduleContract.Sessions} query parameters.
     */
    private interface SessionsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Sessions.SESSION_LEVEL,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_REQUIREMENTS,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
                ScheduleContract.Sessions.SESSION_HASHTAG,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_YOUTUBE_URL,
                ScheduleContract.Sessions.SESSION_PDF_URL,
                ScheduleContract.Sessions.SESSION_NOTES_URL,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_URL,
                ScheduleContract.Sessions.SESSION_MODERATOR_URL,
                ScheduleContract.Sessions.ROOM_ID,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Sessions.SESSION_COLOR,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_RELATED_CONTENT,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_SPEAKER_NAMES
        };

        int START = 0;
        int END = 1;
        int LEVEL = 2;
        int TITLE = 3;
        int ABSTRACT = 4;
        int REQUIREMENTS = 5;
        int IN_MY_SCHEDULE = 6;
        int HASHTAG = 7;
        int URL = 8;
        int YOUTUBE_URL = 9;
        int PDF_URL = 10;
        int NOTES_URL = 11;
        int LIVESTREAM_URL = 12;
        int MODERATOR_URL = 13;
        int ROOM_ID = 14;
        int ROOM_NAME = 15;
        int COLOR = 16;
        int PHOTO_URL = 17;
        int RELATED_CONTENT = 18;
        int TAGS = 19;
        int SPEAKER_NAMES = 20;

        int[] LINKS_INDICES = {
                YOUTUBE_URL,
                MODERATOR_URL,
                PDF_URL,
                NOTES_URL,
        };

        int[] LINKS_TITLES = {
                R.string.session_link_youtube,
                R.string.session_link_moderator,
                R.string.session_link_pdf,
                R.string.session_link_notes,
        };
    }

    private interface SpeakersQuery {
        int _TOKEN = 0x3;

        String[] PROJECTION = {
                ScheduleContract.Speakers.SPEAKER_NAME,
                ScheduleContract.Speakers.SPEAKER_IMAGE_URL,
                ScheduleContract.Speakers.SPEAKER_COMPANY,
                ScheduleContract.Speakers.SPEAKER_ABSTRACT,
                ScheduleContract.Speakers.SPEAKER_URL,
        };

        int SPEAKER_NAME = 0;
        int SPEAKER_IMAGE_URL = 1;
        int SPEAKER_COMPANY = 2;
        int SPEAKER_ABSTRACT = 3;
        int SPEAKER_URL = 4;
    }

    private interface FeedbackQuery {
        int _TOKEN = 0x4;

        String[] PROJECTION = {
                ScheduleContract.Feedback.SESSION_ID
        };
    }

    private static final int TAG_METADATA_TOKEN = 0x5;

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == SessionsQuery._TOKEN){
			loader = new CursorLoader(getActivity(), mSessionUri, SessionsQuery.PROJECTION, null,
					null, null);
		} else if (id == SpeakersQuery._TOKEN  && mSessionUri != null){
			Uri speakersUri = ScheduleContract.Sessions.buildSpeakersDirUri(mSessionId);
			loader = new CursorLoader(getActivity(), speakersUri, SpeakersQuery.PROJECTION, null,
                    null, ScheduleContract.Speakers.DEFAULT_SORT);
        } else if (id == FeedbackQuery._TOKEN) {
            Uri feedbackUri = ScheduleContract.Feedback.buildFeedbackUri(mSessionId);
            loader = new CursorLoader(getActivity(), feedbackUri, FeedbackQuery.PROJECTION, null,
                    null, null);
        } else if (id == TAG_METADATA_TOKEN) {
            loader = TagMetadata.createCursorLoader(getActivity());
        }
        return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (!isAdded()) {
            return;
        }

        if (loader.getId() == SessionsQuery._TOKEN) {
            onSessionQueryComplete(cursor);
        } else if (loader.getId() == SpeakersQuery._TOKEN) {
            onSpeakersQueryComplete(cursor);
        } else if (loader.getId() == FeedbackQuery._TOKEN) {
            onFeedbackQueryComplete(cursor);
        } else if (loader.getId() == TAG_METADATA_TOKEN) {
            mTagMetadata = new TagMetadata(cursor);
            cursor.close();
            tryRenderTags();
        } else {
            cursor.close();
        }
	}

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {}
}
