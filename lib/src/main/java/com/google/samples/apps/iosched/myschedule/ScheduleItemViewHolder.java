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
package com.google.samples.apps.iosched.myschedule;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.Date;


public class ScheduleItemViewHolder extends RecyclerView.ViewHolder {

    private final TextView title;
    private final TextView description;
    private final Button feedback;
    private final View live;
    private final FlexboxLayout tagsHolder;
    private final ImageView bookmark;

    private final Callbacks mCallbacks;

    private final StringBuilder mTmpStringBuilder = new StringBuilder();

    public interface Callbacks {
        /**
         * @param sessionId The ID of the session
         */
        void onSessionClicked(String sessionId);

        /**
         * @return true if bookmark icons should be clickable and call back to this listener.
         */
        boolean bookmarkingEnabled();

        /**
         * @param sessionId The ID of the session
         * @param isInSchedule Whether the session is bookmarked in the backing data
         */
        void onBookmarkClicked(String sessionId, boolean isInSchedule);

        /**
         * @param sessionId The ID of the session
         * @param sessionTitle The title of the session
         */
        void onFeedbackClicked(String sessionId, String sessionTitle);

        /**
         * @param tag The tag that was clicked
         */
        void onTagClicked(Tag tag);
    }

    public ScheduleItemViewHolder(View itemView, Callbacks callbacks) {
        super(itemView);
        mCallbacks = callbacks;

        title = (TextView) itemView.findViewById(R.id.slot_title);
        description = (TextView) itemView.findViewById(R.id.slot_description);
        feedback = (Button) itemView.findViewById(R.id.give_feedback_button);
        live = itemView.findViewById(R.id.live_now_badge);
        tagsHolder = (FlexboxLayout) itemView.findViewById(R.id.tags_holder);
        bookmark = (ImageView) itemView.findViewById(R.id.bookmark);
    }

    public void onBind(@NonNull final ScheduleItem item, TagMetadata tagMetadata) {
        itemView.setOnClickListener(null);
        itemView.setClickable(false);

        final Context context = itemView.getContext();
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
                        if (mCallbacks != null) {
                            mCallbacks.onFeedbackClicked(item.sessionId,
                                    item.title);
                        }
                    }
                });
            }

            title.setVisibility(View.VISIBLE);
            title.setText(item.title);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallbacks != null) {
                        mCallbacks.onSessionClicked(item.sessionId);
                    }
                }
            });

            // TODO show conflicts?
//            if ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) != 0) {
//                 description.setTextColor(mColorConflict);
//            }
//            if ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_NEXT) != 0) {
//                 description.setTextColor(mColorConflict);
//            }

            if (tagMetadata != null) {
                final TagMetadata.Tag mainTag = tagMetadata.getTag(item.mainTag);
                if (mainTag != null) {
                    TextView tagView = (TextView) layoutInflater.inflate(
                            R.layout.include_schedule_tag, tagsHolder, false);
                    tagView.setText(mainTag.getName());
                    ViewCompat.setBackgroundTintList(tagView,
                            ColorStateList.valueOf(mainTag.getColor()));
                    tagsHolder.addView(tagView);

                    tagView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mCallbacks != null) {
                                mCallbacks.onTagClicked(mainTag);
                            }
                        }
                    });
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
            bookmark.setEnabled(false);

            boolean bookmarkEnabled = !item.isKeynote();
            if (mCallbacks != null) {
                bookmarkEnabled &= mCallbacks.bookmarkingEnabled();
            }

            if (bookmarkEnabled) {
                bookmark.setEnabled(true);
                bookmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        // Toggle now so that it looks immediate
                        view.setActivated(!view.isActivated());

                        if (mCallbacks != null) {
                            mCallbacks.onBookmarkClicked(item.sessionId,
                                    item.inSchedule);
                        }
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
