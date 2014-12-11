/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.*;

import java.util.ArrayList;
import java.util.Date;

import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * Adapter that produces views to render (one day of) the "My Schedule" screen.
 */
public class MyScheduleAdapter implements ListAdapter, AbsListView.RecyclerListener {
    private static final String TAG = makeLogTag("MyScheduleAdapter");
    private static final int TAG_ID_FOR_VIEW_TYPE = R.id.myschedule_viewtype_tagkey;
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_NOW = 1;
    private static final int VIEW_TYPE_PAST_DURING_CONFERENCE = 2;

    private final Context mContext;
    private final LUtils mLUtils;

    // additional top padding to add to first item of list
    int mContentTopClearance = 0;

    // list of items served by this adapter
    ArrayList<ScheduleItem> mItems = new ArrayList<ScheduleItem>();

    // observers to notify about changes in the data
    ArrayList<DataSetObserver> mObservers = new ArrayList<DataSetObserver>();

    ImageLoader mImageLoader;

    int mDefaultSessionColor;
    int mDefaultStartTimeColor;
    int mDefaultEndTimeColor;

    // increased every time the data is updated; used when deciding whether to
    // recycle views so we can tell that a view is from a previous generation of
    // the data and thus shouldn't be used
    int mDataGeneration = 0;

    public MyScheduleAdapter(Context context, LUtils lUtils) {
        mContext = context;
        mLUtils = lUtils;

        mDefaultSessionColor = mContext.getResources().getColor(R.color.default_session_color);
        mDefaultStartTimeColor = mContext.getResources().getColor(R.color.body_text_2);
        mDefaultEndTimeColor = mContext.getResources().getColor(R.color.body_text_3);
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

    public void setContentTopClearance(int padding) {
        mContentTopClearance = padding;
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

    private static final String MY_VIEW_TAG = "MyScheduleAdapter_MY_VIEW_TAG";

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(mContext);
        }

        Resources res = mContext.getResources();

        TextView startTimeView = null;
        TextView endTimeView = null;

        int itemViewType = getItemViewType(position);
        boolean isNowPlaying = false;
        boolean isPastDuringConference = false;
        int layoutResId = R.layout.my_schedule_item;
        if (itemViewType == VIEW_TYPE_NOW) {
            isNowPlaying = true;
            layoutResId = R.layout.my_schedule_item_now;
        } else if (itemViewType == VIEW_TYPE_PAST_DURING_CONFERENCE) {
            isPastDuringConference = true;
            layoutResId = R.layout.my_schedule_item_past;
        }

        // If the view to recycle is null or is for the wrong view type or data
        // generation, ignore it and create a new one.
        if (view == null || !MY_VIEW_TAG.equals(view.getTag())
                || view.getTag(TAG_ID_FOR_VIEW_TYPE) == null
                || !view.getTag(TAG_ID_FOR_VIEW_TYPE).equals(itemViewType)) {
            view = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(layoutResId, parent, false);
            // save this view's type, so we only recycle when the view's type is the same:
            view.setTag(TAG_ID_FOR_VIEW_TYPE, itemViewType);
            // Use one listener per view, so when the view is recycled, the listener is reused as
            // well. Use the View tag as a container for the destination Uri.
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Object tag = v.getTag(R.id.myschedule_uri_tagkey);
                    if (tag != null && tag instanceof Uri) {
                        Uri uri = (Uri) tag;
                        /* [ANALYTICS:EVENT]
                         * TRIGGER:   Select a slot on My Agenda
                         * CATEGORY:  'My Schedule'
                         * ACTION:    'selectslot'
                         * LABEL:     URI indicating session ID or time interval of slot.
                         * [/ANALYTICS]
                         */
                        AnalyticsManager.sendEvent("My Schedule", "selectslot", uri.toString());
                        mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
                }
            });
        }

        if (position < 0 || position >= mItems.size()) {
            LOGE(TAG, "Invalid view position passed to MyScheduleAdapter: " + position);
            return view;
        }
        final ScheduleItem item = mItems.get(position);
        ScheduleItem nextItem = (position < mItems.size() - 1) ? mItems.get(position + 1) : null;

        view.setTag(MY_VIEW_TAG);
        startTimeView = (TextView) view.findViewById(R.id.start_time);
        endTimeView = (TextView) view.findViewById(R.id.end_time);
        ImageView bgImageView = (ImageView) view.findViewById(R.id.background_image);
        final ImageView sessionImageView = (ImageView) view.findViewById(R.id.session_image);
        FrameLayout boxView = (FrameLayout) view.findViewById(R.id.box);
        TextView slotTitleView = (TextView) view.findViewById(R.id.slot_title);
        TextView slotSubtitleView = (TextView) view.findViewById(R.id.slot_subtitle);
        ImageButton giveFeedbackButton = (ImageButton) view.findViewById(R.id.give_feedback_button);
        int heightNormal = res.getDimensionPixelSize(R.dimen.my_schedule_item_height);
        int heightBreak = ViewGroup.LayoutParams.WRAP_CONTENT;
        int heightPast = res.getDimensionPixelSize(R.dimen.my_schedule_item_height_past);

        long now = UIUtils.getCurrentTime(view.getContext());
        boolean showEndTime = false;
        boolean isBlockNow = false;
        if (item.endTime <= now) {
            // session has ended
            startTimeView.setText(R.string.session_finished);
        } else if (item.startTime <= now) {
            // session is happening now!
            isBlockNow = true;
            startTimeView.setText(R.string.session_now);
            showEndTime = nextItem == null || nextItem.startTime != item.endTime;
        } else {
            // session in the future
            startTimeView.setText(TimeUtils.formatShortTime(mContext, new Date(item.startTime)));
            // do we need and end time view?
            showEndTime = nextItem == null || nextItem.startTime != item.endTime;
        }

        if (endTimeView != null) {
            if (showEndTime) {
                endTimeView.setVisibility(View.VISIBLE);
                endTimeView.setText(res.getString(R.string.schedule_end_time,
                        TimeUtils.formatShortTime(mContext, new Date(item.endTime))));
            } else {
                // no need to show end time
                endTimeView.setVisibility(View.GONE);
            }
        }

        View liveNowBadge =  view.findViewById(R.id.live_now_badge);
        View conflictWarning =  view.findViewById(R.id.conflict_warning);
        if (liveNowBadge != null) {
            liveNowBadge.setVisibility(View.GONE);
        }
        if (conflictWarning != null) {
            conflictWarning.setVisibility(View.GONE);
        }

        // Set default colors to time indicators, in case they were overridden by conflict warning:
        if (!isNowPlaying) {
            if (startTimeView != null) {
                startTimeView.setTextColor(mDefaultStartTimeColor);
            }
            if (endTimeView != null) {
                endTimeView.setTextColor(mDefaultEndTimeColor);
            }
        }

        view.setTag(R.id.myschedule_uri_tagkey, null);
        if (item.type == ScheduleItem.FREE) {
            view.getLayoutParams().height = isPastDuringConference ? heightPast : heightNormal;
            boxView.setBackgroundResource(R.drawable.schedule_item_free);
            boxView.setForeground(res.getDrawable(R.drawable.schedule_item_touchoverlay_dark));
            bgImageView.setVisibility(View.GONE);
            sessionImageView.setVisibility(View.GONE);
            if (giveFeedbackButton != null) {
                giveFeedbackButton.setVisibility(View.GONE);
            }
            slotTitleView.setText(R.string.browse_sessions);
            slotTitleView.setTextColor(res.getColor(R.color.theme_primary));
            slotTitleView.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
            if (slotSubtitleView != null) {
                slotSubtitleView.setText(item.subtitle);
                slotSubtitleView.setTextColor(res.getColor(R.color.body_text_2));
            }
            Uri uri = ScheduleContract.Sessions.buildUnscheduledSessionsInInterval(
                    item.startTime, item.endTime);
            view.setTag(R.id.myschedule_uri_tagkey, uri);

        } else if (item.type == ScheduleItem.BREAK) {
            view.getLayoutParams().height = isPastDuringConference ? heightPast : heightBreak;
            boxView.setBackgroundResource(R.drawable.schedule_item_break);
            boxView.setForeground(null);
            bgImageView.setVisibility(View.GONE);
            sessionImageView.setVisibility(View.GONE);
            if (giveFeedbackButton != null) {
                giveFeedbackButton.setVisibility(View.GONE);
            }
            slotTitleView.setText(item.title);
            slotTitleView.setTextColor(res.getColor(R.color.body_text_1));
            slotTitleView.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
            if (slotSubtitleView != null) {
                slotSubtitleView.setText(item.subtitle);
                slotSubtitleView.setTextColor(res.getColor(R.color.body_text_2));
            }

        } else if (item.type == ScheduleItem.SESSION) {
            view.getLayoutParams().height = isPastDuringConference ? heightPast : heightNormal;
            boxView.setBackgroundResource(R.drawable.schedule_item_session);
            boxView.setForeground(res.getDrawable(R.drawable.schedule_item_touchoverlay));
            bgImageView.setVisibility(View.VISIBLE);
            sessionImageView.setVisibility(View.VISIBLE);
            if (giveFeedbackButton != null) {
                boolean showFeedbackButton = !item.hasGivenFeedback;
                // Can't use isPastDuringConference because we want to show feedback after the
                // conference too.
                if (showFeedbackButton) {
                    if (item.endTime > now) {
                        // Session hasn't finished yet, don't show button.
                        showFeedbackButton = false;
                    }
                }
                giveFeedbackButton.setVisibility(showFeedbackButton ? View.VISIBLE : View.GONE);
                giveFeedbackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        /* [ANALYTICS:EVENT]
                         * TRIGGER:   Click on the Send Feedback action for a session on the Schedule page.
                         * CATEGORY:  'Session'
                         * ACTION:    'Feedback'
                         * LABEL:     session title/subtitle
                         * [/ANALYTICS]
                         */
                        AnalyticsManager.sendEvent("My Schedule", "Feedback", item.title, 0L);
                        Intent feedbackIntent = new Intent(Intent.ACTION_VIEW,
                                ScheduleContract.Sessions.buildSessionUri(item.sessionId),
                                mContext, SessionFeedbackActivity.class);
                        mContext.startActivity(feedbackIntent);
                    }
                });
            }
            int color = UIUtils.scaleSessionColorToDefaultBG(
                    item.backgroundColor == 0 ? mDefaultSessionColor : item.backgroundColor);

            final ColorDrawable colorDrawable = new ColorDrawable(color);
            bgImageView.setImageDrawable(colorDrawable);
            ColorFilter scrimFilter = UIUtils.makeSessionImageScrimColorFilter(color);
            bgImageView.setColorFilter(scrimFilter);

            if (TextUtils.isEmpty(item.backgroundImageUrl)) {
                sessionImageView.setVisibility(View.GONE);
            } else {
                sessionImageView.setColorFilter(scrimFilter);
                mImageLoader.loadImage(item.backgroundImageUrl, sessionImageView, null,
                        colorDrawable);
            }
            slotTitleView.setText(item.title);
            slotTitleView.setTextColor(isBlockNow
                    ? Color.WHITE
                    : res.getColor(R.color.body_text_1_inverse));
            mLUtils.setMediumTypeface(slotTitleView);
            if (slotSubtitleView != null) {
                slotSubtitleView.setText(item.subtitle);
                slotSubtitleView.setTextColor(res.getColor(R.color.body_text_2_inverse));
            }
            view.setTag(R.id.myschedule_uri_tagkey,
                    ScheduleContract.Sessions.buildSessionUri(item.sessionId));

            // show or hide the "LIVE NOW" badge
            if (liveNowBadge != null) {
                final boolean showLiveBadge = 0 != (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM)
                        && isBlockNow;
                liveNowBadge.setVisibility(showLiveBadge ? View.VISIBLE : View.GONE);
            }

            // show or hide the "conflict" warning
            if (!isPastDuringConference) {
                final boolean showConflict = 0 != (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS);
                conflictWarning.setVisibility(showConflict ? View.VISIBLE : View.GONE);
                if (showConflict && !isNowPlaying) {
                    int conflictColor = res.getColor(R.color.my_schedule_conflict);
                    startTimeView.setTextColor(conflictColor);
                    if (endTimeView != null) {
                        endTimeView.setTextColor(conflictColor);
                    }
                }
            }

        } else {
            LOGE(TAG, "Invalid item type in MyScheduleAdapter: " + item.type);
        }

        return view;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= mItems.size()) {
            LOGE(TAG, "Invalid view position passed to MyScheduleAdapter: " + position);
            return VIEW_TYPE_NORMAL;
        }
        ScheduleItem item = mItems.get(position);
        long now = UIUtils.getCurrentTime(mContext);
        if (item.startTime <= now && now <= item.endTime && item.type == ScheduleItem.SESSION) {
            return VIEW_TYPE_NOW;
        } else if (item.endTime <= now && now < Config.CONFERENCE_END_MILLIS) {
            return VIEW_TYPE_PAST_DURING_CONFERENCE;
        } else {
            return VIEW_TYPE_NORMAL;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
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

    public void updateItems(ArrayList<ScheduleItem> items) {
        mItems.clear();
        if (items != null) {
            for (ScheduleItem item : items) {
                LOGD(TAG, "Adding schedule item: "+item+" start="+new Date(item.startTime));
                mItems.add((ScheduleItem) item.clone());
            }
        }
        notifyObservers();
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        if (view != null) {
            ImageView sessionImageView = (ImageView) view.findViewById(R.id.session_image);
            if (sessionImageView != null) {
                sessionImageView.clearAnimation();
            }
        }
    }
}
