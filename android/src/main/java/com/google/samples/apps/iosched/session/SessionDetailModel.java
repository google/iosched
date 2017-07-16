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

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Pair;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.ModelWithLoaderManager;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.archframework.UserActionEnum;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.service.SessionAlarmService;
import com.google.samples.apps.iosched.service.SessionCalendarService;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ExtendedSessionHelper;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class SessionDetailModel
        extends ModelWithLoaderManager<SessionDetailModel.SessionDetailQueryEnum,
        SessionDetailModel.SessionDetailUserActionEnum> {

    protected final static String TAG = makeLogTag(SessionDetailModel.class);

    /**
     * The cursor fields for the links. The corresponding resource ids for the links descriptions
     * are in  {@link #LINKS_DESCRIPTION_RESOURCE_IDS}.
     */
    private static final String[] LINKS_CURSOR_FIELDS = {
            ScheduleContract.Sessions.SESSION_YOUTUBE_URL,
            ScheduleContract.Sessions.SESSION_MODERATOR_URL,
            ScheduleContract.Sessions.SESSION_PDF_URL,
            ScheduleContract.Sessions.SESSION_NOTES_URL
    };

    /**
     * The resource ids for the links descriptions. The corresponding cursor fields for the links
     * are in {@link #LINKS_CURSOR_FIELDS}.
     */
    private static final int[] LINKS_DESCRIPTION_RESOURCE_IDS = {
            R.string.session_link_youtube,
            R.string.session_link_moderator,
            R.string.session_link_pdf,
            R.string.session_link_notes,
    };

    private final Context mContext;

    private final SessionsHelper mSessionsHelper;

    private String mSessionId;

    private Uri mSessionUri;

    private boolean mSessionLoaded = false;

    private String mTitle;

    private String mSubtitle;

    private int mSessionColor;

    private String mMainTag;

    private boolean mInSchedule;

    private boolean mInScheduleWhenSessionFirstLoaded;

    private boolean mIsKeynote;

    private long mSessionStart;

    private long mSessionEnd;

    private String mSessionAbstract;

    private String mHashTag;

    private String mUrl = "";

    private String mRoomId;

    private String mRoomName;

    private String mTagsString;

    private String mLiveStreamId;

    private String mYouTubeUrl;

    private String mPhotoUrl;

    private boolean mHasLiveStream = false;

    private boolean mLiveStreamVideoWatched = false;

    private boolean mHasFeedback = false;

    private String mRequirements;

    private String mSpeakersNames;

    private TagMetadata mTagMetadata;

    /**
     * Holds a list of links for the session. The first element of the {@code Pair} is the resource
     * id for the string describing the link, the second is the {@code Intent} to launch when
     * selecting the link.
     */
    private List<Pair<Integer, Intent>> mLinks = new ArrayList<Pair<Integer, Intent>>();

    private List<Speaker> mSpeakers = new ArrayList<Speaker>();

    private StringBuilder mBuffer = new StringBuilder();

    public SessionDetailModel(Uri sessionUri, Context context, SessionsHelper sessionsHelper,
            LoaderManager loaderManager) {
        super(SessionDetailQueryEnum.values(), SessionDetailUserActionEnum.values(), loaderManager);
        mContext = context;
        mSessionsHelper = sessionsHelper;
        mSessionUri = sessionUri;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public String getSessionTitle() {
        return mTitle;
    }

    public String getSessionSubtitle() {
        return mSubtitle;
    }

    public String getSessionUrl() {
        return mUrl;
    }

    public String getRoomId() {
        return mRoomId;
    }

    public String getLiveStreamId() {
        return mLiveStreamId;
    }

    public String getYouTubeUrl() {
        return mYouTubeUrl;
    }

    public int getSessionColor() {
        return mSessionColor;
    }

    public boolean isSessionTrackColorAvailable() {
        return mTagMetadata != null && mMainTag != null;
    }

    public int getSessionTrackColor() {
        if (isSessionTrackColorAvailable()) {
            final TagMetadata.Tag tag = mTagMetadata.getTag(mMainTag);
            if (tag != null) {
                return tag.getColor();
            }
        }
        // else default to the session color which is defaulted to the theme primary color
        return mSessionColor;
    }

    public String getSessionAbstract() {
        return mSessionAbstract;
    }

    public boolean getLiveStreamVideoWatched() {
        return mLiveStreamVideoWatched;
    }

    public boolean isSessionOngoing() {
        long currentTimeMillis = TimeUtils.getCurrentTime(mContext);
        return currentTimeMillis > mSessionStart && currentTimeMillis <= mSessionEnd;
    }

    /**
     * Live stream should be shown if url is available and the session will start in no more than 10
     * minutes, or is ongoing or has ended.
     */
    public boolean showLiveStream() {
        if (!hasLiveStream()) {
            return false;
        }
        long currentTimeMillis = TimeUtils.getCurrentTime(mContext);
        return currentTimeMillis >
                mSessionStart - SessionDetailConstants.LIVESTREAM_BEFORE_SESSION_START_MS;
    }

    public boolean hasSessionStarted() {
        long currentTimeMillis = TimeUtils.getCurrentTime(mContext);
        return currentTimeMillis > mSessionStart;
    }

    public boolean hasSessionEnded() {
        long currentTimeMillis = TimeUtils.getCurrentTime(mContext);
        return currentTimeMillis > mSessionEnd;
    }

    /**
     * Returns the number of minutes, rounded down, since session has started, or 0 if not started
     * yet.
     */
    public long minutesSinceSessionStarted() {
        if (!hasSessionStarted()) {
            return 0l;
        } else {
            long currentTimeMillis = TimeUtils.getCurrentTime(mContext);
            // Rounded down number of minutes.
            return (currentTimeMillis - mSessionStart) / 60000;
        }
    }

    /**
     * Returns the number of minutes, rounded up, until session stars, or 0 if already started.
     */
    public long minutesUntilSessionStarts() {
        if (hasSessionStarted()) {
            return 0l;
        } else {
            long currentTimeMillis = TimeUtils.getCurrentTime(mContext);
            int minutes = (int) ((mSessionStart - currentTimeMillis) / 60000);
            // Rounded up number of minutes.
            return minutes * 60000 < (mSessionStart - currentTimeMillis) ? minutes + 1 : minutes;
        }
    }

    public long minutesUntilSessionEnds() {
        if(hasSessionEnded()) {
            // If session has ended, return 0 minutes until end of session.
            return 0l;
        } else {
            long currentTimeMillis = TimeUtils.getCurrentTime(mContext);
            int minutes = (int) ((mSessionEnd - currentTimeMillis) / 60000);
            // Rounded up number of minutes.
            return minutes * 60000 < (mSessionEnd - currentTimeMillis) ? minutes + 1 : minutes;
        }
    }

    public boolean shouldShowExtendedSessionLink() {
        // If display of link is conditional, place conditions here.
        // For instance if it should only be shown during a session, use isSessionOngoing().
        return ExtendedSessionHelper.shouldShowExtendedSessionLink();
    }

    public boolean isSessionReadyForFeedback() {
        long currentTimeMillis = TimeUtils.getCurrentTime(mContext);
        return currentTimeMillis
                > mSessionEnd - SessionDetailConstants.FEEDBACK_MILLIS_BEFORE_SESSION_END_MS;
    }

    public boolean hasLiveStream() {
        return mHasLiveStream || (!TextUtils.isEmpty(mYouTubeUrl) && !mYouTubeUrl.equals("null"));
    }

    /**
     * Show header image if it has a photo url and it is keynote session, or has a youTube
     * url or has a live stream for a session that is about to start, is ongoing, or has ended.
     */
    public boolean shouldShowHeaderImage() {
        boolean hasYouTubeUrl = !TextUtils.isEmpty(mYouTubeUrl) && !mYouTubeUrl.equals("null");
        return hasPhotoUrl() && (isKeynote() || hasYouTubeUrl || showLiveStream());
    }

    public boolean isInSchedule() {
        return mInSchedule;
    }

    public boolean isInScheduleWhenSessionFirstLoaded() {
        return mInScheduleWhenSessionFirstLoaded;
    }

    public boolean isKeynote() {
        return mIsKeynote;
    }

    public boolean hasFeedback() {
        return mHasFeedback;
    }

    public boolean hasPhotoUrl() {
        return !TextUtils.isEmpty(mPhotoUrl);
    }

    public String getPhotoUrl() {
        return mPhotoUrl;
    }

    public String getRequirements() {
        return mRequirements;
    }

    public String getHashTag() {
        return mHashTag;
    }

    public TagMetadata getTagMetadata() {
        return mTagMetadata;
    }

    public String getTagsString() {
        return mTagsString;
    }

    public List<Pair<Integer, Intent>> getLinks() {
        return mLinks;
    }

    public List<Speaker> getSpeakers() {
        return mSpeakers;
    }

    public boolean hasSummaryContent() {
        return !TextUtils.isEmpty(mSessionAbstract) || !TextUtils.isEmpty(mRequirements);
    }

    @Override
    public boolean readDataFromCursor(Cursor cursor, SessionDetailQueryEnum query) {
        boolean success = false;

        if (cursor != null && cursor.moveToFirst()) {

            if (SessionDetailQueryEnum.SESSIONS == query) {
                readDataFromSessionCursor(cursor);
                mSessionLoaded = true;
                success = true;
            } else if (SessionDetailQueryEnum.TAG_METADATA == query) {
                readDataFromTagMetadataCursor(cursor);
                success = true;
            } else if (SessionDetailQueryEnum.FEEDBACK == query) {
                readDataFromFeedbackCursor(cursor);
                success = true;
            } else if (SessionDetailQueryEnum.SPEAKERS == query) {
                readDataFromSpeakersCursor(cursor);
                success = true;
            } else if (SessionDetailQueryEnum.MY_VIEWED_VIDEOS == query) {
                readDataFromMyViewedVideosCursor(cursor);
                success = true;
            }
        }

        return success;
    }

    private void readDataFromMyViewedVideosCursor(Cursor cursor) {
        String videoID = cursor.getString(cursor.getColumnIndex(
                ScheduleContract.MyViewedVideos.VIDEO_ID));
        if (videoID != null && videoID.equals(mLiveStreamId)) {
            mLiveStreamVideoWatched = true;
        }
    }

    private void readDataFromSessionCursor(Cursor cursor) {
        mTitle = cursor.getString(cursor.getColumnIndex(
                ScheduleContract.Sessions.SESSION_TITLE));

        mInSchedule = cursor.getInt(cursor.getColumnIndex(
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE)) != 0;
        if (!mSessionLoaded) {
            mInScheduleWhenSessionFirstLoaded = mInSchedule;
        }
        mTagsString = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_TAGS));
        mIsKeynote = mTagsString != null && mTagsString.contains(Config.Tags.SPECIAL_KEYNOTE);

        mSessionColor = cursor.getInt(
                cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_COLOR));
        if (mSessionColor == 0) {
            mSessionColor = ContextCompat.getColor(mContext, R.color.default_session_color);
        } else {
            mSessionColor = UIUtils.setColorOpaque(mSessionColor);
        }

        mMainTag = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_MAIN_TAG));

        mLiveStreamId = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_LIVESTREAM_ID));

        mHasLiveStream = !TextUtils.isEmpty(mLiveStreamId);

        mYouTubeUrl = cursor.getString(cursor.getColumnIndex(
                ScheduleContract.Sessions.SESSION_YOUTUBE_URL));

        mSessionStart = cursor
                .getLong(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_START));
        mSessionEnd = cursor.getLong(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_END));

        mRoomName = cursor.getString(cursor.getColumnIndex(ScheduleContract.Sessions.ROOM_NAME));
        mRoomId = cursor.getString(cursor.getColumnIndex(ScheduleContract.Sessions.ROOM_ID));

        mHashTag = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_HASHTAG));

        mPhotoUrl =
                cursor.getString(
                        cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_PHOTO_URL));
        mUrl = cursor.getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_URL));

        mSessionAbstract = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_ABSTRACT));

        mSpeakersNames = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_SPEAKER_NAMES));

        mRequirements = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_REQUIREMENTS));

        formatSubtitle();

        buildLinks(cursor);
    }

    @VisibleForTesting
    public void formatSubtitle() {
        mSubtitle = UIUtils.formatSessionSubtitle(
                mSessionStart, mSessionEnd, mRoomName, mBuffer, mContext);
        if (mHasLiveStream) {
            mSubtitle += " " + UIUtils.getLiveBadgeText(mContext, mSessionStart, mSessionEnd);
        }
    }

    @VisibleForTesting
    public String getExtendedSessionUrl() {
        return ExtendedSessionHelper.getExtendedSessionUrl(this);
    }

    private void buildLinks(Cursor cursor) {
        mLinks.clear();

        if (hasLiveStream() && isSessionOngoing()) {
            mLinks.add(new Pair<Integer, Intent>(
                    R.string.session_link_livestream,
                    getWatchLiveIntent()));
        }

        if (!hasFeedback() && isSessionReadyForFeedback()) {
            mLinks.add(new Pair<Integer, Intent>(
                    R.string.session_feedback_submitlink,
                    getFeedbackIntent()
            ));
        }

        for (int i = 0; i < LINKS_CURSOR_FIELDS.length; i++) {
            final String linkUrl = cursor.getString(cursor.getColumnIndex(LINKS_CURSOR_FIELDS[i]));
            if (TextUtils.isEmpty(linkUrl)) {
                continue;
            }

            mLinks.add(new Pair<Integer, Intent>(
                    LINKS_DESCRIPTION_RESOURCE_IDS[i],
                    new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                            .addFlags(getFlagForUrlLink())
            ));
        }
    }

    private int getFlagForUrlLink() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        } else {
            return Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        }
    }

    public Intent getWatchLiveIntent() {
        final String youtubeLink = String.format(
                Locale.US, Config.VIDEO_LIBRARY_URL_FMT,
                TextUtils.isEmpty(mLiveStreamId) ? "" : mLiveStreamId
        );
        return new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeLink));
    }

    public Intent getFeedbackIntent() {
        return new Intent(Intent.ACTION_VIEW, mSessionUri, mContext,
                SessionFeedbackActivity.class);
    }

    private void readDataFromTagMetadataCursor(Cursor cursor) {
        mTagMetadata = new TagMetadata(cursor);
    }

    private void readDataFromFeedbackCursor(Cursor cursor) {
        mHasFeedback = cursor.getCount() > 0;
    }

    private void readDataFromSpeakersCursor(Cursor cursor) {
        mSpeakers.clear();

        // Not using while(cursor.moveToNext()) because it would lead to issues when writing tests.
        // Either we would mock cursor.moveToNext() to return true and the test would have infinite
        // loop, or we would mock cursor.moveToNext() to return false, and the test would be for an
        // empty cursor.
        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            final String speakerName =
                    cursor.getString(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_NAME));
            if (TextUtils.isEmpty(speakerName)) {
                continue;
            }

            final String speakerImageUrl = cursor.getString(
                    cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_IMAGE_URL));
            final String speakerCompany = cursor.getString(
                    cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_COMPANY));
            final String speakerUrl = cursor.getString(
                    cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_URL));
            final String speakerPlusoneUrl = cursor.getString(
                    cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_PLUSONE_URL));
            final String speakerTwitterUrl = cursor.getString(
                    cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_TWITTER_URL));
            final String speakerAbstract = cursor.getString(
                    cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_ABSTRACT));

            mSpeakers.add(new Speaker(speakerName, speakerImageUrl, speakerCompany, speakerUrl,
                    speakerPlusoneUrl, speakerTwitterUrl, speakerAbstract));
        }
    }

    @Override
    public Loader<Cursor> createCursorLoader(SessionDetailQueryEnum query, Bundle args) {
        CursorLoader loader = null;
        if (query == null) {
            return loader;
        }
        switch (query) {
            case SESSIONS:
                mSessionId = getSessionId(mSessionUri);
                loader = getCursorLoaderInstance(mContext, mSessionUri,
                        SessionDetailQueryEnum.SESSIONS.getProjection(), null, null, null);
                break;
            case SPEAKERS:
                Uri speakersUri = getSpeakersDirUri(mSessionId);
                loader = getCursorLoaderInstance(mContext, speakersUri,
                        SessionDetailQueryEnum.SPEAKERS.getProjection(), null, null,
                        ScheduleContract.Speakers.DEFAULT_SORT);
                break;
            case FEEDBACK:
                Uri feedbackUri = getFeedbackUri(mSessionId);
                loader = getCursorLoaderInstance(mContext, feedbackUri,
                        SessionDetailQueryEnum.FEEDBACK.getProjection(), null, null, null);
                break;
            case TAG_METADATA:
                loader = getTagMetadataLoader();
                break;
            case MY_VIEWED_VIDEOS:
                Uri myPlayedVideoUri = ScheduleContract.MyViewedVideos.buildMyViewedVideosUri(
                        AccountUtils.getActiveAccountName(mContext));
                loader = getCursorLoaderInstance(mContext, myPlayedVideoUri,
                        SessionDetailQueryEnum.MY_VIEWED_VIDEOS.getProjection(), null, null, null);
                break;
        }
        return loader;
    }

    @VisibleForTesting
    public CursorLoader getCursorLoaderInstance(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        return new CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder);
    }

    @VisibleForTesting
    public CursorLoader getTagMetadataLoader() {
        return TagMetadata.createCursorLoader(mContext);
    }

    @VisibleForTesting
    public Uri getFeedbackUri(String sessionId) {
        return ScheduleContract.Feedback.buildFeedbackUri(sessionId);
    }

    @VisibleForTesting
    public Uri getSpeakersDirUri(String sessionId) {
        return ScheduleContract.Sessions.buildSpeakersDirUri(sessionId);
    }

    @VisibleForTesting
    public String getSessionId(Uri uri) {
        return ScheduleContract.Sessions.getSessionId(uri);
    }

    @Override
    public void processUserAction(SessionDetailUserActionEnum action,
            @android.support.annotation.Nullable Bundle args,
            UserActionCallback callback) {
        switch (action) {
            case STAR:
                mInSchedule = true;
                mSessionsHelper.setSessionStarred(mSessionUri, true, null);
                amendCalendarAndSetUpNotificationIfRequired();
                sendAnalyticsEventForStarUnstarSession(true);
                callback.onModelUpdated(this, action);
                break;
            case UNSTAR:
                mInSchedule = false;
                mSessionsHelper.setSessionStarred(mSessionUri, false, null);
                amendCalendarAndSetUpNotificationIfRequired();
                sendAnalyticsEventForStarUnstarSession(false);
                callback.onModelUpdated(this, action);
                break;
            case SHOW_MAP:
                // ANALYTICS EVENT: Click on Map action in Session Details page.
                // Contains: Session title/subtitle
                sendAnalyticsEvent("Session", "Map", mTitle);
                callback.onModelUpdated(this, action);
                break;
            case SHOW_SHARE:
                // ANALYTICS EVENT: Share a session.
                // Contains: Session title.
                sendAnalyticsEvent("Session", "Shared", mTitle);
                callback.onModelUpdated(this, action);
                break;
            case GIVE_FEEDBACK:
                // ANALYTICS EVENT: Click on the "send feedback" action in Session Details.
                // Contains: The session title.
                sendAnalyticsEvent("Session", "Feedback", getSessionTitle());
                callback.onModelUpdated(this, action);
                break;
            case EXTENDED:
                // ANALYTICS EVENT: Click on the extended session link in Session Details.
                sendAnalyticsEvent("Session", "Extended Session", getSessionTitle());
                callback.onModelUpdated(this, action);
                break;

            default:
                callback.onError(action);
        }
    }

    private void amendCalendarAndSetUpNotificationIfRequired() {
        if (!hasSessionStarted()) {
            Intent intent;
            if (mInSchedule) {
                intent = new Intent(SessionCalendarService.ACTION_ADD_SESSION_CALENDAR,
                        mSessionUri);
                intent.putExtra(SessionCalendarService.EXTRA_SESSION_START,
                        mSessionStart);
                intent.putExtra(SessionCalendarService.EXTRA_SESSION_END,
                        mSessionEnd);
                intent.putExtra(SessionCalendarService.EXTRA_SESSION_ROOM, mRoomName);
                intent.putExtra(SessionCalendarService.EXTRA_SESSION_TITLE, mTitle);
            } else {
                intent = new Intent(SessionCalendarService.ACTION_REMOVE_SESSION_CALENDAR,
                        mSessionUri);
                intent.putExtra(SessionCalendarService.EXTRA_SESSION_START,
                        mSessionStart);
                intent.putExtra(SessionCalendarService.EXTRA_SESSION_END,
                        mSessionEnd);
                intent.putExtra(SessionCalendarService.EXTRA_SESSION_TITLE, mTitle);
            }
            intent.setClass(mContext, SessionCalendarService.class);
            mContext.startService(intent);

            if (mInSchedule) {
                setUpNotification();
            }
        }
    }

    private void setUpNotification() {
        Intent scheduleIntent;

        // Schedule session notification
        if (!hasSessionStarted()) {
            LOGD(TAG, "Scheduling notification about session start.");
            scheduleIntent = new Intent(
                    SessionAlarmService.ACTION_SCHEDULE_STARRED_BLOCK,
                    null, mContext, SessionAlarmService.class);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, mSessionStart);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, mSessionEnd);
            mContext.startService(scheduleIntent);
        } else {
            LOGD(TAG, "Not scheduling notification about session start, too late.");
        }

        // Schedule feedback notification
        if (!hasSessionEnded()) {
            LOGD(TAG, "Scheduling notification about session feedback.");
            scheduleIntent = new Intent(
                    SessionAlarmService.ACTION_SCHEDULE_FEEDBACK_NOTIFICATION,
                    null, mContext, SessionAlarmService.class);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ID, mSessionId);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, mSessionStart);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, mSessionEnd);
            scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_TITLE, mTitle);
            mContext.startService(scheduleIntent);
        } else {
            LOGD(TAG, "Not scheduling feedback notification, too late.");
        }
    }

    @VisibleForTesting
    public void sendAnalyticsEvent(String category, String action, String label) {
        AnalyticsHelper.sendEvent(category, action, label);
    }

    private void sendAnalyticsEventForStarUnstarSession(boolean starred) {
        // ANALYTICS EVENT: Add or remove a session from My Schedule (starred vs unstarred)
        // Contains: Session title, whether it was added or removed (starred or unstarred)
        sendAnalyticsEvent("Session", starred ? "Starred" : "Unstarred", mTitle);
    }

    @Override
    public void cleanUp() {
        // Nothing to clean up
    }

    public static class Speaker {

        private String mName;

        private String mImageUrl;

        private String mCompany;

        private String mUrl;

        private String mPlusoneUrl;

        private String mTwitterUrl;

        private String mAbstract;

        public Speaker(String name, String imageUrl, String company, String url, String plusoneUrl,
                String twitterUrl, String anAbstract) {
            mName = name;
            mImageUrl = imageUrl;
            mCompany = company;
            mUrl = url;
            mPlusoneUrl = plusoneUrl;
            mTwitterUrl = twitterUrl;
            mAbstract = anAbstract;
        }

        public String getName() {
            return mName;
        }

        public String getImageUrl() {
            return mImageUrl;
        }

        public String getCompany() {
            return mCompany;
        }

        public String getUrl() {
            return mUrl;
        }

        public String getPlusoneUrl() {
            return mPlusoneUrl;
        }

        public String getTwitterUrl() {
            return mTwitterUrl;
        }

        public String getAbstract() {
            return mAbstract;
        }
    }

    public enum SessionDetailQueryEnum implements QueryEnum {
        SESSIONS(0, new String[]{ScheduleContract.Sessions.SESSION_START,
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
                ScheduleContract.Sessions.SESSION_LIVESTREAM_ID,
                ScheduleContract.Sessions.SESSION_MODERATOR_URL,
                ScheduleContract.Sessions.ROOM_ID,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Sessions.SESSION_COLOR,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_RELATED_CONTENT,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_SPEAKER_NAMES,
                ScheduleContract.Sessions.SESSION_MAIN_TAG}),
        SPEAKERS(1, new String[]{ScheduleContract.Speakers.SPEAKER_NAME,
                ScheduleContract.Speakers.SPEAKER_IMAGE_URL,
                ScheduleContract.Speakers.SPEAKER_COMPANY,
                ScheduleContract.Speakers.SPEAKER_ABSTRACT,
                ScheduleContract.Speakers.SPEAKER_URL,
                ScheduleContract.Speakers.SPEAKER_PLUSONE_URL,
                ScheduleContract.Speakers.SPEAKER_TWITTER_URL}),
        FEEDBACK(2, new String[]{ScheduleContract.Feedback.SESSION_ID}),
        TAG_METADATA(3, null),
        MY_VIEWED_VIDEOS(4, new String[]{ScheduleContract.MyViewedVideos.VIDEO_ID});

        private int id;

        private String[] projection;

        SessionDetailQueryEnum(int id, String[] projection) {
            this.id = id;
            this.projection = projection;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String[] getProjection() {
            return projection;
        }

    }

    public enum SessionDetailUserActionEnum implements UserActionEnum {
        STAR(1),
        UNSTAR(2),
        SHOW_MAP(3),
        SHOW_SHARE(4),
        GIVE_FEEDBACK(5),
        EXTENDED(6);

        private int id;

        SessionDetailUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

    }
}