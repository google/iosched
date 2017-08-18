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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Pair;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.archframework.ModelWithLoaderManager;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.archframework.UserActionEnum;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.ScheduleItemHelper;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum;
import com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailUserActionEnum;
import com.google.samples.apps.iosched.session.data.QueueAction;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.samples.apps.iosched.model.ScheduleItem.detectSessionType;
import static com.google.samples.apps.iosched.session.SessionDetailConstants.FIREBASE_NODE_QUEUE;
import static com.google.samples.apps.iosched.session.SessionDetailConstants.FIREBASE_NODE_RESERVATIONS;
import static com.google.samples.apps.iosched.session.SessionDetailConstants.FIREBASE_NODE_RESULTS;
import static com.google.samples.apps.iosched.session.SessionDetailConstants.FIREBASE_NODE_SEATS;
import static com.google.samples.apps.iosched.session.SessionDetailConstants.FIREBASE_NODE_SEATS_AVAILABLE;
import static com.google.samples.apps.iosched.session.SessionDetailConstants.FIREBASE_NODE_SESSIONS;
import static com.google.samples.apps.iosched.session.SessionDetailConstants.FIREBASE_NODE_STATUS;
import static com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum.AUTH_REGISTRATION;
import static com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum.RESERVATION_FAILED;
import static com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum.RESERVATION_PENDING;
import static com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum.RESERVATION_RESULT;
import static com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum.RESERVATION_SEAT_AVAILABILITY;
import static com.google.samples.apps.iosched.session.SessionDetailModel.SessionDetailQueryEnum.RESERVATION_STATUS;
import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class SessionDetailModel extends ModelWithLoaderManager<SessionDetailQueryEnum,
        SessionDetailUserActionEnum> {

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

    private int mServerReservationStatus;

    private boolean mIsKeynote;

    private long mSessionStart;

    private long mSessionEnd;

    private String mSessionAbstract;

    private String mHashTag;

    private String mUrl = "";

    private String mRoomId;

    private String mRoomName;

    private String[] mTags;

    private String mLiveStreamId;

    private String mYouTubeUrl;

    private String mPhotoUrl;

    private boolean mHasLiveStream = false;

    private boolean mLiveStreamVideoWatched = false;

    private boolean mHasFeedback = false;

    private String mRequirements;

    private String mSpeakersNames;

    private TagMetadata mTagMetadata;

    private int mSessionType;

    // Request pending
    private boolean mReservationPending = false;
    private boolean mReturnPending = false;

    // Reservation status
    private String mReservationStatus;
    private String mReservationResult;

    // Seats available
    private boolean mSeatsAvailable;

    /**
     * Holds a list of links for the session. The first element of the {@code Pair} is the resource
     * id for the string describing the link, the second is the {@code Intent} to launch when
     * selecting the link.
     */
    private List<Pair<Integer, Intent>> mLinks = new ArrayList<>();

    private List<Speaker> mSpeakers = new ArrayList<>();

    private List<ScheduleItem> mRelatedSessions;

    private StringBuilder mBuffer = new StringBuilder();

    private ValueEventListener mQueueEventListener;
    private ValueEventListener mSessionReservationStatusEventListener;
    private ValueEventListener mSessionReservationResultEventListener;
    private ValueEventListener mSeatAvailabilityEventListener;

    private DatabaseReference mQueueReference;
    private DatabaseReference mSessionReservationStatusReference;
    private DatabaseReference mSessionReservationResultReference;
    private DatabaseReference mSeatAvailabilityReference;

    public SessionDetailModel(Uri sessionUri, Context context, SessionsHelper sessionsHelper,
            LoaderManager loaderManager) {
        super(SessionDetailQueryEnum.values(), SessionDetailUserActionEnum.values(), loaderManager);
        mContext = context;
        mSessionsHelper = sessionsHelper;
        mSessionUri = sessionUri;
        mSessionId = extractSessionId(sessionUri);
    }

    private static DatabaseReference getQueueReference(String userUid) {
        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_NODE_QUEUE)
                .child(userUid);
    }

    private static DatabaseReference getReservationStatusReference
            (String userUid, String sessionId) {
        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_NODE_SESSIONS)
                .child(sessionId)
                .child(FIREBASE_NODE_RESERVATIONS)
                .child(userUid)
                .child(FIREBASE_NODE_STATUS);
    }

    private static DatabaseReference getReservationResultsReference
            (String userUid, String sessionId) {
        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_NODE_SESSIONS)
                .child(sessionId)
                .child(FIREBASE_NODE_RESERVATIONS)
                .child(userUid)
                .child(FIREBASE_NODE_RESULTS);
    }

    private static DatabaseReference getSeatAvailabilityReference(String sessionId) {
        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_NODE_SESSIONS)
                .child(sessionId)
                .child(FIREBASE_NODE_SEATS)
                .child(FIREBASE_NODE_SEATS_AVAILABLE);
    }

    @Override
    public void requestData(@NonNull SessionDetailQueryEnum query,
            @NonNull DataQueryCallback<SessionDetailQueryEnum> callback) {
        switch (query) {
            case RESERVATION_PENDING:
            case RESERVATION_STATUS:
            case RESERVATION_RESULT:
            case RESERVATION_FAILED:
            case RESERVATION_SEAT_AVAILABILITY:
                mDataQueryCallbacks.put(query, callback);
                break;
            default:
                super.requestData(query, callback);
                break;
        }
    }

    public void initReservationListeners() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            mQueueReference = getQueueReference(currentUser.getUid());
            mSessionReservationStatusReference =
                    getReservationStatusReference(currentUser.getUid(), mSessionId);
            mSessionReservationResultReference =
                    getReservationResultsReference(currentUser.getUid(), mSessionId);
            mSeatAvailabilityReference = getSeatAvailabilityReference(mSessionId);

            mSessionReservationStatusEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        String reservationStatus = dataSnapshot.getValue(String.class);
                        if (reservationStatus != null) {
                            mReservationStatus = reservationStatus;
                            mSessionsHelper.setReservationStatus(mSessionUri,
                                    ScheduleContract.MyReservations.fromFirebaseString(mReservationStatus),
                                    mTitle);
                            final DataQueryCallback<SessionDetailQueryEnum> reservationStatusCallback =
                                    mDataQueryCallbacks.get(RESERVATION_STATUS);
                            reservationStatusCallback.onModelUpdated(SessionDetailModel.this,
                                    RESERVATION_STATUS);
                        }
                    } catch (DatabaseException e) {
                        LOGE(TAG, e.getMessage());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    LOGE(TAG, databaseError.getMessage());
                }
            };
            mSessionReservationResultEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        String reservationResult = dataSnapshot.getValue(String.class);
                        if (reservationResult != null) {
                            mReservationResult = reservationResult;
                            DataQueryCallback<SessionDetailQueryEnum> reservationResultCallback
                                    = mDataQueryCallbacks.get(RESERVATION_RESULT);
                            reservationResultCallback.onModelUpdated(SessionDetailModel.this,
                                    RESERVATION_RESULT);
                            mSessionReservationResultReference.removeEventListener(this);
                        }
                    } catch (DatabaseException e) {
                        LOGE(TAG, e.getMessage());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    LOGE(TAG, databaseError.getMessage());
                }
            };
            mQueueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        DataQueryCallback<SessionDetailQueryEnum> reservationPendingCallback
                                = mDataQueryCallbacks.get(RESERVATION_PENDING);
                        QueueAction queueAction = dataSnapshot.getValue(QueueAction.class);
                        if (queueAction == null) {
                            LOGD(TAG, "Exit queue");
                            mReturnPending = false;
                            mReservationPending = false;
                            mQueueReference.removeEventListener(this);
                            reservationPendingCallback.onModelUpdated(
                                    SessionDetailModel.this, RESERVATION_PENDING);
                        }
                    } catch (DatabaseException e) {
                        LOGE(TAG, e.getMessage());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    LOGE(TAG, databaseError.getMessage());
                }
            };
            mSeatAvailabilityEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    DataQueryCallback<SessionDetailQueryEnum> seatAvailabilityCallback
                            = mDataQueryCallbacks.get(RESERVATION_SEAT_AVAILABILITY);
                    Boolean seatsAvailable = dataSnapshot.getValue(Boolean.class);
                    if (seatsAvailable != null) {
                        mSeatsAvailable = seatsAvailable;
                        seatAvailabilityCallback
                                .onModelUpdated(SessionDetailModel.this,
                                        RESERVATION_SEAT_AVAILABILITY);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    LOGE(TAG, databaseError.getMessage());
                }
            };

            mSessionReservationStatusReference.addValueEventListener(mSessionReservationStatusEventListener);
            mSeatAvailabilityReference.addValueEventListener(mSeatAvailabilityEventListener);
        } else {
            LOGD(TAG, "Not signed in.");
            invalidateReservationCache();
            DataQueryCallback<SessionDetailQueryEnum> authCallback
                    = mDataQueryCallbacks.get(AUTH_REGISTRATION);
            authCallback.onModelUpdated(SessionDetailModel.this, AUTH_REGISTRATION);
        }
    }

    private void invalidateReservationCache() {
        mReservationStatus = null;
        mReservationResult = null;
        mReservationPending = false;
        mReturnPending = false;
    }

    private void removeReservationListeners() {
        if (mQueueEventListener != null && mQueueReference != null) {
            mQueueReference.removeEventListener(mQueueEventListener);
        }
        if (mSeatAvailabilityReference != null && mSeatAvailabilityEventListener != null) {
            mSeatAvailabilityReference.removeEventListener(mSeatAvailabilityEventListener);
        }
        if (mSessionReservationStatusReference != null &&
                mSessionReservationStatusEventListener != null) {
            mSessionReservationStatusReference
                    .removeEventListener(mSessionReservationStatusEventListener);
        }
        if (mSessionReservationResultReference != null &&
                mSessionReservationResultEventListener != null) {
            mSessionReservationResultReference
                    .removeEventListener(mSessionReservationResultEventListener);
        }
    }

    public boolean getSeatsAvailability() {
        return mSeatsAvailable;
    }

    public String getReservationStatus() {
        return mReservationStatus;
    }

    public String getReservationResult() {
        return mReservationResult;
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

    public boolean isSessionTrackColorAvailable() {
        return mTagMetadata != null;
    }

    public int getSessionTrackColor() {
        if (mTagMetadata != null && mMainTag != null) {
            final TagMetadata.Tag tag = mTagMetadata.getTag(mMainTag);
            if (tag != null) {
                return tag.getColor();
            }
        }
        return Color.TRANSPARENT;
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
        if (hasSessionEnded()) {
            // If session has ended, return 0 minutes until end of session.
            return 0l;
        } else {
            long currentTimeMillis = TimeUtils.getCurrentTime(mContext);
            int minutes = (int) ((mSessionEnd - currentTimeMillis) / 60000);
            // Rounded up number of minutes.
            return minutes * 60000 < (mSessionEnd - currentTimeMillis) ? minutes + 1 : minutes;
        }
    }

    public boolean isSessionReadyForFeedback() {
        long now = TimeUtils.getCurrentTime(mContext);
        return now > mSessionEnd - SessionDetailConstants.FEEDBACK_MILLIS_BEFORE_SESSION_END_MS;
    }

    public boolean hasLiveStream() {
        return mHasLiveStream || (!TextUtils.isEmpty(mYouTubeUrl) && !mYouTubeUrl.equals("null"));
    }

    /**
     * Show header image only if the session has a either a valid youTube url or a livestream Id. We
     * have historically had more than one video ID associated with a video, one for when the
     * session is livestreamed and another for when a video is made available after the session has
     * concluded. These may be the same. One or both may be null.
     */
    boolean shouldShowHeaderImage() {
        return (!TextUtils.isEmpty(mYouTubeUrl) || !TextUtils.isEmpty(mLiveStreamId)) &&
                showLiveStream();
    }

    public boolean isInSchedule() {
        return mInSchedule;
    }

    public boolean isInScheduleWhenSessionFirstLoaded() {
        return mInScheduleWhenSessionFirstLoaded;
    }

    public int getServerReservationStatus() {
        return mServerReservationStatus;
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

    public String getMainTag() {
        return mMainTag;
    }

    public String[] getTags() {
        return mTags;
    }

    public List<Pair<Integer, Intent>> getLinks() {
        return mLinks;
    }

    public List<Speaker> getSpeakers() {
        return mSpeakers;
    }

    public List<ScheduleItem> getRelatedSessions() {
        return mRelatedSessions;
    }

    public boolean hasSummaryContent() {
        return !TextUtils.isEmpty(mTitle)
                || !TextUtils.isEmpty(mSubtitle)
                || !TextUtils.isEmpty(mSessionAbstract);
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
            } else if (SessionDetailQueryEnum.RELATED == query) {
                readDataFromRelatedSessionsCursor(cursor);
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

        mSessionType = detectSessionType(
                cursor.getString(cursor.getColumnIndex(Sessions.SESSION_TAGS)));

        mInSchedule = cursor.getInt(cursor.getColumnIndex(
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE)) != 0;

        mServerReservationStatus = cursor.getInt(cursor.getColumnIndex(
                ScheduleContract.Sessions.SESSION_RESERVATION_STATUS));

        if (!mSessionLoaded) {
            mInScheduleWhenSessionFirstLoaded = mInSchedule;
        }

        String tagsString = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_TAGS));
        if (tagsString != null) {
            mIsKeynote = tagsString.contains(Config.Tags.SPECIAL_KEYNOTE);
            mTags = tagsString.split(",");
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

    public int getSessionType() {
        return mSessionType;
    }

    @VisibleForTesting
    public void formatSubtitle() {
        mSubtitle = UIUtils.formatSessionSubtitle(mSessionStart, mSessionEnd, mRoomName, mBuffer,
                mContext);
    }

    private void buildLinks(Cursor cursor) {
        mLinks.clear();

        if (hasLiveStream() && isSessionOngoing()) {
            mLinks.add(new Pair<>(
                    R.string.session_link_livestream,
                    getWatchLiveIntent()));
        }

        if (!hasFeedback() && hasSessionEnded()) {
            mLinks.add(new Pair<>(
                    R.string.session_feedback_submitlink,
                    getFeedbackIntent()
            ));
        }

        for (int i = 0; i < LINKS_CURSOR_FIELDS.length; i++) {
            final String linkUrl = cursor.getString(cursor.getColumnIndex(LINKS_CURSOR_FIELDS[i]));
            if (TextUtils.isEmpty(linkUrl)) {
                continue;
            }

            mLinks.add(new Pair<>(
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

    private void readDataFromRelatedSessionsCursor(Cursor cursor) {
        mRelatedSessions = ScheduleItemHelper.cursorToItems(cursor, mContext);
    }

    @Override
    public Loader<Cursor> createCursorLoader(SessionDetailQueryEnum query, Bundle args) {
        CursorLoader loader = null;
        if (query == null) {
            return loader;
        }
        switch (query) {
            case SESSIONS:
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
            case RELATED:
                Uri relatedSessionsUri = Sessions.buildRelatedSessionsDirUri(mSessionId);
                loader = getCursorLoaderInstance(mContext, relatedSessionsUri,
                        SessionDetailQueryEnum.RELATED.getProjection(), null, null, null);
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
    public String extractSessionId(Uri uri) {
        return ScheduleContract.Sessions.getSessionId(uri);
    }

    @Override
    public void processUserAction(SessionDetailUserActionEnum action, @Nullable Bundle args,
            UserActionCallback<SessionDetailUserActionEnum> callback) {
        switch (action) {
            case STAR:
            case UNSTAR:
                mInSchedule = action == SessionDetailUserActionEnum.STAR;
                setSessionBookmark(mSessionId, mInSchedule, mTitle);
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
            case STAR_RELATED:
            case UNSTAR_RELATED:
                String sessionId = args == null ? null : args.getString(Sessions.SESSION_ID);
                if (!TextUtils.isEmpty(sessionId)) {
                    boolean inSchedule = action == SessionDetailUserActionEnum.STAR_RELATED;
                    setSessionBookmark(sessionId, inSchedule, mTitle);
                    for (ScheduleItem item : mRelatedSessions) {
                        if (TextUtils.equals(sessionId, item.sessionId)) {
                            item.inSchedule = inSchedule;
                            break;
                        }
                    }
                    callback.onModelUpdated(this, action);
                }
                break;
            case RESERVE:
                attemptReserve();
                break;
            case RETURN:
                attemptReturnReservation();
                break;
            default:
                callback.onError(action);
        }
    }

    private void setSessionBookmark(String sessionId, boolean bookmarked, String title) {
        Uri sessionUri = Sessions.buildSessionUri(sessionId);
        mSessionsHelper.setSessionStarred(sessionUri, bookmarked, title);
    }

    @VisibleForTesting
    public void sendAnalyticsEvent(String category, String action, String label) {
        AnalyticsHelper.sendEvent(category, action, label);
    }

    @Override
    public void cleanUp() {
        removeReservationListeners();
    }

    public boolean isReservationPending() {
        return mReservationPending;
    }

    public boolean isReturnPending() {
        return mReturnPending;
    }

    public String generateReserveRequestId() {
        return "" + System.currentTimeMillis();
    }

    public void attemptReserve() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mQueueReference != null && currentUser != null) {
            final String requestId = generateReserveRequestId();
            QueueAction queueAction = new QueueAction(mSessionId, "reserve", requestId);
            mQueueReference.addValueEventListener(mQueueEventListener);
            mQueueReference.setValue(queueAction).addOnCompleteListener(
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            LOGD(TAG, "Enter queue.");
                            mReservationPending = true;
                            mReturnPending = false;
                            DataQueryCallback<SessionDetailQueryEnum> reservationPendingCallback
                                    = mDataQueryCallbacks.get(RESERVATION_PENDING);
                            reservationPendingCallback.onModelUpdated(
                                    SessionDetailModel.this, RESERVATION_PENDING);
                            mSessionReservationResultReference
                                    .child(requestId)
                                    .addValueEventListener(mSessionReservationResultEventListener);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    LOGE(TAG, e.getMessage());
                    mReservationPending = false;
                    mReturnPending = false;
                    DataQueryCallback<SessionDetailQueryEnum> reservationFailedCallback
                            = mDataQueryCallbacks.get(RESERVATION_FAILED);
                    reservationFailedCallback
                            .onModelUpdated(SessionDetailModel.this, RESERVATION_FAILED);
                }
            });
        }
    }

    public void attemptReturnReservation() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mQueueReference != null && currentUser != null) {
            final String requestId = generateReserveRequestId();
            QueueAction queueAction = new QueueAction(mSessionId, "return", requestId);
            mQueueReference.addValueEventListener(mQueueEventListener);
            mQueueReference.setValue(queueAction).addOnCompleteListener(
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            LOGD(TAG, "Enter queue.");
                            mReservationPending = false;
                            mReturnPending = true;
                            DataQueryCallback<SessionDetailQueryEnum> reservationPendingCallback
                                    = mDataQueryCallbacks.get(RESERVATION_PENDING);
                            reservationPendingCallback.onModelUpdated(
                                    SessionDetailModel.this, RESERVATION_PENDING);
                            mSessionReservationResultReference
                                    .child(requestId)
                                    .addValueEventListener(mSessionReservationResultEventListener);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    LOGE(TAG, e.getMessage());
                    mReservationPending = false;
                    mReturnPending = false;
                    DataQueryCallback<SessionDetailQueryEnum> reservationFailedCallback
                            = mDataQueryCallbacks.get(RESERVATION_FAILED);
                    reservationFailedCallback
                            .onModelUpdated(SessionDetailModel.this, RESERVATION_FAILED);
                }
            });
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
                ScheduleContract.Sessions.SESSION_RESERVATION_STATUS,
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
        MY_VIEWED_VIDEOS(4, new String[]{ScheduleContract.MyViewedVideos.VIDEO_ID}),
        RELATED(5, ScheduleItemHelper.REQUIRED_SESSION_COLUMNS),
        RESERVATION_STATUS(6, null),
        RESERVATION_RESULT(7, null),
        RESERVATION_PENDING(8, null),
        RESERVATION_FAILED(9, null),
        RESERVATION_SEAT_AVAILABILITY(10, null),
        AUTH_REGISTRATION(11, null);

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
        EXTENDED(6),
        STAR_RELATED(7),
        UNSTAR_RELATED(8),
        RESERVE(9),
        RETURN(10); // Cancel reservation
        private int id;

        SessionDetailUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

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
}
