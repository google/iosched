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

package com.google.samples.apps.iosched.debug.actions;

import android.content.Context;
import android.os.AsyncTask;

import com.google.samples.apps.iosched.debug.DebugAction;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.ScheduleItemHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * A DebugAction that tests a few cases of schedule conflicts.
 */
public class TestScheduleHelperAction implements DebugAction {

    StringBuilder out = new StringBuilder();

    @Override
    public void run(final Context context, final Callback callback) {
        new AsyncTask<Context, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Context... contexts) {
                return startTest();
            }

            @Override
            protected void onPostExecute(Boolean success) {
                callback.done(success, out.toString());
            }
        }.execute(context);
    }

    @Override
    public String getLabel() {
        return "Test scheduler conflict handling";
    }

    public Boolean startTest() {
        boolean success = true;
        ArrayList<ScheduleItem> items = new ArrayList<>();

        items.add(item(false, -1, "14:00", "14:30", "m1"));
        items.add(item(true, -1, "14:25", "14:50", "i1"));
        success &= check("no intersection - within range",
                ScheduleItemHelper.processItems(items),
                new ScheduleItem[]{
                        item(false, -1, "14:00", "14:30", "m1"),
                        item(true, -1, "14:25", "14:50", "i1")});

        items.clear();
        items.add(item(true, 0, "14:30", "15:00", "i1"));
        items.add(item(true, -1, "16:30", "19:00", "i2"));
        items.add(item(true, -1, "16:30", "17:00", "i3"));
        items.add(item(true, -1, "18:00", "18:30", "i4"));
        success &= check("conflicting sessions",
                ScheduleItemHelper.processItems(items),
                new ScheduleItem[]{
                        item(true, -1, "14:30", "15:00", "i1"),
                        item(true, -1, "16:30", "19:00", "i2"),
                        item(true, -1, "16:30", "17:00", "i3", true),
                        item(true, -1, "18:00", "18:30", "i4", true),
                });

        return success;
    }

    private boolean check(String testDescription, ArrayList<ScheduleItem> actual,
            ScheduleItem[] expected) {
        out.append("testing " + testDescription + "...");
        boolean equal = true;
        if (actual.size() != expected.length) {
            equal = false;
        } else {
            int i = 0;
            for (ScheduleItem item : actual) {
                if (!item.title.equals(expected[i].title) ||
                        item.startTime != expected[i].startTime ||
                        item.endTime != expected[i].endTime ||
                        item.reservationStatus != expected[i].reservationStatus ||
                        (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) !=
                                (expected[i].flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS)) {
                    equal = false;
                    break;
                }
                i++;
            }
        }
        if (!equal) {
            out.append("ERROR!:\n");
            out.append("       expected\n");
            for (ScheduleItem item : expected) {
                out.append("  " + format(item) + "\n");
            }
            out.append("       actual\n");
            for (ScheduleItem item : actual) {
                out.append("  " + format(item) + "\n");
            }
        } else {
            out.append("OK\n");
        }
        return equal;
    }

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private String format(ScheduleItem item) {
        return item.title + "  " + timeStr(item.startTime) + "-" + timeStr(item.endTime) +
                ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) > 0 ? "  conflict" : "");
    }

    private String timeStr(long time) {
        Date d = new Date(time);
        return d.getHours() + ":" + d.getMinutes();
    }

    private long date(String hourMinute) {
        try {
            return sdf.parse("2014-06-25 " + hourMinute + ":00").getTime();
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ScheduleItem item(boolean inSchedule, int inReservationStatus, String start, String end, String id) {
        return item(inSchedule, inReservationStatus, start, end, id, false);
    }

    private ScheduleItem item(boolean inSchedule, int inReservationStatus, String start, String end, String id, int type) {
        return item(inSchedule, inReservationStatus, start, end, id, false, type);
    }

    private ScheduleItem item(boolean inSchedule, int inReservationStatus, String start, String end, String id,
            boolean conflict) {
        return item(inSchedule, inReservationStatus, start, end, id, conflict, ScheduleItem.SESSION);
    }

    private ScheduleItem item(boolean inSchedule, int inReservationStatus, String start, String end, String id,
            boolean conflict, int type) {
        ScheduleItem i = new ScheduleItem();
        i.title = id;
        i.startTime = date(start);
        i.endTime = date(end);
        i.type = type;
        i.inSchedule = inSchedule;
        i.reservationStatus = inReservationStatus;
        if (conflict) {
            i.flags = ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS;
        }
        return i;
    }

}
