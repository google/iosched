/*
 * Copyright (c) 2017 Google Inc.
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
package com.google.samples.apps.iosched.info.event;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.ui.widget.HtmlTextView;
import com.google.samples.apps.iosched.util.AnalyticsHelper;

public class EventView extends FrameLayout {

    private HtmlTextView mDescriptionView;
    private EventViewClickListener mListener;

    public interface EventViewClickListener {
        void onViewSessionsClicked(EventView view, String filterTag);
        void onViewMapClicked(EventView view, String mapUri);
    }

    public EventView(Context context) {
        this(context, null);
    }

    public EventView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EventView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.EventView,
                defStyleAttr, 0);
        final String eventTitle = arr.getString(R.styleable.EventView_eventTitle);
        final String eventDescription = arr.getString(R.styleable.EventView_eventDescription);
        final Drawable eventIconDrawable = arr.getDrawable(R.styleable.EventView_eventIcon);
        final int boxColor = arr.getColor(R.styleable.EventView_boxColor,
                ContextCompat.getColor(getContext(), R.color.sunflower_yellow));
        final String sessionFilterTag = arr.getString(R.styleable.EventView_scheduleFilterTag);
        final String sessionsText = arr.getString(R.styleable.EventView_sessionsText);
        final String mapUri = arr.getString(R.styleable.EventView_mapLinkUri);
        arr.recycle();

        View rootView = LayoutInflater.from(context)
                .inflate(R.layout.info_event_content_card_view, this, true);
        TextView titleView = (TextView) rootView.findViewById(R.id.event_title);
        mDescriptionView = (HtmlTextView) rootView.findViewById(R.id.event_content_description);
        mDescriptionView.setHtmlText(eventDescription);
        View header = rootView.findViewById(R.id.header);
        ImageView iconView = (ImageView) rootView.findViewById(R.id.event_icon);
        titleView.setText(eventTitle);
        header.setBackgroundColor(boxColor);
        iconView.setImageDrawable(eventIconDrawable);
        Button viewSessions = (Button) rootView.findViewById(R.id.event_view_sessions);
        if (sessionFilterTag != null) {
            if (sessionsText != null) {
                viewSessions.setText(sessionsText);
            }
            viewSessions.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onViewSessionsClicked(EventView.this, sessionFilterTag);
                        AnalyticsHelper.sendEvent(eventTitle, "Event Info", "view sessions");
                    }
                }
            });
        } else {
            viewSessions.setVisibility(GONE);
        }
        View viewMap = rootView.findViewById(R.id.event_view_map);
        if (mapUri != null) {
            viewMap.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onViewMapClicked(EventView.this, mapUri);
                        AnalyticsHelper.sendEvent(eventTitle, "Event Info", "view map");
                    }
                }
            });
        } else {
            viewMap.setVisibility(GONE);
        }
    }

    public void setEventDescription(CharSequence description) {
        mDescriptionView.setText(description);
    }

    public void setEventViewClickListener(EventViewClickListener listener) {
        mListener = listener;
    }
}
