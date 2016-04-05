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

import android.support.annotation.NonNull;

import com.google.samples.apps.iosched.model.TagMetadata;

import java.util.List;

/**
 * Models a group of videos that belong to a track.
 */
public class VideoTrack {

    private final String mTrack;

    private final int mTrackId;

    private String mTrackImageUrl;

    private final List<Video> mVideos;

    public VideoTrack(final String track,
            final int trackId,
            final List<Video> videos) {
        mTrack = track;
        mTrackId = trackId;
        mVideos = videos;
    }

    public String getTrack() {
        return mTrack;
    }

    public int getTrackId() {
        return mTrackId;
    }

    public List<Video> getVideos() {
        return mVideos;
    }

    public String getTrackImageUrl() {
        return mTrackImageUrl;
    }

    public void setTrackImageUrlIfAvailable(@NonNull TagMetadata tags) {
        TagMetadata.Tag tag = tags.getTag(mTrack);
        if (tag != null) {
            mTrackImageUrl = tag.getPhotoUrl();
        }
    }

    public boolean hasVideos() {
        return mVideos != null && !mVideos.isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final VideoTrack that = (VideoTrack) o;

        return mTrackId == that.mTrackId;

    }

    @Override
    public int hashCode() {
        return mTrackId;
    }

    @Override
    public String toString() {
        return "VideoTrack " + mTrack + "/ " + mTrackId + " with " + mVideos.size() + " videos";
    }
}
