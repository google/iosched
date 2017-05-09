/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.myio;

import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.TagMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyIOModel {

    private List<ScheduleItem> mSessionItems;

    private List<ScheduleItem> mBlockItems;

    private List<ScheduleItem> mScheduleItems = new ArrayList<>();

    private TagMetadata mTagMetadata;

    MyIOModel() {}

    void setSessionItems(List<ScheduleItem> scheduleItems) {
        mSessionItems = scheduleItems;
        merge();
    }

    void setBlockItems(List<ScheduleItem> blockItems) {
        mBlockItems = blockItems;
        merge();
    }

    List<ScheduleItem> getScheduleItems() {
        return mScheduleItems;
    }

    void setTagMetadata(TagMetadata tagMetadata) {
        mTagMetadata = tagMetadata;
    }

    TagMetadata getTagMetadata() {
        return mTagMetadata;
    }

    /**
     * Merges session and block items and sorts the merged collection.
     */
    private void merge() {
        mScheduleItems.clear();

        if (mBlockItems != null) {
            mScheduleItems.addAll(mBlockItems);
        }

        if (mSessionItems != null) {
            mScheduleItems.addAll(mSessionItems);
        }

        Collections.sort(mScheduleItems, new Comparator<ScheduleItem>() {
            @Override
            public int compare(ScheduleItem lhs, ScheduleItem rhs) {
                return Long.compare(lhs.startTime, rhs.startTime);
            }
        });
    }
}
