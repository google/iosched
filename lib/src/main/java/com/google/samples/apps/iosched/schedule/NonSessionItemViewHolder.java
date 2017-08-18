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

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.ScheduleItem;

/**
 * A {@link RecyclerView.ViewHolder} modeling non-sessions items in the schedule i.e. breaks,
 * codelabs, concert etc.
 */
public class NonSessionItemViewHolder extends ScheduleItemViewHolder
        implements DividerDecoration.Divided {

    private final TextView mTitle;
    private final TextView mDescription;
    private final ImageView mIcon;

    private NonSessionItemViewHolder(View itemView, SessionTimeFormat timeFormat) {
        super(itemView, timeFormat);
        mTitle = (TextView) itemView.findViewById(R.id.slot_title);
        mDescription = (TextView) itemView.findViewById(R.id.slot_description);
        mIcon = (ImageView) itemView.findViewById(R.id.slot_icon);
    }

    public static NonSessionItemViewHolder newInstance(ViewGroup parent) {
        return new NonSessionItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.schedule_non_session_item, parent, false), SessionTimeFormat.SPAN);
    }

    public void bind(@NonNull final ScheduleItem item) {
        if (item.type != ScheduleItem.BREAK) {
            return;
        }

        mTitle.setText(item.title);
        setDescription(mDescription, item);
        mIcon.setImageResource(item.getBreakIcon());
    }

}
