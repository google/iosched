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

package com.google.samples.apps.iosched.mockdata;

import android.database.MatrixCursor;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.util.TimeUtils;

/**
 * This has methods to create stub cursors for explore feature. To generate different mock cursors,
 * refer to {@link com.google.samples.apps.iosched.debug
 * .OutputMockData#generateMatrixCursorCodeForCurrentRow(Cursor)}.
 */
public class ExploreMockCursor {

    public static final String TITLE_KEYNOTE = "KEYNOTE";

    public static final String TRACK_ANDROID_TITLE1 = "TRACK_ANDROID_TITLE1";

    public static final String TRACK_ANDROID_TITLE2 = "TRACK_ANDROID_TITLE2";

    public static final String TRACK_TOOLS_TITLE1 = "TRACK_TOOLS_TITLE1";

    public static final String TRACK_TOOLS_TITLE2 = "TRACK_TOOLS_TITLE2";

    public static final String TRACK_TOOLS_TITLE3 = "TRACK_TOOLS_TITLE3";

    public static final String TRACK_TOOLS_TITLE4 = "TRACK_TOOLS_TITLE4";

    public static final String TRACK_MOBILEWEB_TITLE1 = "TRACK_MOBILEWEB_TITLE1";

    public static final String TRACK_MOBILEWEB_TITLE2 = "TRACK_MOBILEWEB_TITLE2";

    public static final String KEYNOTE_ID = "__keynote__";

    public static final String TRACK_ANDROID = "TRACK_ANDROID";

    public static final String TRACK_ANDROID_NAME = "ANDROID_NAME";

    public static final String TRACK_MOBILEWEB = "TRACK_MOBILEWEB";

    public static final String TRACK_MOBILEWEB_NAME = "MOBILEWEB_NAME";

    public static final String TRACK_CLOUD = "TRACK_CLOUD";

    public static final String TRACK_CLOUD_NAME = "CLOUD_NAME";

    public static final String TRACK_SEARCH = "TRACK_SEARCH";

    public static final String TYPE_SESSION = "TYPE_SESSION";

    public static final String TYPE_SESSION_NAME = "SESSION_NAME";

    public static final String THEME_WHATSNEXT = "THEME_WHATSNEXT";

    public static final String THEME_WHATSNEXT_NAME = "WHATSNEXT_NAME";

    public static final String THEME_GROW_EARN = "THEME_GROW&EARN";

    public static final String THEME_GROW_EARN_NAME = "GROW&EARN";

    public static final String TAG_CATEGORY = "TYPE";

    private static final String SEP = ",";

    private static final String LIVESTREAM_URL = "wtLJPvx7-ys";

    /**
     * @return a list of tags, including {@link #THEME_GROW_EARN}. {@link #THEME_WHATSNEXT}, {@link
     * #TRACK_ANDROID}, {@link #TRACK_MOBILEWEB}, {@link #TRACK_CLOUD} and {@link #TYPE_SESSION}.
     */
    public static MatrixCursor getCursorForTags() {
        String[] columns = {"_id", "tag_id", "tag_name", "tag_category", "tag_order_in_category",
                "tag_abstract", "tag_color", "tag_photo_url"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        String[] data1 =
                {"TYPE_CODELABS", "TYPE_CODELABS", "Code labs", TAG_CATEGORY, "2", "", "-3355444",
                        ""};
        matrixCursor.addRow(data1);
        String[] data2 =
                {"TRACK_AUDIENCEGROWTH", "TRACK_AUDIENCEGROWTH", "Audience Growth", TAG_CATEGORY,
                        "2", "", "-3355444", ""};
        matrixCursor.addRow(data2);
        String[] data3 =
                {THEME_WHATSNEXT, THEME_WHATSNEXT, THEME_WHATSNEXT_NAME, TAG_CATEGORY, "2", "",
                        "-3355444", ""};
        matrixCursor.addRow(data3);
        String[] data4 =
                {TYPE_SESSION, TYPE_SESSION, TYPE_SESSION_NAME, TAG_CATEGORY, "2", "", "-3355444",
                        ""};
        matrixCursor.addRow(data4);
        String[] data5 =
                {"TYPE_SANDBOXTALKS", "TYPE_SANDBOXTALKS", "Sandbox talks", TAG_CATEGORY, "2", "",
                        "-3355444", ""};
        matrixCursor.addRow(data5);
        String[] data6 =
                {TRACK_CLOUD, TRACK_CLOUD, TRACK_CLOUD_NAME, TAG_CATEGORY, "2", "", "-3355444", ""};
        matrixCursor.addRow(data6);
        String[] data7 =
                {"TRACK_AUTO", "TRACK_AUTO", "Auto", TAG_CATEGORY, "2", "", "-3355444", ""};
        matrixCursor.addRow(data7);
        String[] data8 =
                {TRACK_ANDROID, TRACK_ANDROID, TRACK_ANDROID_NAME, TAG_CATEGORY, "2", "",
                        "-3355444", ""};
        matrixCursor.addRow(data8);
        String[] data9 =
                {TRACK_ANDROID, TRACK_ANDROID, TRACK_MOBILEWEB_NAME, TAG_CATEGORY, "2", "",
                        "-3355444", ""};
        matrixCursor.addRow(data9);
        String[] data10 =
                {THEME_GROW_EARN, THEME_GROW_EARN, THEME_GROW_EARN_NAME, TAG_CATEGORY, "2", "",
                        "-3355444", ""};
        matrixCursor.addRow(data10);
        return matrixCursor;
    }

    /**
     * @return a cursor that contains 9 sessions, including a keynote session, 4 sessions with
     * {@link #THEME_WHATSNEXT}, 4 sessions with {@link #THEME_GROW_EARN}, 4 sessions with main
     * {@link #TRACK_CLOUD}, 2 sessions with main {@link #TRACK_ANDROID}, and 2 sessions with main
     * {@link #TRACK_MOBILEWEB}. 4 of those 9 sessions are livestreamed (@code now), and their
     * titles are {@link #TRACK_TOOLS_TITLE1}, {@link #TRACK_ANDROID_TITLE1}, {@link
     * #TRACK_ANDROID_TITLE2}, and {@link #TRACK_MOBILEWEB_TITLE1}.
     */
    public static MatrixCursor getCursorForLivestreamSessions(long now) {
        return getCursor(new boolean[]{false, true, true, false, true, true, false, false, false},
                now);
    }

    /**
     * @return a cursor that contains 9 sessions, including a keynote session, 4 sessions with
     * {@link #THEME_WHATSNEXT}, 4 sessions with {@link #THEME_GROW_EARN}, 4 sessions with main
     * {@link #TRACK_CLOUD}, 2 sessions with main {@link #TRACK_ANDROID}, and 2 sessions with main
     * {@link #TRACK_MOBILEWEB}. None of those sessions are livestreamed.
     */
    public static MatrixCursor getCursorForExplore() {
        return getCursor(null, 0L);
    }

    /**
     * @param livestreamed Pass in an array of length 9  (the number of sessions), and true if the
     *                     given session should be livestreamed {@code now}. Pass in null if none of
     *                     the session should be livestreamed.
     * @param now          When the sessions should be made to be livestream
     * @return a cursor that contains 9 sessions, including a keynote session, 4 sessions with
     * {@link #THEME_WHATSNEXT}, 4 sessions with {@link #THEME_GROW_EARN}, 4 sessions with main
     * {@link #TRACK_CLOUD}, 2 sessions with main {@link #TRACK_ANDROID}, and 2 sessions with main
     * {@link #TRACK_MOBILEWEB}.
     */
    private static MatrixCursor getCursor(boolean[] livestreamed, long now) {
        String[] columns = {"session_id", "session_title", "session_abstract", "session_tags",
                "session_main_tag", "session_photo_url", "session_start", "session_end",
                "session_livestream_url", "session_youtube_url", "session_in_my_schedule"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        String[] subData = constructStartEndAndLivestream(livestreamed, now, 0);
        String[] data1 = {KEYNOTE_ID, TITLE_KEYNOTE,
                "Join us to learn about product and platform innovations at Google, starting with" +
                        " a live kickoff from our Senior Vice-President of Products, Sundar " +
                        "Pichai.",
                "FLAG_KEYNOTE", "FLAG_KEYNOTE",
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/14f5088b-d0e2-e411-b87f" +
                        "-00155d5066d7.jpg",
                subData[0], subData[1], subData[2], "null", "0"};
        matrixCursor.addRow(data1);
        subData = constructStartEndAndLivestream(livestreamed, now, 1);
        String[] data2 = {"74718f8b-b6d4-e411-b87f-00155d5066d7", TRACK_TOOLS_TITLE1,
                "The next big things from Google Cloud Messaging (GCM), simplifying your mobile " +
                        "messaging and notifications solution.",
                TYPE_SESSION + SEP + THEME_WHATSNEXT + SEP + TRACK_CLOUD, TRACK_CLOUD,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/74718f8b-b6d4-e411-b87f" +
                        "-00155d5066d7.jpg",
                subData[0], subData[1], subData[2], "null", "0"};
        matrixCursor.addRow(data2);
        subData = constructStartEndAndLivestream(livestreamed, now, 2);
        String[] data3 = {"ea96312e-e3d3-e411-b87f-00155d5066d7", TRACK_ANDROID_TITLE1,
                "This session will highlight the most exciting new developer features of the " +
                        "Android platform.",
                TYPE_SESSION + SEP + THEME_WHATSNEXT + SEP + TRACK_ANDROID + SEP + TRACK_CLOUD,
                TRACK_ANDROID,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/ea96312e-e3d3-e411-b87f" +
                        "-00155d5066d7.jpg",
                subData[0], subData[1], subData[2], "null", "0"};
        matrixCursor.addRow(data3);
        subData = constructStartEndAndLivestream(livestreamed, now, 3);
        String[] data4 = {"881a8930-f0e2-e411-b87f-00155d5066d7",
                TRACK_TOOLS_TITLE2,
                "Mobile development is growing more and more complex. To make things easier for " +
                        "publishers, we have created a smart mobile ads platform that goes beyond" +
                        " the traditional ad server. Come listen to Jonathan Alferness talk about" +
                        " how AdMob brings the power, scale and innovation to app developers who " +
                        "want to monetize effectively through tools like ad mediation for higher " +
                        "fill rates, new formats like native ads, and Audience builder with " +
                        "Google Analytics for list building and targeting. In this talk, we will " +
                        "also be unveiling some new features that will help make things simpler " +
                        "for developers.",
                TYPE_SESSION + SEP + THEME_GROW_EARN + SEP + TRACK_SEARCH + SEP + TRACK_CLOUD,
                TRACK_CLOUD,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/881a8930-f0e2-e411-b87f" +
                        "-00155d5066d7.jpg",
                subData[0], subData[1], subData[2], "null", "0"};
        matrixCursor.addRow(data4);
        subData = constructStartEndAndLivestream(livestreamed, now, 4);
        String[] data5 =
                {"0c718f8b-b6d4-e411-b87f-00155d5066d7", TRACK_ANDROID_TITLE2,
                        "In this session we will provide an in depth tour of the Android " +
                                "development tools and take a closer look at everything new - " +
                                "along with tips and tricks for getting the most out of them!",
                        TYPE_SESSION + SEP + THEME_WHATSNEXT + SEP + TRACK_ANDROID + SEP +
                                TRACK_CLOUD,
                        TRACK_ANDROID,
                        "https://storage.googleapis.com/io2015-data.appspot" +
                                ".com/images/sessions/__w-200-400-600-800-1000__/0c718f8b-b6d4" +
                                "-e411-b87f-00155d5066d7.jpg",
                        subData[0], subData[1], subData[2], "null", "0"};
        matrixCursor.addRow(data5);
        subData = constructStartEndAndLivestream(livestreamed, now, 5);
        String[] data6 = {"3a718f8b-b6d4-e411-b87f-00155d5066d7", TRACK_MOBILEWEB_TITLE1,
                "The games industry has never been more promising and full of opportunities. In " +
                        "addition to consoles, PC, and browser gaming, as well as phone and " +
                        "tablet games, there are emerging fields including virtual reality and " +
                        "mobile games in the living room. This talk covers how Google is helping " +
                        "developers across this broad range of platforms.",
                TYPE_SESSION + SEP + THEME_WHATSNEXT + SEP + TRACK_MOBILEWEB + SEP + TRACK_CLOUD,
                TRACK_MOBILEWEB,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/3a718f8b-b6d4-e411-b87f" +
                        "-00155d5066d7.jpg",
                subData[0], subData[1], subData[2], "null", "0"};
        matrixCursor.addRow(data6);
        subData = constructStartEndAndLivestream(livestreamed, now, 6);
        String[] data7 = {"e096312e-e3d3-e411-b87f-00155d5066d7",
                TRACK_MOBILEWEB_TITLE2,
                "Are we as developers taking full advantage of the new sensing capabilities (like" +
                        " GPS, Bluetooth Low Energy, accelerometers, barometers) of mobile " +
                        "devices to make user experiences delightful? These new sensors introduce" +
                        " the capability to know where a person is and what they’re doing as well" +
                        " as many other clues. Join us as we explore the opportunities, show some" +
                        " old and new tools Google has created to help, share our lessons " +
                        "building experiences like the Now “Where did I park my car?” card, and " +
                        "predict what the future may hold for context-aware computing.",
                TYPE_SESSION + SEP + THEME_GROW_EARN + SEP + TRACK_MOBILEWEB + SEP + TRACK_CLOUD,
                TRACK_MOBILEWEB,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/e096312e-e3d3-e411-b87f" +
                        "-00155d5066d7.jpg",
                subData[0], subData[1], subData[2], "null", "0"};
        matrixCursor.addRow(data7);
        subData = constructStartEndAndLivestream(livestreamed, now, 7);
        String[] data8 = {"21718f8b-b6d4-e411-b87f-00155d5066d7",
                TRACK_TOOLS_TITLE3,
                "The latest version of Polymer is fast and lean. With new elements and tools, " +
                        "Polymer 1.0 is ready for production. Learn how teams at Google have " +
                        "successfully launched on google.com using Polymer and the latest " +
                        "platform APIs: Web Animations, Service Workers for offline and push " +
                        "notifications, and material design. Last but not least, we’ll show you " +
                        "how to leverage the same tools as Google to be successful using Polymer " +
                        "Starter Kit - a new toolkit for building mobile-first apps.",
                TYPE_SESSION + SEP + THEME_GROW_EARN + SEP + TRACK_MOBILEWEB + SEP + TRACK_CLOUD,
                TRACK_CLOUD,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/21718f8b-b6d4-e411-b87f" +
                        "-00155d5066d7.jpg",
                subData[0], subData[1], subData[2], "null", "0"};
        matrixCursor.addRow(data8);
        subData = constructStartEndAndLivestream(livestreamed, now, 8);
        String[] data9 = {"60718f8b-b6d4-e411-b87f-00155d5066d7",
                TRACK_TOOLS_TITLE4,
                "How does an engineer, developer or computer scientist harness his or her skills " +
                        "to address humanity’s biggest challenges? Listen to technologists -- " +
                        "from Charity:water, Code for America, NexLeaf and HandUp -- who are " +
                        "leading innovative projects at forward-thinking nonprofit organizations " +
                        "supported by Google.org. They’ll share the challenges, successes, and " +
                        "rewards of applying technology to causes like clean water, global " +
                        "health, education, or increasing access for the more than three billions" +
                        " of people around the world facing some form of disability. You might be" +
                        " inspired to harness your own skills and talents toward creating a " +
                        "better world faster.",
                TYPE_SESSION + SEP + THEME_GROW_EARN + SEP + TRACK_CLOUD,
                TRACK_CLOUD,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/60718f8b-b6d4-e411-b87f" +
                        "-00155d5066d7.jpg",
                subData[0], subData[1], subData[2], "null", "0"};
        matrixCursor.addRow(data9);
        return matrixCursor;
    }

    /**
     * Builds session details based on whether the session at {@code index} should be {@code
     * livestreamened} {@code now}.
     *
     * @return A String array with 3 elements. The first is the started date, the second is the end
     * date, and the third is the livestream url.
     */
    private static String[] constructStartEndAndLivestream(boolean[] livestreamed, long now,
            int index) {
        String livestreamUrl = livestreamed != null && livestreamed[index] ? LIVESTREAM_URL : null;
        long start = livestreamUrl != null ? now :
                Config.CONFERENCE_START_MILLIS + index * now + TimeUtils.HOUR;
        long end = start + TimeUtils.HOUR;
        return new String[]{"" + start, "" + end, livestreamUrl};
    }

}
