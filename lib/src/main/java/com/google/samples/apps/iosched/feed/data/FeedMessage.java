/*
 * Copyright (c) 2017 Google Inc.
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
package com.google.samples.apps.iosched.feed.data;

import android.content.res.Resources;
import android.support.annotation.Keep;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.samples.apps.iosched.lib.R;

import java.util.HashMap;

@Keep
@IgnoreExtraProperties
public class FeedMessage implements Comparable<FeedMessage> {
    private static HashMap<String, Integer> categoryColorMap;
    private static int defaultCategoryColor;
    private static String FEED_CATEGORY_AGENDA;
    private static String FEED_CATEGORY_EVENT;
    private static String FEED_CATEGORY_SOCIAL;
    private static String FEED_CATEGORY_EMERGENCY;
    private static String FEED_CATEGORY_TRAVEL;
    private static String FEED_CATEGORY_GIVEAWAY;
    private static String FEED_CATEGORY_AFTER_HOURS;
    private static String FEED_CATEGORY_VIDEO;

    public int id;
    public String category;
    public String title;
    public String message;
    public boolean clickable;
    public String link;
    public String imageUrl;
    public String imageFileName;
    public String timestamp;
    public boolean active;
    public boolean priority;
    public boolean expanded;

    public FeedMessage() {
    }

    public static void initCategoryColorMap(Resources resources) {
        categoryColorMap = new HashMap<>();

        FEED_CATEGORY_AGENDA = resources.getString(R.string.feed_category_agenda);
        FEED_CATEGORY_EVENT = resources.getString(R.string.feed_category_event);
        FEED_CATEGORY_SOCIAL = resources.getString(R.string.feed_category_social);
        FEED_CATEGORY_EMERGENCY = resources.getString(R.string.feed_category_emergency);
        FEED_CATEGORY_TRAVEL = resources.getString(R.string.feed_category_travel);
        FEED_CATEGORY_GIVEAWAY = resources.getString(R.string.feed_category_giveaway);
        FEED_CATEGORY_AFTER_HOURS = resources.getString(R.string.feed_category_afterhours);
        FEED_CATEGORY_VIDEO = resources.getString(R.string.feed_category_video);

        categoryColorMap.put(FEED_CATEGORY_AGENDA,
                resources.getColor(R.color.feed_category_agenda));
        categoryColorMap.put(FEED_CATEGORY_EVENT,
                resources.getColor(R.color.feed_category_event));
        categoryColorMap.put(FEED_CATEGORY_SOCIAL,
                resources.getColor(R.color.feed_category_social));
        categoryColorMap.put(FEED_CATEGORY_EMERGENCY,
                resources.getColor(R.color.feed_category_emergency));
        categoryColorMap.put(FEED_CATEGORY_TRAVEL,
                resources.getColor(R.color.feed_category_travel));
        categoryColorMap.put(FEED_CATEGORY_GIVEAWAY,
                resources.getColor(R.color.feed_category_giveaway));
        categoryColorMap.put(FEED_CATEGORY_AFTER_HOURS,
                resources.getColor(R.color.feed_category_afterhours));
        categoryColorMap.put(FEED_CATEGORY_VIDEO,
                resources.getColor(R.color.feed_category_video));
        defaultCategoryColor = resources.getColor(R.color.io16_light_grey);
    }

    @Exclude
    @Override
    public int compareTo(FeedMessage o) {
        if (this.priority && !o.priority) {
            return -1;
        } else if (!this.priority && o.priority) {
            return 1;
        } else {
            return (int) (o.getTimestamp() - getTimestamp());
        }
    }

    /*
     * Checks content+id equivalence (everything expanded)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedMessage that = (FeedMessage) o;

        if (id != that.id) return false;
        if (clickable != that.clickable) return false;
        if (active != that.active) return false;
        if (priority != that.priority) return false;
        if (!category.equals(that.category)) return false;
        if (!title.equals(that.title)) return false;
        if (!message.equals(that.message)) return false;
        if (link != null ? !link.equals(that.link) : that.link != null) return false;
        if (imageUrl != null ? !imageUrl.equals(that.imageUrl) : that.imageUrl != null)
            return false;
        if (imageFileName != null ? !imageFileName.equals(that.imageFileName) : that.imageFileName != null)
            return false;
        return timestamp.equals(that.timestamp);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + category.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (clickable ? 1 : 0);
        result = 31 * result + (link != null ? link.hashCode() : 0);
        result = 31 * result + (imageUrl != null ? imageUrl.hashCode() : 0);
        result = 31 * result + (imageFileName != null ? imageFileName.hashCode() : 0);
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + (active ? 1 : 0);
        result = 31 * result + (priority ? 1 : 0);
        return result;
    }

    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public int getCategoryColor() {
        if (categoryColorMap.containsKey(category)) {
            return categoryColorMap.get(category);
        } else {
            return defaultCategoryColor;
        }
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isClickable() {
        return clickable;
    }

    public String getLink() {
        return link;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public long getTimestamp() {
        return Long.parseLong(timestamp);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPriority() {
        return priority;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void flipExpanded() {
        expanded = !expanded;
    }

    public boolean isEmergency() {
        return getCategory().equals(FEED_CATEGORY_EMERGENCY);
    }

    public String getImageFileName() {
        return imageFileName;
    }
}