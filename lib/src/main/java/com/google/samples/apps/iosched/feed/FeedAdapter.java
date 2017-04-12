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
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.feed.data.FeedMessage;

/**
 * Adapter for the {@link RecyclerView} that holds a list of conference updates.
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedViewHolder> {

    private static final int MIN_CAPACITY = 10;

    private SortedList<FeedMessage> mDataset;

    FeedAdapter(Context context) {
        mDataset = new SortedList<>(FeedMessage.class, new FeedMessageCallback(), MIN_CAPACITY);
        mDataset.add(FeedMessage.getDefaultFirstMessage(context));
    }

    @Override
    public FeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return FeedViewHolder.newInstance(parent);
    }

    @Override
    public void onBindViewHolder(final FeedViewHolder holder, final int position) {
        holder.bind(mDataset.get(position));
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    void addFeedMessage(final FeedMessage feedMessage) {
        if (feedMessage.isActive()) {
            mDataset.add(feedMessage);
        }
    }

    void updateFeedMessage(final FeedMessage feedMessage) {
        if (!feedMessage.isActive()) {
            removeFeedMessage(feedMessage);
        } else {
            boolean found = false;
            for (int i = 0; i < mDataset.size(); i++) {
                if (mDataset.get(i) != null) {
                    if (feedMessage.getId() == mDataset.get(i).getId()) {
                        mDataset.updateItemAt(i, feedMessage);
                        found = true;
                    }
                }
            }
            if (!found) {
                mDataset.add(feedMessage);
            }
        }
    }

    void removeFeedMessage(final FeedMessage feedMessage) {
        mDataset.remove(feedMessage);
    }

    private class FeedMessageCallback extends SortedList.Callback<FeedMessage> {
        @Override
        public int compare(FeedMessage o1, FeedMessage o2) {
            return o1.compareTo(o2);
        }

        @Override
        public void onChanged(final int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(FeedMessage oldItem, FeedMessage newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(FeedMessage item1, FeedMessage item2) {
            return item1.getId() == (item2.getId());
        }

        @Override
        public void onInserted(final int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }
    }
}