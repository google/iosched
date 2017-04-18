/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.google.samples.apps.iosched.feed;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.samples.apps.iosched.feed.data.FeedMessage;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.ui.widget.HtmlTextView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.samples.apps.iosched.util.UIUtils.formatDateTime;

class FeedViewHolder extends RecyclerView.ViewHolder {

    private static final int COLLAPSED_DESC_MAX_LINES = 3;
    private static final int EXPANDED_DESC_MAX_LINES = 30;

    boolean expanded = false;
    private ConstraintLayout mainLayout;
    private ConstraintSet collapsedConstraints = new ConstraintSet();
    private ConstraintSet expandedConstraints = new ConstraintSet();
    private TextView title;
    private TextView dateTime;
    private ImageView image;
    private HtmlTextView description;
    private TextView category;
    private ImageButton expandIcon;
    private ImageView emergencyIcon;
    private ImageView priorityIcon;
    private @Nullable FeedMessage feedMessage;

    public static FeedViewHolder newInstance(@NonNull ViewGroup parent) {
        return new FeedViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.feed_message_card_collapsed, parent, false));
    }

    private FeedViewHolder(final View itemView) {
        super(itemView);
        mainLayout = (ConstraintLayout) itemView;
        title = (TextView) itemView.findViewById(R.id.title);
        dateTime = (TextView) itemView.findViewById(R.id.date_time);
        image = (ImageView) itemView.findViewById(R.id.image);
        description = (HtmlTextView) itemView.findViewById(R.id.description);
        category = (TextView) itemView.findViewById(R.id.category_text);
        expandIcon = (ImageButton) itemView.findViewById(R.id.expand_icon);
        emergencyIcon = (ImageView) itemView.findViewById(R.id.emergency_icon);
        priorityIcon = (ImageView) itemView.findViewById(R.id.priority_icon);
        final View.OnClickListener expandClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(mainLayout, TransitionInflater.from(
                        itemView.getContext()).inflateTransition(expanded ?
                        R.transition.feed_message_collapse : R.transition.feed_message_expand));
                feedMessage.flipExpanded();
                bind(feedMessage);
            }
        };
        expandIcon.setOnClickListener(expandClick);
        title.setOnClickListener(expandClick);
        collapsedConstraints.clone(mainLayout);
        expandedConstraints.clone(mainLayout.getContext(), R.layout.feed_message_card_expanded);
    }

    private void setExpanded(boolean isExpanded) {
        if (expanded == isExpanded) return;
        expanded = isExpanded;
        if (expanded) {
            expandedConstraints.applyTo(mainLayout);
        } else {
            collapsedConstraints.applyTo(mainLayout);
        }
    }

    void bind(@NonNull FeedMessage message) {
        feedMessage = message;
        setExpanded(feedMessage.isExpanded());
        dateTime.setText(formatDateTime(feedMessage.getTimestamp(), itemView.getContext()));
        priorityIcon.setVisibility(feedMessage.isPriority() ? VISIBLE : GONE);
        category.setText(feedMessage.getCategory());
        category.setBackgroundTintList(ColorStateList.valueOf(feedMessage.getCategoryColor()));
        updateEmergencyStatus(feedMessage.isEmergency());
        title.setText(feedMessage.getTitle());
        expandIcon.setActivated(expanded);
        expandIcon.setRotation(expanded ? 180f : 0f);
        if (!TextUtils.isEmpty(feedMessage.getImageUrl())) {
            image.setVisibility(VISIBLE);
            Glide.with(image.getContext())
                    .load(feedMessage.getImageUrl())
                    .into(image);
        } else {
            image.setVisibility(GONE);
        }
        description.setHtmlText(feedMessage.getMessage());
        int maxLines = expanded ? EXPANDED_DESC_MAX_LINES : COLLAPSED_DESC_MAX_LINES;
        description.setMaxLines(maxLines);
        setClickListener(feedMessage.isClickable(), feedMessage.getLink());
    }

    private void updateEmergencyStatus(boolean isEmergency) {
        emergencyIcon.setVisibility(isEmergency ? VISIBLE : GONE);
        expandIcon.setVisibility(isEmergency ? GONE : VISIBLE);
        title.setActivated(isEmergency);
        category.setActivated(isEmergency);
    }

    private void setClickListener(boolean isClickable, final String link) {
        if (TextUtils.isEmpty(link)) {
            return;
        }
        itemView.setClickable(isClickable);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                linkIntent.setData(Uri.parse(link));
                itemView.getContext().startActivity(linkIntent);
            }
        });
    }
}