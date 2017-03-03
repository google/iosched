/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.videolibrary.data;

/**
 * This represent a Video that is pulled from the Video Library. The model has one mutable field,
 * that is {@link #mAlreadyPlayed}.
 */
public class Video {

    private final String mId;

    private final int mYear;

    private final String mTopic;

    private final String mTitle;

    private final String mDesc;

    private final String mVid;

    private final String mSpeakers;

    private final String mThumbnailUrl;

    private boolean mAlreadyPlayed = false;

    private boolean mDataUpdated = false;

    public Video(String id, int year, String topic, String title, String desc, String vid,
            String speakers, String thumbnailUrl) {
        mId = id;
        mYear = year;
        mTopic = topic;
        mTitle = title;
        mDesc = desc;
        mVid = vid;
        mSpeakers = speakers;
        mThumbnailUrl = thumbnailUrl;
    }

    public String getId() {
        return mId;
    }

    public int getYear() {
        return mYear;
    }

    public String getTopic() {
        return mTopic;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDesc() {
        return mDesc;
    }

    public String getVid() {
        return mVid;
    }

    public String getSpeakers() {
        return mSpeakers;
    }

    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    public boolean getAlreadyPlayed() {
        return mAlreadyPlayed;
    }

    public void setAlreadyPlayed(boolean alreadyPlayed) {
        if (mAlreadyPlayed != alreadyPlayed) {
            mDataUpdated = true;
        }
        mAlreadyPlayed = alreadyPlayed;
    }

    /**
     * @return true if the data has been updated
     */
    public boolean dataUpdated() {
        return mDataUpdated;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Video video = (Video) o;

        if (mYear != video.mYear) {
            return false;
        }
        if (mAlreadyPlayed != video.mAlreadyPlayed) {
            return false;
        }
        if (mId != null ? !mId.equals(video.mId) : video.mId != null) {
            return false;
        }
        if (mTopic != null ? !mTopic.equals(video.mTopic) : video.mTopic != null) {
            return false;
        }
        if (mTitle != null ? !mTitle.equals(video.mTitle) : video.mTitle != null) {
            return false;
        }
        if (mDesc != null ? !mDesc.equals(video.mDesc) : video.mDesc != null) {
            return false;
        }
        if (mVid != null ? !mVid.equals(video.mVid) : video.mVid != null) {
            return false;
        }
        if (mSpeakers != null ? !mSpeakers.equals(video.mSpeakers) : video.mSpeakers != null) {
            return false;
        }
        return mThumbnailUrl != null ? mThumbnailUrl.equals(video.mThumbnailUrl) :
                video.mThumbnailUrl == null;

    }

    @Override
    public int hashCode() {
        int result = mId != null ? mId.hashCode() : 0;
        result = 31 * result + mYear;
        result = 31 * result + (mTopic != null ? mTopic.hashCode() : 0);
        result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
        result = 31 * result + (mDesc != null ? mDesc.hashCode() : 0);
        result = 31 * result + (mVid != null ? mVid.hashCode() : 0);
        result = 31 * result + (mSpeakers != null ? mSpeakers.hashCode() : 0);
        result = 31 * result + (mThumbnailUrl != null ? mThumbnailUrl.hashCode() : 0);
        result = 31 * result + (mAlreadyPlayed ? 1 : 0);
        return result;
    }
}
