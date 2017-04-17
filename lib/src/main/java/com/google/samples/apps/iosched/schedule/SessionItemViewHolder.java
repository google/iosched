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
package com.google.samples.apps.iosched.schedule;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.RegistrationUtils;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.samples.apps.iosched.Config.Tags.CATEGORY_TRACK;
import static com.google.samples.apps.iosched.provider.ScheduleContract.MyReservations.RESERVATION_STATUS_RESERVED;
import static com.google.samples.apps.iosched.provider.ScheduleContract.MyReservations.RESERVATION_STATUS_UNRESERVED;
import static com.google.samples.apps.iosched.provider.ScheduleContract.MyReservations.RESERVATION_STATUS_WAITLISTED;

/**
 * A {@link ViewHolder} modeling Sessions.
 */
public class SessionItemViewHolder extends ScheduleItemViewHolder
        implements DividerDecoration.Divided {

    private final TextView mTitle;
    private final TextView mReservationStatus;
    private final TextView mDescription;
    private final ViewGroup mTagsHolder;
    private final ImageButton mBookmark;
    private final View mLiveNow;
    private final Button mRate;

    private final Callbacks mCallbacks;
    private final View.OnClickListener mTagClick;
    @Nullable
    private ScheduleItem mSession;

    private SessionItemViewHolder(View itemView, final Callbacks callbacks) {
        super(itemView);
        mCallbacks = callbacks;
        mTitle = (TextView) itemView.findViewById(R.id.slot_title);
        mReservationStatus = (TextView) itemView.findViewById(R.id.reserve_status);
        mDescription = (TextView) itemView.findViewById(R.id.slot_description);
        mTagsHolder = (FlexboxLayout) itemView.findViewById(R.id.tags_holder);
        mBookmark = (ImageButton) itemView.findViewById(R.id.bookmark);
        mLiveNow = itemView.findViewById(R.id.live_now_badge);
        mRate = (Button) itemView.findViewById(R.id.give_feedback_button);

        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallbacks == null || mSession == null) {
                    return;
                }
                mCallbacks.onSessionClicked(mSession.sessionId);
            }
        });
        mRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallbacks == null || mSession == null) {
                    return;
                }
                mCallbacks.onFeedbackClicked(mSession.sessionId, mSession.title);
            }
        });
        mBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (mCallbacks == null || mSession == null) {
                    return;
                }
                mBookmark.setActivated(!mBookmark.isActivated());
                mCallbacks.onBookmarkClicked(mSession.sessionId, mSession.inSchedule);
            }
        });
        mTagClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Tag tag = (Tag) v.getTag(R.id.key_session_tag);
                if (tag == null || mCallbacks == null) {
                    return;
                }
                mCallbacks.onTagClicked(tag);
            }
        };
    }

    public static SessionItemViewHolder newInstance(ViewGroup parent, Callbacks callbacks) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.schedule_session_item, parent, false);
        return new SessionItemViewHolder(itemView, callbacks);
    }

    private void updateReservationStatus(ScheduleItem item) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null ||
                !RegistrationUtils.isRegisteredAttendee(mReservationStatus.getContext()) ||
                item.isKeynote() ||
                item.reservationStatus == RESERVATION_STATUS_UNRESERVED) {
            mReservationStatus.setVisibility(GONE);
            return;
        }
        if (item.reservationStatus == RESERVATION_STATUS_RESERVED ||
                item.reservationStatus == RESERVATION_STATUS_WAITLISTED) {
            mReservationStatus.setCompoundDrawablesWithIntrinsicBounds(
                    item.reservationStatus == RESERVATION_STATUS_RESERVED ?
                            R.drawable.ic_reserved : R.drawable.ic_waitlisted, 0, 0, 0);
            mReservationStatus.setText(item.reservationStatus == RESERVATION_STATUS_RESERVED ?
                    R.string.schedule_item_reserved : R.string.schedule_item_waitlisted);
        }
    }

    public void onBind(@NonNull final ScheduleItem item, TagMetadata tagMetadata) {
        if (item.type != ScheduleItem.SESSION) {
            return;
        }
        mSession = item;
        final Context context = itemView.getContext();

        mTitle.setText(item.title);
        updateReservationStatus(item);
        mDescription.setText(formatDescription(context, item));

        mTagsHolder.removeAllViews();
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        if (tagMetadata != null) {
            List<Tag> tags = new ArrayList<>();
            final TagMetadata.Tag mainTag = tagMetadata.getTag(item.mainTag);
            if (mainTag != null) {
                tags.add(mainTag);
            }
            for (String tagId : item.tags) {
                if (!tagId.startsWith(CATEGORY_TRACK)) {
                    continue;
                }
                Tag tag = tagMetadata.getTagById(tagId);
                if (tag == null || tag.equals(mainTag)) {
                    continue;
                }
                tags.add(tag);
            }
            for (Tag tag : tags) {
                TextView tagView = (TextView) layoutInflater.inflate(
                        R.layout.include_schedule_tag, mTagsHolder, false);
                tagView.setText(tag.getName());
                tagView.setBackgroundTintList(ColorStateList.valueOf(tag.getColor()));
                tagView.setTag(R.id.key_session_tag, tag);
                mTagsHolder.addView(tagView);
                tagView.setOnClickListener(mTagClick);
            }
        }

        if (item.isKeynote() || (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM) != 0) {
            mTagsHolder.addView(layoutInflater.inflate(R.layout.include_schedule_live,
                    mTagsHolder, false));
        }
        mTagsHolder.setVisibility(mTagsHolder.getChildCount() > 0 ? VISIBLE : GONE);

        if (mCallbacks.bookmarkingEnabled() && !item.isKeynote()) {
            mBookmark.setVisibility(VISIBLE);
            // activated is proxy for in-schedule
            mBookmark.setActivated(item.inSchedule);
        } else {
            mBookmark.setVisibility(GONE);
        }

        final long now = TimeUtils.getCurrentTime(context);
        final boolean streamingNow = item.startTime <= now && now <= item.endTime
                && (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM) != 0;
        mLiveNow.setVisibility(streamingNow ? VISIBLE : GONE);
        mRate.setVisibility((now > item.endTime && !item.hasGivenFeedback) ? VISIBLE : GONE);
    }

    public interface Callbacks {
        /**
         * @param sessionId The ID of the session
         */
        void onSessionClicked(String sessionId);

        /**
         * @return true if bookmark icons should be shown
         */
        boolean bookmarkingEnabled();

        /**
         * @param sessionId    The ID of the session
         * @param isInSchedule Whether the session is bookmarked in the backing data
         */
        void onBookmarkClicked(String sessionId, boolean isInSchedule);

        /**
         * @param sessionId    The ID of the session
         * @param sessionTitle The title of the session
         */
        void onFeedbackClicked(String sessionId, String sessionTitle);

        /**
         * @param tag The tag that was clicked
         */
        void onTagClicked(Tag tag);
    }
}
