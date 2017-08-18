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

package com.google.samples.apps.iosched.model;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.samples.apps.iosched.Config.Tags;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.samples.apps.iosched.model.ScheduleItem.detectSessionType;

public class ScheduleItemHelper {

    public static final long ALLOWED_OVERLAP = 5 * 60 * 1000; // 5 minutes

    /**
     * Minimum column projection for querying the content provider if planning to convert a sessions
     * cursor into ScheduleItems.
     */
    public static final String[] REQUIRED_SESSION_COLUMNS = {
            Sessions.SESSION_ID,
            Sessions.SESSION_TITLE,
            Sessions.SESSION_START,
            Sessions.SESSION_END,
            Sessions.ROOM_NAME,
            Sessions.SESSION_IN_MY_SCHEDULE,
            Sessions.SESSION_RESERVATION_STATUS,
            Sessions.SESSION_LIVESTREAM_ID,
            Sessions.SESSION_SPEAKER_NAMES,
            Sessions.SESSION_PHOTO_URL,
            Sessions.SESSION_COLOR,
            Sessions.SESSION_TAGS,
            Sessions.SESSION_MAIN_TAG,
    };

    /**
     * Find and resolve time slot conflicts. Items should already be ordered by start time.
     * Conflicts among mutableItems, if any, won't be checked, and they will be left as is.
     **/
    public static ArrayList<ScheduleItem> processItems(
            @NonNull final ArrayList<ScheduleItem> items) {
        // mark conflicting immutable:
        markConflicting(items);

        final ArrayList<ScheduleItem> result = new ArrayList<>(items);
        Collections.sort(result, new Comparator<ScheduleItem>() {
            @Override
            public int compare(ScheduleItem lhs, ScheduleItem rhs) {
                return lhs.startTime < rhs.startTime ? -1 : 1;
            }
        });

        return result;
    }

    static void markConflicting(@NonNull final ArrayList<ScheduleItem> items) {
        final int size = items.size();
        for (int i = 0; i < size; i++) {
            final ScheduleItem item = items.get(i);
            // Notice that we only care about sessions when checking conflicts.
            if (item.type == ScheduleItem.SESSION && item.inSchedule) {
                for (int j = i + 1; j < size; j++) {
                    final ScheduleItem other = items.get(j);
                    if (other.type == ScheduleItem.SESSION && other.inSchedule) {
                        if (intersect(other, item, true)) {
                            other.flags |= ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS;
                            item.flags |= ScheduleItem.FLAG_CONFLICTS_WITH_NEXT;
                        } else {
                            // we assume the list is ordered by starttime so break from inner loop
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean intersect(ScheduleItem block1, ScheduleItem block2, boolean useOverlap) {
        return block2.endTime > (block1.startTime + (useOverlap ? ALLOWED_OVERLAP : 0))
                && (block2.startTime + (useOverlap ? ALLOWED_OVERLAP : 0)) < block1.endTime;
    }

    public static boolean sameStartTime(ScheduleItem block1, ScheduleItem block2,
            boolean useOverlap) {
        return  Math.abs(block1.startTime - block2.startTime) <= (useOverlap ? ALLOWED_OVERLAP : 0);
    }

    public static List<ScheduleItem> cursorToItems(Cursor cursor, Context context) {
        List<ScheduleItem> list = new ArrayList<>(cursor.getCount());
        cursorToItems(cursor, context, list);
        return list;
    }

    @SuppressWarnings("WrongConstant")
    public static void cursorToItems(Cursor cursor, Context context, List<ScheduleItem> list) {
        while (cursor.moveToNext()) {
            ScheduleItem item = new ScheduleItem();
            item.type = ScheduleItem.SESSION;
            item.sessionId = cursor.getString(cursor.getColumnIndex(Sessions.SESSION_ID));
            item.title = cursor.getString(cursor.getColumnIndex(Sessions.SESSION_TITLE));
            item.startTime = cursor.getLong(cursor.getColumnIndex(Sessions.SESSION_START));
            item.endTime = cursor.getLong(cursor.getColumnIndex(Sessions.SESSION_END));
            if (!TextUtils.isEmpty(
                    cursor.getString(cursor.getColumnIndex(Sessions.SESSION_LIVESTREAM_ID)))) {
                item.flags |= ScheduleItem.FLAG_HAS_LIVESTREAM;
            }
            item.subtitle = UIUtils.formatSessionSubtitle(
                    cursor.getString(cursor.getColumnIndex(Sessions.ROOM_NAME)),
                    cursor.getString(cursor.getColumnIndex(Sessions.SESSION_SPEAKER_NAMES)),
                    context);
            item.room = cursor.getString(cursor.getColumnIndex(Sessions.ROOM_NAME));
            item.backgroundImageUrl = cursor.getString(
                    cursor.getColumnIndex(Sessions.SESSION_PHOTO_URL));
            item.backgroundColor = cursor.getInt(cursor.getColumnIndex(Sessions.SESSION_COLOR));
            item.sessionType = detectSessionType(
                    cursor.getString(cursor.getColumnIndex(Sessions.SESSION_TAGS)));
            item.mainTag = cursor.getString(cursor.getColumnIndex(Sessions.SESSION_MAIN_TAG));
            String tags = cursor.getString(cursor.getColumnIndex(Sessions.SESSION_TAGS));
            if (tags != null) {
                item.tags = tags.split(",");
                item.isKeynote = tags.contains(Tags.SPECIAL_KEYNOTE);
            }
            item.inSchedule =
                    cursor.getInt(cursor.getColumnIndex(Sessions.SESSION_IN_MY_SCHEDULE)) != 0;
            item.reservationStatus =
                    cursor.getInt(cursor.getColumnIndex(Sessions.SESSION_RESERVATION_STATUS));
            list.add(item);
        }
    }
}
