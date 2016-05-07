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

package com.google.samples.apps.iosched.explore.data;

import android.database.Cursor;
import android.support.annotation.Nullable;

import com.google.samples.apps.iosched.provider.ScheduleContract;

public class EventCard {
    @Override
    public String toString() {
        return "EventCard{" +
                "mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mActionUrl='" + mActionUrl + '\'' +
                ", mActionString='" + mActionString + '\'' +
                ", mMessage='" + mDescription + '\'' +
                '}';
    }

    private EventCard(final String title, final String actionString, final String actionUrl,
            final String description, final String actionType) {
        mTitle = title;
        mActionString = actionString;
        mActionUrl = actionUrl;
        mDescription = description;
        mActionType = actionType;
    }

    @Nullable
    public static EventCard fromCursorRow(Cursor cursor) {
        // TODO: Validate parameters and return null.
        EventCard card = new EventCard(
                cursor.getString(cursor.getColumnIndex(ScheduleContract.Cards.TITLE)),
                cursor.getString(cursor.getColumnIndex(ScheduleContract.Cards.ACTION_TEXT)),
                cursor.getString(cursor.getColumnIndex(ScheduleContract.Cards.ACTION_URL)),
                cursor.getString(cursor.getColumnIndex(ScheduleContract.Cards.MESSAGE)),
                cursor.getString(cursor.getColumnIndex(ScheduleContract.Cards.ACTION_TYPE)));

        return card;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getActionUrl() {
        return mActionUrl;
    }

    public String getActionString() {
        return mActionString;
    }

    public String getActionType() { return mActionType; }

    private String mTitle;
    private String mDescription;
    private String mActionUrl;
    private String mActionString;
    private String mActionType;
}
