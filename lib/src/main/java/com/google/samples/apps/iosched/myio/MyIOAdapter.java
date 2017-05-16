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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
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
import com.google.samples.apps.iosched.messages.MessageCardHelper;
import com.google.samples.apps.iosched.messages.MessageData;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.schedule.NonSessionItemViewHolder;
import com.google.samples.apps.iosched.schedule.SessionItemViewHolder;
import com.google.samples.apps.iosched.schedule.TagPool;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.util.WelcomeUtils;

import java.util.ArrayList;
import java.util.List;

import io.doist.recyclerviewext.sticky_headers.StickyHeaders;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.samples.apps.iosched.schedule.ScheduleItemViewHolder.SessionTimeFormat.SPAN;

class MyIOAdapter extends Adapter<ViewHolder> implements StickyHeaders, StickyHeaders.ViewSetup {

    private static final int VIEW_TYPE_SESSION = 0;
    private static final int VIEW_TYPE_NON_SESSION = 1;
    private static final int VIEW_TYPE_SPACER = 2;
    private static final int VIEW_TYPE_DAY_HEADER = 3;
    private static final int VIEW_TYPE_MESSAGE_CARD = 4;
    private static final int PAYLOAD_TAG_META = 7;

    private static final List<DaySeparator> DAY_SEPARATORS;

    static {
        DAY_SEPARATORS = new ArrayList<>(Config.CONFERENCE_DAYS.length + 1);
        DAY_SEPARATORS.add(new DaySeparator(-1, Config.PRECONFERENCE_START));

        for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            DAY_SEPARATORS.add(new DaySeparator(i, Config.CONFERENCE_DAYS[i][0]));
        }
    }

    private final List<Object> mItems = new ArrayList<>();
    private final TagPool mTagPool = new TagPool();
    private final Callbacks mCallbacks;
    private final float stuckHeaderElevation;
    private TagMetadata mTagMetadata;

    private Context mContext;

    interface Callbacks extends SessionItemViewHolder.Callbacks {

        /**
         * @param conferenceDay the conference day for the clicked header, where 0 is the first day
         */
        void onAddEventsClicked(int conferenceDay);
    }

    MyIOAdapter(Context context, Callbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;
        stuckHeaderElevation = context.getResources().getDimension(R.dimen.card_elevation);
        setHasStableIds(true);
        setItems(null); // build the initial list of items
    }

    void setItems(List<ScheduleItem> items) {
        List<Object> newData = new ArrayList<>();

        MessageData notSignedInCard =
                MessageCardHelper.notSignedInCard();
        notSignedInCard.setStartButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                WelcomeUtils.markHidePostOnboardingCard(mContext);
            }
        });
        notSignedInCard.setEndButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                WelcomeUtils.markHidePostOnboardingCard(mContext);
                ((MyIOActivity) mContext).signIn();
            }
        });

        MessageData signedInMessageCard = MessageCardHelper.signedInMessageCard();
        signedInMessageCard.setStartButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                WelcomeUtils.markHidePostOnboardingCard(mContext);
            }
        });

        if (WelcomeUtils.showPostOnboardingCard(mContext)) {
            if (AccountUtils.hasActiveAccount(mContext)) {
                newData.add(signedInMessageCard);
            } else {
                newData.add(notSignedInCard);
            }
        }

        int day = 0;
        if (items != null && !items.isEmpty()) {
            // Add the items to our list, interleaving separators as we go
            long separatorTime = DAY_SEPARATORS.get(day).mStartTime;
            for (ScheduleItem item : items) {
                if (item.startTime >= separatorTime) {
                    // add the separator first
                    newData.add(new SeparatorSpacer());
                    newData.add(DAY_SEPARATORS.get(day));
                    day++;
                    if (day >= DAY_SEPARATORS.size()) {
                        // run the list to the end
                        separatorTime = Long.MAX_VALUE;
                    } else {
                        separatorTime = DAY_SEPARATORS.get(day).mStartTime;
                    }
                }
                // Add the item
                newData.add(item);
            }
        }

        // Add any remaining separators
        for (; day < DAY_SEPARATORS.size(); day++) {
            newData.add(new SeparatorSpacer());
            newData.add(DAY_SEPARATORS.get(day));
        }

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new MyIoDiff(mItems, newData));
        mItems.clear();
        mItems.addAll(newData);
        diff.dispatchUpdatesTo(this);
    }

    /**
     * Removes the first item if it is the post onboarding message card.
     */
    void removePostOnboardingMessageCard() {
        if (mItems.get(0) instanceof MessageData) {
            mItems.remove(0);
            notifyDataSetChanged();
        }
    }

    void setTagMetadata(TagMetadata tagMetadata) {
        if (mTagMetadata != tagMetadata) {
            mTagMetadata = tagMetadata;
            notifyItemRangeChanged(0, getItemCount() - 1, PAYLOAD_TAG_META);
        }
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    public long getItemId(int position) {
        Object item = mItems.get(position);
        if (item instanceof ScheduleItem) {
            return item.hashCode();
        }
        if (item instanceof DaySeparator) {
            return ((DaySeparator) item).mStartTime;
        }
        if (item instanceof MessageData) {
            return 0;
        }
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = mItems.get(position);
        if (item instanceof ScheduleItem) {
            if (((ScheduleItem) item).type == ScheduleItem.BREAK) {
                return VIEW_TYPE_NON_SESSION;
            }
            return VIEW_TYPE_SESSION;
        }
        if (item instanceof SeparatorSpacer) {
            return VIEW_TYPE_SPACER;
        }
        if (item instanceof DaySeparator) {
            return VIEW_TYPE_DAY_HEADER;
        }
        if (item instanceof MessageData) {
            return VIEW_TYPE_MESSAGE_CARD;
        }
        return RecyclerView.INVALID_TYPE;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_SESSION:
                return SessionItemViewHolder.newInstance(parent, mCallbacks, SPAN);
            case VIEW_TYPE_NON_SESSION:
                return NonSessionItemViewHolder.newInstance(parent);
            case VIEW_TYPE_SPACER:
                return SeparatorSpacerViewHolder.newInstance(parent);
            case VIEW_TYPE_DAY_HEADER:
                return DaySeparatorViewHolder.newInstance(parent, mCallbacks);
            case VIEW_TYPE_MESSAGE_CARD:
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return new MessageCardViewHolder(
                        inflater.inflate(R.layout.message_card, parent, false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Object item = mItems.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_SESSION:
                ((SessionItemViewHolder) holder).bind((ScheduleItem)item, mTagPool, mTagMetadata);
                break;
            case VIEW_TYPE_NON_SESSION:
                ((NonSessionItemViewHolder) holder).bind((ScheduleItem) item);
                break;
            case VIEW_TYPE_DAY_HEADER:
                ((DaySeparatorViewHolder) holder).bind((DaySeparator) item);
                break;
            case VIEW_TYPE_MESSAGE_CARD:
                ((MessageCardViewHolder) holder).onBind((MessageData) item);
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
    public boolean isStickyHeader(int position) {
        return getItemViewType(position) == VIEW_TYPE_DAY_HEADER;
    }

    @Override
    public void setupStickyHeaderView(View view) {
        view.setTranslationZ(stuckHeaderElevation);
    }

    @Override
    public void teardownStickyHeaderView(View view) {
        view.setTranslationZ(0f);
    }

    /**
     * Return the position of the first item that has not finished.
     */
    int findPositionForTime(final long time) {
        for (int i = 0; i < mItems.size(); i++) {
            Object item = mItems.get(i);
            if (item instanceof ScheduleItem) {
                if (((ScheduleItem) item).endTime > time) {
                    return i;
                }
            } else if (item instanceof DaySeparator) {
                if (((DaySeparator) item).mStartTime > time) {
                    return i;
                }
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private class MessageCardViewHolder extends ViewHolder {
        private final TextView mMessage;
        private final Button mStartButton;
        private final Button mEndButton;

        private MessageData mMessageData;

        MessageCardViewHolder(View itemView) {
            super(itemView);
            mMessage = (TextView) itemView.findViewById(R.id.text);
            mStartButton = (Button) itemView.findViewById(R.id.negative_button);
            mEndButton = (Button) itemView.findViewById(R.id.positive_button);

            mStartButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mMessageData != null) {
                        if (mMessageData.getStartButtonClickListener() != null) {
                            mMessageData.getStartButtonClickListener().onClick(v);
                        }
                        if (mItems.get(0) instanceof MessageData) {
                            mItems.remove(0); // message card is always at the top
                            notifyDataSetChanged();
                        }
                    }
                }
            });
            mEndButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mMessageData != null) {
                        if (mMessageData.getEndButtonClickListener() != null) {
                            mMessageData.getEndButtonClickListener().onClick(v);
                        }
                        if (mItems.get(0) instanceof MessageData) {
                            mItems.remove(0); // message card is always at the top
                            notifyDataSetChanged();
                        }
                    }
                }
            });
        }

        private void onBind(@NonNull MessageData messageData) {
            mMessageData = messageData;

            mMessage.setText(messageData.getMessageString(mMessage.getContext()));
            int startButtonResId = mMessageData.getStartButtonStringResourceId();
            if (startButtonResId != -1) {
                mStartButton.setText(startButtonResId);
                mStartButton.setVisibility(VISIBLE);
            } else {
                mStartButton.setVisibility(GONE);
            }
            int endButtonResId = mMessageData.getEndButtonStringResourceId();
            if (endButtonResId != -1) {
                mEndButton.setText(endButtonResId);
                mEndButton.setVisibility(VISIBLE);
            } else {
                mEndButton.setVisibility(GONE);
            }
        }
    }

    private static class DaySeparatorViewHolder extends ViewHolder {

        private final TextView mDateText;
        private final Button mAddEventsButton;
        private final Callbacks mCallbacks;

        private static final StringBuilder FORMAT_STRINGBUILDER = new StringBuilder();

        static DaySeparatorViewHolder newInstance(ViewGroup parent, Callbacks callbacks) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.myio_list_item_day_separator, parent, false);
            return new DaySeparatorViewHolder(itemView, callbacks);
        }

        private DaySeparatorViewHolder(View itemView, Callbacks callbacks) {
            super(itemView);
            mCallbacks = callbacks;
            mDateText = (TextView) itemView.findViewById(R.id.text);
            mAddEventsButton = (Button) itemView.findViewById(R.id.add_events);
        }

        private void bind(final DaySeparator separator) {
            mDateText.setText(UIUtils.formatDaySeparator(itemView.getContext(),
                    FORMAT_STRINGBUILDER, separator.mStartTime));
            mAddEventsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallbacks != null && separator.mDay >= 0) {
                        mCallbacks.onAddEventsClicked(separator.mDay);
                    }
                }
            });
            mAddEventsButton.setVisibility(separator.mDay >= 0 ? VISIBLE : GONE);
        }
    }

    private static class DaySeparator {
        private final int mDay;
        private final long mStartTime;

        DaySeparator(int day, long startTime) {
            mDay = day;
            mStartTime = startTime;
        }
    }

    private static class SeparatorSpacer { }

    private static class SeparatorSpacerViewHolder extends ViewHolder {

        private SeparatorSpacerViewHolder(View itemView) {
            super(itemView);
        }

        static SeparatorSpacerViewHolder newInstance(@NonNull ViewGroup parent) {
            return new SeparatorSpacerViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.spacer, parent, false));
        }
    }

    private static class MyIoDiff extends DiffUtil.Callback {

        private final List<Object> oldItems;
        private final List<Object> newItems;

        MyIoDiff(List<Object> oldItems, List<Object> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            return newItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldItems.get(oldItemPosition);
            Object newItem = newItems.get(newItemPosition);
            if (oldItem instanceof SeparatorSpacer && newItem instanceof SeparatorSpacer) {
                return true;
            } else if (oldItem instanceof DaySeparator && newItem instanceof DaySeparator) {
                return ((DaySeparator) oldItem).mDay == ((DaySeparator) newItem).mDay;
            } else if ((oldItem instanceof ScheduleItem && newItem instanceof ScheduleItem)
                    || (oldItem instanceof MessageData && newItem instanceof MessageData)) {
                return oldItem.equals(newItem);
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return true; // all items are stateless
        }
    }
}
