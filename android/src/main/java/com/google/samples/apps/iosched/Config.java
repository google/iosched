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

package com.google.samples.apps.iosched;

import android.net.Uri;

import com.google.samples.apps.iosched.util.ParserUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class Config {
    // General configuration

    // Is this an internal dogfood build?
    public static final boolean IS_DOGFOOD_BUILD = false;

    // Warning messages for dogfood build
    public static final String DOGFOOD_BUILD_WARNING_TITLE = "Test build";
    public static final String DOGFOOD_BUILD_WARNING_TEXT = "This is a test build.";

    // Public data manifest URL
    public static final String PROD_CONFERENCE_DATA_MANIFEST_URL = "";

    // Manifest URL override for Debug (staging) builds:
    public static final String MANIFEST_URL = PROD_CONFERENCE_DATA_MANIFEST_URL;

    public static final String BOOTSTRAP_DATA_TIMESTAMP = "Thu, 10 Apr 2014 00:01:03 GMT";

    // Conference hashtag
    public static final String CONFERENCE_HASHTAG = "#io14";

    // Patterns that, when absent from a hashtag, will trigger the addition of the
    // CONFERENCE_HASHTAG on sharing snippets. Ex: "#Android" will be shared as "#io14 #Android",
    // but "#iohunt" won't be modified.
    public static final String CONFERENCE_HASHTAG_PREFIX = "#io";

    // Hard-coded conference dates. This is hardcoded here instead of extracted from the conference
    // data to avoid the Schedule UI breaking if some session is incorrectly set to a wrong date.
    public static final int CONFERENCE_YEAR = 2014;

    public static final long[][] CONFERENCE_DAYS = new long[][] {
            // start and end of day 1
            { ParserUtils.parseTime("2014-06-25T07:00:00.000Z"),
              ParserUtils.parseTime("2014-06-26T06:59:59.999Z") },
            // start and end of day 2
            { ParserUtils.parseTime("2014-06-26T07:00:00.000Z"),
              ParserUtils.parseTime("2014-06-27T06:59:59.999Z") },
        };

    public static final TimeZone CONFERENCE_TIMEZONE = TimeZone.getTimeZone("America/Los_Angeles");

    public static final long CONFERENCE_START_MILLIS = CONFERENCE_DAYS[0][0];
    public static final long CONFERENCE_END_MILLIS = CONFERENCE_DAYS[CONFERENCE_DAYS.length-1][1];

    // shorthand for some units of time
    public static final long SECOND_MILLIS = 1000;
    public static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;
    public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    public static final long DAY_MILLIS = 24 * HOUR_MILLIS;

    // OAuth 2.0 related config
    public static final String APP_NAME = "GoogleIO-Android";
    public static final String API_KEY = "";

    // Announcements
    public static final String ANNOUNCEMENTS_PLUS_ID = "";

    // YouTube API config
    public static final String YOUTUBE_API_KEY = "";

    // YouTube share URL
    public static final String YOUTUBE_SHARE_URL_PREFIX = "http://youtu.be/";

    // Live stream captions config
    public static final String LIVESTREAM_CAPTIONS_DARK_THEME_URL_PARAM = "&theme=dark";

    // Conference public WiFi AP parameters
    public static final String WIFI_SSID = "IO2014";
    public static final String WIFI_PASSPHRASE = "letsdothis";

    // GCM config
    public static final String GCM_SERVER_PROD_URL = "";
    public static final String GCM_SERVER_URL = "";

    // the GCM sender ID is the ID of the app in Google Cloud Console
    public static final String GCM_SENDER_ID = "";

    // The registration api KEY in the gcm server (configured in the GCM
    // server's AuthHelper.java file)
    public static final String GCM_API_KEY = "";

    // When do we start to offer to set up the user's wifi?
    public static final long WIFI_SETUP_OFFER_START =
            CONFERENCE_START_MILLIS - 3 * DAY_MILLIS; // 3 days before conference

    // Format of the youtube link to a Video Library video
    public static final String VIDEO_LIBRARY_URL_FMT = "https://www.youtube.com/watch?v=%s";

    // Fallback URL to get a youtube video thumbnail in case one is not provided in the data
    // (normally it should, but this is a safety fallback if it doesn't)
    public static final String VIDEO_LIBRARY_FALLBACK_THUMB_URL_FMT =
            "http://img.youtube.com/vi/%s/default.jpg";

    // Link to Google I/O Extended events presented in Explore screen
    public static final String IO_EXTENDED_LINK = "http://www.google.com/events/io/io-extended";

    // 2014-07-25: Time of expiration for experts directory data.
    // Represented as elapsed milliseconds since the epoch.
    public static final long EXPERTS_DIRECTORY_EXPIRATION = 1406214000000L;

    /**
     * Check if the experts directory data expired.
     *
     * @return True if the experts directory data expired and should be removed.
     */
    public static boolean hasExpertsDirectoryExpired() {
        return EXPERTS_DIRECTORY_EXPIRATION < System.currentTimeMillis();
    }

    // URL to use for resolving NearbyDevice metadata.
    public static final String METADATA_URL =
            // "http://url-caster.appspot.com/resolve-scan"
            rep("http://example-caster", "example", "url") + "."
                    + rep("example.com", "example", "appspot")
                    + rep("/resolve-link", "link", "scan");

    // How long before a session we display "This session starts in N minutes." in the
    // Session details page.
    public static final long HINT_TIME_BEFORE_SESSION = 60 * MINUTE_MILLIS; // 60 min

    // how long before the end of a session the user can give feedback
    public static final long FEEDBACK_MILLIS_BEFORE_SESSION_END = 15 * MINUTE_MILLIS; // 15min

    // Auto sync interval. Shouldn't be too small, or it might cause battery drain.
    public static final long AUTO_SYNC_INTERVAL_LONG_BEFORE_CONFERENCE = 6 * HOUR_MILLIS;
    public static final long AUTO_SYNC_INTERVAL_AROUND_CONFERENCE = 2 * HOUR_MILLIS;
    public static final long AUTO_SYNC_INTERVAL_AFTER_CONFERENCE = 12 * HOUR_MILLIS;

    // How many days before the conference we consider to be "around the conference date"
    // for purposes of sync interval (at which point the AUTO_SYNC_INTERVAL_AROUND_CONFERENCE
    // interval kicks in)
    public static final long AUTO_SYNC_AROUND_CONFERENCE_THRESH = 3 * DAY_MILLIS;

    // Minimum interval between two consecutive syncs. This is a safety mechanism to throttle
    // syncs in case conference data gets updated too often or something else goes wrong that
    // causes repeated syncs.
    public static final long MIN_INTERVAL_BETWEEN_SYNCS = 10 * MINUTE_MILLIS;

    // If data is not synced in this much time, we show the "data may be stale" warning
    public static final long STALE_DATA_THRESHOLD_NOT_DURING_CONFERENCE = 2 * DAY_MILLIS;
    public static final long STALE_DATA_THRESHOLD_DURING_CONFERENCE = 12 * HOUR_MILLIS;

    // How long we snooze the stale data notification for after the user has acted on it
    // (to keep from showing it repeatedly and being annoying)
    public static final long STALE_DATA_WARNING_SNOOZE = 10 * MINUTE_MILLIS;

    // Package name for the I/O Hunt game
    public static final String IO_HUNT_PACKAGE_NAME = "com.google.wolff.androidhunt2";

    // Play store URL prefix
    public static final String PLAY_STORE_URL_PREFIX
            = "https://play.google.com/store/apps/details?id=";

    // Known session tags that induce special behaviors
    public interface Tags {
        // tag that indicates a session is a live session
        public static final String SESSIONS = "TYPE_SESSIONS";

        // the tag category that we use to group sessions together when displaying them
        public static final String SESSION_GROUPING_TAG_CATEGORY = "TYPE";

        // tag categories
        public static final String CATEGORY_THEME = "THEME";
        public static final String CATEGORY_TOPIC = "TOPIC";
        public static final String CATEGORY_TYPE = "TYPE";

        public static final Map<String, Integer> CATEGORY_DISPLAY_ORDERS
                = new HashMap<String, Integer>();

        public static final String SPECIAL_KEYNOTE = "FLAG_KEYNOTE";

        public static final String[] EXPLORE_CATEGORIES =
                { CATEGORY_THEME, CATEGORY_TOPIC, CATEGORY_TYPE };

        public static final int[] EXPLORE_CATEGORY_ALL_STRING = {
                R.string.all_themes, R.string.all_topics, R.string.all_types
        };

        public static final int[] EXPLORE_CATEGORY_TITLE = {
                R.string.themes, R.string.topics, R.string.types
        };
    }

    static {
        Tags.CATEGORY_DISPLAY_ORDERS.put(Tags.CATEGORY_THEME, 0);
        Tags.CATEGORY_DISPLAY_ORDERS.put(Tags.CATEGORY_TOPIC, 1);
        Tags.CATEGORY_DISPLAY_ORDERS.put(Tags.CATEGORY_TYPE, 2);
    }

    // Values for the EventPoint feedback API. Sync happens at the same time as schedule sync,
    // and before that values are stored locally in the database.

    public static final String FEEDBACK_API_CODE = "";
    public static final String FEEDBACK_URL = "";
    public static final String FEEDBACK_API_KEY = "";
    public static final String FEEDBACK_DUMMY_REGISTRANT_ID = "";
    public static final String FEEDBACK_SURVEY_ID = "";

    // URL prefix for web links to session pages
    public static final Uri SESSION_DETAIL_WEB_URL_PREFIX
            = Uri.parse("https://www.google.com/events/io/schedule/session/");


    // Profile URLs for simulated badge reads for the debug feature.
    public static final String[] DEBUG_SIMULATED_BADGE_URLS = new String[] {};

    private static String piece(String s, char start, char end) {
        int startIndex = s.indexOf(start), endIndex = s.indexOf(end);
        return s.substring(startIndex + 1, endIndex);
    }

    private static String piece(String s, char start) {
        int startIndex = s.indexOf(start);
        return s.substring(startIndex + 1);
    }

    private static String rep(String s, String orig, String replacement) {
        return s.replaceAll(orig, replacement);
    }
}
