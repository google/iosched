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

import android.database.Cursor;
import android.database.MatrixCursor;

import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.debug.OutputMockData;

/**
 * This has methods to create stub cursors for videos. To generate different mock cursors, log the
 * output of {@link OutputMockData#generateMatrixCursorCodeForCurrentRow(Cursor)} in {@link
 * com.google.samples.apps.iosched.archframework.ModelWithLoaderManager#onLoadFinished(QueryEnum,
 * Cursor)} and copy the logged string into a method that returns a {@link MatrixCursor}.
 */
public class VideosMockCursor {

    public final static String VIDEO_TITLE_NULL_TOPIC = "VIDEO_TITLE_NULL_TOPIC";

    public final static String VIDEO_TOPIC1 = "VIDEO_TOPIC1";

    public final static String VIDEO_TOPIC2 = "VIDEO_TOPIC2";

    public final static String VIDEO_TITLE1 = "VIDEO_TITLE1";

    public final static String VIDEO_TITLE2 = "VIDEO_TITLE2";

    public final static String VIDEO_TITLE3 = "VIDEO_TITLE3";

    public final static String VIDEO_TITLE4 = "VIDEO_TITLE4";

    public final static String VIDEO_TITLE5 = "VIDEO_TITLE5";

    public final static String VIDEO_TITLE6 = "VIDEO_TITLE6";

    public final static String VIDEO_TITLE7 = "VIDEO_TITLE7";

    public final static String VIDEO_TITLE8 = "VIDEO_TITLE8";

    public final static String VIDEO_TITLE9 = "VIDEO_TITLE9";

    public final static String VIDEO_TITLE10 = "VIDEO_TITLE10";

    public final static String VIDEO_TITLE11 = "VIDEO_TITLE11";

    public final static String VIDEO_TITLE12 = "VIDEO_TITLE12";

    public final static String VIDEO_TITLE13 = "VIDEO_TITLE13";

    public final static String VIDEO_YOUTUBE_LINK = "vcSj8ln-BlE";

    public final static String VIDEO_2014 = "2014";

    public final static String VIDEO_2015 = "2015";

    public static Cursor getCursorForFilter() {
        String[] columns = {"video_year","video_topic"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        String[] data1 = {VIDEO_2014, VIDEO_TOPIC1};
        matrixCursor.addRow(data1);
        String[] data2 = {VIDEO_2014, VIDEO_TOPIC2};
        matrixCursor.addRow(data2);
        String[] data3 = {VIDEO_2015,VIDEO_TOPIC1};
        matrixCursor.addRow(data3);
        String[] data4 = {VIDEO_2015,VIDEO_TOPIC2};
        return matrixCursor;
    }


    /**
     * @return 1 video with null topic, 5 videos with {@link #VIDEO_TOPIC1} topic, and 8 videos with
     * {@link #VIDEO_TOPIC2} topic.
     */
    public static Cursor getCursorForVideos() {
        String[] columns =
                {"video_id", "video_year", "video_title", "video_desc", "video_vid", "video_topic",
                        "video_speakers", "video_thumbnail_url"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        String[] data1 = {"yC7Pf3Ad9t8", VIDEO_2014, VIDEO_TITLE_NULL_TOPIC,
                "From the applications built by the thousands of Google Cast developers to our " +
                        "evolving ambient home screen experience, users have only seen a glimpse " +
                        "of the future of the living room.  Please join us as we give you more " +
                        "insight about the making of Chromecast, the vision for multi-screen " +
                        "devices, and the future of the Google Cast ecosystem. ",
                "yC7Pf3Ad9t8", null, "Majd Bakar, John Affaki",
                "http://img.youtube.com/vi/yC7Pf3Ad9t8/hqdefault.jpg"};
        matrixCursor.addRow(data1);
        String[] data2 = {"vcSj8ln-BlE", VIDEO_2015,
                VIDEO_TITLE1,
                "Android M extends Android for Work functionality with a new set of APIs for " +
                        "Enterprise Mobility Management providers to offer new features and " +
                        "policy controls to IT Departments. Key among these APIs is better " +
                        "support for IT Admins to administer the Work Profile and an improved " +
                        "BYOD Experience for end users. We have also added an exciting new " +
                        "capability for the Android Platform to support Corporate Owned Single " +
                        "Use devices under IT Control. ",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC1, "",
                "http://img.youtube.com/vi/vcSj8ln-BlE/hqdefault.jpg"};
        matrixCursor.addRow(data2);
        String[] data3 = {"cY77sSctzec", VIDEO_2015,
                VIDEO_TITLE2,
                "Smart Lock for Passwords empowers your Android app by eliminating user's " +
                        "friction of entering passwords. In this video, learn some of the Smart " +
                        "Lock best practices from Eiji Kitamura. ",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC1, "",
                "http://img.youtube.com/vi/cY77sSctzec/hqdefault.jpg"};
        matrixCursor.addRow(data3);
        String[] data4 = {"aJNzuxhZSxQ", VIDEO_2014, VIDEO_TITLE3,
                "Cloud Services help developers succeed by letting them focus on building great " +
                        "apps and user experiences. Today, developers rely on Google Cloud " +
                        "Messaging to deliver messages to their Android users. We're releasing an" +
                        " exciting set of new features to inspire our developers to achieve even " +
                        "more, including upstream messaging, GCM for Chrome, and more.",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC1, "Francesco Nerieri",
                "http://img.youtube.com/vi/aJNzuxhZSxQ/hqdefault.jpg"};
        matrixCursor.addRow(data4);
        String[] data5 = {"9vjntxXCUNA", VIDEO_2014, VIDEO_TITLE4,
                "This is a developer-focused session that goes “under the hood” on the " +
                        "just-announced Android Auto SDK.  The session will go into depth on our " +
                        "API set, including coding examples, and also more detail on the Android " +
                        "Auto technology solution.  Learn what it takes to car-enable your audio " +
                        "or messaging app now so you're ready for the launch of Android Auto " +
                        "later this year!",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC1, "Gabriel Peal, Nick Pelly, Andy Brenner",
                "http://img.youtube.com/vi/9vjntxXCUNA/hqdefault.jpg"};
        matrixCursor.addRow(data5);
        String[] data6 = {"sha_w3_5c2c", VIDEO_2014, VIDEO_TITLE5,
                "Take a developer's tour of the Android Wear platform and Google's new wearable " +
                        "APIs. Learn about our simple and powerful tools for creating apps for " +
                        "Android Wear devices and bringing wearable experiences to your Android " +
                        "apps. We'll walk step-by-step through designing and building a small, " +
                        "contextual app for Android Wear.",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC1, "Austin Robison, Justin Koh",
                "http://img.youtube.com/vi/sha_w3_5c2c/hqdefault.jpg"};
        matrixCursor.addRow(data6);
        String[] data7 = {"K3meJyiYWFw", VIDEO_2014, VIDEO_TITLE6,
                "Pull up a chair and join the Android platform team for a fireside chat. It's " +
                        "your opportunity to ask us about the platform and learn a little bit " +
                        "more about why things work the way they do, from the people who built it" +
                        ". Moderated by Android Developer Advocate Reto Meier.",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC2,
                "Rachel Garb, Dianne Hackborn, Ficus Kirkpatrick, Miles Barr, Mike Cleron, Dave " +
                        "Burke, Jhilmil Jain, Gabe Cohen, Chet Haase, Xavier Ducrohet, Reto " +
                        "Meier, Adam Powell, Matias Duarte",
                "http://img.youtube.com/vi/K3meJyiYWFw/hqdefault.jpg"};
        matrixCursor.addRow(data7);
        String[] data8 =
                {"92fgcUNCHic", VIDEO_2015, VIDEO_TITLE7,
                        "Performance and innovative capabilities are essential in building a " +
                                "successful media app. Join the Android media team for a look at " +
                                "new and recent features and how you can use them to optimize " +
                                "performance and create great user experiences. We'll start with " +
                                "best practices for audio and video APIs and go on to look at " +
                                "what's new in the Android camera API.",
                        VIDEO_YOUTUBE_LINK, VIDEO_TOPIC2,
                        "Glenn Kasten, Andy Hung, Eddy Talvala, Lajos Molnar, Oliver Woodman, " +
                                "Rachad Alao",
                        "http://img.youtube.com/vi/92fgcUNCHic/hqdefault.jpg"};
        matrixCursor.addRow(data8);
        String[] data9 = {"BnxPwDTUKdg", VIDEO_2014, VIDEO_TITLE8,
                "Think your app or game has what it takes to become a global hit? Get key " +
                        "insights into major international markets and trends of successful apps " +
                        "and games in those regions. Leverage these pro tips and best practices " +
                        "to expand your game to a global audience.",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC2, "Koh Kim, Hirotaka Yoshitsugu",
                "http://img.youtube.com/vi/BnxPwDTUKdg/hqdefault.jpg"};
        matrixCursor.addRow(data9);
        String[] data10 = {"vcSj8ln-BlE", VIDEO_2015,
                VIDEO_TITLE9,
                "Android M extends Android for Work functionality with a new set of APIs for " +
                        "Enterprise Mobility Management providers to offer new features and " +
                        "policy controls to IT Departments. Key among these APIs is better " +
                        "support for IT Admins to administer the Work Profile and an improved " +
                        "BYOD Experience for end users. We have also added an exciting new " +
                        "capability for the Android Platform to support Corporate Owned Single " +
                        "Use devices under IT Control. ",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC2, "",
                "http://img.youtube.com/vi/vcSj8ln-BlE/hqdefault.jpg"};
        matrixCursor.addRow(data10);
        String[] data11 = {"cY77sSctzec", VIDEO_2015,
                VIDEO_TITLE10,
                "Smart Lock for Passwords empowers your Android app by eliminating user's " +
                        "friction of entering passwords. In this video, learn some of the Smart " +
                        "Lock best practices from Eiji Kitamura. ",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC2, "",
                "http://img.youtube.com/vi/cY77sSctzec/hqdefault.jpg"};
        matrixCursor.addRow(data11);
        String[] data12 = {"aJNzuxhZSxQ", VIDEO_2014, VIDEO_TITLE11,
                "Cloud Services help developers succeed by letting them focus on building great " +
                        "apps and user experiences. Today, developers rely on Google Cloud " +
                        "Messaging to deliver messages to their Android users. We're releasing an" +
                        " exciting set of new features to inspire our developers to achieve even " +
                        "more, including upstream messaging, GCM for Chrome, and more.",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC2, "Francesco Nerieri",
                "http://img.youtube.com/vi/aJNzuxhZSxQ/hqdefault.jpg"};
        matrixCursor.addRow(data12);
        String[] data13 = {"9vjntxXCUNA", VIDEO_2014, VIDEO_TITLE12,
                "This is a developer-focused session that goes “under the hood” on the " +
                        "just-announced Android Auto SDK.  The session will go into depth on our " +
                        "API set, including coding examples, and also more detail on the Android " +
                        "Auto technology solution.  Learn what it takes to car-enable your audio " +
                        "or messaging app now so you're ready for the launch of Android Auto " +
                        "later this year!",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC2, "Gabriel Peal, Nick Pelly, Andy Brenner",
                "http://img.youtube.com/vi/9vjntxXCUNA/hqdefault.jpg"};
        matrixCursor.addRow(data13);
        String[] data14 = {"sha_w3_5c2c", VIDEO_2014, VIDEO_TITLE13,
                "Take a developer's tour of the Android Wear platform and Google's new wearable " +
                        "APIs. Learn about our simple and powerful tools for creating apps for " +
                        "Android Wear devices and bringing wearable experiences to your Android " +
                        "apps. We'll walk step-by-step through designing and building a small, " +
                        "contextual app for Android Wear.",
                VIDEO_YOUTUBE_LINK, VIDEO_TOPIC2, "Austin Robison, Justin Koh",
                "http://img.youtube.com/vi/sha_w3_5c2c/hqdefault.jpg"};
        matrixCursor.addRow(data14);
        return matrixCursor;
    }
}
