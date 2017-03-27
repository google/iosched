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
package com.google.samples.apps.iosched.feed;

import android.content.Context;
import android.graphics.Point;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.samples.apps.iosched.feed.data.FeedMessage;
import com.google.samples.apps.iosched.lib.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the {@link RecyclerView} that holds a list of conference updates.
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
    private static final int SHORT_DESCRIPTION_MAX_LINES = 3;
    private static final int LONG_DESCRIPTION_MAX_LINES = 30;

    private final Point mScreenSize;
    private Context mContext;
    private List<FeedMessage> mDataset;
    private int mPaddingNormal;
    private int mMessageCardImageWidth;
    private int mMessageCardImageHeight;

    public FeedAdapter(Context context, Point screenSize) {
        mContext = context;
        mDataset = new ArrayList<>();
        mScreenSize = screenSize;
        mPaddingNormal =
                (int) context.getResources().getDimension(R.dimen.padding_normal);
        mMessageCardImageWidth = ((int) context.getResources()
                .getDimension(R.dimen.feed_message_card_image_width));
        mMessageCardImageHeight = ((int) context.getResources()
                .getDimension(R.dimen.feed_message_card_image_height));
    }

    @Override
    public FeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater li = LayoutInflater.from(parent.getContext());
        return new FeedViewHolder
                (li.inflate(R.layout.feed_message_card, parent, false));
    }

    @Override
    public void onBindViewHolder(FeedViewHolder holder, int position) {
        FeedMessage feedMessage = mDataset.get(position);
        holder.expanded = feedMessage.isExpanded();
        holder.hasImage = !feedMessage.getImageUrl().isEmpty();
        holder.updateExpandOrCollapse
                (false, mPaddingNormal, mMessageCardImageWidth, mMessageCardImageHeight);
        holder.setExpandListener
                (this, position, feedMessage, mPaddingNormal, mMessageCardImageWidth, mMessageCardImageHeight);
        holder.updateTitle(feedMessage.getTitle());
        holder.updateDateTime(feedMessage.getDate());
        holder.updateDescription(feedMessage.getDescription());
        holder.updateImage(mContext, mScreenSize, feedMessage.getImageUrl());
        holder.updateCategory(feedMessage.getCategory(), feedMessage.getCategoryColor());
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void updateItems(final List<FeedMessage> feedMessages) {
        mDataset = feedMessages;
        notifyDataSetChanged();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout mainLayout;
        TextView title;
        TextView dateTime;
        ImageView image;
        TextView description;
        TextView category;
        LinearLayout imageDescriptionLayout;
        ImageView expandIcon;
        boolean expanded;
        boolean hasImage;

        public FeedViewHolder(View itemView) {
            super(itemView);
            mainLayout = (RelativeLayout) itemView.findViewById(R.id.feedMessageCardLayout);
            title = (TextView) itemView.findViewById(R.id.title);
            dateTime = (TextView) itemView.findViewById(R.id.dateTime);
            image = (ImageView) itemView.findViewById(R.id.image);
            description = (TextView) itemView.findViewById(R.id.description);
            category = (TextView) itemView.findViewById(R.id.categoryText);
            imageDescriptionLayout =
                    (LinearLayout) itemView.findViewById(R.id.imageDescriptionLayout);
            expandIcon = (ImageView) itemView.findViewById(R.id.expandIcon);
            expanded = false;
            hasImage = false;
        }

        public void updateCategory(String categoryString, int color) {
            category.setText(categoryString);
            category.setBackgroundColor(color);
        }

        public void updateImage(Context context, Point screenSize, String imageUrlString) {
            if (imageUrlString.isEmpty()) {
                image.setVisibility(View.GONE);
            }
            Glide.with(context)
                    .load(imageUrlString)
                    .override(screenSize.x, screenSize.x * 9 / 16) // Guaranteed 16:9 aspect ratio
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model,
                                Target<GlideDrawable> target, boolean isFirstResource) {
                            hasImage = false;
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model,
                                Target<GlideDrawable> target, boolean isFromMemoryCache,
                                boolean isFirstResource) {
                            hasImage = true;
                            return false;
                        }
                    })
                    .into(image);
        }

        public void updateDescription(String descriptionString) {
            description.setText(descriptionString);
            if (expanded) {
                description.setMaxLines(Integer.MAX_VALUE);
            } else {
                description.setMaxLines(SHORT_DESCRIPTION_MAX_LINES);
            }
        }

        public void updateDateTime(long unixTime) {
            DateFormat dateFormat = DateFormat
                    .getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
            dateTime.setText(dateFormat.format(unixTime));
        }

        public void updateTitle(String titleString) {
            title.setText(titleString);
        }

        public void setExpandListener(final FeedAdapter feedAdapter, final int position, final FeedMessage feedMessage, final int paddingNormal,
                final int messageCardImageWidth, final int messageCardImageHeight) {
            expandIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    expanded = !expanded;
                    updateExpandOrCollapse(true, paddingNormal, messageCardImageWidth,
                            messageCardImageHeight);
                    feedMessage.flipExpanded();
                    feedAdapter.notifyItemChanged(position);
                }
            });
        }

        public void updateExpandOrCollapse(boolean changed, int paddingNormal, int messageCardImageWidth,
                int messageCardImageHeight) {
            rotateExpandIcon(changed);
            LinearLayout.LayoutParams imageLayoutParams =
                    (LinearLayout.LayoutParams) image.getLayoutParams();
            LinearLayout.LayoutParams descriptionLayoutParams =
                    (LinearLayout.LayoutParams) description.getLayoutParams();
            if (expanded) {
                imageDescriptionLayout.setOrientation(LinearLayout.VERTICAL);
                imageLayoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                imageLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                descriptionLayoutParams.setMarginStart(0);
                description.setMaxLines(LONG_DESCRIPTION_MAX_LINES);
            } else {
                imageDescriptionLayout.setOrientation(LinearLayout.HORIZONTAL);
                imageLayoutParams.width = messageCardImageWidth;
                imageLayoutParams.height = messageCardImageHeight;
                if (hasImage) {
                    descriptionLayoutParams.setMarginStart(paddingNormal);
                }
                description.setMaxLines(SHORT_DESCRIPTION_MAX_LINES);
            }
            TransitionManager.beginDelayedTransition(imageDescriptionLayout);
        }

        private void rotateExpandIcon(boolean changed) {
            float rotationAngle = expanded ? 180f : 0f;
            if (changed) {
                expandIcon.animate().rotation(rotationAngle).start();
            } else {
                expandIcon.setRotation(rotationAngle);
            }
        }
    }
}