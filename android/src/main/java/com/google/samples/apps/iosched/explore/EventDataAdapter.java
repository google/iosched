/*
 * Copyright (c) 2016 Google Inc.
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

package com.google.samples.apps.iosched.explore;

import android.app.Activity;
import android.app.usage.UsageEvents;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.data.EventCard;
import com.google.samples.apps.iosched.explore.data.EventData;
import com.google.samples.apps.iosched.map.MapActivity;
import com.google.samples.apps.iosched.session.SessionDetailActivity;
import com.google.samples.apps.iosched.ui.widget.recyclerview.UpdatableAdapter;
import com.google.samples.apps.iosched.util.ActivityUtils;
import com.google.samples.apps.iosched.util.MapUtils;

import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;

/**
 * A {@link RecyclerView.Adapter} for a list of {@link EventData} cards.
 */
public class EventDataAdapter
        extends UpdatableAdapter<List<EventCard>, EventDataAdapter.EventCardViewHolder> {

    private final Activity mHost;

    private final LayoutInflater mInflater;

    private final ColorDrawable[] mBackgroundColors;

    private final List<EventCard> mCards;

    public EventDataAdapter(@NonNull Activity activity,
            @NonNull List<EventCard> eventCards) {
        mHost = activity;
        mInflater = LayoutInflater.from(activity);
        mCards = eventCards;

        // load the background colors
        int[] colors = mHost.getResources().getIntArray(R.array.session_tile_backgrounds);
        mBackgroundColors = new ColorDrawable[colors.length];
        for (int i = 0; i < colors.length; i++) {
            mBackgroundColors[i] = new ColorDrawable(colors[i]);
        }
    }

    @Override
    public EventCardViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new EventCardViewHolder(
                mInflater.inflate(R.layout.explore_io_event_data_item_list_tile, parent, false));
    }

    @Override
    public void onBindViewHolder(final EventCardViewHolder holder, final int position) {
        final EventCard card = mCards.get(position);
        holder.itemView.setBackgroundDrawable(
                mBackgroundColors[position % mBackgroundColors.length]);
        holder.mCardContent = mCards.get(position);
        holder.mTitleView.setText(card.getDescription());
        holder.mActionNameView.setText(card.getActionString());
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    @Override
    public void update(@NonNull final List<EventCard> eventCards) {
        // No-op for this class; no update-able state
    }

    public class EventCardViewHolder extends RecyclerView.ViewHolder {
        final TextView mTitleView;
        final TextView mActionNameView;
        EventCard mCardContent;
        public EventCardViewHolder(View itemView) {
            super(itemView);
            mTitleView = (TextView) itemView.findViewById(R.id.title_text);
            mActionNameView = (TextView) itemView.findViewById(R.id.action_text);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (mCardContent != null && mCardContent.isValid()) {
                        if (EventCard.ACTION_TYPE_LINK.equalsIgnoreCase(mCardContent.getActionType())) {
                            try {
                                Intent myIntent =
                                        new Intent(Intent.ACTION_VIEW,
                                                Uri.parse(mCardContent.getActionUrl()));
                                mHost.startActivity(myIntent);
                                return;
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(mHost, "Browser not available.", Toast.LENGTH_LONG)
                                     .show();
                            }
                        }
                        if (EventCard.ACTION_TYPE_MAP.equalsIgnoreCase(mCardContent.getActionType())) {
                            ActivityUtils.createBackStack(mHost,
                                    new Intent(mHost, MapActivity.class));
                            mHost.finish();
                            return;
                        }
                        if (EventCard.ACTION_TYPE_SESSION.equalsIgnoreCase(mCardContent.getActionType())) {
                            SessionDetailActivity.startSessionDetailActivity(mHost, mCardContent.getActionExtra());
                        }
                    }
                }
            });
        }
    }
}
