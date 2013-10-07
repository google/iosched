/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Pair;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.service.SessionAlarmService;
import com.google.android.apps.iosched.ui.widget.ObservableScrollView;
import com.google.android.apps.iosched.util.*;
import com.google.android.gms.plus.PlusOneButton;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that shows detail information for a session, including session title, abstract,
 * time information, speaker photos and bios, etc.
 */
public class SessionDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ObservableScrollView.Callbacks {

    private static final String TAG = makeLogTag(SessionDetailFragment.class);

    // Set this boolean extra to true to show a variable height header
    public static final String EXTRA_VARIABLE_HEIGHT_HEADER =
            "com.google.android.iosched.extra.VARIABLE_HEIGHT_HEADER";

    private Handler mHandler = new Handler();

    private String mSessionId;
    private Uri mSessionUri;

    private long mSessionBlockStart;
    private long mSessionBlockEnd;
    private String mTitleString;
    private String mHashtags;
    private String mUrl;
    private String mRoomId;

    private boolean mStarred;
    private boolean mInitStarred;
    private MenuItem mStarMenuItem;
    private MenuItem mSocialStreamMenuItem;
    private MenuItem mShareMenuItem;

    private ViewGroup mRootView;
    private TextView mTitle;
    private TextView mSubtitle;
    private PlusOneButton mPlusOneButton;

    private ObservableScrollView mScrollView;
    private CheckableLinearLayout mAddScheduleButton;
    private View mAddSchedulePlaceholderView;

    private TextView mAbstract;
    private TextView mRequirements;

    private boolean mSessionCursor = false;
    private boolean mSpeakersCursor = false;
    private boolean mHasSummaryContent = false;
    private boolean mVariableHeightHeader = false;

    private ImageLoader mImageLoader;
    private List<Runnable> mDeferredUiOperations = new ArrayList<Runnable>();

    private StringBuilder mBuffer = new StringBuilder();
    private Rect mBufferRect = new Rect();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mSessionUri = intent.getData();

        if (mSessionUri == null) {
            return;
        }

        mSessionId = ScheduleContract.Sessions.getSessionId(mSessionUri);

        mVariableHeightHeader = intent.getBooleanExtra(EXTRA_VARIABLE_HEIGHT_HEADER, false);

        LoaderManager manager = getLoaderManager();
        manager.restartLoader(SessionsQuery._TOKEN, null, this);
        manager.restartLoader(SpeakersQuery._TOKEN, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_session_detail, null);

        mTitle = (TextView) mRootView.findViewById(R.id.session_title);
        mSubtitle = (TextView) mRootView.findViewById(R.id.session_subtitle);

        // Larger target triggers plus one button
        mPlusOneButton = (PlusOneButton) mRootView.findViewById(R.id.plus_one_button);
        View headerView = mRootView.findViewById(R.id.header_session);
        FractionalTouchDelegate.setupDelegate(headerView, mPlusOneButton,
                new RectF(0.6f, 0f, 1f, 1.0f));

        mAbstract = (TextView) mRootView.findViewById(R.id.session_abstract);
        mRequirements = (TextView) mRootView.findViewById(R.id.session_requirements);

        mAddScheduleButton = (CheckableLinearLayout)
                mRootView.findViewById(R.id.add_schedule_button);
        mAddScheduleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setSessionStarred(!mStarred, true);
            }
        });
        mAddScheduleButton.setVisibility(View.GONE);

        if (mVariableHeightHeader) {
            ViewGroup.LayoutParams layoutParams = headerView.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            headerView.setLayoutParams(layoutParams);
        }

        setupCustomScrolling(mRootView);

        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof ImageLoader.ImageLoaderProvider) {
            mImageLoader = ((ImageLoader.ImageLoaderProvider) getActivity()).getImageLoaderInstance();
        }
    }

    private void setupCustomScrolling(View rootView) {
        mAddSchedulePlaceholderView = rootView.findViewById(
                R.id.add_to_schedule_button_placeholder);
        if (mAddSchedulePlaceholderView == null) {
            mAddScheduleButton.setVisibility(View.VISIBLE);
            return;
        }

        mScrollView = (ObservableScrollView) rootView.findViewById(R.id.scroll_view);
        mScrollView.setCallbacks(this);
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
            onScrollChanged();
            mAddScheduleButton.setVisibility(View.VISIBLE);
        }
    };

    @Override
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void onScrollChanged() {
        float newTop = Math.max(mAddSchedulePlaceholderView.getTop(), mScrollView.getScrollY());
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)
                mAddScheduleButton.getLayoutParams();
        if (UIUtils.hasICS()) {
            mAddScheduleButton.setTranslationY(newTop);
        } else {
            lp.gravity = Gravity.TOP | Gravity.START; // needed for earlier platform versions
            lp.topMargin = (int) newTop;
            mScrollView.requestLayout();
        }

        mScrollView.getGlobalVisibleRect(mBufferRect);
        int parentLeft = mBufferRect.left;
        int parentRight = mBufferRect.right;
        if (mAddSchedulePlaceholderView.getGlobalVisibleRect(mBufferRect)) {
            lp.leftMargin = mBufferRect.left - parentLeft;
            lp.rightMargin = parentRight - mBufferRect.right;
        }
        mAddScheduleButton.setLayoutParams(lp);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePlusOneButton();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mInitStarred != mStarred) {
            if (mStarred && UIUtils.getCurrentTime(getActivity()) < mSessionBlockStart) {
                setupNotification();
            }
        }
    }

    private void setupNotification() {
        // Schedule an alarm that fires a system notification when expires.
        final Context context = getActivity();
        Intent scheduleIntent = new Intent(
                SessionAlarmService.ACTION_SCHEDULE_STARRED_BLOCK,
                null, context, SessionAlarmService.class);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, mSessionBlockStart);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, mSessionBlockEnd);
        context.startService(scheduleIntent);
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

        // Format time block this session occupies
        mSessionBlockStart = cursor.getLong(SessionsQuery.BLOCK_START);
        mSessionBlockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
        String roomName = cursor.getString(SessionsQuery.ROOM_NAME);
        final String subtitle = UIUtils.formatSessionSubtitle(
                mTitleString, mSessionBlockStart, mSessionBlockEnd, roomName, mBuffer,
                getActivity());

        mTitle.setText(mTitleString);

        mUrl = cursor.getString(SessionsQuery.URL);
        if (TextUtils.isEmpty(mUrl)) {
            mUrl = "";
        }

        mHashtags = cursor.getString(SessionsQuery.HASHTAGS);
        if (!TextUtils.isEmpty(mHashtags)) {
            enableSocialStreamMenuItemDeferred();
        }

        mRoomId = cursor.getString(SessionsQuery.ROOM_ID);

        setupShareMenuItemDeferred();
        showStarredDeferred(mInitStarred = (cursor.getInt(SessionsQuery.STARRED) != 0), false);

        final String sessionAbstract = cursor.getString(SessionsQuery.ABSTRACT);
        if (!TextUtils.isEmpty(sessionAbstract)) {
            UIUtils.setTextMaybeHtml(mAbstract, sessionAbstract);
            mAbstract.setVisibility(View.VISIBLE);
            mHasSummaryContent = true;
        } else {
            mAbstract.setVisibility(View.GONE);
        }

        updatePlusOneButton();

        final View requirementsBlock = mRootView.findViewById(R.id.session_requirements_block);
        final String sessionRequirements = cursor.getString(SessionsQuery.REQUIREMENTS);
        if (!TextUtils.isEmpty(sessionRequirements)) {
            UIUtils.setTextMaybeHtml(mRequirements, sessionRequirements);
            requirementsBlock.setVisibility(View.VISIBLE);
            mHasSummaryContent = true;
        } else {
            requirementsBlock.setVisibility(View.GONE);
        }

        // Show empty message when all data is loaded, and nothing to show
        if (mSpeakersCursor && !mHasSummaryContent) {
            mRootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }

        // Compile list of links (I/O live link, submit feedback, and normal links)
        ViewGroup linkContainer = (ViewGroup) mRootView.findViewById(R.id.links_container);
        linkContainer.removeAllViews();

        final Context context = mRootView.getContext();

        List<Pair<Integer, Intent>> links = new ArrayList<Pair<Integer, Intent>>();

        final boolean hasLivestream = !TextUtils.isEmpty(
                cursor.getString(SessionsQuery.LIVESTREAM_URL));
        long currentTimeMillis = UIUtils.getCurrentTime(context);
        if (UIUtils.hasHoneycomb() // Needs Honeycomb+ for the live stream
                && hasLivestream
                && currentTimeMillis > mSessionBlockStart
                && currentTimeMillis <= mSessionBlockEnd) {
            links.add(new Pair<Integer, Intent>(
                    R.string.session_link_livestream,
                    new Intent(Intent.ACTION_VIEW, mSessionUri)
                            .setClass(context, SessionLivestreamActivity.class)));
        }

        // Add session feedback link
        links.add(new Pair<Integer, Intent>(
                R.string.session_feedback_submitlink,
                new Intent(Intent.ACTION_VIEW, mSessionUri, getActivity(), SessionFeedbackActivity.class)
        ));

        for (int i = 0; i < SessionsQuery.LINKS_INDICES.length; i++) {
            final String linkUrl = cursor.getString(SessionsQuery.LINKS_INDICES[i]);
            if (TextUtils.isEmpty(linkUrl)) {
                continue;
            }

            links.add(new Pair<Integer, Intent>(
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
                final Pair<Integer, Intent> link = links.get(i);

                // Create link view
                TextView linkView = (TextView) inflater.inflate(R.layout.list_item_session_link,
                        linkContainer, false);
                linkView.setText(getString(link.first));
                linkView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        fireLinkEvent(link.first);
                        try {
                            startActivity(link.second);
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

        // Show past/present/future and livestream status for this block.
        UIUtils.updateTimeAndLivestreamBlockUI(context,
                mSessionBlockStart, mSessionBlockEnd, hasLivestream,
                null, mSubtitle, subtitle);

        EasyTracker.getTracker().sendView("Session: " + mTitleString);
        LOGD("Tracker", "Session: " + mTitleString);
    }

    private void updatePlusOneButton() {
        if (mPlusOneButton == null) {
            return;
        }

        if (!TextUtils.isEmpty(mUrl)) {
            mPlusOneButton.initialize(mUrl, 0);
            mPlusOneButton.setVisibility(View.VISIBLE);
        } else {
            mPlusOneButton.setVisibility(View.GONE);
        }
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
        mStarMenuItem.setTitle(starred
                ? R.string.description_remove_schedule
                : R.string.description_add_schedule);
        mStarMenuItem.setIcon(starred
                ? R.drawable.ic_action_remove_schedule
                : R.drawable.ic_action_add_schedule);
        mStarred = starred;

        mAddScheduleButton.setChecked(mStarred);
        ImageView iconView = (ImageView) mAddScheduleButton.findViewById(R.id.add_schedule_icon);
        setOrAnimateIconTo(iconView, starred
                ? R.drawable.add_schedule_button_icon_checked
                : R.drawable.add_schedule_button_icon_unchecked,
                allowAnimate && starred);
        TextView textView = (TextView) mAddScheduleButton.findViewById(R.id.add_schedule_text);
        textView.setText(starred
                ? R.string.remove_from_schedule
                : R.string.add_to_schedule);
        mAddScheduleButton.setContentDescription(getString(starred
                ? R.string.remove_from_schedule_desc
                : R.string.add_to_schedule_desc));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setOrAnimateIconTo(final ImageView imageView, final int imageResId,
            boolean animate) {
        if (UIUtils.hasICS() && imageView.getTag() != null) {
            if (imageView.getTag() instanceof Animator) {
                Animator anim = (Animator) imageView.getTag();
                anim.end();
                imageView.setAlpha(1f);
            }
        }

        animate = animate && UIUtils.hasICS();
        if (animate) {
            int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);

            Animator outAnimator = ObjectAnimator.ofFloat(imageView, View.ALPHA, 0f);
            outAnimator.setDuration(duration / 2);
            outAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageView.setImageResource(imageResId);
                }
            });

            AnimatorSet inAnimator = new AnimatorSet();
            outAnimator.setDuration(duration);
            inAnimator.playTogether(
                    ObjectAnimator.ofFloat(imageView, View.ALPHA, 1f),
                    ObjectAnimator.ofFloat(imageView, View.SCALE_X, 0f, 1f),
                    ObjectAnimator.ofFloat(imageView, View.SCALE_Y, 0f, 1f)
            );

            AnimatorSet set = new AnimatorSet();
            set.playSequentially(outAnimator, inAnimator);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageView.setTag(null);
                }
            });
            imageView.setTag(set);
            set.start();
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageResource(imageResId);
                }
            });
        }
    }

    private void setupShareMenuItemDeferred() {
        mDeferredUiOperations.add(new Runnable() {
            @Override
            public void run() {
                new SessionsHelper(getActivity()).tryConfigureShareMenuItem(mShareMenuItem,
                        R.string.share_template, mTitleString, mHashtags, mUrl);
            }
        });
        tryExecuteDeferredUiOperations();
    }

    private void tryExecuteDeferredUiOperations() {
        if (mStarMenuItem != null && mSocialStreamMenuItem != null) {
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

            if (!TextUtils.isEmpty(speakerImageUrl) && mImageLoader != null) {
                mImageLoader.get(UIUtils.getConferenceImageUrl(speakerImageUrl), speakerImageView);
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
        mStarMenuItem = menu.findItem(R.id.menu_star);
        mStarMenuItem.setVisible(false); // functionality taken care of by button
        mSocialStreamMenuItem = menu.findItem(R.id.menu_social_stream);
        mShareMenuItem = menu.findItem(R.id.menu_share);
        tryExecuteDeferredUiOperations();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SessionsHelper helper = new SessionsHelper(getActivity());
        switch (item.getItemId()) {
            case R.id.menu_map:
                EasyTracker.getTracker().sendEvent(
                        "Session", "Map", mTitleString, 0L);
                LOGD("Tracker", "Map: " + mTitleString);

                helper.startMapActivity(mRoomId);
                return true;

            case R.id.menu_star:
                setSessionStarred(!mStarred, true);
                return true;

            case R.id.menu_share:
                // On ICS+ devices, we normally won't reach this as ShareActionProvider will handle
                // sharing.
                helper.shareSession(getActivity(), R.string.share_template, mTitleString,
                        mHashtags, mUrl);
                return true;

            case R.id.menu_social_stream:
                EasyTracker.getTracker().sendEvent(
                        "Session", "Stream", mTitleString, 0L);
                LOGD("Tracker", "Stream: " + mTitleString);

                helper.startSocialStream(mHashtags);
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
        EasyTracker.getTracker().sendEvent("Session", getString(actionId), mTitleString, 0L);
        LOGD("Tracker", getString(actionId) + ": " + mTitleString);
    }

    void setSessionStarred(boolean star, boolean allowAnimate) {
        SessionsHelper helper = new SessionsHelper(getActivity());
        showStarred(star, allowAnimate);
        helper.setSessionStarred(mSessionUri, star, mTitleString);
        EasyTracker.getTracker().sendEvent(
                "Session", star ? "Starred" : "Unstarred", mTitleString, 0L);
        LOGD("Tracker", (star ? "Starred: " : "Unstarred: ") + mTitleString);
    }
    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Sessions} query parameters.
     */
    private interface SessionsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
                ScheduleContract.Sessions.SESSION_LEVEL,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_REQUIREMENTS,
                ScheduleContract.Sessions.SESSION_STARRED,
                ScheduleContract.Sessions.SESSION_HASHTAGS,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_YOUTUBE_URL,
                ScheduleContract.Sessions.SESSION_PDF_URL,
                ScheduleContract.Sessions.SESSION_NOTES_URL,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_URL,
                ScheduleContract.Sessions.SESSION_MODERATOR_URL,
                ScheduleContract.Sessions.ROOM_ID,
                ScheduleContract.Rooms.ROOM_NAME,
        };

        int BLOCK_START = 0;
        int BLOCK_END = 1;
        int LEVEL = 2;
        int TITLE = 3;
        int ABSTRACT = 4;
        int REQUIREMENTS = 5;
        int STARRED = 6;
        int HASHTAGS = 7;
        int URL = 8;
        int YOUTUBE_URL = 9;
        int PDF_URL = 10;
        int NOTES_URL = 11;
        int LIVESTREAM_URL = 12;
        int MODERATOR_URL = 13;
        int ROOM_ID = 14;
        int ROOM_NAME = 15;

        int[] LINKS_INDICES = {
                URL,
                YOUTUBE_URL,
                MODERATOR_URL,
                PDF_URL,
                NOTES_URL,
        };

        int[] LINKS_TITLES = {
                R.string.session_link_main,
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

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == SessionsQuery._TOKEN){
			loader = new CursorLoader(getActivity(), mSessionUri, SessionsQuery.PROJECTION, null,
					null, null);
		} else if (id == SpeakersQuery._TOKEN  && mSessionUri != null){
			Uri speakersUri = ScheduleContract.Sessions.buildSpeakersDirUri(mSessionId);
			loader = new CursorLoader(getActivity(), speakersUri, SpeakersQuery.PROJECTION, null,
					null, null);
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
        } else {
            cursor.close();
        }
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {}
}
