/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched;

public class Config {
    // General configuration
    public static final int CONFERENCE_YEAR = 2013;

    // OAuth 2.0 related config
    public static final String APP_NAME = "GoogleIO-Android";
    // TODO: Add your Google API key here.
    public static final String API_KEY = "YOUR_API_KEY_HERE";

    // Conference API-specific config
    public static final String EVENT_ID = "googleio2013";
    public static final String CONFERENCE_IMAGE_PREFIX_URL = "https://developers.google.com";

    // Announcements
    public static final String ANNOUNCEMENTS_PLUS_ID = "111395306401981598462";

    // Static file host for the map data
    public static final String GET_MAP_URL = "http://2013.ioschedmap.appspot.com/map.json";

    // YouTube API config
    // TODO: Add your YouTube API key here.
    public static final String YOUTUBE_API_KEY = "YOUR_API_KEY_HERE";
    // YouTube share URL
    public static final String YOUTUBE_SHARE_URL_PREFIX = "http://youtu.be/";

    // Livestream captions config
    public static final String PRIMARY_LIVESTREAM_CAPTIONS_URL =
            "http://io-captions.appspot.com/?event=e1&android=t";
    public static final String SECONDARY_LIVESTREAM_CAPTIONS_URL =
            "http://io-captions.appspot.com/?event=e2&android=t";
    public static final String PRIMARY_LIVESTREAM_TRACK = "android";
    public static final String SECONDARY_LIVESTREAM_TRACK = "chrome";

    // Conference public WiFi AP parameters
    public static final String WIFI_SSID = "Google5G";
    public static final String WIFI_PASSPHRASE = "gomobileio";

    // GCM config
    // TODO: Add your GCM information here.
    public static final String GCM_SERVER_URL = "https://YOUR_GCM_APP_ID_HERE.appspot.com";
    public static final String GCM_SENDER_ID = "YOUR_GCM_SENDER_ID_HERE";
    public static final String GCM_API_KEY = "YOUR_GCM_API_KEY_HERE";
}
