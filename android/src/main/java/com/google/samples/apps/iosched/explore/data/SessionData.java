package com.google.samples.apps.iosched.explore.data;

import com.google.samples.apps.iosched.util.UIUtils;

import android.content.Context;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.Date;

/**
 * This represent a Session that is pulled from the schedule.
 */
public class SessionData {
    private String mSessionName;
    private String mDetails;
    private String mSessionId;
    private String mImageUrl;
    private String mMainTag;
    private Date mStartDate;
    private Date mEndDate;
    private String mLiveStreamId;
    private String mYouTubeUrl;
    private String mTags;
    private boolean mInSchedule;

    public SessionData() { }

    public SessionData(String sessionName, String details, String sessionId, String imageUrl,
                       String mainTag, long startTime, long endTime, String liveStreamId,
                       String youTubeUrl, String tags, boolean inSchedule) {
        updateData(sessionName, details, sessionId, imageUrl, mainTag, startTime, endTime,
                liveStreamId, youTubeUrl, tags, inSchedule);
    }

    public void updateData(String sessionName, String details, String sessionId, String imageUrl,
                           String mainTag, long startTime, long endTime, String liveStreamId,
                           String youTubeUrl, String tags, boolean inSchedule) {
        mSessionName = sessionName;
        mDetails = details;
        mSessionId = sessionId;
        mImageUrl = imageUrl;
        mMainTag = mainTag;
        try { mStartDate = new java.util.Date(startTime); } catch (Exception ignored) { }
        try { mEndDate = new java.util.Date(endTime); } catch (Exception ignored) { }
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
        now.setTimeInMillis(UIUtils.getCurrentTime(context));
        if (mStartDate.before(now.getTime()) && mEndDate.after(now.getTime())) {
            return true;
        } else {
            return false;
        }
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

    public Date getStartDate() { return mStartDate; }

    public Date getEndDate() { return mEndDate; }

    public String getLiveStreamId() { return mLiveStreamId; }

    public String getYouTubeUrl() { return mYouTubeUrl; }

    public String getTags() { return mTags; }

    public boolean isInSchedule() { return mInSchedule; }
}
