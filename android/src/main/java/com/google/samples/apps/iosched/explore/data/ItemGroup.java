/*
 * Copyright 2016 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.explore.data;

import android.support.annotation.Nullable;

import com.google.samples.apps.iosched.model.TagMetadata;

import java.util.ArrayList;

public class ItemGroup {

    private String mTitleId;
    private String mTitle;
    private String mId;
    private String mPhotoUrl;
    private ArrayList<SessionData> sessions = new ArrayList<SessionData>();

    public void addSessionData(SessionData session) {
        sessions.add(session);
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    public void formatTitle(TagMetadata tagMetadata) {
        if (tagMetadata != null && tagMetadata.getTagById(mTitleId) != null) {
            mTitle = tagMetadata.getTagById(mTitleId).getName();
        }
    }

    public String getTitleId() {
        return mTitleId;
    }

    public void setTitleId(String titleId) { mTitleId = titleId; }

    public String getId() { return mId; }

    public void setId(String id) { mId = id; }

    public String getPhotoUrl() {
        return mPhotoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        mPhotoUrl = photoUrl;
    }

    public ArrayList<SessionData> getSessions() { return sessions; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemGroup)) {
            return false;
        }

        final ItemGroup itemGroup = (ItemGroup) o;

        return mId != null ? mId.equals(itemGroup.mId) : itemGroup.mId == null;

    }

    @Override
    public int hashCode() {
        return mId != null ? mId.hashCode() : 0;
    }

}
