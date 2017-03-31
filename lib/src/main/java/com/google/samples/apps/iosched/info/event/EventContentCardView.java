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
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;

public class EventContentCardView extends CardView {
    private TextView mEventTitle;
    private TextView mEventDescription;
    private LinearLayout mColorBox;
    private ImageView mEventIcon;
    private TextView mSessionsLink;
    private TextView mMapLink;

    public EventContentCardView(Context context) {
        this(context, null);
    }

    public EventContentCardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EventContentCardView(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.EventContent, 0, 0);
        String eventTitle = arr.getString(R.styleable.EventContent_eventTitle);
        String eventDescription = arr.getString(R.styleable.EventContent_eventDescription);
        Drawable eventIconDrawable = arr.getDrawable(R.styleable.EventContent_eventIcon);
        int boxColor = arr.getColor(R.styleable.EventContent_boxColor, getResources()
                .getColor(R.color.io16_light_grey));
        arr.recycle();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.info_event_content_card_view, this, true);
        mEventTitle = (TextView) view.findViewById(R.id.event_title);
        mEventDescription = (TextView) view.findViewById(R.id.event_content_description);
        mColorBox = (LinearLayout) view.findViewById(R.id.color_box);
        mEventIcon = (ImageView) view.findViewById(R.id.event_icon);
        mSessionsLink = (TextView) view.findViewById(R.id.event_view_sessions);
        mMapLink = (TextView) view.findViewById(R.id.event_view_map);
        mEventTitle.setText(eventTitle);
        mEventDescription.setText(eventDescription);
        mColorBox.setBackgroundColor(boxColor);
        mEventIcon.setImageDrawable(eventIconDrawable);
        mSessionsLink.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO link to sessions
            }
        });
        mMapLink.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO link to map
            }
        });
    }
}
