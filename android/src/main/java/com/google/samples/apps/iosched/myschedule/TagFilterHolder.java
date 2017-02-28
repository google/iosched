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

package com.google.samples.apps.iosched.myschedule;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Class responsible for storing, managing and retrieving Tag filters used in the filter drawer.
 */
public class TagFilterHolder implements Parcelable {

    private static final String TAG = makeLogTag(TagFilterHolder.class);

    // TODO we should work with Tags directly
    private final Set<String> mSelectedTopics;
    private boolean mShowLiveStreamedOnly;
    private boolean mShowSessionsOnly;

    public TagFilterHolder() {
        mSelectedTopics = new HashSet<>();
    }

    /**
     * @param tagId The tagId to check in the filter
     * @return boolean Return a boolean indicating that the tagId is present.
     */
    public boolean contains(String tagId) {
        return mSelectedTopics.contains(tagId);
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
        return isCategoryValid(category) && mSelectedTopics.add(tagId);
    }

    /**
     * @param tagId    Tag to be remove from the filter set.
     * @param category The category of the tag being removed.
     * @return boolean Returns a boolean to indicate whether the operation was successful.
     */
    public boolean remove(String tagId, String category) {
        return isCategoryValid(category) && mSelectedTopics.remove(tagId);
    }

    public void clear() {
        mSelectedTopics.clear();
        mShowLiveStreamedOnly = false;
        mShowSessionsOnly = false;
    }

    /**
     * @return String[] containing all the tags from all the categories.
     */
    public String[] toStringArray() {
        return mSelectedTopics.toArray(new String[mSelectedTopics.size()]);
    }

    /**
     * @return An unmodifiable set with all the filters.
     */
    public Set<String> getSelectedTopics() {
        return Collections.unmodifiableSet(mSelectedTopics);
    }

    /**
     * Returns the number of tag categories for the filter query against the content provider.
     * Currently we only use track tags, so this is always 1.
     */
    public int getCategoryCount() {
        return 1;
    }

    /**
     * @return The number of topic filters currently in use. Note that non-topic filters are not
     * considered; use {@link #hasAnyFilters()} instead if trying to determine whether any filters
     * are active.
     */
    public int getSelectedTopicsCount() {
        return mSelectedTopics.size();
    }

    /**
     * @return true if any filters are active, including non-topic filters (e.g. live streamed)
     */
    public boolean hasAnyFilters() {
        return mShowLiveStreamedOnly || mShowSessionsOnly || !mSelectedTopics.isEmpty();
    }

    /**
     * @param show whether only live streamed events should be shown
     */
    public void setShowLiveStreamedOnly(boolean show) {
        mShowLiveStreamedOnly = show;
    }

    /**
     * @return true if only live streamed events should be shown, false otherwise
     */
    public boolean showLiveStreamedOnly() {
        return mShowLiveStreamedOnly;
    }

    /**
     * @param show whether only sessions should be shown
     */
    public void setShowSessionsOnly(boolean show) {
        mShowSessionsOnly = show;
    }

    /**
     * @return true if only sessions should be shown, false otherwise
     */
    public boolean showSessionsOnly() {
        return mShowSessionsOnly;
    }

    private static boolean isCategoryValid(String category) {
        return Config.Tags.CATEGORY_TRACK.equals(category); // we only care about tracks
    }

    public CharSequence describeFilters(Resources res, Theme theme) {
        if (!hasAnyFilters()) {
            return null;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(res.getString(R.string.active_filters_description));
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // TODO this could probably be cleaner
        int filterNamesStart = builder.length();
        boolean needComma = false;
        if (mShowLiveStreamedOnly) {
            builder.append(res.getString(R.string.session_live_streamed));
            needComma = true;
        }
        if (mShowSessionsOnly) {
            if (needComma) {
                builder.append(", ");
            }
            builder.append(res.getString(R.string.filter_label_sessions_only));
            needComma = true;
        }
        for (String tag : toStringArray()) {
            if (needComma) {
                builder.append(", ");
            }
            builder.append(tag);
            needComma = true;
        }

        int color = ResourcesCompat.getColor(res, R.color.theme_primary, theme);
        builder.setSpan(new ForegroundColorSpan(color), filterNamesStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(mSelectedTopics.toArray(new String[mSelectedTopics.size()]));
        dest.writeInt(mShowLiveStreamedOnly ? 1 : 0);
        dest.writeInt(mShowSessionsOnly ? 1 : 0);
    }

    public static final Creator<TagFilterHolder> CREATOR = new Creator<TagFilterHolder>() {

        @Override
        public TagFilterHolder createFromParcel(Parcel in) {
            TagFilterHolder holder = new TagFilterHolder();

            String[] filters = in.createStringArray();
            Collections.addAll(holder.mSelectedTopics, filters);

            holder.mShowLiveStreamedOnly = in.readInt() == 1;
            holder.mShowSessionsOnly = in.readInt() == 1;

            return holder;
        }

        @Override
        public TagFilterHolder[] newArray(int size) {
            return new TagFilterHolder[size];
        }
    };
}
