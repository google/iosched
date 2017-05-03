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
package com.google.samples.apps.iosched.schedule;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config.Tags;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.schedule.DividerDecoration.Divided;

import java.util.ArrayList;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class SessionsFilterAdapter extends Adapter<ViewHolder> {

    private static final String TAG = makeLogTag(SessionsFilterAdapter.class);
    private static final String STATE_FILTERS = "com.google.samples.apps.iosched.STATE_FILTERS";

    private static final int TYPE_TYPE = 0;
    private static final int TYPE_TOPIC = 1;
    private static final int TYPE_HEADER = 2;

    private static final Object PAYLOAD_CHECK_CHANGED = new Object();

    private final List<Object> mItems;
    private final LayoutInflater mInflater;
    private TagFilterHolder mTagFilterHolder;
    private OnFiltersChangedListener mListener;

    SessionsFilterAdapter(Context context, TagMetadata filters, Bundle savedState) {
        restoreFromState(savedState);
        mInflater = LayoutInflater.from(context);
        mItems = new ArrayList<>();
        buildFiltersList(filters);
    }

    private void buildFiltersList(TagMetadata tagMetadata) {
        mItems.clear();
        if (tagMetadata == null) {
            return;
        }
        // Types
        List<TagMetadata.Tag> types = tagMetadata.getTagsInCategory(Tags.CATEGORY_TYPE);
        if (types != null && !types.isEmpty()) {
            mItems.addAll(types);
        }
        // "Topics" header
        mItems.add(new TopicsHeader());
        // Topics (aka Tracks)
        List<TagMetadata.Tag> topics = tagMetadata.getTagsInCategory(Tags.CATEGORY_TRACK);
        if (topics != null && !topics.isEmpty()) {
            mItems.addAll(topics);
        }
    }

    void setTagMetadata(TagMetadata tagMetadata) {
        buildFiltersList(tagMetadata);
        notifyDataSetChanged();
    }

    void setSessionFilterAdapterListener(OnFiltersChangedListener listener) {
        mListener = listener;
    }

    void addTag(Tag tag) {
        if (mTagFilterHolder.add(tag)) {
            int position = mItems.indexOf(tag);
            if (position >= 0) {
                notifyItemChanged(position, PAYLOAD_CHECK_CHANGED);
                dispatchFiltersChanged();
            }
        }
    }

    public TagFilterHolder getFilters() {
        return mTagFilterHolder;
    }

    void clearAllFilters() {
        mTagFilterHolder.clear();
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_CHECK_CHANGED);
        dispatchFiltersChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case TYPE_TYPE:
                return new FilterViewHolder(mInflater.inflate(
                        R.layout.list_item_filter_drawer_session_type, parent, false));
            case TYPE_TOPIC:
                return new FilterViewHolder(mInflater.inflate(
                        R.layout.list_item_filter_drawer, parent, false));
            case TYPE_HEADER:
                return new HeaderViewHolder(mInflater.inflate(
                        R.layout.list_item_filter_drawer_topics_header, parent, false));
            default:
                LOGE(TAG, "Unknown view type");
                throw new IllegalArgumentException("Unknown view type");
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        switch (holder.getItemViewType()) {
            case TYPE_TYPE: // fall through
            case TYPE_TOPIC:
                FilterViewHolder fvh = (FilterViewHolder) holder;
                Tag filter = (Tag) mItems.get(position);
                fvh.bind(filter, mTagFilterHolder.contains(filter));
                break;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.contains(PAYLOAD_CHECK_CHANGED)) {
            if (holder instanceof FilterViewHolder) {
                Tag filter = (Tag) mItems.get(position);
                ((FilterViewHolder) holder).setChecked(mTagFilterHolder.contains(filter));
            }
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(final int position) {
        Object item = mItems.get(position);
        if (item instanceof TopicsHeader) {
            return TYPE_HEADER;
        } else if (item instanceof Tag) {
            Tag tag = (Tag) item;
            if (Tags.CATEGORY_TYPE.equals(tag.getCategory())) {
                return TYPE_TYPE;
            }
            return TYPE_TOPIC;
        }
        return RecyclerView.INVALID_TYPE;
    }

    private void updateTagFilter(Tag filter, boolean checked) {
        if ((checked && mTagFilterHolder.add(filter))
                || (!checked && mTagFilterHolder.remove(filter))) {
            dispatchFiltersChanged();
        }
    }

    private void dispatchFiltersChanged() {
        if (mListener != null) {
            mListener.onFiltersChanged(mTagFilterHolder);
        }
    }

    void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_FILTERS, mTagFilterHolder);
    }

    private void restoreFromState(Bundle savedState) {
        if (savedState != null) {
            TagFilterHolder filterHolder = savedState.getParcelable(STATE_FILTERS);
            if (filterHolder != null) {
                mTagFilterHolder = filterHolder;
            }
        }
        if (mTagFilterHolder == null) {
            mTagFilterHolder = new TagFilterHolder();
        }
    }

    public interface OnFiltersChangedListener {

        void onFiltersChanged(TagFilterHolder filterHolder);
    }

    // -- Marker objects

    private static class TopicsHeader {
    }

    // -- ViewHolders

    private class FilterViewHolder extends ViewHolder implements OnCheckedChangeListener, Divided {

        final TextView mLabel;
        final CheckBox mCheckbox;
        private @Nullable Tag mTagFilter;

        FilterViewHolder(final View itemView) {
            super(itemView);
            mLabel = (TextView) itemView.findViewById(R.id.filter_label);
            mCheckbox = (CheckBox) itemView.findViewById(R.id.filter_checkbox);
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCheckbox.performClick();
                }
            });
        }

        void bind(Tag filter, boolean checked) {
            mTagFilter = filter;

            mLabel.setText(filter.getName());
            mCheckbox.setContentDescription(mLabel.getText());
            setChecked(checked);
            if (filter.getId().startsWith(Tags.CATEGORY_TRACK)) {
                if (mLabel.getBackground() == null) {
                    mLabel.setBackground(AppCompatResources.getDrawable(mLabel.getContext(),
                            R.drawable.tag_session_background));
                }
                ViewCompat.setBackgroundTintList(mLabel, ColorStateList.valueOf(filter.getColor()));
            } else {
                mLabel.setBackground(null);
            }
        }

        protected final void setChecked(boolean checked) {
            // avoid dispatching changes when recycling views
            mCheckbox.setOnCheckedChangeListener(null);
            mCheckbox.setChecked(checked);
            mCheckbox.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateTagFilter(mTagFilter, isChecked);
        }
    }

    private class HeaderViewHolder extends ViewHolder {

        HeaderViewHolder(final View itemView) {
            super(itemView);
        }
    }
}
