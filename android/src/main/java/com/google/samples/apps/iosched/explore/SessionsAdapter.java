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

package com.google.samples.apps.iosched.explore;


import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.data.SessionData;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.session.SessionDetailActivity;
import com.google.samples.apps.iosched.ui.widget.recyclerview.UpdatableAdapter;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A {@link RecyclerView.Adapter} for a collection of {@link SessionData}. This adapter runs in two
 * <i>modes</i>: compact and detail.
 * <p/>
 * Compact mode is created via the {@link #createHorizontal(Activity, List)} factory method & shows
 * a smaller representation of a session (title + time). It is used by {@link ExploreIOFragment}.
 * <p/>
 * The detail mode is created via the {@link #createVerticalGrid(Activity, List, int)} factory
 * method and adds headers for day/time blocks and shows a larger representation of a session (title
 * + description). It is used by {@link ExploreSessionsFragment}.
 */
public class SessionsAdapter extends UpdatableAdapter<List<SessionData>, RecyclerView.ViewHolder> {

    // Constants
    private static final String TAG = makeLogTag(SessionsAdapter.class);

    private static final int TYPE_SESSION = 0;

    private static final int TYPE_HEADER_DAY = 1;

    private static final int TYPE_HEADER_TIME = 2;

    // Immutable state
    private final Activity mHost;

    private final LayoutInflater mInflater;

    private final ColorDrawable[] mBackgroundColors;

    private final boolean mCompactMode;

    private final int mColumns;

    // State
    private List mItems;

    // Private constructor, see the more meaningful static factory methods
    private SessionsAdapter(@NonNull Activity activity,
            @NonNull final List<SessionData> sessions,
            boolean compact,
            int columns) {
        mHost = activity;
        mInflater = LayoutInflater.from(activity);
        mCompactMode = compact;
        mColumns = columns;

        // Load the background colors
        int[] colors = mHost.getResources().getIntArray(R.array.session_tile_backgrounds);
        mBackgroundColors = new ColorDrawable[colors.length];
        for (int i = 0; i < colors.length; i++) {
            mBackgroundColors[i] = new ColorDrawable(colors[i]);
        }
        mItems = processData(sessions);
    }

    public static SessionsAdapter createHorizontal(@NonNull Activity activity,
            @NonNull final List<SessionData> sessions) {
        return new SessionsAdapter(activity, sessions, true, -1);
    }

    public static SessionsAdapter createVerticalGrid(@NonNull Activity activity,
            @NonNull final List<SessionData> sessions,
            int columns) {
        return new SessionsAdapter(activity, sessions, false, columns);
    }

    @Override
    public void update(@NonNull final List<SessionData> updatedData) {
        // Attempt to update data in place i.e. only if it has changed so that we don't lose scroll
        // position etc when an item updates e.g. adding/removing an item from your schedule
        final List newItems = processData(updatedData);
        if (newItems.size() != mItems.size()) {
            mItems = newItems;
            notifyDataSetChanged();
            return;
        }
        for (int i = 0; i < newItems.size(); i++) {
            final Object oldItem = mItems.get(i);
            final Object newItem = newItems.get(i);
            if (!oldItem.equals(newItem)) {
                mItems.set(i, newItem);
                notifyItemChanged(i);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case TYPE_SESSION:
                return createSessionViewHolder(parent);
            case TYPE_HEADER_DAY:
                return createHeaderViewHolder(parent, R.layout.grid_header_minor);
            case TYPE_HEADER_TIME:
                return createHeaderViewHolder(parent, R.layout.grid_header_major);
            default:
                LOGE(TAG, "Unknown view type.");
                throw new IllegalArgumentException("Unknown view type.");
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        switch (getItemViewType(position)) {
            case TYPE_SESSION:
                bindSession((SessionViewHolder) holder,
                        (SessionData) mItems.get(position), position);
                break;
            case TYPE_HEADER_DAY:
                bindDayHeaderHolder((HeaderViewHolder) holder, (DayHeader) mItems.get(position));
                break;
            case TYPE_HEADER_TIME:
                bindTimeHeaderHolder((HeaderViewHolder) holder, (TimeHeader) mItems.get(position));
                break;
        }
    }

    @Override
    public int getItemViewType(final int position) {
        if (mCompactMode) {
            return TYPE_SESSION;
        }
        final Object item = mItems.get(position);
        if (item instanceof SessionData) {
            return TYPE_SESSION;
        } else if (item instanceof TimeHeader) {
            return TYPE_HEADER_TIME;
        } else if (item instanceof DayHeader) {
            return TYPE_HEADER_DAY;
        }
        throw new IllegalArgumentException("Unknown view type.");
    }

    public int getSpanSize(int position) {
        if (mCompactMode || getItemViewType(position) == TYPE_SESSION) {
            return 1;
        } else {
            return mColumns;
        }
    }

    /**
     * Process the given list of sessions. If we are in detail mode then insert day/time headers.
     */
    private List processData(final List<SessionData> sessions) {
        final List data = new ArrayList(sessions.size());

        // First sort sessions by start time.
        // TODO Can we upstream this requirement?
        Collections.sort(sessions, new Comparator<SessionData>() {
            @Override
            public int compare(final SessionData lhs, final SessionData rhs) {
                return lhs.getStartDate().compareTo(rhs.getStartDate());
            }
        });

        if (mCompactMode) {
            data.addAll(sessions);
        } else {
            // loop over the sessions inserting headings at each day, hour boundary
            int day = -1, time = -1;
            for (SessionData session : sessions) {
                if (session.getStartDate().get(Calendar.DAY_OF_YEAR) > day) {
                    day = session.getStartDate().get(Calendar.DAY_OF_YEAR);
                    data.add(new DayHeader(
                            TimeUtils.formatShortDate(mHost, session.getStartDate().getTime())));
                    time = -1;
                }
                if (session.getStartDate().get(Calendar.HOUR_OF_DAY) > time) {
                    time = session.getStartDate().get(Calendar.HOUR_OF_DAY);
                    data.add(new TimeHeader(
                            TimeUtils.formatShortTime(mHost, session.getStartDate().getTime())));
                }
                data.add(session);
            }
        }
        return data;
    }

    @NonNull
    private SessionViewHolder createSessionViewHolder(final ViewGroup parent) {
        final SessionViewHolder holder = mCompactMode ? new CompactSessionViewHolder(
                mInflater.inflate(R.layout.explore_io_session_list_tile, parent, false)) :
                new DetailSessionViewHolder(mInflater.inflate(
                        R.layout.explore_sessions_session_grid_tile, parent, false));
        if (mCompactMode) {
            ViewCompat.setImportantForAccessibility(holder.itemView,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
            ViewCompat.setImportantForAccessibility(holder.title,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
            ViewCompat.setImportantForAccessibility(((CompactSessionViewHolder) holder).footer,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        return holder;
    }

    private HeaderViewHolder createHeaderViewHolder(ViewGroup parent,
            @LayoutRes int headerLayoutRedId) {
        return new HeaderViewHolder(mInflater.inflate(headerLayoutRedId, parent, false));
    }

    private void bindSession(SessionViewHolder holder,
            final SessionData session,
            final int position) {
        holder.itemView.setBackgroundDrawable(
                mBackgroundColors[position % mBackgroundColors.length]);
        holder.itemView.setOnClickListener(mSessionClick);
        holder.title.setText(session.getSessionName());
        holder.inSchedule.setVisibility(session.isInSchedule() ? View.VISIBLE : View.INVISIBLE);
        if (mCompactMode) {
            bindCompactSession((CompactSessionViewHolder) holder, session);
        } else {
            bindDetailSession((DetailSessionViewHolder) holder, session);
        }
    }

    private final View.OnClickListener mSessionClick = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (!(lp instanceof RecyclerView.LayoutParams)) {
                return;
            }
            final int position = ((RecyclerView.LayoutParams) lp).getViewAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            final SessionData sessionData = (SessionData) mItems.get(position);
            final Intent intent = new Intent(mHost, SessionDetailActivity.class);
            intent.setData(ScheduleContract.Sessions.buildSessionUri(sessionData.getSessionId()));
            final Bundle options;
            if (Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22) { // Lollipop
                options = null;
            } else {
                options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(mHost, v,
                                mHost.getString(R.string.transition_session_background))
                        .toBundle();
            }
            ActivityCompat.startActivity(mHost, intent, options);
        }
    };

    private void bindCompactSession(CompactSessionViewHolder holder, final SessionData session) {
        holder.footer
                .setText(TimeUtils.formatShortDateTime(mHost, session.getStartDate().getTime()));
    }

    private void bindDetailSession(DetailSessionViewHolder holder, final SessionData session) {
        holder.description.setText(session.getDetails());
    }

    private void bindTimeHeaderHolder(final HeaderViewHolder holder, final TimeHeader timeHeader) {
        holder.header.setText(timeHeader.time);
    }

    private void bindDayHeaderHolder(final HeaderViewHolder holder, final DayHeader dayHeader) {
        holder.header.setText(dayHeader.day);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private static abstract class SessionViewHolder extends RecyclerView.ViewHolder {

        final TextView title;
        final ImageView inSchedule;

        public SessionViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            inSchedule = (ImageView) itemView.findViewById(R.id.indicator_in_schedule);
        }
    }

    private static class CompactSessionViewHolder extends SessionViewHolder {

        final TextView footer;

        public CompactSessionViewHolder(View itemView) {
            super(itemView);
            footer = (TextView) itemView.findViewById(R.id.footer);
        }
    }

    private static class DetailSessionViewHolder extends SessionViewHolder {

        final TextView description;

        public DetailSessionViewHolder(View itemView) {
            super(itemView);
            description = (TextView) itemView.findViewById(R.id.description);
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        final TextView header;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            header = (TextView) itemView;
        }
    }

    private static class DayHeader {

        private final String day;

        public DayHeader(String day) {
            this.day = day;
        }
    }

    private static class TimeHeader {

        private String time;

        public TimeHeader(String time) {
            this.time = time;
        }
    }
}
