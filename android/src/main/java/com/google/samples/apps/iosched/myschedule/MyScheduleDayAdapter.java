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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.UpdatableView.UserActionListener;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel.MyScheduleUserActionEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.LUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Adapter that produces views to render (one day of) the "My Schedule" screen.
 */
public class MyScheduleDayAdapter implements ListAdapter, AbsListView.RecyclerListener {
    private static final String TAG = makeLogTag("MyScheduleDayAdapter");

    private final Context mContext;
    private final LUtils mLUtils;

    // list of items served by this adapter
    ArrayList<ScheduleItem> mItems = new ArrayList<>();

    // observers to notify about changes in the data
    ArrayList<DataSetObserver> mObservers = new ArrayList<>();

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

    public MyScheduleDayAdapter(Context context, LUtils lUtils,
            UserActionListener<MyScheduleUserActionEnum> listener) {
        mContext = context;
        mLUtils = lUtils;
        mListener = listener;
        Resources resources = context.getResources();
        mHourColorDefault = resources.getColor(R.color.my_schedule_hour_header_default);
        mHourColorPast = resources.getColor(R.color.my_schedule_hour_header_finished);
        mTitleColorDefault = resources.getColor(R.color.my_schedule_session_title_default);
        mTitleColorPast = resources.getColor(R.color.my_schedule_session_title_finished);
        mIconColorDefault = resources.getColor(R.color.my_schedule_icon_default);
        mIconColorPast = resources.getColor(R.color.my_schedule_icon_finished);
        mColorConflict = resources.getColor(R.color.my_schedule_conflict);
        mColorBackgroundDefault = resources.getColor(android.R.color.white);
        mColorBackgroundPast = resources.getColor(R.color.my_schedule_past_background);
        mListSpacing = resources.getDimensionPixelOffset(R.dimen.element_spacing_normal);
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.selectableItemBackground});
        mSelectableItemBackground = a.getResourceId(0, 0);
        a.recycle();
        mIsRtl = UIUtils.isRtl(context);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
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
    public Object getItem(int position) {
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

    private View.OnClickListener mUriOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Object tag = v.getTag(R.id.myschedule_uri_tagkey);
            if (tag != null && tag instanceof Uri) {
                Uri uri = (Uri) tag;
                Bundle bundle = new Bundle();
                bundle.putString(MyScheduleModel.SESSION_URL_KEY, uri.toString());
                mListener.onUserAction(MyScheduleUserActionEnum.SESSION_SLOT, bundle);

                mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        }
    };

    private void setUriClickable(View view, Uri uri) {
        view.setTag(R.id.myschedule_uri_tagkey, uri);
        view.setOnClickListener(mUriOnClickListener);
        view.setBackgroundResource(mSelectableItemBackground);
    }

    private static void clearClickable(View view) {
        view.setOnClickListener(null);
        view.setBackgroundResource(0);
        view.setClickable(false);
    }

    /**
     * Enforces right-alignment to all the TextViews in the {@code holder}. This is not necessary if
     * all the data is localized in the targeted RTL language, but as we will not be able to
     * localize the conference data, we hack it.
     *
     * @param holder The {@link ViewHolder} of the list item.
     */
    @SuppressLint("RtlHardcoded")
    private void adjustForRtl(ViewHolder holder) {
        if (mIsRtl) {
            holder.startTime.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            holder.title.setGravity(Gravity.RIGHT);
            holder.description.setGravity(Gravity.RIGHT);
            holder.browse.setGravity(Gravity.RIGHT);
            android.util.Log.d(TAG, "Gravity right");
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(mContext);
        }

        ViewHolder holder;
        // Create a new view if it is not ready yet.
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.my_schedule_item, parent, false);
            holder = new ViewHolder();
            holder.startTime = (TextView) view.findViewById(R.id.start_time);
            holder.more = (TextView) view.findViewById(R.id.more);
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.slot_title);
            holder.description = (TextView) view.findViewById(R.id.slot_description);
            holder.browse = (TextView) view.findViewById(R.id.browse_sessions);
            holder.feedback = (Button) view.findViewById(R.id.give_feedback_button);
            holder.separator = view.findViewById(R.id.separator);
            holder.touchArea = view.findViewById(R.id.touch_area);
            holder.live = view.findViewById(R.id.live_now_badge);
            view.setTag(holder);
            // Typeface
            mLUtils.setMediumTypeface(holder.startTime);
            mLUtils.setMediumTypeface(holder.browse);
            mLUtils.setMediumTypeface(holder.title);
            adjustForRtl(holder);
        } else {
            holder = (ViewHolder) view.getTag();
            // Clear event listeners
            clearClickable(view);
            clearClickable(holder.startTime);
            clearClickable(holder.touchArea);
            //Make sure it doesn't retain conflict coloring
            holder.description.setTextColor(mHourColorDefault);
        }

        if (position < 0 || position >= mItems.size()) {
            LOGE(TAG, "Invalid view position passed to MyScheduleDayAdapter: " + position);
            return view;
        }
        final ScheduleItem item = mItems.get(position);
        ScheduleItem nextItem = position < mItems.size() - 1 ? mItems.get(position + 1) : null;

        long now = TimeUtils.getCurrentTime(view.getContext());
        boolean isNowPlaying =
                item.startTime <= now && now <= item.endTime && item.type == ScheduleItem.SESSION;
        boolean isPastDuringConference = item.endTime <= now && now < Config.CONFERENCE_END_MILLIS;

        if (isPastDuringConference) {
            view.setBackgroundColor(mColorBackgroundPast);
            holder.startTime.setTextColor(mHourColorPast);
            holder.title.setTextColor(mTitleColorPast);
            holder.description.setVisibility(View.GONE);
            holder.icon.setColorFilter(mIconColorPast);
        } else {
            view.setBackgroundColor(mColorBackgroundDefault);
            holder.startTime.setTextColor(mHourColorDefault);
            holder.title.setTextColor(mTitleColorDefault);
            holder.description.setVisibility(View.VISIBLE);
            holder.icon.setColorFilter(mIconColorDefault);
        }

        holder.startTime.setText(TimeUtils.formatShortTime(mContext, new Date(item.startTime)));

        // show or hide the "LIVE NOW" badge
        holder.live.setVisibility(0 != (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM)
                && isNowPlaying ? View.VISIBLE : View.GONE);

        holder.touchArea.setTag(R.id.myschedule_uri_tagkey, null);
        if (item.type == ScheduleItem.FREE) {
            holder.startTime.setVisibility(View.VISIBLE);
            holder.more.setVisibility(View.GONE);
            holder.icon.setImageResource(R.drawable.ic_browse);
            holder.feedback.setVisibility(View.GONE);
            holder.title.setVisibility(View.GONE);
            holder.browse.setVisibility(View.VISIBLE);
            setUriClickable(view, ScheduleContract.Sessions.buildUnscheduledSessionsInInterval(
                    item.startTime, item.endTime));
            holder.description.setVisibility(View.GONE);
        } else if (item.type == ScheduleItem.BREAK) {
            holder.startTime.setVisibility(View.VISIBLE);
            holder.more.setVisibility(View.GONE);
            holder.feedback.setVisibility(View.GONE);
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(item.title);
            holder.icon.setImageResource(UIUtils.getBreakIcon(item.title));
            holder.browse.setVisibility(View.GONE);
            holder.description.setText(formatDescription(item));
        } else if (item.type == ScheduleItem.SESSION) {
            if (holder.feedback != null) {
                boolean showFeedbackButton = !item.hasGivenFeedback;
                // Can't use isPastDuringConference because we want to show feedback after the
                // conference too.
                if (showFeedbackButton) {
                    if (item.endTime > now) {
                        // Session hasn't finished yet, don't show button.
                        showFeedbackButton = false;
                    }
                }
                holder.feedback.setVisibility(showFeedbackButton ? View.VISIBLE : View.GONE);
                holder.feedback.setOnClickListener(new View.OnClickListener() {
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
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(item.title);
            holder.more.setVisibility(item.isKeynote() ? View.GONE : View.VISIBLE);
            holder.browse.setVisibility(View.GONE);
            holder.icon.setImageResource(UIUtils.getSessionIcon(item.sessionType));

            final Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(item.sessionId);
            if (0 != (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS)) {
                holder.startTime.setVisibility(View.GONE);
                holder.description.setTextColor(mColorConflict);
                setUriClickable(holder.touchArea, sessionUri);
            } else {
                holder.startTime.setVisibility(View.VISIBLE);
                setUriClickable(holder.startTime, ScheduleContract.Sessions
                        .buildUnscheduledSessionsInInterval(item.startTime, item.endTime));

                // Padding fix needed for KitKat (padding gets removed by setting the background)
                holder.startTime.setPadding(
                        (int) mContext.getResources().getDimension(R.dimen.keyline_2), 0,
                        (int) mContext.getResources().getDimension(R.dimen.keyline_2), 0);
                setUriClickable(holder.touchArea, sessionUri);
                if (0 != (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_NEXT)) {
                    holder.description.setTextColor(mColorConflict);
                }
            }
            holder.description.setText(formatDescription(item));
        } else {
            LOGE(TAG, "Invalid item type in MyScheduleDayAdapter: " + item.type);
        }

        holder.separator.setVisibility(nextItem == null ||
                0 != (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_NEXT) ? View.GONE :
                View.VISIBLE);

        if (position == 0) { // First item
            view.setPadding(0, mListSpacing, 0, 0);
        } else if (nextItem == null) { // Last item
            view.setPadding(0, 0, 0, mListSpacing);
        } else {
            view.setPadding(0, 0, 0, 0);
        }

        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }


    @Override
    public int getViewTypeCount() {
        return 1;
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

    public void updateItems(List<ScheduleItem> items) {
        mItems.clear();
        if (items != null) {
            for (ScheduleItem item : items) {
                LOGD(TAG, "Adding schedule item: " + item + " start=" + new Date(item.startTime));
                mItems.add((ScheduleItem) item.clone());
            }
        }
        notifyObservers();
    }

    @Override
    public void onMovedToScrapHeap(View view) {
    }

    private static class ViewHolder {
        public TextView startTime;
        public TextView more;
        public ImageView icon;
        public TextView title;
        public TextView description;
        public Button feedback;
        public TextView browse;
        public View live;
        public View separator;
        public View touchArea;
    }

}
