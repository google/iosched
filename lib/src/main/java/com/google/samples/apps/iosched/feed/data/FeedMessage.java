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

public class FeedMessage implements Comparable<FeedMessage> {

    private final int id;
    private final String category;
    private final int categoryColor;
    private final String title;
    private final String description;
    private final boolean clickable;
    private final String link;
    private final String imageUrl;
    private final long date;
    private final boolean active;
    private final boolean priority;
    private boolean expanded;
    public FeedMessage(int id, String category, int categoryColor, String title, String description, boolean clickable, String link, String imageUrl, long date, boolean active, boolean priority) {
        this.id = id;
        this.category = category;
        this.categoryColor = categoryColor;
        this.title = title;
        this.description = description;
        this.clickable = clickable;
        this.link = link;
        this.imageUrl = imageUrl;
        this.date = date;
        this.active = active;
        this.priority = priority;
        this.expanded = false;
    }

    @Override
    public int compareTo(FeedMessage o) {
        if (this.priority && !o.priority) {
            return 1;
        } else if (!this.priority && o.priority) {
            return -1;
        } else {
            return (int) (this.date - o.date);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeedMessage that = (FeedMessage) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public int getCategoryColor() {
        return categoryColor;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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

    public long getDate() {
        return date;
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
        return getCategory().equals(Categories.EMERGENCY);
    }

    public static final class Categories {
        public static final String AGENDA = "Agenda";
        public static final String EVENT = "Event";
        public static final String SOCIAL = "Social";
        public static final String EMERGENCY = "Emergency";
        public static final String TRAVEL = "Travel";
        public static final String GIVEAWAY = "Giveaway";
        public static final String AFTER_HOURS = "After Hours";
        public static final String VIDEO = "Video";

    }
}