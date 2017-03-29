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
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.feed.data.FeedMessage;
import com.google.samples.apps.iosched.lib.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the {@link RecyclerView} that holds a list of conference updates.
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedViewHolder> {
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
    public void onBindViewHolder(final FeedViewHolder holder, final int position) {
        final FeedMessage feedMessage = mDataset.get(position);

        if (feedMessage.isEmergency()) {
            feedMessage.setExpanded(true);
            holder.expanded = feedMessage.isExpanded();
            holder.updateIconVisibilityForEmergency();
            holder.hasImage = !feedMessage.getImageUrl().isEmpty();
            holder.updateExpandOrCollapse
                    (false, mPaddingNormal, mMessageCardImageWidth, mMessageCardImageHeight);
        } else {
            holder.updateIconVisibilityForNonEmergency();
            holder.expanded = feedMessage.isExpanded();
            holder.updateExpandIcon(false);
            holder.hasImage = !feedMessage.getImageUrl().isEmpty();
            holder.updateExpandOrCollapse
                    (false, mPaddingNormal, mMessageCardImageWidth, mMessageCardImageHeight);
            holder.setOnFeedItemExpandListener(new OnFeedItemExpandListener() {
                @Override
                public void onFeedItemExpand() {
                    holder.updateExpandIcon(true);
                    holder.updateExpandOrCollapse
                            (true, mPaddingNormal, mMessageCardImageWidth, mMessageCardImageHeight);
                    feedMessage.flipExpanded();
                    int pos = holder.getAdapterPosition();
                    notifyItemChanged(pos);
                }
            });
        }
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
}