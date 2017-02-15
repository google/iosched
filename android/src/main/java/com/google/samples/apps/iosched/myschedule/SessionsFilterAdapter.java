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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
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

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;

import java.util.ArrayList;
import java.util.List;

public class SessionsFilterAdapter extends Adapter<ViewHolder> {

    private static final String TAG = makeLogTag(SessionsFilterAdapter.class);

    private static final int TYPE_TAG_FILTER = 0;
    private static final int TYPE_STATIC_FILTER = 1;
    private static final int TYPE_TOPICS_HEADER = 2;

    private static final Object PAYLOAD_CLEAR_CHECK = new Object();

    private final List<Object> mItems;
    private TagFilterHolder mTagFilterHolder;
    private final LayoutInflater mInflater;

    private OnFiltersChangedListener mListener;

    public interface OnFiltersChangedListener {

        void onFiltersChanged(TagFilterHolder filterHolder);
    }

    public SessionsFilterAdapter(Context context, TagFilterHolder tagFilterHolder,
            TagMetadata filters) {
        mInflater = LayoutInflater.from(context);
        mItems = new ArrayList<>();
        mTagFilterHolder = tagFilterHolder;
        processFilters(context.getResources(), filters);
    }

    /**
     * Build the list of items
     */
    private void processFilters(Resources res, TagMetadata tagMetadata) {
        // Live Streamed
        mItems.add(new StaticFilter(res.getString(R.string.session_live_streamed)));
        // Sessions only
        mItems.add(new StaticFilter(res.getString(R.string.filter_label_sessions_only)));
        // "Topics" header
        mItems.add(new TopicsHeader());
        // Topics. We only care about tracks.
        List<TagMetadata.Tag> topics = tagMetadata.getTagsInCategory(Config.Tags.CATEGORY_TRACK);
        if (topics != null && !topics.isEmpty()) {
            for (TagMetadata.Tag topic : topics) {
                mItems.add(topic);
            }
        }
    }

    public void setSessionFilterAdapterListener(OnFiltersChangedListener listener) {
        mListener = listener;
    }

    public void clearAllFilters() {
        mTagFilterHolder.clear();
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_CLEAR_CHECK);
        dispatchFiltersChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case TYPE_TAG_FILTER:
                return new TagFilterViewHolder(mInflater.inflate(
                        R.layout.list_item_filter_drawer, parent, false));
            case TYPE_STATIC_FILTER:
                return new StaticFilterViewHolder(mInflater.inflate(
                        R.layout.list_item_filter_drawer, parent, false));
            case TYPE_TOPICS_HEADER:
                return new HeaderViewHolder(mInflater.inflate(
                        R.layout.list_item_filter_drawer_topics_header, parent, false));
            default:
                LOGE(TAG, "Unknown view type");
                throw new IllegalArgumentException("Unknown view type");
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        switch (getItemViewType(position)) {
            case TYPE_TAG_FILTER:
                TagFilterViewHolder tfvh = (TagFilterViewHolder) holder;
                Tag filter = (Tag) mItems.get(position);
                tfvh.bindFilter(filter, mTagFilterHolder.contains(filter.getId()));
                break;
            case TYPE_STATIC_FILTER:
                StaticFilterViewHolder sfvh = (StaticFilterViewHolder) holder;
                sfvh.bindFilter((StaticFilter) mItems.get(position),
                        isStaticFilterChecked(position));
                break;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.contains(PAYLOAD_CLEAR_CHECK)) {
            if (holder instanceof FilterViewHolder) {
                ((FilterViewHolder) holder).setChecked(false);
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
        if (item instanceof StaticFilter) {
            return TYPE_STATIC_FILTER;
        } else if (item instanceof TopicsHeader) {
            return TYPE_TOPICS_HEADER;
        }
        return TYPE_TAG_FILTER;
    }

    private boolean isStaticFilterChecked(int position) {
        switch (position) {
            case 0:
                return mTagFilterHolder.isShowLiveStreamedOnly();
            case 1:
                return mTagFilterHolder.showSessionsOnly();
        }
        return false;
    }

    private void updateStaticFilter(int position, boolean checked) {
        switch (position) {
            case 0:
                mTagFilterHolder.setShowLiveStreamedOnly(checked);
                dispatchFiltersChanged();
                break;
            case 1:
                mTagFilterHolder.setShowSessionsOnly(checked);
                dispatchFiltersChanged();
                break;
        }
    }

    private void updateTagFilter(Tag filter, boolean checked) {
        if ((checked && mTagFilterHolder.add(filter.getId(), filter.getCategory()))
                || (!checked && mTagFilterHolder.remove(filter.getId(), filter.getCategory()))) {
            dispatchFiltersChanged();
        }
    }

    private void dispatchFiltersChanged() {
        if (mListener != null) {
            mListener.onFiltersChanged(mTagFilterHolder);
        }
    }

    // -- Marker objects

    private static class TopicsHeader {
    }

    private static class StaticFilter {
        final CharSequence mLabel;

        private StaticFilter(CharSequence label) {
            mLabel = label;
        }
    }

    // -- ViewHolders

    private abstract class FilterViewHolder extends ViewHolder implements OnCheckedChangeListener {

        protected final TextView mLabel;
        protected final CheckBox mCheckbox;

        public FilterViewHolder(final View itemView) {
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

        protected void onBind(CharSequence label, boolean checked) {
            mLabel.setText(label);
            setChecked(checked);
        }

        protected final void setChecked(boolean checked) {
            // avoid dispatching changes when recycling views
            mCheckbox.setOnCheckedChangeListener(null);
            mCheckbox.setChecked(checked);
            mCheckbox.setOnCheckedChangeListener(this);
        }
    }

    private class StaticFilterViewHolder extends FilterViewHolder {

        public StaticFilterViewHolder(final View itemView) {
            super(itemView);
        }

        void bindFilter(StaticFilter filter, boolean checked) {
            onBind(filter.mLabel, checked);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateStaticFilter(getAdapterPosition(), isChecked);
        }
    }

    private class TagFilterViewHolder extends FilterViewHolder {

        private Tag mTagFilter;

        public TagFilterViewHolder(final View itemView) {
            super(itemView);
        }

        void bindFilter(Tag filter, boolean checked) {
            mTagFilter = filter;
            onBind(filter.getName(), checked);
            if (mLabel.getBackground() == null) {
                mLabel.setBackground(AppCompatResources.getDrawable(mLabel.getContext(),
                        R.drawable.tag_session_background));
            }
            ViewCompat.setBackgroundTintList(mLabel, ColorStateList.valueOf(filter.getColor()));
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateTagFilter(mTagFilter, isChecked);
        }
    }

    private class HeaderViewHolder extends ViewHolder {

        public HeaderViewHolder(final View itemView) {
            super(itemView);
        }
    }
}
