/*
 * Copyright 2011 Google Inc.
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

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.ActivityHelper;
import com.google.android.apps.iosched.util.AnalyticsUtils;
import com.google.android.apps.iosched.util.BitmapUtils;
import com.google.android.apps.iosched.util.CatchNotesHelper;
import com.google.android.apps.iosched.util.FractionalTouchDelegate;
import com.google.android.apps.iosched.util.NotifyingAsyncQueryHandler;
import com.google.android.apps.iosched.util.UIUtils;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

/**
 * A fragment that shows detail information for a session, including session title, abstract,
 * time information, speaker photos and bios, etc.
 */
public class SessionDetailFragment extends Fragment implements
        NotifyingAsyncQueryHandler.AsyncQueryListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "SessionDetailFragment";

    /**
     * Since sessions can belong tracks, the parent activity can send this extra specifying a
     * track URI that should be used for coloring the title-bar.
     */
    public static final String EXTRA_TRACK = "com.google.android.iosched.extra.TRACK";

    private static final String TAG_SUMMARY = "summary";
    private static final String TAG_NOTES = "notes";
    private static final String TAG_LINKS = "links";

    private static StyleSpan sBoldSpan = new StyleSpan(Typeface.BOLD);

    private String mSessionId;
    private Uri mSessionUri;
    private Uri mTrackUri;

    private String mTitleString;
    private String mHashtag;
    private String mUrl;
    private TextView mTagDisplay;
    private String mRoomId;

    private ViewGroup mRootView;
    private TabHost mTabHost;
    private TextView mTitle;
    private TextView mSubtitle;
    private CompoundButton mStarred;
    
    private TextView mAbstract;
    private TextView mRequirements;

    private NotifyingAsyncQueryHandler mHandler;

    private boolean mSessionCursor = false;
    private boolean mSpeakersCursor = false;
    private boolean mHasSummaryContent = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mSessionUri = intent.getData();
        mTrackUri = resolveTrackUri(intent);

        if (mSessionUri == null) {
            return;
        }

        mSessionId = ScheduleContract.Sessions.getSessionId(mSessionUri);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNotesTab();

        // Start listening for time updates to adjust "now" bar. TIME_TICK is
        // triggered once per minute, which is how we move the bar over time.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mPackageChangesReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mPackageChangesReceiver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mSessionUri == null) {
            return;
        }

        // Start background queries to load session and track details
        final Uri speakersUri = ScheduleContract.Sessions.buildSpeakersDirUri(mSessionId);

        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
        mHandler.startQuery(SessionsQuery._TOKEN, mSessionUri, SessionsQuery.PROJECTION);
        mHandler.startQuery(TracksQuery._TOKEN, mTrackUri, TracksQuery.PROJECTION);
        mHandler.startQuery(SpeakersQuery._TOKEN, speakersUri, SpeakersQuery.PROJECTION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_session_detail, null);
        mTabHost = (TabHost) mRootView.findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mTitle = (TextView) mRootView.findViewById(R.id.session_title);
        mSubtitle = (TextView) mRootView.findViewById(R.id.session_subtitle);
        mStarred = (CompoundButton) mRootView.findViewById(R.id.star_button);

        mStarred.setFocusable(true);
        mStarred.setClickable(true);

        // Larger target triggers star toggle
        final View starParent = mRootView.findViewById(R.id.header_session);
        FractionalTouchDelegate.setupDelegate(starParent, mStarred, new RectF(0.6f, 0f, 1f, 0.8f));

        mAbstract = (TextView) mRootView.findViewById(R.id.session_abstract);
        mRequirements = (TextView) mRootView.findViewById(R.id.session_requirements);

        setupSummaryTab();
        setupNotesTab();
        setupLinksTab();

        return mRootView;
    }

    /**
     * Build and add "summary" tab.
     */
    private void setupSummaryTab() {
        // Summary content comes from existing layout
        mTabHost.addTab(mTabHost.newTabSpec(TAG_SUMMARY)
                .setIndicator(buildIndicator(R.string.session_summary))
                .setContent(R.id.tab_session_summary));
    }

    /**
     * Build a {@link View} to be used as a tab indicator, setting the requested string resource as
     * its label.
     *
     * @param textRes
     * @return View
     */
    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getActivity().getLayoutInflater()
                .inflate(R.layout.tab_indicator,
                        (ViewGroup) mRootView.findViewById(android.R.id.tabs), false);
        indicator.setText(textRes);
        return indicator;
    }

    /**
     * Derive {@link com.google.android.apps.iosched.provider.ScheduleContract.Tracks#CONTENT_ITEM_TYPE}
     * {@link Uri} based on incoming {@link Intent}, using
     * {@link #EXTRA_TRACK} when set.
     * @param intent
     * @return Uri
     */
    private Uri resolveTrackUri(Intent intent) {
        final Uri trackUri = intent.getParcelableExtra(EXTRA_TRACK);
        if (trackUri != null) {
            return trackUri;
        } else {
            return ScheduleContract.Sessions.buildTracksDirUri(mSessionId);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (token == SessionsQuery._TOKEN) {
            onSessionQueryComplete(cursor);
        } else if (token == TracksQuery._TOKEN) {
            onTrackQueryComplete(cursor);
        } else if (token == SpeakersQuery._TOKEN) {
            onSpeakersQueryComplete(cursor);
        } else {
            cursor.close();
        }
    }

    /**
     * Handle {@link SessionsQuery} {@link Cursor}.
     */
    private void onSessionQueryComplete(Cursor cursor) {
        try {
            mSessionCursor = true;
            if (!cursor.moveToFirst()) {
                return;
            }

            // Format time block this session occupies
            final long blockStart = cursor.getLong(SessionsQuery.BLOCK_START);
            final long blockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
            final String roomName = cursor.getString(SessionsQuery.ROOM_NAME);
            final String subtitle = UIUtils.formatSessionSubtitle(blockStart,
                    blockEnd, roomName, getActivity());

            mTitleString = cursor.getString(SessionsQuery.TITLE);
            mTitle.setText(mTitleString);
            mSubtitle.setText(subtitle);

            mUrl = cursor.getString(SessionsQuery.URL);
            if (TextUtils.isEmpty(mUrl)) {
                mUrl = "";
            }

            mHashtag = cursor.getString(SessionsQuery.HASHTAG);
            mTagDisplay = (TextView) mRootView.findViewById(R.id.session_tags_button);
            if (!TextUtils.isEmpty(mHashtag)) {
                // Create the button text
                SpannableStringBuilder sb = new SpannableStringBuilder();
                sb.append(getString(R.string.tag_stream) + " ");
                int boldStart = sb.length();
                sb.append(getHashtagsString());
                sb.setSpan(sBoldSpan, boldStart, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                mTagDisplay.setText(sb);

                mTagDisplay.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), TagStreamActivity.class);
                        intent.putExtra(TagStreamFragment.EXTRA_QUERY, getHashtagsString());
                        startActivity(intent);
                    }
                });
            } else {
                mTagDisplay.setVisibility(View.GONE);
            }

            mRoomId = cursor.getString(SessionsQuery.ROOM_ID);

            // Unregister around setting checked state to avoid triggering
            // listener since change isn't user generated.
            mStarred.setOnCheckedChangeListener(null);
            mStarred.setChecked(cursor.getInt(SessionsQuery.STARRED) != 0);
            mStarred.setOnCheckedChangeListener(this);

            final String sessionAbstract = cursor.getString(SessionsQuery.ABSTRACT);
            if (!TextUtils.isEmpty(sessionAbstract)) {
                UIUtils.setTextMaybeHtml(mAbstract, sessionAbstract);
                mAbstract.setVisibility(View.VISIBLE);
                mHasSummaryContent = true;
            } else {
                mAbstract.setVisibility(View.GONE);
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

            AnalyticsUtils.getInstance(getActivity()).trackPageView("/Sessions/" + mTitleString);

            updateLinksTab(cursor);
            updateNotesTab();

        } finally {
            cursor.close();
        }
    }

    /**
     * Handle {@link TracksQuery} {@link Cursor}.
     */
    private void onTrackQueryComplete(Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) {
                return;
            }

            // Use found track to build title-bar
            ActivityHelper activityHelper = ((BaseActivity) getActivity()).getActivityHelper();
            activityHelper.setActionBarTitle(cursor.getString(TracksQuery.TRACK_NAME));
            activityHelper.setActionBarColor(cursor.getInt(TracksQuery.TRACK_COLOR));
        } finally {
            cursor.close();
        }
    }

    private void onSpeakersQueryComplete(Cursor cursor) {
        try {
            mSpeakersCursor = true;
            // TODO: remove any existing speakers from layout, since this cursor
            // might be from a data change notification.
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
                final ImageView speakerImgView = (ImageView) speakerView
                        .findViewById(R.id.speaker_image);
                final TextView speakerUrlView = (TextView) speakerView
                        .findViewById(R.id.speaker_url);
                final TextView speakerAbstractView = (TextView) speakerView
                        .findViewById(R.id.speaker_abstract);

                if (!TextUtils.isEmpty(speakerImageUrl)) {
                    BitmapUtils.fetchImage(getActivity(), speakerImageUrl, null, null,
                            new BitmapUtils.OnFetchCompleteListener() {
                                public void onFetchComplete(Object cookie, Bitmap result) {
                                    if (result != null) {
                                        speakerImgView.setImageBitmap(result);
                                    }
                                }
                            });
                }

                speakerHeaderView.setText(speakerHeader);
                UIUtils.setTextMaybeHtml(speakerAbstractView, speakerAbstract);

                if (!TextUtils.isEmpty(speakerUrl)) {
                    UIUtils.setTextMaybeHtml(speakerUrlView, speakerUrl);
                    speakerUrlView.setVisibility(View.VISIBLE);
                } else {
                    speakerUrlView.setVisibility(View.GONE);
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
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.session_detail_menu_items, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String shareString;
        final Intent intent;

        switch (item.getItemId()) {
            case R.id.menu_map:
                intent = new Intent(getActivity().getApplicationContext(),
                        UIUtils.getMapActivityClass(getActivity()));
                intent.putExtra(MapFragment.EXTRA_ROOM, mRoomId);
                startActivity(intent);
                return true;

            case R.id.menu_share:
                // TODO: consider bringing in shortlink to session
                shareString = getString(R.string.share_template, mTitleString, getHashtagsString(),
                        mUrl);
                intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, shareString);
                startActivity(Intent.createChooser(intent, getText(R.string.title_share)));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle toggling of starred checkbox.
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final ContentValues values = new ContentValues();
        values.put(ScheduleContract.Sessions.SESSION_STARRED, isChecked ? 1 : 0);
        mHandler.startUpdate(mSessionUri, values);

        // Because change listener is set to null during initialization, these won't fire on
        // pageview.
        AnalyticsUtils.getInstance(getActivity()).trackEvent(
                "Sandbox", isChecked ? "Starred" : "Unstarred", mTitleString, 0);
    }

    /**
     * Build and add "notes" tab.
     */
    private void setupNotesTab() {
        // Make powered-by clickable
        ((TextView) mRootView.findViewById(R.id.notes_powered_by)).setMovementMethod(
                LinkMovementMethod.getInstance());

        // Setup tab
        mTabHost.addTab(mTabHost.newTabSpec(TAG_NOTES)
                .setIndicator(buildIndicator(R.string.session_notes))
                .setContent(R.id.tab_session_notes));
    }

    /*
     * Event structure:
     * Category -> "Session Details"
     * Action -> "Create Note", "View Note", etc
     * Label -> Session's Title
     * Value -> 0.
     */
    public void fireNotesEvent(int actionId) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent(
                "Session Details", getActivity().getString(actionId), mTitleString, 0);
    }

    /*
     * Event structure:
     * Category -> "Session Details"
     * Action -> Link Text
     * Label -> Session's Title
     * Value -> 0.
     */
    public void fireLinkEvent(int actionId) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent(
                "Link Details", getActivity().getString(actionId), mTitleString, 0);
    }

    private void updateNotesTab() {
        final CatchNotesHelper helper = new CatchNotesHelper(getActivity());
        final boolean notesInstalled = helper.isNotesInstalledAndMinimumVersion();

        final Intent marketIntent = helper.notesMarketIntent();
        final Intent newIntent = helper.createNoteIntent(
                getString(R.string.note_template, mTitleString, getHashtagsString()));
        
        final Intent viewIntent = helper.viewNotesIntent(getHashtagsString());

        // Set icons and click listeners
        ((ImageView) mRootView.findViewById(R.id.notes_catch_market_icon)).setImageDrawable(
                UIUtils.getIconForIntent(getActivity(), marketIntent));
        ((ImageView) mRootView.findViewById(R.id.notes_catch_new_icon)).setImageDrawable(
                UIUtils.getIconForIntent(getActivity(), newIntent));
        ((ImageView) mRootView.findViewById(R.id.notes_catch_view_icon)).setImageDrawable(
                UIUtils.getIconForIntent(getActivity(), viewIntent));

        // Set click listeners
        mRootView.findViewById(R.id.notes_catch_market_link).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        startActivity(marketIntent);
                        fireNotesEvent(R.string.notes_catch_market_title);
                    }
                });

        mRootView.findViewById(R.id.notes_catch_new_link).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        startActivity(newIntent);
                        fireNotesEvent(R.string.notes_catch_new_title);
                    }
                });

        mRootView.findViewById(R.id.notes_catch_view_link).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        startActivity(viewIntent);
                        fireNotesEvent(R.string.notes_catch_view_title);
                    }
                });

        // Show/hide elements
        mRootView.findViewById(R.id.notes_catch_market_link).setVisibility(
                notesInstalled ? View.GONE : View.VISIBLE);
        mRootView.findViewById(R.id.notes_catch_market_separator).setVisibility(
                notesInstalled ? View.GONE : View.VISIBLE);

        mRootView.findViewById(R.id.notes_catch_new_link).setVisibility(
                !notesInstalled ? View.GONE : View.VISIBLE);
        mRootView.findViewById(R.id.notes_catch_new_separator).setVisibility(
                !notesInstalled ? View.GONE : View.VISIBLE);

        mRootView.findViewById(R.id.notes_catch_view_link).setVisibility(
                !notesInstalled ? View.GONE : View.VISIBLE);
        mRootView.findViewById(R.id.notes_catch_view_separator).setVisibility(
                !notesInstalled ? View.GONE : View.VISIBLE);
    }

    /**
     * Build and add "summary" tab.
     */
    private void setupLinksTab() {
        // Summary content comes from existing layout
        mTabHost.addTab(mTabHost.newTabSpec(TAG_LINKS)
                .setIndicator(buildIndicator(R.string.session_links))
                .setContent(R.id.tab_session_links));
    }

    private void updateLinksTab(Cursor cursor) {
        ViewGroup container = (ViewGroup) mRootView.findViewById(R.id.links_container);

        // Remove all views but the 'empty' view
        int childCount = container.getChildCount();
        if (childCount > 1) {
            container.removeViews(1, childCount - 1);
        }

        LayoutInflater inflater = getLayoutInflater(null);

        boolean hasLinks = false;
        for (int i = 0; i < SessionsQuery.LINKS_INDICES.length; i++) {
            final String url = cursor.getString(SessionsQuery.LINKS_INDICES[i]);
            if (!TextUtils.isEmpty(url)) {
                hasLinks = true;
                ViewGroup linkContainer = (ViewGroup)
                        inflater.inflate(R.layout.list_item_session_link, container, false);
                ((TextView) linkContainer.findViewById(R.id.link_text)).setText(
                        SessionsQuery.LINKS_TITLES[i]);
                final int linkTitleIndex = i;
                linkContainer.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        fireLinkEvent(SessionsQuery.LINKS_TITLES[linkTitleIndex]);
                    	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        startActivity(intent);
                        
                    }
                });

                container.addView(linkContainer);

                // Create separator
                View separatorView = new ImageView(getActivity());
                separatorView.setLayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                separatorView.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
                container.addView(separatorView);
            }
        }

        container.findViewById(R.id.empty_links).setVisibility(hasLinks ? View.GONE : View.VISIBLE);
    }

    private String getHashtagsString() {
        if (!TextUtils.isEmpty(mHashtag)) {
            return TagStreamFragment.CONFERENCE_HASHTAG + " #" + mHashtag;
        } else {
            return TagStreamFragment.CONFERENCE_HASHTAG;
        }
    }

    private BroadcastReceiver mPackageChangesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNotesTab();
        }
    };
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
                ScheduleContract.Sessions.SESSION_HASHTAG,
                ScheduleContract.Sessions.SESSION_SLUG,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_MODERATOR_URL,
                ScheduleContract.Sessions.SESSION_YOUTUBE_URL,
                ScheduleContract.Sessions.SESSION_PDF_URL,
                ScheduleContract.Sessions.SESSION_FEEDBACK_URL,
                ScheduleContract.Sessions.SESSION_NOTES_URL,
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
        int HASHTAG = 7;
        int SLUG = 8;
        int URL = 9;
        int MODERATOR_URL = 10;
        int YOUTUBE_URL = 11;
        int PDF_URL = 12;
        int FEEDBACK_URL = 13;
        int NOTES_URL = 14;
        int ROOM_ID = 15;
        int ROOM_NAME = 16;

        int[] LINKS_INDICES = {
                URL,
                MODERATOR_URL,
                YOUTUBE_URL,
                PDF_URL,
                FEEDBACK_URL,
                NOTES_URL,
        };

        int[] LINKS_TITLES = {
                R.string.session_link_main,
                R.string.session_link_moderator,
                R.string.session_link_youtube,
                R.string.session_link_pdf,
                R.string.session_link_feedback,
                R.string.session_link_notes,
        };
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Tracks} query parameters.
     */
    private interface TracksQuery {
        int _TOKEN = 0x2;

        String[] PROJECTION = {
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_COLOR,
        };

        int TRACK_NAME = 0;
        int TRACK_COLOR = 1;
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
