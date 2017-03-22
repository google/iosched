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
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.feed.data.FeedMessage;
import com.google.samples.apps.iosched.lib.R;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the Adapter for the {@link RecyclerView} that holds a list of conference updates.
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
    private Context mContext;
    private List<FeedMessage> mDataset;

    public FeedAdapter(Context context) {
        mContext = context;
        mDataset = new ArrayList<>();
    }

    @Override
    public FeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater li = LayoutInflater.from(parent.getContext());
        return new FeedViewHolder
                (li.inflate(R.layout.feed_message_card, parent, false));
    }

    @Override
    public void onBindViewHolder(FeedViewHolder holder, int position) {
        Log.d("test", "position == " + position);
        FeedMessage feedMessage = mDataset.get(position);
        holder.title.setText(feedMessage.getTitle());
        holder.description.setText(feedMessage.getTitle());
        //TODO(sigelbaum) set icon based on category.
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public void updateItems(final List<FeedMessage> feedMessages) {
        mDataset = feedMessages;
        notifyDataSetChanged();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView icon;
        TextView description;

        public FeedViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            description = (TextView) itemView.findViewById(R.id.description);
        }
    }


}
