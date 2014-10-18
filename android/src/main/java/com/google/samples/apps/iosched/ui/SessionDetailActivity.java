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

import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.request.bitmap.RequestListener;
import com.bumptech.glide.request.target.Target;
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
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.BeamUtils;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

/**
 * An activity that shows detail information for a session, including session title, abstract,
 * time information, speaker photos and bios, etc.
 */
public class SessionDetailActivity extends BaseActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ObservableScrollView.Callbacks {
    private static final String TAG = LogUtils.makeLogTag(SessionDetailActivity.class);

    private static final int[] SECTION_HEADER_RES_IDS = {
            R.id.session_links_header,
            R.id.session_speakers_header,
            R.id.session_requirements_header,
            R.id.related_videos_header,
    };
    private static final float PHOTO_ASPECT_RATIO = 1.7777777f;

    public static final String TRANSITION_NAME_PHOTO = "photo";

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
    private View mDetailsContainer;

    private boolean mSessionCursor = false;
    private boolean mSpeakersCursor = false;
    private boolean mHasSummaryContent = false;

    private ImageLoader mSpeakersImageLoader, mNoPlaceholderImageLoader;
    private List<Runnable> mDeferredUiOperations = new ArrayList<Runnable>();

    private StringBuilder mBuffer = new StringBuilder();

    private int mPhotoHeightPixels;
    private int mHeaderHeightPixels;
    private int mAddScheduleButtonHeightPixels;

    private boolean mHasPhoto;
    private View mPhotoViewContainer;
    private ImageView mPhotoView;
    private int mSessionColor;
    private String mLivestreamUrl;

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
    protected void onCreate(Bundle savedInstanceState) {
        UIUtils.tryTranslateHttpIntent(this);
        BeamUtils.tryUpdateIntentFromBeam(this);
        boolean shouldBeFloatingWindow = shouldBeFloatingWindow();
        if (shouldBeFloatingWindow) {
            setupFloatingWindow();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        final Toolbar toolbar = getActionBarToolbar();
        toolbar.setNavigationIcon(shouldBeFloatingWindow
                ? R.drawable.ic_ab_close : R.drawable.ic_up);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                toolbar.setTitle("");
            }
        });

        if (savedInstanceState == null) {
            Uri sessionUri = getIntent().getData();
            BeamUtils.setBeamSessionUri(this, sessionUri);
        }

        mSessionUri = getIntent().getData();

        if (mSessionUri == null) {
            return;
        }

        mSessionId = ScheduleContract.Sessions.getSessionId(mSessionUri);

        mFABElevation = getResources().getDimensionPixelSize(R.dimen.fab_elevation);
        mMaxHeaderElevation = getResources().getDimensionPixelSize(
                R.dimen.session_detail_max_header_elevation);

        mTagColorDotSize = getResources().getDimensionPixelSize(R.dimen.tag_color_dot_size);

        mHandler = new Handler();

        if (mSpeakersImageLoader == null) {
            mSpeakersImageLoader = new ImageLoader(this, R.drawable.person_image_empty);
        }
        if (mNoPlaceholderImageLoader == null) {
            mNoPlaceholderImageLoader = new ImageLoader(this);
        }

        mScrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
        mScrollView.addCallbacks(this);
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }

        mScrollViewChild = findViewById(R.id.scroll_view_child);
        mScrollViewChild.setVisibility(View.INVISIBLE);

        mDetailsContainer = findViewById(R.id.details_container);
        mHeaderBox = findViewById(R.id.header_session);
        mTitle = (TextView) findViewById(R.id.session_title);
        mSubtitle = (TextView) findViewById(R.id.session_subtitle);
        mPhotoViewContainer = findViewById(R.id.session_photo_container);
        mPhotoView = (ImageView) findViewById(R.id.session_photo);

        mPlusOneButton = (PlusOneButton) findViewById(R.id.plus_one_button);
        mAbstract = (TextView) findViewById(R.id.session_abstract);
        mRequirements = (TextView) findViewById(R.id.session_requirements);
        mTags = (LinearLayout) findViewById(R.id.session_tags);
        mTagsContainer = (ViewGroup) findViewById(R.id.session_tags_container);

        mAddScheduleButton = (CheckableFrameLayout) findViewById(R.id.add_schedule_button);
        mAddScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean starred = !mStarred;
                SessionsHelper helper = new SessionsHelper(SessionDetailActivity.this);
                showStarred(starred, true);
                helper.setSessionStarred(mSessionUri, starred, mTitleString);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mAddScheduleButton.announceForAccessibility(starred ?
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

        ViewCompat.setTransitionName(mPhotoView, TRANSITION_NAME_PHOTO);

        LoaderManager manager = getLoaderManager();
        manager.initLoader(SessionsQuery._TOKEN, null, this);
        manager.initLoader(SpeakersQuery._TOKEN, null, this);
        manager.initLoader(TAG_METADATA_TOKEN, null, this);
    }

    @Override
    public Intent getParentActivityIntent() {
        // TODO(mangini): make this Activity navigate up to the right screen depending on how it was launched
        return new Intent(this, MyScheduleActivity.class);
    }

    private void setupFloatingWindow() {
        // configure this Activity as a floating window, dimming the background
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.session_details_floating_width);
        params.height = getResources().getDimensionPixelSize(R.dimen.session_details_floating_height);
        params.alpha = 1;
        params.dimAmount = 0.4f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }

    private boolean shouldBeFloatingWindow() {
        Resources.Theme theme = getTheme();
        TypedValue floatingWindowFlag = new TypedValue();
        if (theme == null || !theme.resolveAttribute(R.attr.isFloatingWindow, floatingWindowFlag, true)) {
            // isFloatingWindow flag is not defined in theme
            return false;
        }
        return (floatingWindowFlag.data != 0);
    }

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
    protected void onDestroy() {
        super.onDestroy();
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
        // Reposition the header bar -- it's normally anchored to the top of the content,
        // but locks to the top of the screen on scroll
        int scrollY = mScrollView.getScrollY();

        float newTop = Math.max(mPhotoHeightPixels, scrollY);
        mHeaderBox.setTranslationY(newTop);
        mAddScheduleButton.setTranslationY(newTop + mHeaderHeightPixels
                - mAddScheduleButtonHeightPixels / 2);

        float gapFillProgress = 1;
        if (mPhotoHeightPixels != 0) {
            gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                    0,
                    mPhotoHeightPixels), 0), 1);
        }

        ViewCompat.setElevation(mHeaderBox, gapFillProgress * mMaxHeaderElevation);
        ViewCompat.setElevation(mAddScheduleButton, gapFillProgress * mMaxHeaderElevation
                + mFABElevation);

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
            if (UIUtils.getCurrentTime(this) < mSessionStart) {
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
                intent.setClass(this, SessionCalendarService.class);
                startService(intent);

                if (mStarred) {
                    setupNotification();
                }
            }
        }
    }

    private void setupNotification() {
        Intent scheduleIntent;

        // Schedule session notification
        if (UIUtils.getCurrentTime(this) < mSessionStart) {
            LOGD(TAG, "Scheduling notification about session start.");
            scheduleIntent = new Intent(
                    SessionAlarmService.ACTION_SCHEDULE_STARRED_BLOCK,
                    null, this, SessionAlarmService.class);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, mSessionStart);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, mSessionEnd);
            startService(scheduleIntent);
        } else {
            LOGD(TAG, "Not scheduling notification about session start, too late.");
        }

        // Schedule feedback notification
        if (UIUtils.getCurrentTime(this) < mSessionEnd) {
            LOGD(TAG, "Scheduling notification about session feedback.");
            scheduleIntent = new Intent(
                    SessionAlarmService.ACTION_SCHEDULE_FEEDBACK_NOTIFICATION,
                    null, this, SessionAlarmService.class);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ID, mSessionId);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, mSessionStart);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, mSessionEnd);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_TITLE, mTitleString);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ROOM, mRoomName);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_SPEAKERS, mSpeakers);
            startService(scheduleIntent);
        } else {
            LOGD(TAG, "Not scheduling feedback notification, too late.");
        }
    }

    private void updateTimeBasedUi() {
        long currentTimeMillis = UIUtils.getCurrentTime(this);
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

        if (TimeUtils.hasConferenceEnded(this)) {
            // no time hint to display
            timeHint = "";
        } else if (currentTimeMillis >= mSessionEnd) {
            timeHint = getString(R.string.time_hint_session_ended);
        } else if (currentTimeMillis >= mSessionStart) {
            long minutesAgo = (currentTimeMillis - mSessionStart) / 60000;
            if (minutesAgo > 1) {
                timeHint = getString(R.string.time_hint_started_min, minutesAgo);
            } else {
                timeHint = getString(R.string.time_hint_started_just);
            }
        } else if (countdownMillis > 0 && countdownMillis < Config.HINT_TIME_BEFORE_SESSION) {
            long millisUntil = mSessionStart - currentTimeMillis;
            long minutesUntil = millisUntil / 60000 + (millisUntil % 1000 > 0 ? 1 : 0);
            if (minutesUntil > 1) {
                timeHint = getString(R.string.time_hint_about_to_start_min, minutesUntil);
            } else {
                timeHint = getString(R.string.time_hint_about_to_start_shortly, minutesUntil);
            }
        }

        final TextView timeHintView = (TextView) findViewById(R.id.time_hint);

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
        // Is there existing feedback for this session?
        mAlreadyGaveFeedback = cursor.getCount() > 0;

        if (mAlreadyGaveFeedback) {
            final MessageCardView giveFeedbackCardView = (MessageCardView) findViewById(R.id.give_feedback_card);
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
            // TODO: Remove this in favor of a callbacks interface that the activity
            // can implement.
            finish();
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

        mHeaderBox.setBackgroundColor(mSessionColor);
        getLUtils().setStatusBarColor(UIUtils.scaleColor(mSessionColor, 0.8f, false));

        mLivestreamUrl = cursor.getString(SessionsQuery.LIVESTREAM_URL);
        mHasLivestream = !TextUtils.isEmpty(mLivestreamUrl);

        // Format the time this session occupies
        mSessionStart = cursor.getLong(SessionsQuery.START);
        mSessionEnd = cursor.getLong(SessionsQuery.END);
        mRoomName = cursor.getString(SessionsQuery.ROOM_NAME);
        mSpeakers = cursor.getString(SessionsQuery.SPEAKER_NAMES);
        String subtitle = UIUtils.formatSessionSubtitle(
                mSessionStart, mSessionEnd, mRoomName, mBuffer, this);
        if (mHasLivestream) {
            subtitle += " " + UIUtils.getLiveBadgeText(this, mSessionStart, mSessionEnd);
        }

        mTitle.setText(mTitleString);
        mSubtitle.setText(subtitle);

        for (int resId : SECTION_HEADER_RES_IDS) {
            ((TextView) findViewById(resId)).setTextColor(mSessionColor);
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
                (AccountUtils.hasActiveAccount(this) && !mIsKeynote)
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
        final View requirementsBlock = findViewById(R.id.session_requirements_block);
        final String sessionRequirements = cursor.getString(SessionsQuery.REQUIREMENTS);
        if (!TextUtils.isEmpty(sessionRequirements)) {
            UIUtils.setTextMaybeHtml(mRequirements, sessionRequirements);
            requirementsBlock.setVisibility(View.VISIBLE);
            mHasSummaryContent = true;
        } else {
            requirementsBlock.setVisibility(View.GONE);
        }

        // Build related videos section
        final ViewGroup relatedVideosBlock = (ViewGroup) findViewById(R.id.related_videos_block);
        relatedVideosBlock.setVisibility(View.GONE);

        // Build links section
        buildLinksSection(cursor);

        updateEmptyView();

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
                updateTimeBasedUi();
                mHandler.postDelayed(mTimeHintUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
            }
        };
        mHandler.postDelayed(mTimeHintUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
    }

    private void tryRenderTags() {
        if (mTagMetadata == null || mTagsString == null) {
            return;
        }

        if (TextUtils.isEmpty(mTagsString)) {
            mTagsContainer.setVisibility(View.GONE);
        } else {
            mTagsContainer.setVisibility(View.VISIBLE);
            mTags.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);
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

                chipView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish(); // TODO: better encapsulation
                        Intent intent = new Intent(SessionDetailActivity.this, BrowseSessionsActivity.class)
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
        // Compile list of links (I/O live link, submit feedback, and normal links)
        ViewGroup linkContainer = (ViewGroup) findViewById(R.id.links_container);
        linkContainer.removeAllViews();


        // Build links section
        // the Object can be either a string URL or an Intent
        List<Pair<Integer, Object>> links = new ArrayList<Pair<Integer, Object>>();

        long currentTimeMillis = UIUtils.getCurrentTime(this);
        if (mHasLivestream
                && currentTimeMillis > mSessionStart
                && currentTimeMillis <= mSessionEnd) {
            links.add(new Pair<Integer, Object>(
                    R.string.session_link_livestream,
                    getWatchLiveIntent(this)));
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
            LayoutInflater inflater = LayoutInflater.from(this);
            int columns = getResources().getInteger(R.integer.links_columns);

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

            findViewById(R.id.session_links_header).setVisibility(View.VISIBLE);
            findViewById(R.id.links_container).setVisibility(View.VISIBLE);

        } else {
            findViewById(R.id.session_links_header).setVisibility(View.GONE);
            findViewById(R.id.links_container).setVisibility(View.GONE);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                YouTubeIntents.canResolvePlayVideoIntent(context)) {
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
        final MessageCardView messageCardView = (MessageCardView) findViewById(R.id.live_now_card);
        messageCardView.show();
        messageCardView.setListener(new MessageCardView.OnMessageCardButtonClicked() {
            @Override
            public void onMessageCardButtonClicked(String tag) {
                if ("WATCH_NOW".equals(tag)) {
                    Intent intent = getWatchLiveIntent(SessionDetailActivity.this);
                    startActivity(intent);
                } else {
                    mDismissedWatchLivestreamCard = true;
                    messageCardView.dismiss();
                }
            }
        });
    }

    private void showGiveFeedbackCard() {
        final MessageCardView messageCardView = (MessageCardView) findViewById(R.id.give_feedback_card);
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
        return new Intent(Intent.ACTION_VIEW, mSessionUri, this,
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
        getLUtils().setOrAnimatePlusCheckIcon(iconView, starred, allowAnimate);
        mAddScheduleButton.setContentDescription(getString(starred
                ? R.string.remove_from_schedule_desc
                : R.string.add_to_schedule_desc));
    }

    private void setupShareMenuItemDeferred() {
        mDeferredUiOperations.add(new Runnable() {
            @Override
            public void run() {
                new SessionsHelper(SessionDetailActivity.this).tryConfigureShareMenuItem(mShareMenuItem,
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
        final ViewGroup speakersGroup = (ViewGroup) findViewById(R.id.session_speakers_block);

        // Remove all existing speakers (everything but first child, which is the header)
        for (int i = speakersGroup.getChildCount() - 1; i >= 1; i--) {
            speakersGroup.removeViewAt(i);
        }

        final LayoutInflater inflater = getLayoutInflater();

        boolean hasSpeakers = false;

        cursor.moveToPosition(-1); // move to just before first record
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
                speakerImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent speakerProfileIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(speakerUrl));
                        speakerProfileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        UIUtils.preferPackageForIntent(SessionDetailActivity.this,
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
            mHasSummaryContent = true;
        }

        speakersGroup.setVisibility(hasSpeakers ? View.VISIBLE : View.GONE);
        updateEmptyView();
    }

    private void updateEmptyView() {
        findViewById(android.R.id.empty).setVisibility(
                (mSpeakersCursor && mSessionCursor && !mHasSummaryContent)
                        ? View.VISIBLE
                        : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.session_detail, menu);
        mSocialStreamMenuItem = menu.findItem(R.id.menu_social_stream);
        mShareMenuItem = menu.findItem(R.id.menu_share);
        tryExecuteDeferredUiOperations();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SessionsHelper helper = new SessionsHelper(this);
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
                helper.shareSession(this, R.string.share_template, mTitleString,
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
                    UIUtils.showHashtagStream(this, mHashTag);
                }
                return true;
        }
        return false;
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
            loader = new CursorLoader(this, mSessionUri, SessionsQuery.PROJECTION, null,
                    null, null);
        } else if (id == SpeakersQuery._TOKEN  && mSessionUri != null){
            Uri speakersUri = ScheduleContract.Sessions.buildSpeakersDirUri(mSessionId);
            loader = new CursorLoader(this, speakersUri, SpeakersQuery.PROJECTION, null,
                    null, ScheduleContract.Speakers.DEFAULT_SORT);
        } else if (id == FeedbackQuery._TOKEN) {
            Uri feedbackUri = ScheduleContract.Feedback.buildFeedbackUri(mSessionId);
            loader = new CursorLoader(this, feedbackUri, FeedbackQuery.PROJECTION, null,
                    null, null);
        } else if (id == TAG_METADATA_TOKEN) {
            loader = TagMetadata.createCursorLoader(this);
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
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
