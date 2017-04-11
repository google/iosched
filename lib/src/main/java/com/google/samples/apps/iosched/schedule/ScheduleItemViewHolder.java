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
package com.google.samples.apps.iosched.schedule;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.Date;


abstract class ScheduleItemViewHolder extends RecyclerView.ViewHolder {

    private static final StringBuilder mTmpStringBuilder = new StringBuilder();

    ScheduleItemViewHolder(View itemView) {
        super(itemView);
    }

    String formatDescription(@NonNull Context context,
                             @NonNull final ScheduleItem item) {
        final StringBuilder description = mTmpStringBuilder;
        mTmpStringBuilder.setLength(0); // clear the builder

        description.append(TimeUtils.formatShortTime(context, new Date(item.startTime)));
        if (!Config.Tags.SPECIAL_KEYNOTE.equals(item.mainTag)) {
            description.append(" - ");
            description.append(TimeUtils.formatShortTime(context, new Date(item.endTime)));
        }
        if (!TextUtils.isEmpty(item.room)) {
            description.append(" / ");
            description.append(item.room);
        }
        return description.toString();
    }
}
