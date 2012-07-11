/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.iosched.ui.widget;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.util.UIUtils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.text.format.DateUtils;
import android.widget.Button;

import java.util.TimeZone;

/**
 * Custom view that represents a {@link Blocks#BLOCK_ID} instance, including its
 * title and time span that it occupies. Usually organized automatically by
 * {@link BlocksLayout} to match up against a {@link TimeRulerView} instance.
 */
public class BlockView extends Button {
    private static final int TIME_STRING_FLAGS = DateUtils.FORMAT_SHOW_DATE
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY |
            DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME;

    private final String mBlockId;
    private final String mTitle;
    private final long mStartTime;
    private final long mEndTime;
    private final boolean mContainsStarred;
    private final int mColumn;

    public BlockView(Context context, String blockId, String title, long startTime,
            long endTime, boolean containsStarred, int column) {
        super(context);

        mBlockId = blockId;
        mTitle = title;
        mStartTime = startTime;
        mEndTime = endTime;
        mContainsStarred = containsStarred;
        mColumn = column;

        setText(mTitle);

        // TODO: turn into color state list with layers?
        int textColor = Color.WHITE;
        int accentColor = -1;
        switch (mColumn) {
            case 0:
                accentColor = getResources().getColor(R.color.block_column_1);
                break;
            case 1:
                accentColor = getResources().getColor(R.color.block_column_2);
                break;
            case 2:
                accentColor = getResources().getColor(R.color.block_column_3);
                break;
        }

        LayerDrawable buttonDrawable = (LayerDrawable)
                context.getResources().getDrawable(R.drawable.btn_block);
        buttonDrawable.getDrawable(0).setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
        buttonDrawable.getDrawable(1).setAlpha(mContainsStarred ? 255 : 0);

        setTextColor(textColor);
        setBackgroundDrawable(buttonDrawable);
    }

    public String getBlockId() {
        return mBlockId;
    }

    public String getBlockTimeString() {
        TimeZone.setDefault(UIUtils.CONFERENCE_TIME_ZONE);
        return DateUtils.formatDateTime(getContext(), mStartTime, TIME_STRING_FLAGS);
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public int getColumn() {
        return mColumn;
    }
}
