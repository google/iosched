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

package com.google.samples.apps.iosched.ui.debug.actions;

import android.content.Context;
import android.os.AsyncTask;

import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.ScheduleItemHelper;
import com.google.samples.apps.iosched.ui.debug.DebugAction;

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
        ArrayList<ScheduleItem> mut = new ArrayList<ScheduleItem>();
        ArrayList<ScheduleItem> immut = new ArrayList<ScheduleItem>();

        mut.add(item("14:00", "14:30", "m1"));
        immut.add(item("14:25", "14:50", "i1"));
        success &= check("no intersection - within range",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{item("14:00", "14:30", "m1"), item("14:25", "14:50", "i1")});

        mut.clear(); immut.clear();
        mut.add(item("14:00", "16:00", "m1"));
        immut.add(item("15:00", "16:00", "i1"));
        success &= check("Simple intersection1",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{item("14:00", "15:00", "m1"), item("15:00", "16:00", "i1")});

        mut.clear(); immut.clear();
        mut.add(item("14:00", "16:00", "m1"));
        immut.add(item("13:00", "15:00", "i1"));
        success &= check("Simple intersection2",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{item("13:00", "15:00", "i1"), item("15:00", "16:00", "m1")});

        mut.clear(); immut.clear();
        mut.add(item("14:00", "16:00", "m1"));
        immut.add(item("14:00", "16:00", "i1"));
        success &= check("same time",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{item("14:00", "16:00", "i1")});

        mut.clear(); immut.clear();
        mut.add(item("14:00", "16:09", "m1"));
        immut.add(item("14:05", "16:00", "i1"));
        success &= check("no split, remaining not big enough",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{item("14:05", "16:00", "i1")});

        mut.clear(); immut.clear();
        mut.add(item("14:00", "16:10", "m1"));
        immut.add(item("14:00", "16:00", "i1"));
        success &= check("split",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{item("14:00", "16:00", "i1"), item("16:00", "16:10", "m1")});

        mut.clear(); immut.clear();
        mut.add(item("14:00", "17:00", "m1"));
        immut.add(item("14:30", "15:00", "i1"));
        immut.add(item("15:30", "16:00", "i2"));
        success &= check("2 splits",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{
                        item("14:00", "14:30", "m1"),
                        item("14:30", "15:00", "i1"),
                        item("15:00", "15:30", "m1"),
                        item("15:30", "16:00", "i2"),
                        item("16:00", "17:00", "m1"),
                });

        mut.clear(); immut.clear();
        mut.add(item("14:00", "17:00", "m1"));
        immut.add(item("14:30", "15:00", "i1"));
        immut.add(item("16:30", "16:51", "i2"));
        success &= check("2 splits with no remaining",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{
                        item("14:00", "14:30", "m1"),
                        item("14:30", "15:00", "i1"),
                        item("15:00", "16:30", "m1"),
                        item("16:30", "16:51", "i2"),
                });

        mut.clear(); immut.clear();
        mut.add(item("12:00", "15:00", "m1"));
        mut.add(item("15:00", "17:00", "m2"));
        mut.add(item("17:00", "17:40", "m3"));
        immut.add(item("14:30", "15:00", "i1"));
        immut.add(item("16:30", "16:51", "i2"));
        success &= check("2 splits, 3 free blocks, no remaining",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{
                        item("12:00", "14:30", "m1"),
                        item("14:30", "15:00", "i1"),
                        item("15:00", "16:30", "m2"),
                        item("16:30", "16:51", "i2"),
                        item("17:00", "17:40", "m3"),
                });

        mut.clear(); immut.clear();
        mut.add(item("12:00", "15:00", "m1"));
        mut.add(item("15:00", "17:00", "m2"));
        mut.add(item("17:00", "17:40", "m3"));
        immut.add(item("14:30", "15:00", "i1"));
        immut.add(item("16:30", "16:51", "i2"));
        immut.add(item("16:30", "16:40", "i3"));
        success &= check("conflicting sessions, 2 splits, 3 free blocks, no remaining",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{
                        item("12:00", "14:30", "m1"),
                        item("14:30", "15:00", "i1"),
                        item("15:00", "16:30", "m2"),
                        item("16:30", "16:51", "i2"),
                        item("16:30", "16:40", "i3", true),
                        item("17:00", "17:40", "m3"),
                });


        mut.clear(); immut.clear();
        mut.add(item("12:00", "15:00", "m1"));
        mut.add(item("15:00", "17:00", "m2"));
        mut.add(item("17:00", "17:40", "m3"));
        immut.add(item("14:30", "15:00", "i1"));
        immut.add(item("16:30", "16:51", "i2"));
        immut.add(item("16:50", "17:00", "i3"));
        success &= check("borderline conflicting sessions, 2 splits, 3 free blocks, no remaining",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{
                        item("12:00", "14:30", "m1"),
                        item("14:30", "15:00", "i1"),
                        item("15:00", "16:30", "m2"),
                        item("16:30", "16:51", "i2"),
                        item("16:50", "17:00", "i3"),
                        item("17:00", "17:40", "m3"),
                });


        mut.clear(); immut.clear();
        immut.add(item("14:30", "15:00", "i1"));
        immut.add(item("16:30", "19:00", "i2"));
        immut.add(item("16:30", "17:00", "i3"));
        immut.add(item("18:00", "18:30", "i4"));
        success &= check("conflicting sessions",
                ScheduleItemHelper.processItems(mut, immut),
                new ScheduleItem[]{
                        item("14:30", "15:00", "i1"),
                        item("16:30", "19:00", "i2"),
                        item("16:30", "17:00", "i3", true),
                        item("18:00", "18:30", "i4", true),
                });

        return success;
    }

    private boolean check(String testDescription, ArrayList<ScheduleItem> actual, ScheduleItem[] expected) {
        out.append("testing " + testDescription + "...");
        boolean equal = true;
        if (actual.size() != expected.length ) {
            equal = false;
        } else {
            int i=0;
            for (ScheduleItem item: actual) {
                if (!item.title.equals(expected[i].title) ||
                        item.startTime != expected[i].startTime ||
                        item.endTime != expected[i].endTime ||
                        ( item.flags&ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) !=
                                (expected[i].flags&ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS)) {
                    equal = false;
                    break;
                }
                i++;
            }
        }
        if (!equal) {
            out.append("ERROR!:\n");
            out.append("       expected\n");
            for (ScheduleItem item: expected) {
                out.append("  " + format(item) + "\n");
            }
            out.append("       actual\n");
            for (ScheduleItem item: actual) {
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
                ((item.flags&ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS ) > 0 ? "  conflict":"");
    }

    private String timeStr(long time) {
        Date d = new Date(time);
        return d.getHours()+":"+d.getMinutes();
    }

    private long date(String hourMinute) {
        try {
            return sdf.parse("2014-06-25 " + hourMinute + ":00").getTime();
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    private ScheduleItem item(String start, String end, String id) {
        return item(start, end, id, false);
    }

    private ScheduleItem item(String start, String end, String id, int type) {
        return item(start, end, id, false, type);
    }

    private ScheduleItem item(String start, String end, String id, boolean conflict) {
        return item(start, end, id, conflict, ScheduleItem.SESSION);
    }
    private ScheduleItem item(String start, String end, String id, boolean conflict, int type) {
        ScheduleItem i = new ScheduleItem();
        i.title = id;
        i.startTime = date(start);
        i.endTime = date(end);
        i.type = type;
        if (conflict) i.flags = ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS;
        return i;
    }

}
