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
package com.google.samples.apps.iosched.myschedule;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;

import java.util.ArrayList;
import java.util.List;

public class SessionsFilterAdapter extends Adapter<ViewHolder> {

    private static final String TAG = makeLogTag(SessionsFilterAdapter.class);

    private static final int TYPE_FILTER = 0;
    private static final int TYPE_LIVE_STREAM_FILTER = 1;
    private static final int TYPE_DIVIDER = 2;

    private final List mItems;
    private TagFilterHolder mTagFilterHolder;
    private final LayoutInflater mInflater;

    private SessionFilterAdapterListener mListener;

    public interface SessionFilterAdapterListener {
        void reloadFragment();
        void updateFilters(Tag filter, boolean checked);
    }

    public SessionsFilterAdapter(Context context, TagFilterHolder tagFilterHolder,
            TagMetadata filters) {
        mInflater = LayoutInflater.from(context);
        mItems = new ArrayList();
        mTagFilterHolder =tagFilterHolder;
        processFilters(filters);
    }

    public void setSessionFilterAdapterListener(SessionFilterAdapterListener listener) {
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case TYPE_FILTER:
                return createFilterViewHolder(parent);
            case TYPE_LIVE_STREAM_FILTER:
                return createLiveStreamFilterViewHolder(parent);
            case TYPE_DIVIDER:
                return createDividerViewHolder(parent);
            default:
                LOGE(TAG, "Unknown view type");
                throw new IllegalArgumentException("Unknown view type");
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        switch (getItemViewType(position)) {
            case TYPE_FILTER:
                bindFilter((FilterViewHolder) holder, (TagMetadata.Tag) mItems.get(position));
                break;
            case TYPE_LIVE_STREAM_FILTER:
                bindLiveStreamFilter((FilterViewHolder) holder);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(final int position) {
        Object item = mItems.get(position);
        if (item instanceof LiveStream) {
            return TYPE_LIVE_STREAM_FILTER;
        } else {
            if (item instanceof Divider) {
                return TYPE_DIVIDER;
            }
        }
        return TYPE_FILTER;
    }

    /**
     * We transform the provided data into a structure suitable for the RecyclerView i.e. we build
     * up {@link #mItems}, adding 'marker' items for dividers & live stream.
     */
    private void processFilters(TagMetadata tagMetadata) {
        List<TagMetadata.Tag> themes = tagMetadata.getTagsInCategory(Config.Tags.CATEGORY_THEME);
        if (themes != null && !themes.isEmpty()) {
            for (TagMetadata.Tag theme : themes) {
                mItems.add(theme);
            }
        }
        mItems.add(new Divider());

        List<TagMetadata.Tag> sessionTypes = tagMetadata.getTagsInCategory(
                Config.Tags.CATEGORY_TYPE);
        if (sessionTypes != null && !sessionTypes.isEmpty()) {
            for (TagMetadata.Tag type : sessionTypes) {
                mItems.add(type);
            }
        }
        mItems.add(new Divider());

        mItems.add(new LiveStream());
        mItems.add(new Divider());

        List<TagMetadata.Tag> topics = tagMetadata.getTagsInCategory(Config.Tags.CATEGORY_TRACK);
        if (topics != null && !topics.isEmpty()) {
            for (TagMetadata.Tag topic : topics) {
                mItems.add(topic);
            }
        }
    }

    private FilterViewHolder createFilterViewHolder(final ViewGroup parent) {
        final FilterViewHolder holder = new FilterViewHolder(
                mInflater.inflate(R.layout.explore_sessions_list_item_alt_drawer, parent, false));
        holder.checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mListener != null) {
                    final Tag filter = (Tag) mItems.get(holder.getAdapterPosition());
                    mListener.updateFilters(filter, holder.checkbox.isChecked());
                }
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                holder.checkbox.performClick();
            }
        });
        return holder;
    }

    private FilterViewHolder createLiveStreamFilterViewHolder(final ViewGroup parent) {
        final FilterViewHolder holder = new FilterViewHolder(
                mInflater.inflate(R.layout.explore_sessions_list_item_livestream_alt_drawer, parent,
                        false));
        holder.checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mTagFilterHolder.setShowLiveStreamedSessions(holder.checkbox.isChecked());
                if (mListener != null) {
                    mListener.reloadFragment();
                }
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                holder.checkbox.performClick();
            }
        });
        return holder;
    }

    private DividerViewHolder createDividerViewHolder(final ViewGroup parent) {
        // TODO we should use an ItemDecoration rather than a view
        return new DividerViewHolder(
                mInflater.inflate(R.layout.explore_sessions_list_item_alt_header, parent, false));
    }

    private void bindFilter(final FilterViewHolder holder, final TagMetadata.Tag filter) {
        holder.label.setText(filter.getName());
        holder.checkbox.setChecked(mTagFilterHolder.contains(filter.getId()));
    }

    private void bindLiveStreamFilter(final FilterViewHolder holder) {
        holder.checkbox.setChecked(mTagFilterHolder.isShowLiveStreamedSessions());
    }

    private static class Divider {
    }

    private static class LiveStream {
    }

    private static class FilterViewHolder extends RecyclerView.ViewHolder {

        final TextView label;
        final CheckBox checkbox;

        public FilterViewHolder(final View itemView) {
            super(itemView);
            label = (TextView) itemView.findViewById(R.id.text_view);
            checkbox = (CheckBox) itemView.findViewById(R.id.filter_checkbox);
        }
    }

    private static class DividerViewHolder extends RecyclerView.ViewHolder {

        public DividerViewHolder(final View itemView) {
            super(itemView);
        }
    }
}
