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

package com.google.samples.apps.iosched.myschedule;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.archframework.UpdatableView.UserActionListener;
import com.google.samples.apps.iosched.messages.MessageData;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.ScheduleItemHelper;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleUserActionEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Adapter that produces views to render (one day of) the "My Schedule" screen.
 */
public class MyScheduleDayAdapter
        extends RecyclerView.Adapter<MyScheduleDayAdapter.ListViewHolder> {
    private static final String TAG = makeLogTag("MyScheduleDayAdapter");

    private static final long[] ID_ARRAY = new long[4];

    private static final int ITEM_TYPE_SLOT = 0;
    private static final int ITEM_TYPE_TIME_HEADER = 1;
    private static final int ITEM_TYPE_MESSAGE = 2;

    private final Context mContext;

    // list of items served by this adapter
    private final ArrayList mItems = new ArrayList();
    private final UserActionListener<MyScheduleUserActionEnum> mListener;

    private TagMetadata mTagMetadata;

    public MyScheduleDayAdapter(@NonNull Context context,
            @NonNull UserActionListener<MyScheduleUserActionEnum> listener,
            @Nullable TagMetadata tagMetadata) {
        mContext = context;
        mListener = listener;
        mTagMetadata = tagMetadata;

        setHasStableIds(true);
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
            return ((TimeSeperatorItem) item).hashCode();
        } else if (item instanceof MessageData) {
            return ((MessageData) item).hashCode();
        }
        return position;
    }

    private final View.OnClickListener mUriOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Object tag = v.getTag(R.id.myschedule_uri_tagkey);
            if (tag instanceof Uri) {
                Uri uri = (Uri) tag;
                Bundle bundle = new Bundle();
                bundle.putString(MyScheduleModel.SESSION_URL_KEY, uri.toString());
                mListener.onUserAction(MyScheduleUserActionEnum.SESSION_SLOT, bundle);

                mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        }
    };

    void setUriClickable(View view, Uri uri) {
        view.setTag(R.id.myschedule_uri_tagkey, uri);
        view.setOnClickListener(mUriOnClickListener);
    }

    static void clearClickable(View view) {
        view.setOnClickListener(null);
        view.setClickable(false);
    }

    @Override
    public ListViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final LayoutInflater li = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ITEM_TYPE_SLOT:
                return new ItemViewHolder(
                        li.inflate(R.layout.my_schedule_item, parent, false));
            case ITEM_TYPE_TIME_HEADER:
                return new TimeSeperatorViewHolder(
                        li.inflate(R.layout.my_schedule_item_time_separator, parent, false));
            case ITEM_TYPE_MESSAGE:
                return new MessageViewHolder(
                        li.inflate(R.layout.my_schedule_item_message_card, parent, false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final ListViewHolder holder, final int position) {
        holder.onBind(mItems.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        final Object item = mItems.get(position);
        if (item instanceof ScheduleItem) {
            return ITEM_TYPE_SLOT;
        } else if (item instanceof TimeSeperatorItem) {
            return ITEM_TYPE_TIME_HEADER;
        } else if (item instanceof MessageData) {
            return ITEM_TYPE_MESSAGE;
        }
        return -1;
    }

    public void setTagMetadata(final TagMetadata tagMetadata) {
        if (mTagMetadata != tagMetadata) {
            mTagMetadata = tagMetadata;
            notifyDataSetChanged();
        }
    }

    public void updateItems(final List<MessageData> messages, final List<ScheduleItem> items) {
        mItems.clear();

        // Add all messages first
        if (items != null && !items.isEmpty()) {
            mItems.addAll(messages);
        }

        if (items != null) {
            for (int i = 0, size = items.size(); i < size; i++) {
                final ScheduleItem prev = i > 0 ? items.get(i - 1) : null;
                final ScheduleItem item = items.get(i);

                if (prev == null || !ScheduleItemHelper.sameStartTime(prev, item, true)) {
                    LOGD(TAG, "Adding time seperator item: " + item + " start="
                            + new Date(item.startTime));
                    mItems.add(new TimeSeperatorItem(item));
                }

                LOGD(TAG, "Adding schedule item: " + item + " start=" + new Date(item.startTime));
                mItems.add(item);
            }
        }

        // TODO use DiffUtil
        notifyDataSetChanged();
    }

    class ItemViewHolder extends ListViewHolder<ScheduleItem> {
        private final TextView title;
        private final TextView description;
        private final Button feedback;
        private final View live;
        private final View separator;
        private final FlexboxLayout tagsHolder;
        private final ImageView bookmark;

        private final StringBuilder mTmpStringBuilder = new StringBuilder();

        ItemViewHolder(final View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.slot_title);
            description = (TextView) view.findViewById(R.id.slot_description);
            feedback = (Button) view.findViewById(R.id.give_feedback_button);
            separator = view.findViewById(R.id.separator);
            live = view.findViewById(R.id.live_now_badge);
            tagsHolder = (FlexboxLayout) view.findViewById(R.id.tags_holder);
            bookmark = (ImageView) view.findViewById(R.id.bookmark);
        }

        @Override
        void onBind(@NonNull final ScheduleItem item) {
            final Context context = itemView.getContext();
            clearClickable(itemView);

            final long now = TimeUtils.getCurrentTime(context);
            final boolean isNowPlaying = item.startTime <= now && now <= item.endTime
                    && item.type == ScheduleItem.SESSION;

            // show or hide the "LIVE NOW" badge
            live.setVisibility((item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM) != 0
                    && isNowPlaying ? View.VISIBLE : View.GONE);

            // Mark the bookmark as gone for now. We change the visibility below if needed
            bookmark.setVisibility(View.GONE);

            // Remove all views from tags holder
            tagsHolder.removeAllViews();
            final LayoutInflater layoutInflater = LayoutInflater.from(context);

            itemView.setTag(R.id.myschedule_uri_tagkey, null);
            if (item.type == ScheduleItem.BREAK) {
                feedback.setVisibility(View.GONE);
                title.setVisibility(View.VISIBLE);
                title.setText(item.title);
                description.setText(formatDescription(context, item));
                if (item.isFoodBreak()) {
                    layoutInflater.inflate(R.layout.include_schedule_food, tagsHolder);
                } else if (item.isConcert()) {
                    layoutInflater.inflate(R.layout.include_schedule_party, tagsHolder);
                }
            } else if (item.type == ScheduleItem.SESSION) {
                if (feedback != null) {
                    boolean showFeedbackButton = !item.hasGivenFeedback;
                    // Can't use isPastDuringConference because we want to show feedback after the
                    // conference too.
                    if (showFeedbackButton) {
                        if (item.endTime > now) {
                            // Session hasn't finished yet, don't show button.
                            showFeedbackButton = false;
                        }
                    }
                    feedback.setVisibility(showFeedbackButton ? View.VISIBLE : View.GONE);
                    feedback.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Bundle bundle = new Bundle();
                            bundle.putString(MyScheduleModel.SESSION_ID_KEY, item.sessionId);
                            bundle.putString(MyScheduleModel.SESSION_TITLE_KEY, item.title);
                            mListener.onUserAction(MyScheduleUserActionEnum.FEEDBACK, bundle);

                            Intent feedbackIntent = new Intent(Intent.ACTION_VIEW,
                                    ScheduleContract.Sessions.buildSessionUri(item.sessionId),
                                    context, SessionFeedbackActivity.class);
                            context.startActivity(feedbackIntent);
                        }
                    });
                }

                title.setVisibility(View.VISIBLE);
                title.setText(item.title);

                final Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(item.sessionId);
                setUriClickable(itemView, sessionUri);

                if ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) != 0) {
                    // TODO show conflicts?
                    // description.setTextColor(mColorConflict);
                }
                if ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_NEXT) != 0) {
                    // TODO show conflicts?
                    // description.setTextColor(mColorConflict);
                }

                if (mTagMetadata != null) {
                    final TagMetadata.Tag mainTag = mTagMetadata.getTag(item.mainTag);
                    if (mainTag != null) {
                        TextView tagView = (TextView) layoutInflater.inflate(
                                R.layout.include_schedule_tag, tagsHolder, false);
                        tagView.setText(mainTag.getName());
                        ViewCompat.setBackgroundTintList(tagView,
                                ColorStateList.valueOf(mainTag.getColor()));
                        tagsHolder.addView(tagView);
                    }
                }

                description.setText(formatDescription(context, item));

                // Populate the bookmark icon
                if (bookmark.getDrawable() == null) {
                    Drawable d = ContextCompat.getDrawable(context, R.drawable.session_bookmark);
                    d = DrawableCompat.wrap(d.mutate());
                    DrawableCompat.setTintList(d, AppCompatResources.getColorStateList(
                            context, R.color.session_bookmark_tint));
                    bookmark.setImageDrawable(d);
                }
                // Show as activated is the item is the keynote or in the schedule
                // (it is auto added to schedule on sync)
                bookmark.setActivated(item.isKeynote() || item.inSchedule);
                bookmark.setVisibility(View.VISIBLE);
                clearClickable(bookmark);
                bookmark.setEnabled(false);

                if (!item.isKeynote()) {
                    bookmark.setEnabled(true);
                    bookmark.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View view) {
                            // Toggle now so that it looks immediate
                            view.setActivated(!view.isActivated());

                            MyScheduleUserActionEnum action = item.inSchedule
                                    ? MyScheduleUserActionEnum.SESSION_UNSTAR
                                    : MyScheduleUserActionEnum.SESSION_STAR;
                            Bundle bundle = new Bundle();
                            bundle.putString(MyScheduleModel.SESSION_ID_KEY, item.sessionId);
                            mListener.onUserAction(action, bundle);
                        }
                    });
                }
            }

            if (item.isKeynote() || (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM) != 0) {
                if (tagsHolder.getChildCount() > 0) {
                    // Insert the spacer first
                    layoutInflater.inflate(R.layout.include_schedule_live_spacer, tagsHolder);
                }

                View liveStreamView = layoutInflater.inflate(R.layout.include_schedule_live,
                        tagsHolder, false);
                tagsHolder.addView(liveStreamView);
            }

            tagsHolder.setVisibility(tagsHolder.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }

        private String formatDescription(@NonNull Context context,
                @NonNull final ScheduleItem item) {
            final StringBuilder description = mTmpStringBuilder;
            mTmpStringBuilder.setLength(0); // clear the builder

            description.append(TimeUtils.formatShortTime(context, new Date(item.startTime)));
            if (!Config.Tags.SPECIAL_KEYNOTE.equals(item.mainTag)) {
                description.append(" - ");
                description.append(TimeUtils.formatShortTime(context, new Date(item.endTime)));
            }
            if (!TextUtils.isEmpty(item.room)) {
                description.append(" / ");
                description.append(item.room);
            }
            return description.toString();
        }
    }

    private static class TimeSeperatorViewHolder extends ListViewHolder<TimeSeperatorItem> {
        private final TextView mStartTime;

        TimeSeperatorViewHolder(final View view) {
            super(view);
            mStartTime = (TextView) view.findViewById(R.id.start_time);
        }

        void onBind(@NonNull final TimeSeperatorItem item) {
            mStartTime.setText(TimeUtils.formatShortTime(
                    itemView.getContext(), new Date(item.startTime)));
        }
    }

    abstract static class ListViewHolder<T> extends RecyclerView.ViewHolder {
        public ListViewHolder(final View itemView) {
            super(itemView);
        }

        abstract void onBind(@NonNull final T item);
    }

    class MessageViewHolder extends ListViewHolder<MessageData> {
        final ImageView icon;
        final TextView description;
        final Button buttonStart;
        final Button buttonEnd;

        public MessageViewHolder(final View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            description = (TextView) itemView.findViewById(R.id.description);
            buttonStart = (Button) itemView.findViewById(R.id.buttonStart);
            buttonEnd = (Button) itemView.findViewById(R.id.buttonEnd);

            // Work with pre-existing infrastructure which supplied a click listener and relied on
            // a shared pref listener & a reload to dismiss message cards.
            // By setting our own click listener and manually calling onClick we can remove the
            // item in the adapter directly.
            buttonStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        final MessageData message = (MessageData) mItems.get(position);
                        message.getStartButtonClickListener().onClick(buttonStart);
                        mItems.remove(position);
                        notifyItemRemoved(position);
                    }
                }
            });
            buttonEnd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        final MessageData message = (MessageData) mItems.get(position);
                        message.getEndButtonClickListener().onClick(buttonEnd);
                        mItems.remove(position);
                        notifyItemRemoved(position);
                    }
                }
            });
        }

        @Override
        void onBind(@NonNull final MessageData message) {
            description.setText(message.getMessageString(itemView.getContext()));
            if (message.getIconDrawableId() > 0) {
                icon.setVisibility(View.VISIBLE);
                icon.setImageResource(message.getIconDrawableId());
            } else {
                icon.setVisibility(View.GONE);
            }
            if (message.getStartButtonStringResourceId() != -1) {
                buttonEnd.setVisibility(View.VISIBLE);
                buttonStart.setText(message.getStartButtonStringResourceId());
            } else {
                buttonStart.setVisibility(View.GONE);
            }
            if (message.getEndButtonStringResourceId() != -1) {
                buttonEnd.setVisibility(View.VISIBLE);
                buttonEnd.setText(message.getEndButtonStringResourceId());
            } else {
                buttonEnd.setVisibility(View.GONE);
            }
        }
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
}
