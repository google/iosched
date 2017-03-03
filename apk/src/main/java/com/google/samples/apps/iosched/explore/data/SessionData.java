package com.google.samples.apps.iosched.explore.data;

import android.content.Context;
import android.text.TextUtils;

import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * This represent a Session that is pulled from the schedule.
 */
public class SessionData {
    private String mSessionName;
    private String mDetails;
    private String mSessionId;
    private String mImageUrl;
    private String mMainTag;
    private Calendar mStartDate;
    private Calendar mEndDate;
    private String mLiveStreamId;
    private String mYouTubeUrl;
    private String mTags;
    private boolean mInSchedule;

    public SessionData() { }

    public SessionData(Context context, String sessionName, String details, String sessionId,
            String imageUrl, String mainTag, long startTime, long endTime, String liveStreamId,
            String youTubeUrl, String tags, boolean inSchedule) {
        updateData(context, sessionName, details, sessionId, imageUrl, mainTag, startTime, endTime,
                liveStreamId, youTubeUrl, tags, inSchedule);
    }

    public void updateData(Context context, String sessionName, String details, String sessionId,
            String imageUrl, String mainTag, long startTime, long endTime, String liveStreamId,
            String youTubeUrl, String tags, boolean inSchedule) {
        mSessionName = sessionName;
        mDetails = details;
        mSessionId = sessionId;
        mImageUrl = imageUrl;
        mMainTag = mainTag;
        TimeZone timeZone = SettingsUtils.getDisplayTimeZone(context);
        mStartDate = Calendar.getInstance();
        mStartDate.setTimeInMillis(startTime);
        mStartDate.setTimeZone(timeZone);
        mEndDate = Calendar.getInstance();
        mEndDate.setTimeInMillis(endTime);
        mEndDate.setTimeZone(timeZone);
        mLiveStreamId = liveStreamId;
        mYouTubeUrl = youTubeUrl;
        mTags = tags;
        mInSchedule = inSchedule;
    }

    /**
     * Return whether this is a LiveStreamed session and whether it is happening right now.
     */
    public boolean isLiveStreamNow(Context context) {
        if (!isLiveStreamAvailable()) {
            return false;
        }
        if (mStartDate == null || mEndDate == null) {
            return false;
        }
        Calendar now = java.util.Calendar.getInstance();
        now.setTimeInMillis(TimeUtils.getCurrentTime(context));
        return mStartDate.before(now) && mEndDate.after(now);
    }

    public boolean isLiveStreamAvailable() {
        return !TextUtils.isEmpty(mLiveStreamId);
    }

    public boolean isVideoAvailable() {
        return !TextUtils.isEmpty(mYouTubeUrl);
    }

    public String getSessionName() {
        return mSessionName;
    }

    public String getDetails() {
        return mDetails;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getMainTag() {
        return mMainTag;
    }

    public void setDetails(String details) { mDetails = details; }

    public Calendar getStartDate() { return mStartDate; }

    public Calendar getEndDate() { return mEndDate; }

    public String getLiveStreamId() { return mLiveStreamId; }

    public String getYouTubeUrl() { return mYouTubeUrl; }

    public String getTags() { return mTags; }

    public boolean isInSchedule() { return mInSchedule; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SessionData that = (SessionData) o;

        if (mInSchedule != that.mInSchedule) {
            return false;
        }
        if (mSessionName != null ? !mSessionName.equals(that.mSessionName) :
                that.mSessionName != null) {
            return false;
        }
        if (mDetails != null ? !mDetails.equals(that.mDetails) : that.mDetails != null) {
            return false;
        }
        if (mSessionId != null ? !mSessionId.equals(that.mSessionId) : that.mSessionId != null) {
            return false;
        }
        if (mImageUrl != null ? !mImageUrl.equals(that.mImageUrl) : that.mImageUrl != null) {
            return false;
        }
        if (mMainTag != null ? !mMainTag.equals(that.mMainTag) : that.mMainTag != null) {
            return false;
        }
        if (mStartDate != null ? !mStartDate.equals(that.mStartDate) : that.mStartDate != null) {
            return false;
        }
        if (mEndDate != null ? !mEndDate.equals(that.mEndDate) : that.mEndDate != null) {
            return false;
        }
        if (mLiveStreamId != null ? !mLiveStreamId.equals(that.mLiveStreamId) :
                that.mLiveStreamId != null) {
            return false;
        }
        if (mYouTubeUrl != null ? !mYouTubeUrl.equals(that.mYouTubeUrl) :
                that.mYouTubeUrl != null) {
            return false;
        }
        return mTags != null ? mTags.equals(that.mTags) : that.mTags == null;

    }

    @Override
    public int hashCode() {
        int result = mSessionName != null ? mSessionName.hashCode() : 0;
        result = 31 * result + (mDetails != null ? mDetails.hashCode() : 0);
        result = 31 * result + (mSessionId != null ? mSessionId.hashCode() : 0);
        result = 31 * result + (mImageUrl != null ? mImageUrl.hashCode() : 0);
        result = 31 * result + (mMainTag != null ? mMainTag.hashCode() : 0);
        result = 31 * result + (mStartDate != null ? mStartDate.hashCode() : 0);
        result = 31 * result + (mEndDate != null ? mEndDate.hashCode() : 0);
        result = 31 * result + (mLiveStreamId != null ? mLiveStreamId.hashCode() : 0);
        result = 31 * result + (mYouTubeUrl != null ? mYouTubeUrl.hashCode() : 0);
        result = 31 * result + (mTags != null ? mTags.hashCode() : 0);
        result = 31 * result + (mInSchedule ? 1 : 0);
        return result;
    }
}
