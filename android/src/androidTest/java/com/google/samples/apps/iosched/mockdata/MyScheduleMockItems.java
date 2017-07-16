/*
 * Copyright 2016 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.mockdata;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.ArrayList;

/**
 * This has methods to create stub {@link ScheduleItem}s. To generate different mock cursors, refer
 * to {@link com.google.samples.apps.iosched.debug.OutputMockData#generateScheduleItemCode
 * (ArrayList)}.
 */

public class MyScheduleMockItems {

    public final static String SESSION_TITLE_1 = "My session title 1";

    public final static String SESSION_TITLE_2 = "My session title 2";

    public final static String SESSION_TITLE_AFTER = "Session in schedule in past";

    public final static int SESSION_TITLE_AFTER_START_OFFSET = TimeUtils.HOUR;

    public final static String SESSION_TITLE_BEFORE = "Session in schedule not yet";

    public final static String SESSION_ID = "156fes4f5se";

    public final static long SESSION_AVAILABLE_SLOT_TIME_OFFSET = 2 * TimeUtils.HOUR;

    public final static long SESSION_AVAILABLE_SLOT_TIME_DURATION = 1 * TimeUtils.HOUR;


    /**
     * Generates the schedule items for one day of the conference, including the keynote and 1
     * session at 12PM with title {@code title}. The user attends the conference and the time is
     * set to before the conference.
     *
     * @param dayId         Pass in 1 for the first day, 2 for the second etc
     * @param title         The title of the non keynote session
     * @param feedbackGiven Whether feedback has been given for the session
     * @return the schedule items
     */
    public static ArrayList<ScheduleItem> getItemsForAttendee(int dayId, boolean feedbackGiven,
            String title) {
        long timeBase = Config.CONFERENCE_START_MILLIS + (dayId - 1) * TimeUtils.DAY;
        ArrayList<ScheduleItem> newItems = new ArrayList<ScheduleItem>();
        ScheduleItem newItem1 = new ScheduleItem();
        newItem1.type = 1;
        newItem1.sessionType = 1;
        newItem1.mainTag = "FLAG_KEYNOTE";
        newItem1.startTime = timeBase;
        newItem1.endTime = timeBase + TimeUtils.HOUR;
        newItem1.sessionId = "__keynote__";
        newItem1.title = "Keynote";
        newItem1.subtitle = "Keynote Room (L3)";
        newItem1.room = "Keynote Room (L3)";
        newItem1.hasGivenFeedback = true;
        newItem1.backgroundImageUrl =
                "https://storage.googleapis.com/io2015-data.appspot.com/images/sessions/__w-200-" +
                        "400-600-800-1000__/14f5088b-d0e2-e411-b87f-00155d5066d7.jpg";
        newItem1.backgroundColor = -12627531;
        newItem1.flags = 1;
        newItems.add(newItem1);
        ScheduleItem newItem2 = new ScheduleItem();
        newItem2.type = 1;
        newItem2.sessionType = 2;
        newItem2.startTime = timeBase + SESSION_TITLE_AFTER_START_OFFSET;
        newItem2.endTime = timeBase + SESSION_TITLE_AFTER_START_OFFSET + 1 * TimeUtils.HOUR;
        newItem2.sessionId = SESSION_ID;
        newItem2.title = title;
        newItem2.subtitle = "Develop Sandbox (L2)";
        newItem2.room = "Develop Sandbox (L2)";
        newItem2.hasGivenFeedback = feedbackGiven;
        newItem2.backgroundImageUrl =
                "https://storage.googleapis.com/io2015-data.appspot.com/images/sessions/__w-200-" +
                        "400-600-800-1000__/ac8d5cc7-36e5-e411-b87f-00155d5066d7.jpg";
        newItem2.backgroundColor = -14235942;
        newItems.add(newItem2);
        return newItems;
    }

    /**
     * Generates the schedule items for one day of the conference, including the keynote, 1 session
     * at 12PM and one slow with available sessions. The user attends the conference and the time is
     * set to before the conference.
     *
     * @param dayId Pass in 1 for the first day, 2 for the second etc
     * @return the schedule items
     */
    public static ArrayList<ScheduleItem> getItemsForAttendeeBefore(int dayId) {
        long timeBase = Config.CONFERENCE_START_MILLIS + (dayId - 1) * TimeUtils.DAY;
        ArrayList<ScheduleItem> newItems = new ArrayList<ScheduleItem>();
        ScheduleItem newItem1 = new ScheduleItem();
        newItem1.type = 1;
        newItem1.sessionType = 1;
        newItem1.mainTag = "FLAG_KEYNOTE";
        newItem1.startTime = timeBase;
        newItem1.endTime = timeBase + TimeUtils.HOUR;
        newItem1.sessionId = "__keynote__";
        newItem1.title = "Keynote";
        newItem1.subtitle = "Keynote Room (L3)";
        newItem1.room = "Keynote Room (L3)";
        newItem1.hasGivenFeedback = false;
        newItem1.backgroundImageUrl =
                "https://storage.googleapis.com/io2015-data.appspot.com/images/sessions/__w-200-" +
                        "400-600-800-1000__/14f5088b-d0e2-e411-b87f-00155d5066d7.jpg";
        newItem1.backgroundColor = -12627531;
        newItem1.flags = 1;
        newItems.add(newItem1);
        ScheduleItem newItem2 = new ScheduleItem();
        newItem2.type = 1;
        newItem2.sessionType = 2;
        newItem2.startTime = timeBase + TimeUtils.HOUR;
        newItem2.endTime = timeBase + 2 * TimeUtils.HOUR;
        newItem2.sessionId = SESSION_ID;
        newItem2.title = SESSION_TITLE_BEFORE;
        newItem2.subtitle = "Develop Sandbox (L2)";
        newItem2.room = "Develop Sandbox (L2)";
        newItem2.hasGivenFeedback = false;
        newItem2.backgroundImageUrl =
                "https://storage.googleapis.com/io2015-data.appspot.com/images/sessions/__w-200-" +
                        "400-600-800-1000__/ac8d5cc7-36e5-e411-b87f-00155d5066d7.jpg";
        newItem2.backgroundColor = -14235942;
        newItems.add(newItem2);
        ScheduleItem newItem3 = new ScheduleItem();
        newItem3.type = 0;
        newItem3.sessionType = 4;
        newItem3.startTime = timeBase + SESSION_AVAILABLE_SLOT_TIME_OFFSET;
        newItem3.endTime = timeBase + SESSION_AVAILABLE_SLOT_TIME_OFFSET
                + SESSION_AVAILABLE_SLOT_TIME_DURATION;
        newItem3.numOfSessions = 23;
        newItem3.title = "";
        newItem3.subtitle = "23 available sessions";
        newItem3.room = "";
        newItem3.hasGivenFeedback = false;
        newItems.add(newItem3);
        return newItems;
    }

    /**
     * Generates the schedule items for one day of the conference, including the keynote and 1
     * session at 12PM. The user attends the conference and the time is set to before the
     * conference.
     *
     * @param dayId         Pass in 1 for the first day, 2 for the second etc
     * @param feedbackGiven Whether feedback has been given for the session
     * @return the schedule items
     */
    public static ArrayList<ScheduleItem> getItemsForAttendeeAfter(int dayId,
            boolean feedbackGiven) {
        return getItemsForAttendee(dayId, feedbackGiven, SESSION_TITLE_AFTER);
    }
}
