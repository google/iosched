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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.calendar.SessionAlarmService;
import com.google.android.apps.iosched.calendar.SessionCalendarService;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.FractionalTouchDelegate;
import com.google.android.apps.iosched.util.HelpUtils;
import com.google.android.apps.iosched.util.ImageFetcher;
import com.google.android.apps.iosched.util.SessionsHelper;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.api.android.plus.GooglePlus;
import com.google.api.android.plus.PlusOneButton;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that shows detail information for a session, including session title, abstract,
 * time information, speaker photos and bios, etc.
 *
 * <p>This fragment is used in a number of activities, including
 * {@link com.google.android.apps.iosched.ui.phone.SessionDetailActivity},
 * {@link com.google.android.apps.iosched.ui.tablet.SessionsVendorsMultiPaneActivity},
 * {@link com.google.android.apps.iosched.ui.tablet.MapMultiPaneActivity}, etc.
 */
public class SessionDetailFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(SessionDetailFragment.class);

    // Set this boolean extra to true to show a variable height header
    public static final String EXTRA_VARIABLE_HEIGHT_HEADER =
            "com.google.android.iosched.extra.VARIABLE_HEIGHT_HEADER";

    private String mSessionId;
    private Uri mSessionUri;

    private long mSessionBlockStart;
    private long mSessionBlockEnd;
    private String mTitleString;
    private String mHashtags;
    private String mUrl;
    private String mRoomId;
    private String mRoomName;

    private boolean mStarred;
    private boolean mInitStarred;
    private MenuItem mShareMenuItem;
    private MenuItem mStarMenuItem;
    private MenuItem mSocialStreamMenuItem;

    private ViewGroup mRootView;
    private TextView mTitle;
    private TextView mSubtitle;
    private PlusOneButton mPlusOneButton;

    private TextView mAbstract;
    private TextView mRequirements;

    private boolean mSessionCursor = false;
    private boolean mSpeakersCursor = false;
    private boolean mHasSummaryContent = false;
    private boolean mVariableHeightHeader = false;

    private ImageFetcher mImageFetcher;
    private List<Runnable> mDeferredUiOperations = new ArrayList<Runnable>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GooglePlus.initialize(getActivity(), Config.API_KEY, Config.CLIENT_ID);

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

        mImageFetcher = UIUtils.getImageFetcher(getActivity());
        mImageFetcher.setImageFadeIn(false);

        setHasOptionsMenu(true);

        HelpUtils.maybeShowAddToScheduleTutorial(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_session_detail, null);

        mTitle = (TextView) mRootView.findViewById(R.id.session_title);
        mSubtitle = (TextView) mRootView.findViewById(R.id.session_subtitle);

        // Larger target triggers plus one button
        mPlusOneButton = (PlusOneButton) mRootView.findViewById(R.id.plus_one_button);
        final View plusOneParent = mRootView.findViewById(R.id.header_session);
        FractionalTouchDelegate.setupDelegate(plusOneParent, mPlusOneButton,
                new RectF(0.6f, 0f, 1f, 1.0f));

        mAbstract = (TextView) mRootView.findViewById(R.id.session_abstract);
        mRequirements = (TextView) mRootView.findViewById(R.id.session_requirements);

        if (mVariableHeightHeader) {
            View headerView = mRootView.findViewById(R.id.header_session);
            ViewGroup.LayoutParams layoutParams = headerView.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            headerView.setLayoutParams(layoutParams);
        }

        return mRootView;
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mInitStarred != mStarred) {
            // Update Calendar event through the Calendar API on Android 4.0 or new versions.
            if (UIUtils.hasICS()) {
                Intent intent;
                if (mStarred) {
                    // Set up intent to add session to Calendar, if it doesn't exist already.
                    intent = new Intent(SessionCalendarService.ACTION_ADD_SESSION_CALENDAR,
                            mSessionUri);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_BLOCK_START,
                            mSessionBlockStart);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_BLOCK_END,
                            mSessionBlockEnd);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_ROOM, mRoomName);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_TITLE, mTitleString);

                } else {
                    // Set up intent to remove session from Calendar, if exists.
                    intent = new Intent(SessionCalendarService.ACTION_REMOVE_SESSION_CALENDAR,
                            mSessionUri);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_BLOCK_START,
                            mSessionBlockStart);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_BLOCK_END,
                            mSessionBlockEnd);
                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_TITLE, mTitleString);
                }
                intent.setClass(getActivity(), SessionCalendarService.class);
                getActivity().startService(intent);
            }

            if (mStarred && System.currentTimeMillis() < mSessionBlockStart) {
                setupNotification();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    private void setupNotification() {
        // Schedule an alarm that fires a system notification when expires.
        final Context ctx = getActivity();
        Intent scheduleIntent = new Intent(
                SessionAlarmService.ACTION_SCHEDULE_STARRED_BLOCK,
                null, ctx, SessionAlarmService.class);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, mSessionBlockStart);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, mSessionBlockEnd);
        ctx.startService(scheduleIntent);
    }

    /**
     * Handle {@link SessionsQuery} {@link Cursor}.
     */
    private void onSessionQueryComplete(Cursor cursor) {
        mSessionCursor = true;
        if (!cursor.moveToFirst()) {
            return;
        }

        mTitleString = cursor.getString(SessionsQuery.TITLE);

        // Format time block this session occupies
        mSessionBlockStart = cursor.getLong(SessionsQuery.BLOCK_START);
        mSessionBlockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
        mRoomName = cursor.getString(SessionsQuery.ROOM_NAME);
        final String subtitle = UIUtils.formatSessionSubtitle(
                mTitleString, mSessionBlockStart, mSessionBlockEnd, mRoomName, getActivity());

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
        showStarredDeferred(mInitStarred = (cursor.getInt(SessionsQuery.STARRED) != 0));

        final String sessionAbstract = cursor.getString(SessionsQuery.ABSTRACT);
        if (!TextUtils.isEmpty(sessionAbstract)) {
            UIUtils.setTextMaybeHtml(mAbstract, sessionAbstract);
            mAbstract.setVisibility(View.VISIBLE);
            mHasSummaryContent = true;
        } else {
            mAbstract.setVisibility(View.GONE);
        }

        mPlusOneButton.setSize(PlusOneButton.Size.TALL);
        String url = cursor.getString(SessionsQuery.URL);
        if (TextUtils.isEmpty(url)) {
            mPlusOneButton.setVisibility(View.GONE);
        } else {
            mPlusOneButton.setUrl(url);
        }

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

        ViewGroup linksContainer = (ViewGroup) mRootView.findViewById(R.id.links_container);
        linksContainer.removeAllViews();

        LayoutInflater inflater = getLayoutInflater(null);

        boolean hasLinks = false;

        final Context context = mRootView.getContext();

        // Render I/O live link
        final boolean hasLivestream = !TextUtils.isEmpty(
                cursor.getString(SessionsQuery.LIVESTREAM_URL));
        long currentTimeMillis = UIUtils.getCurrentTime(context);
        if (UIUtils.hasHoneycomb() // Needs Honeycomb+ for the live stream
                && hasLivestream
                && currentTimeMillis > mSessionBlockStart
                && currentTimeMillis <= mSessionBlockEnd) {
            hasLinks = true;

            // Create the link item
            ViewGroup linkContainer = (ViewGroup)
                    inflater.inflate(R.layout.list_item_session_link, linksContainer, false);
            ((TextView) linkContainer.findViewById(R.id.link_text)).setText(
                    R.string.session_link_livestream);
            linkContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fireLinkEvent(R.string.session_link_livestream);
                    Intent livestreamIntent = new Intent(Intent.ACTION_VIEW, mSessionUri);
                    livestreamIntent.setClass(context, SessionLivestreamActivity.class);
                    startActivity(livestreamIntent);
                }
            });

            linksContainer.addView(linkContainer);
        }

        // Render normal links
        for (int i = 0; i < SessionsQuery.LINKS_INDICES.length; i++) {
            final String linkUrl = cursor.getString(SessionsQuery.LINKS_INDICES[i]);
            if (!TextUtils.isEmpty(linkUrl)) {
                hasLinks = true;

                // Create the link item
                ViewGroup linkContainer = (ViewGroup)
                        inflater.inflate(R.layout.list_item_session_link, linksContainer, false);
                ((TextView) linkContainer.findViewById(R.id.link_text)).setText(
                        SessionsQuery.LINKS_TITLES[i]);
                final int linkTitleIndex = i;
                linkContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        fireLinkEvent(SessionsQuery.LINKS_TITLES[linkTitleIndex]);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl));
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        UIUtils.safeOpenLink(context, intent);
                    }
                });

                linksContainer.addView(linkContainer);
            }
        }

        // Show past/present/future and livestream status for this block.
        UIUtils.updateTimeAndLivestreamBlockUI(context,
                mSessionBlockStart, mSessionBlockEnd, hasLivestream,
                null, null, mSubtitle, subtitle);
        mRootView.findViewById(R.id.session_links_block)
                .setVisibility(hasLinks ? View.VISIBLE : View.GONE);
        
        EasyTracker.getTracker().trackView("Session: " + mTitleString);
        LOGD("Tracker", "Session: " + mTitleString);
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

    private void showStarredDeferred(final boolean starred) {
        mDeferredUiOperations.add(new Runnable() {
            @Override
            public void run() {
                showStarred(starred);
            }
        });
        tryExecuteDeferredUiOperations();
    }

    private void showStarred(boolean starred) {
        mStarMenuItem.setTitle(starred
                ? R.string.description_remove_schedule
                : R.string.description_add_schedule);
        mStarMenuItem.setIcon(starred
                ? R.drawable.ic_action_remove_schedule
                : R.drawable.ic_action_add_schedule);
        mStarred = starred;
    }

    private void setupShareMenuItemDeferred() {
        mDeferredUiOperations.add(new Runnable() {
            @Override
            public void run() {
                new SessionsHelper(getActivity())
                        .tryConfigureShareMenuItem(mShareMenuItem, R.string.share_template,
                                mTitleString, mHashtags, mUrl);
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
        // TODO: remove existing speakers from layout, since this cursor might be from a data change
        final ViewGroup speakersGroup = (ViewGroup)
                mRootView.findViewById(R.id.session_speakers_block);
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

            if (!TextUtils.isEmpty(speakerImageUrl)) {
                mImageFetcher.loadThumbnailImage(speakerImageUrl, speakerImageView,
                        R.drawable.person_image_empty);
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
                        UIUtils.safeOpenLink(getActivity(), speakerProfileIntent);
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
        mSocialStreamMenuItem = menu.findItem(R.id.menu_social_stream);
        mShareMenuItem = menu.findItem(R.id.menu_share);
        tryExecuteDeferredUiOperations();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SessionsHelper helper = new SessionsHelper(getActivity());
        switch (item.getItemId()) {
            case R.id.menu_map:                
                EasyTracker.getTracker().trackEvent(
                        "Session", "Map", mTitleString, 0L);
                LOGD("Tracker", "Map: " + mTitleString);
                
                helper.startMapActivity(mRoomId);
                return true;

            case R.id.menu_star:
                boolean star = !mStarred;
                showStarred(star);
                helper.setSessionStarred(mSessionUri, star, mTitleString);
                Toast.makeText(
                        getActivity(),
                        getResources().getQuantityString(star
                                ? R.plurals.toast_added_to_schedule
                                : R.plurals.toast_removed_from_schedule, 1, 1),
                        Toast.LENGTH_SHORT).show();
                
                EasyTracker.getTracker().trackEvent(
                        "Session", star ? "Starred" : "Unstarred", mTitleString, 0L);
                LOGD("Tracker", (star ? "Starred: " : "Unstarred: ") + mTitleString);

                return true;

            case R.id.menu_share:
                // On ICS+ devices, we normally won't reach this as ShareActionProvider will handle
                // sharing.
                helper.shareSession(getActivity(), R.string.share_template, mTitleString,
                        mHashtags, mUrl);
                return true;

            case R.id.menu_social_stream:
                EasyTracker.getTracker().trackEvent(
                        "Session", "Stream", mTitleString, 0L);
                LOGD("Tracker", "Stream: " + mTitleString);

                helper.startSocialStream(UIUtils.getSessionHashtagsString(mHashtags));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Event structure:
     * Category -> "Session Details"
     * Action -> Link Text
     * Label -> Session's Title
     * Value -> 0.
     */
    public void fireLinkEvent(int actionId) {
        EasyTracker.getTracker().trackEvent(
                "Session", getActivity().getString(actionId), mTitleString, 0L);
        LOGD("Tracker", getActivity().getString(actionId) + ": " + mTitleString);
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
        if (getActivity() == null) {
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
        int ROOM_ID = 13;
        int ROOM_NAME = 14;

        int[] LINKS_INDICES = {
                URL,
                YOUTUBE_URL,
                PDF_URL,
                NOTES_URL,
        };

        int[] LINKS_TITLES = {
                R.string.session_link_main,
                R.string.session_link_youtube,
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
}
