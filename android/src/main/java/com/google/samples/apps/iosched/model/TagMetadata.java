/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.model;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.provider.ScheduleContract;

import java.util.*;

public class TagMetadata {

    // list of tags in each category, sorted by the category sort order
    HashMap<String, ArrayList<Tag>> mTagsInCategory = new HashMap<String, ArrayList<Tag>>();

    // hash map from tag ID to tag
    HashMap<String, Tag> mTagsById = new HashMap<String, Tag>();

    public static CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context, ScheduleContract.Tags.CONTENT_URI, TagsQuery.PROJECTION,
                null, null, null);
    }

    public TagMetadata(Cursor cursor) {
        while (cursor.moveToNext()) {
            Tag tag = new Tag(cursor.getString(TagsQuery.TAG_ID),
                    cursor.getString(TagsQuery.TAG_NAME),
                    cursor.getString(TagsQuery.TAG_CATEGORY),
                    cursor.getInt(TagsQuery.TAG_ORDER_IN_CATEGORY),
                    cursor.getString(TagsQuery.TAG_ABSTRACT),
                    cursor.getInt(TagsQuery.TAG_COLOR));
            mTagsById.put(tag.getId(), tag);
            if (!mTagsInCategory.containsKey(tag.getCategory())) {
                mTagsInCategory.put(tag.getCategory(), new ArrayList<Tag>());
            }
            mTagsInCategory.get(tag.getCategory()).add(tag);
        }

        for (ArrayList<Tag> list : mTagsInCategory.values()) {
            Collections.sort(list);
        }
    }

    public Tag getTag(String tagId) {
        return mTagsById.containsKey(tagId) ? mTagsById.get(tagId) : null;
    }

    public List<Tag> getTagsInCategory(String category) {
        return mTagsInCategory.containsKey(category) ?
                Collections.unmodifiableList(mTagsInCategory.get(category)) : null;
    }

    /** Given the set of tags on a session, returns its group label. */
    public Tag getSessionGroupTag(String[] sessionTags) {
        int bestOrder = Integer.MAX_VALUE;
        Tag bestTag = null;
        for (String tagId : sessionTags) {
            Tag tag = getTag(tagId);
            if (tag != null && Config.Tags.SESSION_GROUPING_TAG_CATEGORY.equals(tag.getCategory()) &&
                        tag.getOrderInCategory() < bestOrder) {
                bestOrder = tag.getOrderInCategory();
                bestTag = tag;
            }
        }
        return bestTag;
    }

    public static Comparator<Tag> TAG_DISPLAY_ORDER_COMPARATOR = new Comparator<Tag>() {
        @Override
        public int compare(Tag tag, Tag tag2) {
            if (!TextUtils.equals(tag.getCategory(), tag2.getCategory())) {
                return Config.Tags.CATEGORY_DISPLAY_ORDERS.get(tag.getCategory()) -
                        Config.Tags.CATEGORY_DISPLAY_ORDERS.get(tag2.getCategory());
            } else if (tag.getOrderInCategory() != tag2.getOrderInCategory()) {
                return tag.getOrderInCategory() - tag2.getOrderInCategory();
            }

            return tag.getName().compareTo(tag2.getName());
        }
    };

    private interface TagsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Tags.TAG_ID,
                ScheduleContract.Tags.TAG_NAME,
                ScheduleContract.Tags.TAG_CATEGORY,
                ScheduleContract.Tags.TAG_ORDER_IN_CATEGORY,
                ScheduleContract.Tags.TAG_ABSTRACT,
                ScheduleContract.Tags.TAG_COLOR
        };

        int _ID = 0;
        int TAG_ID = 1;
        int TAG_NAME = 2;
        int TAG_CATEGORY = 3;
        int TAG_ORDER_IN_CATEGORY = 4;
        int TAG_ABSTRACT = 5;
        int TAG_COLOR = 6;
    }

    static public class Tag implements Comparable<Tag> {
        private String mId;
        private String mName;
        private String mCategory;
        private int mOrderInCategory;
        private String mAbstract;
        private int mColor;

        public Tag(String id, String name, String category, int orderInCategory, String _abstract,
                int color) {
            mId = id;
            mName = name;
            mCategory = category;
            mOrderInCategory = orderInCategory;
            mAbstract = _abstract;
            mColor = color;
        }

        public String getId() {
            return mId;
        }

        public String getName() {
            return mName;
        }

        public String getCategory() {
            return mCategory;
        }

        public int getOrderInCategory() {
            return mOrderInCategory;
        }

        public String getAbstract() {
            return mAbstract;
        }

        public int getColor() {
            return mColor;
        }

        @Override
        public int compareTo(Tag another) {
            return mOrderInCategory - another.mOrderInCategory;
        }
    }
}
