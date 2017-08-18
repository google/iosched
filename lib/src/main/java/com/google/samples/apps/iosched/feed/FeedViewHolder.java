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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.samples.apps.iosched.feed.data.FeedMessage;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.ui.widget.HtmlTextView;
import com.google.samples.apps.iosched.util.UIUtils;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.samples.apps.iosched.util.UIUtils.formatDateTime;

class FeedViewHolder extends RecyclerView.ViewHolder {

    private static final int COLLAPSED_DESC_MAX_LINES = 3;
    private static final int EXPANDED_DESC_MAX_LINES = 30;

    private final ConstraintSet collapsedConstraints = new ConstraintSet();
    private final ConstraintSet expandedConstraints = new ConstraintSet();
    private final View.OnTouchListener touchIgnorer = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    };

    private boolean expanded = false;
    private ConstraintLayout mainLayout;
    private TextView title;
    private TextView dateTime;
    private ImageView image;
    private HtmlTextView description;
    private TextView category;
    private ImageView expandIcon;
    private ImageView emergencyIcon;
    private ImageView priorityIcon;
    private Point mScreenSize;
    private
    @Nullable
    FeedMessage feedMessage;

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
        expandIcon = (ImageView) itemView.findViewById(R.id.expand_icon);
        if (SDK_INT < M) {
            expandIcon.setImageTintList(AppCompatResources.getColorStateList(
                    expandIcon.getContext(), R.color.collapsing_section));
        }
        emergencyIcon = (ImageView) itemView.findViewById(R.id.emergency_icon);
        priorityIcon = (ImageView) itemView.findViewById(R.id.priority_icon);
        final View.OnClickListener expandClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Use Transitions to smoothly animate the height change. We need to run the
                // transition over the entire RecyclerView to smoothly move other items as this one
                // expands/collapses. This doesn't play too nicely with RecyclerView as it is not
                // aware that it's items are being animated… but there's no other way to do this
                // performantly. Need to prevent touches during the transition else the item's
                // bounds can be messed up.
                final RecyclerView parent = (RecyclerView) itemView.getParent();
                final Transition transition = TransitionInflater.from(itemView.getContext())
                        .inflateTransition(expanded ? R.transition.feed_message_collapse
                                : R.transition.feed_message_expand);
                parent.setOnTouchListener(touchIgnorer);
                transition.addListener(new UIUtils.TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition) {
                        parent.setOnTouchListener(null);
                    }
                });
                TransitionManager.beginDelayedTransition(parent, transition);
                if (feedMessage != null) {
                    feedMessage.flipExpanded();
                    bind(feedMessage);
                }
            }
        };
        collapsedConstraints.clone(mainLayout);
        expandedConstraints.clone(mainLayout.getContext(), R.layout.feed_message_card_expanded);
        title.setOnClickListener(expandClick);
    }

    private void setExpanded(boolean isExpanded, String titleText) {
        if (expanded == isExpanded) return;
        expanded = isExpanded;
        if (expanded) {
            expandedConstraints.applyTo(mainLayout);
        } else {
            collapsedConstraints.applyTo(mainLayout);
        }
        setTitleContentDescription(titleText);
    }

    void bind(@NonNull FeedMessage message) {
        feedMessage = message;
        setExpanded(feedMessage.isExpanded(), feedMessage.getTitle());
        dateTime.setText(formatDateTime(feedMessage.getTimestamp(), itemView.getContext()));
        priorityIcon.setVisibility(feedMessage.isPriority() ? VISIBLE : GONE);
        category.setText(feedMessage.getCategory());
        category.setBackgroundTintList(ColorStateList.valueOf(feedMessage.getCategoryColor()));
        updateEmergencyStatus(feedMessage.isEmergency());
        title.setText(feedMessage.getTitle());
        setTitleContentDescription(feedMessage.getTitle());
        expandIcon.setActivated(expanded);
        expandIcon.setRotation(expanded ? 180f : 0f);
        if (!TextUtils.isEmpty(feedMessage.getImageUrl())) {
            image.setVisibility(VISIBLE);
            Glide.with(image.getContext())
                    // Add "=s" query string to scale down GCS image to longest dimension (supposed
                    // to be width in this case) without affecting the original aspect ratio. This
                    // is the max resolution that Glide will download.
                    .load(feedMessage.getImageUrl() + "=s" + getScreenWidth(image.getContext()))
                    // Override so Glide knows to not scale image resolution down to fit thumbnail.
                    // Otherwise, image would keep the resolution of the thumbnail size even when
                    // it is expanded.
                    .override(getScreenWidth(image.getContext()),
                            (int) (getScreenWidth(image.getContext()) * 9.0 / 16))
                    .placeholder(R.drawable.io17_logo)
                    .into(image);
        } else {
            image.setVisibility(GONE);
        }
        description.setHtmlText(feedMessage.getMessage());
        int maxLines = expanded ? EXPANDED_DESC_MAX_LINES : COLLAPSED_DESC_MAX_LINES;
        description.setMaxLines(maxLines);
    }

    private void updateEmergencyStatus(boolean isEmergency) {
        emergencyIcon.setVisibility(isEmergency ? VISIBLE : GONE);
        expandIcon.setVisibility(isEmergency ? GONE : VISIBLE);
        title.setActivated(isEmergency);
        category.setActivated(isEmergency);
    }

    private void setTitleContentDescription(String titleText) {
        Context context = expandIcon.getContext();
        title.setContentDescription(titleText + ", " +
                (expanded ? context.getString(R.string.expanded) :
                        context.getString(R.string.collapsed)));
    }

    private int getScreenWidth(Context context) {
        if(mScreenSize == null) {
            mScreenSize = new Point();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                    .getSize(mScreenSize);
        }
        return mScreenSize.x;
    }
}