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

/**
 * This has methods to create stub cursors for explore feature. To generate different mock cursors,
 * refer to {@link com.google.samples.apps.iosched.debug
 * .OutputMockData#generateMatrixCursorCodeForCurrentRow(Cursor)}.
 */
public class ExploreMockCursor {

    public static final String TITLE_KEYNOTE = "KEYNOTE";

    public static final String TOPIC_TOOLS_TITLE1 = "TOPIC_TOOLS_TITLE1";

    public static final String TOPIC_TOOLS_TITLE2 = "TOPIC_TOOLS_TITLE2";

    public static final String TOPIC_TOOLS_TITLE3 = "TOPIC_TOOLS_TITLE3";

    public static final String TOPIC_TOOLS_TITLE4 = "TOPIC_TOOLS_TITLE4";

    public static final String KEYNOTE_ID = "__keynote__";

    public static final String TOPIC_ANDROID = "TOPIC_ANDROID";

    public static final String TOPIC_CHROME = "TOPIC_CHROME";

    public static final String TOPIC_TOOLS = "TOPIC_TOOLS";

    public static final String TOPIC_EARN = "TOPIC_EARN";

    public static final String TYPE_SESSION = "TYPE_SESSION";

    public static final String THEME_DESIGN = "THEME_DESIGN";

    public static final String THEME_EARN = "THEME_EARN";

    private static final String SEP = ",";

    /**
     * @return a list of tags, including {@link #THEME_EARN}. {@link #THEME_DESIGN}, {@link
     * #TOPIC_ANDROID}, {@link #TOPIC_CHROME}, {@link #TOPIC_TOOLS} and {@link {@link #TOPIC_EARN}.
     */
    public static MatrixCursor getCursorForTags() {
        String[] columns = {"tag_id", "tag_name"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        String[] data1 = {"TYPE_CODELABS", "Code labs"};
        matrixCursor.addRow(data1);
        String[] data2 = {"TOPIC_AUDIENCEGROWTH", "Audience Growth"};
        matrixCursor.addRow(data2);
        String[] data3 = {THEME_DESIGN, THEME_DESIGN};
        matrixCursor.addRow(data3);
        String[] data4 = {TYPE_SESSION, TYPE_SESSION};
        matrixCursor.addRow(data4);
        String[] data5 = {"TYPE_SANDBOXTALKS", "Sandbox talks"};
        matrixCursor.addRow(data5);
        String[] data6 = {TOPIC_TOOLS, TOPIC_TOOLS};
        matrixCursor.addRow(data6);
        String[] data7 = {"TOPIC_AUTO", "Auto"};
        matrixCursor.addRow(data7);
        String[] data8 = {TOPIC_ANDROID, TOPIC_ANDROID};
        matrixCursor.addRow(data8);
        String[] data9 = {TOPIC_CHROME, TOPIC_CHROME};
        matrixCursor.addRow(data9);
        String[] data10 = {THEME_EARN, THEME_EARN};
        matrixCursor.addRow(data10);
        return matrixCursor;
    }

    /**
     * @return a cursor that contains 8 sessions, including a keynote session, 4 sessions with
     * {@link #THEME_DESIGN}, 4 sessions with {@link #THEME_EARN}, 4 sessions with main {@link
     * #TOPIC_TOOLS}, 2 sessions with main {@link #TOPIC_ANDROID}, and 2 sessions with main {@link
     * #TOPIC_CHROME}.
     */
    public static MatrixCursor getCursorForExplore() {
        String[] columns = {"session_id", "session_title", "session_abstract", "session_tags",
                "session_main_tag", "session_photo_url", "session_start", "session_end",
                "session_livestream_url", "session_youtube_url", "session_in_my_schedule"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        String[] data1 = {KEYNOTE_ID, TITLE_KEYNOTE,
                "Join us to learn about product and platform innovations at Google, starting with" +
                        " a live kickoff from our Senior Vice-President of Products, Sundar " +
                        "Pichai.",
                "FLAG_KEYNOTE", "FLAG_KEYNOTE",
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/14f5088b-d0e2-e411-b87f" +
                        "-00155d5066d7.jpg",
                "1432830600000", "1432837800000", "wtLJPvx7-ys", "null", "0"};
        matrixCursor.addRow(data1);
        String[] data2 = {"74718f8b-b6d4-e411-b87f-00155d5066d7", TOPIC_TOOLS_TITLE1,
                "The next big things from Google Cloud Messaging (GCM), simplifying your mobile " +
                        "messaging and notifications solution.",
                TYPE_SESSION + SEP + THEME_DESIGN + SEP + TOPIC_TOOLS, TOPIC_TOOLS,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/74718f8b-b6d4-e411-b87f" +
                        "-00155d5066d7.jpg",
                "1432843200000", "1432846800000", "Lj-plmzhyVA", "null", "0"};
        matrixCursor.addRow(data2);
        String[] data3 = {"ea96312e-e3d3-e411-b87f-00155d5066d7", "What's new in Android",
                "This session will highlight the most exciting new developer features of the " +
                        "Android platform.",
                TYPE_SESSION + SEP + THEME_DESIGN + SEP + TOPIC_ANDROID + SEP + TOPIC_TOOLS,
                TOPIC_ANDROID,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/ea96312e-e3d3-e411-b87f" +
                        "-00155d5066d7.jpg",
                "1432843200000", "1432846800000", "5WI_lCmBKE8", "null", "0"};
        matrixCursor.addRow(data3);
        String[] data4 = {"881a8930-f0e2-e411-b87f-00155d5066d7",
                TOPIC_TOOLS_TITLE2,
                "Mobile development is growing more and more complex. To make things easier for " +
                        "publishers, we have created a smart mobile ads platform that goes beyond" +
                        " the traditional ad server. Come listen to Jonathan Alferness talk about" +
                        " how AdMob brings the power, scale and innovation to app developers who " +
                        "want to monetize effectively through tools like ad mediation for higher " +
                        "fill rates, new formats like native ads, and Audience builder with " +
                        "Google Analytics for list building and targeting. In this talk, we will " +
                        "also be unveiling some new features that will help make things simpler " +
                        "for developers.",
                TYPE_SESSION + SEP + THEME_EARN + SEP + TOPIC_EARN + SEP + TOPIC_TOOLS,
                TOPIC_TOOLS,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/881a8930-f0e2-e411-b87f" +
                        "-00155d5066d7.jpg",
                "1432846800000", "1432850400000", "Lj-plmzhyVA", "null", "0"};
        matrixCursor.addRow(data4);
        String[] data5 =
                {"0c718f8b-b6d4-e411-b87f-00155d5066d7", "What’s New in Android Development Tools",
                        "In this session we will provide an in depth tour of the Android " +
                                "development tools and take a closer look at everything new - " +
                                "along with tips and tricks for getting the most out of them!",
                        TYPE_SESSION + SEP + THEME_DESIGN + SEP + TOPIC_ANDROID + SEP + TOPIC_TOOLS,
                        TOPIC_ANDROID,
                        "https://storage.googleapis.com/io2015-data.appspot" +
                                ".com/images/sessions/__w-200-400-600-800-1000__/0c718f8b-b6d4" +
                                "-e411-b87f-00155d5066d7.jpg",
                        "1432846800000", "1432850400000", "5WI_lCmBKE8", "null", "0"};
        matrixCursor.addRow(data5);
        String[] data6 = {"3a718f8b-b6d4-e411-b87f-00155d5066d7", "Google Cloud Messaging 3.0",
                "The games industry has never been more promising and full of opportunities. In " +
                        "addition to consoles, PC, and browser gaming, as well as phone and " +
                        "tablet games, there are emerging fields including virtual reality and " +
                        "mobile games in the living room. This talk covers how Google is helping " +
                        "developers across this broad range of platforms.",
                TYPE_SESSION + SEP + THEME_DESIGN + SEP + TOPIC_CHROME + SEP + TOPIC_TOOLS,
                TOPIC_CHROME,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/3a718f8b-b6d4-e411-b87f" +
                        "-00155d5066d7.jpg",
                "1432854000000", "1432857600000", "4ZV54aJhues", "null", "0"};
        matrixCursor.addRow(data6);
        String[] data7 = {"e096312e-e3d3-e411-b87f-00155d5066d7",
                "Smarter monetization with AdMob and Analytics",
                "Are we as developers taking full advantage of the new sensing capabilities (like" +
                        " GPS, Bluetooth Low Energy, accelerometers, barometers) of mobile " +
                        "devices to make user experiences delightful? These new sensors introduce" +
                        " the capability to know where a person is and what they’re doing as well" +
                        " as many other clues. Join us as we explore the opportunities, show some" +
                        " old and new tools Google has created to help, share our lessons " +
                        "building experiences like the Now “Where did I park my car?” card, and " +
                        "predict what the future may hold for context-aware computing.",
                TYPE_SESSION + SEP + THEME_EARN + SEP + TOPIC_CHROME + SEP + TOPIC_TOOLS,
                TOPIC_CHROME,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/e096312e-e3d3-e411-b87f" +
                        "-00155d5066d7.jpg",
                "1432854000000", "1432857600000", "5WI_lCmBKE8", "null", "0"};
        matrixCursor.addRow(data7);
        String[] data8 = {"21718f8b-b6d4-e411-b87f-00155d5066d7",
                TOPIC_TOOLS_TITLE3,
                "The latest version of Polymer is fast and lean. With new elements and tools, " +
                        "Polymer 1.0 is ready for production. Learn how teams at Google have " +
                        "successfully launched on google.com using Polymer and the latest " +
                        "platform APIs: Web Animations, Service Workers for offline and push " +
                        "notifications, and material design. Last but not least, we’ll show you " +
                        "how to leverage the same tools as Google to be successful using Polymer " +
                        "Starter Kit - a new toolkit for building mobile-first apps.",
                TYPE_SESSION + SEP + THEME_EARN + SEP + TOPIC_CHROME + SEP + TOPIC_TOOLS,
                TOPIC_TOOLS,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/21718f8b-b6d4-e411-b87f" +
                        "-00155d5066d7.jpg",
                "1432915200000", "1432918800000", "5WI_lCmBKE8", "null", "0"};
        matrixCursor.addRow(data8);
        String[] data9 = {"60718f8b-b6d4-e411-b87f-00155d5066d7",
                TOPIC_TOOLS_TITLE4,
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
                TYPE_SESSION + SEP + THEME_EARN + SEP + TOPIC_TOOLS,
                TOPIC_TOOLS,
                "https://storage.googleapis.com/io2015-data.appspot" +
                        ".com/images/sessions/__w-200-400-600-800-1000__/60718f8b-b6d4-e411-b87f" +
                        "-00155d5066d7.jpg",
                "1432918800000", "1432922400000", "4ZV54aJhues", "null", "0"};
        matrixCursor.addRow(data9);
        return matrixCursor;
    }

}
