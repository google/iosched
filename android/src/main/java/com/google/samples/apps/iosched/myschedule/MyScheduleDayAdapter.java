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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.UpdatableView.UserActionListener;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.ScheduleItemHelper;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleUserActionEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Adapter that produces views to render (one day of) the "My Schedule" screen.
 */
public class MyScheduleDayAdapter implements ListAdapter, AbsListView.RecyclerListener {
    private static final String TAG = makeLogTag("MyScheduleDayAdapter");

    private final Context mContext;

    // list of items served by this adapter
    final ArrayList<ScheduleItem> mItems = new ArrayList<>();

    // observers to notify about changes in the data
    final ArrayList<DataSetObserver> mObservers = new ArrayList<>();

    ImageLoader mImageLoader;

    private final int mHourColorDefault;
    private final int mHourColorPast;
    private final int mTitleColorDefault;
    private final int mTitleColorPast;
    private final int mIconColorDefault;
    private final int mIconColorPast;
    private final int mColorConflict;
    private final int mColorBackgroundDefault;
    private final int mColorBackgroundPast;
    private final int mListSpacing;
    private final int mSelectableItemBackground;
    private final boolean mIsRtl;

    private UserActionListener<MyScheduleUserActionEnum> mListener;

    private TagMetadata mTagMetadata;

    public MyScheduleDayAdapter(Context context,
            UserActionListener<MyScheduleUserActionEnum> listener, TagMetadata tagMetadata) {
        mContext = context;
        mListener = listener;
        mTagMetadata = tagMetadata;

        Resources resources = context.getResources();
        mHourColorDefault = ContextCompat.getColor(context,
                R.color.my_schedule_hour_header_default);
        mHourColorPast = ContextCompat.getColor(context,
                R.color.my_schedule_hour_header_finished);
        mTitleColorDefault = ContextCompat.getColor(context,
                R.color.my_schedule_session_title_default);
        mTitleColorPast = ContextCompat.getColor(context,
                R.color.my_schedule_session_title_finished);
        mIconColorDefault = ContextCompat.getColor(context, R.color.my_schedule_icon_default);
        mIconColorPast = ContextCompat.getColor(context, R.color.my_schedule_icon_finished);
        mColorConflict = ContextCompat.getColor(context, R.color.my_schedule_conflict);
        mColorBackgroundDefault = ContextCompat.getColor(context, android.R.color.white);
        mColorBackgroundPast = ContextCompat.getColor(context, R.color.my_schedule_past_background);
        mListSpacing = resources.getDimensionPixelOffset(R.dimen.element_spacing_normal);

        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.selectableItemBackground});
        mSelectableItemBackground = a.getResourceId(0, 0);
        a.recycle();

        mIsRtl = UIUtils.isRtl(context);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        if (!mObservers.contains(observer)) {
            mObservers.add(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (mObservers.contains(observer)) {
            mObservers.remove(observer);
        }
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public ScheduleItem getItem(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    private String formatDescription(ScheduleItem item) {
        StringBuilder description = new StringBuilder();
        description.append(TimeUtils.formatShortTime(mContext, new Date(item.startTime)));
        if (!Config.Tags.SPECIAL_KEYNOTE.equals(item.mainTag)) {
            description.append(" - ");
            description.append(TimeUtils.formatShortTime(mContext, new Date(item.endTime)));
        }
        if (!TextUtils.isEmpty(item.room)) {
            description.append(" / ");
            description.append(item.room);
        }
        return description.toString();
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

    private void setUriClickable(View view, Uri uri, boolean setBackground) {
        view.setTag(R.id.myschedule_uri_tagkey, uri);
        view.setOnClickListener(mUriOnClickListener);
        if (setBackground) {
            view.setBackgroundResource(mSelectableItemBackground);
        }
    }

    private static void clearClickable(View view) {
        view.setOnClickListener(null);
        view.setClickable(false);
    }

    /**
     * Enforces right-alignment to all the TextViews in the {@code holder}. This is not necessary if
     * all the data is localized in the targeted RTL language, but as we will not be able to
     * localize the conference data, we hack it.
     *
     * @param holder The {@link ItemViewHolder} of the list item.
     */
    @SuppressLint("RtlHardcoded")
    private void adjustForRtl(ItemViewHolder holder) {
        if (mIsRtl) {
          // TODO check if this is needed
//            startTime.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
//            title.setGravity(Gravity.RIGHT);
//            description.setGravity(Gravity.RIGHT);
//            browse.setGravity(Gravity.RIGHT);
            LOGD(TAG, "Gravity right");
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(mContext);
        }

        ListViewHolder holder = null;
        // Create a new view if it is not ready yet.
        if (view == null) {
            final LayoutInflater li = LayoutInflater.from(mContext);
            switch (getItemViewType(position)) {
                case 0:
                    holder = new ItemViewHolder(
                            li.inflate(R.layout.my_schedule_item, parent, false));
                    break;
                case 1:
                    holder = new TimeSeperatorViewHolder(
                            li.inflate(R.layout.my_schedule_time_separator_item, parent, false));
                    break;
            }
        } else {
            holder = (ListViewHolder) view.getTag();
        }

        holder.onBind(position);

        return holder.view;
    }

    @Override
    public int getItemViewType(int position) {
        ScheduleItem item = getItem(position);
        if (item instanceof TimeSeperatorItem) {
            return 1;
        }
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public void clear() {
        updateItems(null);
    }

    private void notifyObservers() {
        for (DataSetObserver observer : mObservers) {
            observer.onChanged();
        }
    }

    public void forceUpdate() {
        notifyObservers();
    }

    public void setTagMetadata(final TagMetadata tagMetadata) {
        if (mTagMetadata != tagMetadata) {
            mTagMetadata = tagMetadata;
            forceUpdate();
        }
    }

    public void updateItems(final List<ScheduleItem> items) {
        mItems.clear();
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
        notifyObservers();
    }

    @Override
    public void onMovedToScrapHeap(View view) {
    }

    class ItemViewHolder extends ListViewHolder {
        public TextView title;
        public TextView description;
        public Button feedback;
        public View live;
        public View separator;
        public FlexboxLayout tagsHolder;

        ItemViewHolder(final View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.slot_title);
            description = (TextView) view.findViewById(R.id.slot_description);
            feedback = (Button) view.findViewById(R.id.give_feedback_button);
            separator = view.findViewById(R.id.separator);
            live = view.findViewById(R.id.live_now_badge);
            tagsHolder = (FlexboxLayout) view.findViewById(R.id.tags_holder);
            adjustForRtl(this);
        }

        @Override
        void onBind(int position) {
            clearClickable(view);

            description.setTextColor(mHourColorDefault);

            final ScheduleItem item = mItems.get(position);
            final ScheduleItem nextItem = position < mItems.size() - 1
                    ? mItems.get(position + 1)
                    : null;

            long now = TimeUtils.getCurrentTime(view.getContext());
            boolean isNowPlaying = item.startTime <= now && now <= item.endTime
                    && item.type == ScheduleItem.SESSION;
            boolean isPastDuringConference = item.endTime <= now
                    && now < Config.CONFERENCE_END_MILLIS;

            if (isPastDuringConference) {
                view.setBackgroundColor(mColorBackgroundPast);
                title.setTextColor(mTitleColorPast);
                description.setVisibility(View.GONE);
            } else {
                view.setBackgroundColor(mColorBackgroundDefault);
                title.setTextColor(mTitleColorDefault);
                description.setVisibility(View.VISIBLE);
            }

            // show or hide the "LIVE NOW" badge
            live.setVisibility((item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM) != 0
                    && isNowPlaying ? View.VISIBLE : View.GONE);

            // Remove all views from tags holder
            tagsHolder.removeAllViews();
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);

            view.setTag(R.id.myschedule_uri_tagkey, null);
            if (item.type == ScheduleItem.BREAK) {
                feedback.setVisibility(View.GONE);
                title.setVisibility(View.VISIBLE);
                title.setText(item.title);
                description.setText(formatDescription(item));
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
                                    mContext, SessionFeedbackActivity.class);
                            mContext.startActivity(feedbackIntent);

                        }
                    });
                }

                title.setVisibility(View.VISIBLE);
                title.setText(item.title);

                final Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(item.sessionId);
                setUriClickable(view, sessionUri, true);

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

                description.setText(formatDescription(item));
            }

            if (item.isKeynote() || (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM) != 0) {
                if (tagsHolder.getChildCount() > 0) {
                    // Insert the spacer first
                    layoutInflater.inflate(R.layout.include_schedule_live_spacer, tagsHolder);
                }

                View liveStreamView = layoutInflater.inflate(R.layout.include_schedule_live,
                        tagsHolder, false);
                // TODO: clickable?
                tagsHolder.addView(liveStreamView);
            }

            tagsHolder.setVisibility(tagsHolder.getChildCount() > 0 ? View.VISIBLE : View.GONE);

            if (position == 0) { // First item
                view.setPadding(0, mListSpacing, 0, 0);
            } else if (nextItem == null) { // Last item
                view.setPadding(0, 0, 0, mListSpacing);
            } else {
                view.setPadding(0, 0, 0, 0);
            }
        }
    }

    class TimeSeperatorViewHolder extends ListViewHolder {
        public TextView startTime;

        TimeSeperatorViewHolder(final View view) {
            super(view);
            startTime = (TextView) view.findViewById(R.id.start_time);
        }

        @Override
        void onBind(int position) {
            final ScheduleItem item = mItems.get(position);
            startTime.setText(TimeUtils.formatShortTime(mContext, new Date(item.startTime)));
        }
    }

    abstract static class ListViewHolder {
        final View view;

        ListViewHolder(View view) {
            this.view = view;
            view.setTag(this);
        }

        abstract void onBind(int position);
    }

    static class TimeSeperatorItem extends ScheduleItem {
        TimeSeperatorItem(ScheduleItem item) {
            this.startTime = item.startTime;
        }
    }

}
