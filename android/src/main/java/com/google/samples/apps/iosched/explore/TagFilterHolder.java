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

package com.google.samples.apps.iosched.explore;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.samples.apps.iosched.Config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Class responsible for storing, managing and retrieving Tag filters used in {@link
 * ExploreSessionsActivity}.
 */
public class TagFilterHolder implements Parcelable {

    public static final int CATEGORY_THEME = 0;
    public static final int CATEGORY_TYPE = 1;
    public static final int CATEGORY_TRACK = 2;
    public static final int CATEGORY_INVALID = -1;

    private static final String TAG = makeLogTag(TagFilterHolder.class);

    private final Set<String> mSelectedFilters;
    private final int[] mCategories;
    private boolean mShowLiveStreamedSessions;

    TagFilterHolder() {
        mSelectedFilters = new HashSet<>();
        mCategories = new int[3];
        mCategories[CATEGORY_THEME] = 0;
        mCategories[CATEGORY_TYPE] = 0;
        mCategories[CATEGORY_TRACK] = 0;
    }

    /**
     * @param tagId The tagId to check in the filter
     * @return boolean Return a boolean indicating that the tagId is present.
     */
    public boolean contains(String tagId) {
        return mSelectedFilters.contains(tagId);
    }

    /**
     * Add a tagId to the set of filters. Use the category to update the count of the specific
     * category.
     *
     * @param tagId    The tagId to be included in the filter.
     * @param category The category associated with the given tagId.
     * @return boolean Returns a boolean to indicate whether the operation was successful.
     */
    public boolean add(String tagId, String category) {
        if (isCategoryValid(category)) {
            boolean added = mSelectedFilters.add(tagId);
            if (added) {
                mCategories[categoryId(category)]++;
            }
            return added;
        } else {
            return false;
        }
    }

    /**
     * @param tagId    Tag to be remove from the filter set.
     * @param category The category of the tag being removed.
     * @return boolean Returns a boolean to indicate whether the operation was successful.
     */
    public boolean remove(String tagId, String category) {
        if (isCategoryValid(category)) {
            boolean removed = mSelectedFilters.remove(tagId);
            if (removed) {
                mCategories[categoryId(category)]--;
            }
            return removed;
        } else {
            return false;
        }
    }

    /**
     * @return String[] containing all the tags from all the categories.
     */
    public String[] toStringArray() {
        return mSelectedFilters.toArray(new String[mSelectedFilters.size()]);
    }

    /**
     * @return An unmodifiable set with all the filters.
     */
    public Set<String> getSelectedFilters() {
        return Collections.unmodifiableSet(mSelectedFilters);
    }

    /**
     * Method that returns the number of categories that are in use by this instance. At least 1 and
     * at most 3 categories can be returned by this method.
     * <p/>
     * Example: 1. If there are 2 topics and 1 theme the result would be 2 indicating that two
     * categories are in use by this filter. 2. If there are 2 topics, 2 themes and 3 types then the
     * result would be 3 to indicate the non-zero presence of each category.
     *
     * @return categoryCount Return the number of non categories in this instance.
     */
    public int getCategoryCount() {
        return Math.max(1,
                (mCategories[CATEGORY_THEME] > 0 ? 1 : 0) +
                        (mCategories[CATEGORY_TYPE] > 0 ? 1 : 0) +
                        (mCategories[CATEGORY_TRACK] > 0 ? 1 : 0));
    }

    /**
     * @return Returns whether the collection is empty
     */
    public boolean isEmpty() {
        return mSelectedFilters.isEmpty();
    }

    /**
     * @return Returns the number of filters currently in use.
     */
    public int size() {
        return mSelectedFilters.size();
    }

    /**
     * @param show Set a boolean to indicate whether live streamed sessions should be shown
     */
    public void setShowLiveStreamedSessions(boolean show) {
        this.mShowLiveStreamedSessions = show;
    }

    /**
     * @return Returns whether a live streamed sessions shown be shown.
     */
    public boolean isShowLiveStreamedSessions() {
        return mShowLiveStreamedSessions;
    }

    /**
     * @param category The category to look up.
     * @return Return the number of entries for the given category.
     */
    public int getCountByCategory(String category) {
        if (isCategoryValid(category)) {
            return mCategories[categoryId(category)];
        } else {
            return 0;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(mSelectedFilters.toArray(new String[mSelectedFilters.size()]));
        dest.writeIntArray(mCategories);
        dest.writeInt(mShowLiveStreamedSessions ? 1 : 0);
    }

    /**
     * @param category Must be one of {@link Config.Tags#CATEGORY_THEME}, {@link
     *                 Config.Tags#CATEGORY_TYPE} or{@link Config.Tags#CATEGORY_TRACK}. To verify if
     *                 the {@code category is valid}, call {@link #isCategoryValid(String)}.
     * @return the id of the {@code category}, or {@link #CATEGORY_INVALID}, if the {@code category}
     * isn't valid.
     */
    private static int categoryId(String category) {
        switch (category) {
            case Config.Tags.CATEGORY_THEME:
                return TagFilterHolder.CATEGORY_THEME;
            case Config.Tags.CATEGORY_TYPE:
                return TagFilterHolder.CATEGORY_TYPE;
            case Config.Tags.CATEGORY_TRACK:
                return TagFilterHolder.CATEGORY_TRACK;
            default:
                LOGE(TAG, "Invalid category found " + category);
                return TagFilterHolder.CATEGORY_INVALID;
        }
    }

    private static boolean isCategoryValid(String category) {
        switch (category) {
            case Config.Tags.CATEGORY_THEME:
            case Config.Tags.CATEGORY_TYPE:
            case Config.Tags.CATEGORY_TRACK:
                return true;
            default:
                LOGE(TAG, "Invalid category found " + category);
                return false;
        }
    }

    public static final Creator CREATOR = new Creator() {

        public TagFilterHolder createFromParcel(Parcel in) {
            TagFilterHolder holder = new TagFilterHolder();

            String[] filters = in.createStringArray();
            Collections.addAll(holder.mSelectedFilters, filters);

            int[] categories = in.createIntArray();
            System.arraycopy(categories, 0, holder.mCategories, 0, categories.length);

            holder.mShowLiveStreamedSessions = in.readInt() == 1;

            return holder;
        }

        public TagFilterHolder[] newArray(int size) {
            return new TagFilterHolder[size];
        }
    };
}
