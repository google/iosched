/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.myio;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.myschedule.ScheduleItemViewHolder;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.List;


public class MyIOAdapter extends Adapter<ViewHolder> {

    private static final int VIEW_TYPE_SESSION = 0;
    private static final int VIEW_TYPE_SEPARATOR = 1;

    private static final List<DaySeparator> DAY_SEPARATORS;
    static {
        DAY_SEPARATORS = new ArrayList<>(Config.CONFERENCE_DAYS.length);
        for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            DAY_SEPARATORS.add(new DaySeparator(i, Config.CONFERENCE_DAYS[i][0]));
        }
    }

    private List<Object> mItems = new ArrayList<>();
    private Callbacks mCallbacks;

    interface Callbacks extends ScheduleItemViewHolder.Callbacks {

        /**
         * @param conferenceDay the conference day for the clicked header, where 0 is the first day
         */
        void onAddEventsClicked(int conferenceDay);
    }

    public MyIOAdapter(Callbacks callbacks) {
        mCallbacks = callbacks;
        mItems.addAll(DAY_SEPARATORS); // default state
    }

    public void setItems(List<ScheduleItem> items) {
        mItems.clear();

        int day = 0;
        if (items != null && !items.isEmpty()) {
            // Add the items to our list, interleaving separators as we go
            long separatorTime = DAY_SEPARATORS.get(day).mStartTime;
            for (ScheduleItem item : items) {
                if (item.startTime >= separatorTime) {
                    // add the separator first
                    mItems.add(DAY_SEPARATORS.get(day));
                    day++;
                    if (day >= DAY_SEPARATORS.size()) {
                        // run the list to the end
                        separatorTime = Long.MAX_VALUE;
                    } else {
                        separatorTime = DAY_SEPARATORS.get(day).mStartTime;
                    }
                }
                // Add the item
                mItems.add(item);
            }
        }

        // Add any remaining separators
        for (; day < DAY_SEPARATORS.size(); day++) {
            mItems.add(DAY_SEPARATORS.get(day));
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = mItems.get(position);
        if (item instanceof ScheduleItem) {
            return VIEW_TYPE_SESSION;
        }
        if (item instanceof DaySeparator) {
            return VIEW_TYPE_SEPARATOR;
        }
        return RecyclerView.INVALID_TYPE;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_SESSION:
                return ScheduleItemViewHolder.newInstance(parent, mCallbacks);
            case VIEW_TYPE_SEPARATOR:
                return DaySeparatorViewHolder.newInstance(parent, mCallbacks);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_SESSION:
                ScheduleItem item = (ScheduleItem) mItems.get(position);
                // TODO need TagMetadata
                ((ScheduleItemViewHolder) holder).onBind(item, null);
                break;
            case VIEW_TYPE_SEPARATOR:
                DaySeparator separator = (DaySeparator) mItems.get(position);
                ((DaySeparatorViewHolder) holder).onBind(separator);
                break;
        }
    }

    private static class DaySeparatorViewHolder extends ViewHolder {

        private final TextView mDateText;
        private final Button mAddEventsButton;
        private final Callbacks mCallbacks;

        private static final StringBuilder FORMAT_STRINGBUILDER = new StringBuilder();

        static DaySeparatorViewHolder newInstance(ViewGroup parent, Callbacks callbacks) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.myio_list_item_day_separator, parent, false);
            return new DaySeparatorViewHolder(itemView, callbacks);
        }

        private DaySeparatorViewHolder(View itemView, Callbacks callbacks) {
            super(itemView);
            mCallbacks = callbacks;
            mDateText = (TextView) itemView.findViewById(R.id.text);
            mAddEventsButton = (Button) itemView.findViewById(R.id.add_events);
        }

        private void onBind(final DaySeparator separator) {
            mDateText.setText(UIUtils.formatDaySeparator(itemView.getContext(),
                    FORMAT_STRINGBUILDER, separator.mStartTime));
            mAddEventsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallbacks != null) {
                        mCallbacks.onAddEventsClicked(separator.mDay);
                    }
                }
            });
        }
    }

    static class DaySeparator {
        private final int mDay;
        private final long mStartTime;

        DaySeparator(int day, long startTime) {
            mDay = day;
            mStartTime = startTime;
        }
    }
}
