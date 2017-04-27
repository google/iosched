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

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.samples.apps.iosched.lib.R;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

@Keep
@IgnoreExtraProperties
public class FeedMessage implements Comparable<FeedMessage> {
    private static final String TAG = makeLogTag(FeedMessage.class);
    private static final String FEED_CATEGORY_EMERGENCY = "Emergency";

    public int id;
    public String category;
    public String title;
    public String message;
    public String imageUrl;
    public String imageFileName;
    public String timestamp;
    public boolean active;
    public boolean priority;
    public boolean expanded;
    public String color;

    public FeedMessage() {
    }

    public static FeedMessage getDefaultFirstMessage(final Context context) {
        final FeedMessage message = new FeedMessage();
        applyRemoteConfigToMessage(message, context);
        FirebaseRemoteConfig.getInstance().fetch().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                FirebaseRemoteConfig.getInstance().activateFetched();
                applyRemoteConfigToMessage(message, context);
            }
        });
        return message;
    }

    private static void applyRemoteConfigToMessage(FeedMessage message, Context context) {
        message.id = 0;
        message.category = FirebaseRemoteConfig.getInstance().getString(
                context.getString(R.string.feed_default_first_category_key));
        message.title = FirebaseRemoteConfig.getInstance().getString(
                context.getString(R.string.feed_default_first_title_key));
        message.message = FirebaseRemoteConfig.getInstance().getString(
                context.getString(R.string.feed_default_first_message_key));
        message.imageUrl = FirebaseRemoteConfig.getInstance().getString(
                context.getString(R.string.feed_default_first_imageurl_key));
        message.imageFileName = "";
        message.timestamp = FirebaseRemoteConfig.getInstance().getString(
                context.getString(R.string.feed_default_first_timestamp_key));
        message.active = true;
        message.priority = FirebaseRemoteConfig.getInstance().getBoolean(
                context.getString(R.string.feed_default_first_priority_key));
        message.color = FirebaseRemoteConfig.getInstance().getString(
                context.getString(R.string.feed_default_first_color_key));
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
        if (active != that.active) return false;
        if (priority != that.priority) return false;
        if (!category.equals(that.category)) return false;
        if (!title.equals(that.title)) return false;
        if (!message.equals(that.message)) return false;
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
        // Fallback color if it can't parse the color string.
        int categoryColor = 0xe6e6e6;

        try {
            categoryColor = Color.parseColor(color);
        } catch (Exception e) {
            LOGD(TAG, e.getMessage());
        }
        return categoryColor;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
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
        return expanded || isEmergency();
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean flipExpanded() {
        expanded = !expanded;
        return expanded;
    }

    public boolean isEmergency() {
        return getCategory().equals(FEED_CATEGORY_EMERGENCY);
    }
}