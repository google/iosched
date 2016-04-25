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
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class TagMetadata {

    // List of tags in each category, sorted by the category sort order.
    private HashMap<String, ArrayList<Tag>> mTagsInCategory = new HashMap<String, ArrayList<Tag>>();

    // Hash map from tag ID to tag.
    private HashMap<String, Tag> mTagsById = new HashMap<String, Tag>();

    // Hash map from tag name to tag id.
    private HashMap<String, String> mTagsByName = new HashMap<String, String>();

    public static CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context, ScheduleContract.Tags.CONTENT_URI,
                TagsQueryEnum.TAG.getProjection(), null, null, null);
    }

    protected TagMetadata() {
    }

    public TagMetadata(Cursor cursor) {
        // Not using while(cursor.moveToNext()) because it would lead to issues when writing tests.
        // Either we would mock cursor.moveToNext() to return true and the test would have infinite
        // loop, or we would mock cursor.moveToNext() to return false, and the test would be for an
        // empty cursor.
        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            Tag tag = new Tag(cursor.getString(cursor.getColumnIndex(ScheduleContract.Tags.TAG_ID)),
                    cursor.getString(cursor.getColumnIndex(ScheduleContract.Tags.TAG_NAME)),
                    cursor.getString(cursor.getColumnIndex(ScheduleContract.Tags.TAG_CATEGORY)),
                    cursor.getInt(
                            cursor.getColumnIndex(ScheduleContract.Tags.TAG_ORDER_IN_CATEGORY)),
                    cursor.getString(cursor.getColumnIndex(ScheduleContract.Tags.TAG_ABSTRACT)),
                    cursor.getInt(cursor.getColumnIndex(ScheduleContract.Tags.TAG_COLOR)),
                    cursor.getString(cursor.getColumnIndex(ScheduleContract.Tags.TAG_PHOTO_URL)));
            mTagsById.put(tag.getId(), tag);
            mTagsByName.put(tag.getName(), tag.getId());
            if (!mTagsInCategory.containsKey(tag.getCategory())) {
                mTagsInCategory.put(tag.getCategory(), new ArrayList<Tag>());
            }
            mTagsInCategory.get(tag.getCategory()).add(tag);
        }

        for (ArrayList<Tag> list : mTagsInCategory.values()) {
            Collections.sort(list);
        }
    }

    /**
     * @return the tag with the {@code tagId}, if found.
     */
    public Tag getTagById(String tagId) {
        return mTagsById.containsKey(tagId) ? mTagsById.get(tagId) : null;
    }

    /**
     * @return the tag with the {@code tagName} if found.
     */
    private Tag getTagByName(String tagName) {
        String tagId = mTagsByName.containsKey(tagName) ? mTagsByName.get(tagName) : null;
        return tagId != null ? getTagById(tagId) : null;
    }

    /**
     * @return the tag with the id matching the {@code searchString}, if found; if not found,
     * returns the tag with the name matching the {@code searchString}, if found.
     */
    public Tag getTag(String searchString) {
        Tag tagById = getTagById(searchString);
        if (tagById != null) {
            return tagById;
        } else {
            return getTagByName(searchString);
        }
    }

    public List<Tag> getTagsInCategory(String category) {
        return mTagsInCategory.containsKey(category) ?
                Collections.unmodifiableList(mTagsInCategory.get(category)) : null;
    }

    /**
     * Given the set of tags on a session, returns its group label.
     */
    public Tag getSessionGroupTag(String[] sessionTags) {
        int bestOrder = Integer.MAX_VALUE;
        Tag bestTag = null;
        for (String tagId : sessionTags) {
            Tag tag = getTagById(tagId);
            if (tag != null &&
                    Config.Tags.SESSION_GROUPING_TAG_CATEGORY.equals(tag.getCategory()) &&
                    tag.getOrderInCategory() < bestOrder) {
                bestOrder = tag.getOrderInCategory();
                bestTag = tag;
            }
        }
        return bestTag;
    }

    @Override
    public String toString() {
        return "Tag Metadata has " + mTagsById.size() + " tags in " + mTagsInCategory.size()
                + " categories";
    }

    public static Comparator<Tag> TAG_DISPLAY_ORDER_COMPARATOR = new Comparator<Tag>() {
        @Override
        public int compare(Tag tag, Tag tag2) {
            if (!TextUtils.equals(tag.getCategory(), tag2.getCategory())) {
                return Config.getCategoryDisplayOrder(tag.getCategory()) -
                        Config.getCategoryDisplayOrder(tag2.getCategory());
            } else if (tag.getOrderInCategory() != tag2.getOrderInCategory()) {
                return tag.getOrderInCategory() - tag2.getOrderInCategory();
            }

            return tag.getName().compareTo(tag2.getName());
        }
    };

    public enum TagsQueryEnum implements QueryEnum {
        TAG(0, new String[]{
                BaseColumns._ID,
                ScheduleContract.Tags.TAG_ID,
                ScheduleContract.Tags.TAG_NAME,
                ScheduleContract.Tags.TAG_CATEGORY,
                ScheduleContract.Tags.TAG_ORDER_IN_CATEGORY,
                ScheduleContract.Tags.TAG_ABSTRACT,
                ScheduleContract.Tags.TAG_COLOR,
                ScheduleContract.Tags.TAG_PHOTO_URL
        });

        private int id;

        private String[] projection;

        TagsQueryEnum(int id, String[] projection) {
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

    static public class Tag implements Comparable<Tag> {
        private String mId;
        private String mName;
        private String mCategory;
        private int mOrderInCategory;
        private String mAbstract;
        private int mColor;
        private String mPhotoUrl;

        public Tag(String id, String name, String category, int orderInCategory, String _abstract,
                int color, String photoUrl) {
            mId = id;
            mName = name;
            mCategory = category;
            mOrderInCategory = orderInCategory;
            mAbstract = _abstract;
            mColor = color;
            mPhotoUrl = photoUrl;
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

        public String getPhotoUrl() {
            return mPhotoUrl;
        }

        @Override
        public int compareTo(Tag another) {
            return mOrderInCategory - another.mOrderInCategory;
        }

        @Override
        public String toString() {
            return "TagMetadata.Tag: id = " + mId + " name = " + mName;
        }
    }
}
