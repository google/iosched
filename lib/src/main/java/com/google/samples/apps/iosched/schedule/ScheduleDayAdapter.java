/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.schedule;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.ScheduleItemHelper;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.schedule.SessionItemViewHolder.Callbacks;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.doist.recyclerviewext.sticky_headers.StickyHeaders;

import static com.google.samples.apps.iosched.schedule.ScheduleItemViewHolder.SessionTimeFormat.DURATION;
import static com.google.samples.apps.iosched.schedule.ScheduleItemViewHolder.SessionTimeFormat.SPAN;

/**
 * Adapter that produces views to render (one day of) the "My Schedule" screen.
 */
public class ScheduleDayAdapter extends RecyclerView.Adapter<ViewHolder>
        implements StickyHeaders, StickyHeaders.ViewSetup {

    private static final long[] ID_ARRAY = new long[4];
    private static final int ITEM_TYPE_SESSION = 0;
    private static final int ITEM_TYPE_BREAK = 1;
    private static final int ITEM_TYPE_TIME_HEADER = 2;
    private static final int PAYLOAD_TAG_META = 7;

    // list of items served by this adapter
    private final List<Object> mItems = new ArrayList<>();
    private final TagPool mTagPool = new TagPool();

    private final boolean mShowTimeSeparators;
    private final float stuckHeaderElevation;
    private TagMetadata mTagMetadata;
    private Callbacks mAdapterCallbacks;

    public ScheduleDayAdapter(@NonNull Context context, @NonNull Callbacks adapterCallbacks,
                              @Nullable TagMetadata tagMetadata, boolean showTimeSeparators) {
        mAdapterCallbacks = adapterCallbacks;
        mTagMetadata = tagMetadata;
        mShowTimeSeparators = showTimeSeparators;
        stuckHeaderElevation = context.getResources().getDimension(R.dimen.card_elevation);
        setHasStableIds(true);
    }

    private static long generateIdForScheduleItem(@NonNull ScheduleItem item) {
        final long[] array = ID_ARRAY;
        // This code may look complex but its pretty simple. We need to use stable ids so that
        // any user interaction animations are run correctly (such as ripples). This means that
        // we need to generate a stable id. Not all items have sessionIds so we generate one
        // using the sessionId, title, start time and end time.
        array[0] = !TextUtils.isEmpty(item.sessionId)
                ? item.sessionId.hashCode() : 0;
        array[1] = !TextUtils.isEmpty(item.title) ? item.title.hashCode() : 0;
        array[2] = item.startTime;
        array[3] = item.endTime;
        return Arrays.hashCode(array);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public long getItemId(int position) {
        final Object item = mItems.get(position);
        if (item instanceof ScheduleItem) {
            return generateIdForScheduleItem((ScheduleItem) item);
        } else if (item instanceof TimeSeperatorItem) {
            return item.hashCode();
        }
        return position;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case ITEM_TYPE_SESSION:
                return SessionItemViewHolder.newInstance(parent, mAdapterCallbacks, DURATION);
            case ITEM_TYPE_BREAK:
                return NonSessionItemViewHolder.newInstance(parent);
            case ITEM_TYPE_TIME_HEADER:
                return new TimeSeperatorViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.schedule_item_time_separator, parent, false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Object item = mItems.get(position);
        switch (holder.getItemViewType()) {
            case ITEM_TYPE_SESSION:
                ((SessionItemViewHolder) holder).bind((ScheduleItem) item, mTagPool, mTagMetadata);
                break;
            case ITEM_TYPE_BREAK:
                ((NonSessionItemViewHolder) holder).bind((ScheduleItem) item);
                break;
            case ITEM_TYPE_TIME_HEADER:
            default:
                ((TimeSeperatorViewHolder) holder).bind((TimeSeperatorItem) item);
                break;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (holder instanceof SessionItemViewHolder && payloads.contains(PAYLOAD_TAG_META)) {
            ((SessionItemViewHolder) holder).updateTags((ScheduleItem) mItems.get(position),
                    mTagMetadata, mTagPool);
        } else {
            onBindViewHolder(holder, position);
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        if (holder instanceof SessionItemViewHolder) {
            ((SessionItemViewHolder) holder).unbind(mTagPool);
        }
    }

    @Override
    public int getItemViewType(int position) {
        final Object item = mItems.get(position);
        if (item instanceof ScheduleItem) {
            if (((ScheduleItem) item).type == ScheduleItem.BREAK) {
                return ITEM_TYPE_BREAK;
            }
            return ITEM_TYPE_SESSION;
        } else if (item instanceof TimeSeperatorItem) {
            return ITEM_TYPE_TIME_HEADER;
        }
        return RecyclerView.INVALID_TYPE;
    }

    public void setTagMetadata(final TagMetadata tagMetadata) {
        if (mTagMetadata != tagMetadata) {
            mTagMetadata = tagMetadata;
            notifyItemRangeChanged(0, getItemCount() - 1, PAYLOAD_TAG_META);
        }
    }

    public void updateItems(final List<ScheduleItem> items) {
        mItems.clear();
        if (items == null) {
            notifyDataSetChanged();
            return;
        }

        if (!mShowTimeSeparators) {
            mItems.addAll(items);
        } else {
            for (int i = 0, size = items.size(); i < size; i++) {
                final ScheduleItem prev = i > 0 ? items.get(i - 1) : null;
                final ScheduleItem item = items.get(i);

                if (prev == null || !ScheduleItemHelper.sameStartTime(prev, item, true)) {
                    mItems.add(new TimeSeperatorItem(item));
                }
                mItems.add(item);
            }
        }

        // TODO use DiffUtil
        notifyDataSetChanged();
    }

    public int findTimeHeaderPositionForTime(final long time) {
        for (int j = mItems.size() - 1; j >= 0; j--) {
            Object item = mItems.get(j);
            // Keep going backwards until we find a time separator which has a start time before
            // now
            if (item instanceof TimeSeperatorItem && ((TimeSeperatorItem) item).startTime < time) {
                return j;
            }
        }
        return -1;
    }

    @Override
    public boolean isStickyHeader(int position) {
        return getItemViewType(position) == ITEM_TYPE_TIME_HEADER;
    }

    @Override
    public void setupStickyHeaderView(View view) {
        view.setTranslationZ(stuckHeaderElevation);
    }

    @Override
    public void teardownStickyHeaderView(View view) {
        view.setTranslationZ(0f);
    }

    private static class TimeSeperatorViewHolder extends ViewHolder {
        private final TextView mStartTime;

        TimeSeperatorViewHolder(final View itemView) {
            super(itemView);
            mStartTime = (TextView) itemView;
        }

        void bind(@NonNull final TimeSeperatorItem item) {
            mStartTime.setText(TimeUtils.formatShortTime(
                    itemView.getContext(), new Date(item.startTime)));
        }
    }

    private static class TimeSeperatorItem {
        private final long startTime;

        TimeSeperatorItem(ScheduleItem item) {
            this.startTime = item.startTime;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TimeSeperatorItem that = (TimeSeperatorItem) o;
            if (startTime != that.startTime) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return (int) (startTime ^ (startTime >>> 32));
        }
    }
}
