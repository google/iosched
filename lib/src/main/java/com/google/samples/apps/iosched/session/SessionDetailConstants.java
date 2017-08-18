/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.session;

import com.google.samples.apps.iosched.util.TimeUtils;

public class SessionDetailConstants {

    /**
     * How long before a session "This session starts in N minutes." is displayed.
     */
    public static final long HINT_TIME_BEFORE_SESSION_MIN = 60l;

    /**
     * Every 10 seconds, the time sensitive views of {@link SessionDetailFragment} are updated.
     * Those are related to live streaming, feedback, and information about how soon the session
     * starts.
     */
    public static final int TIME_HINT_UPDATE_INTERVAL = 10000;

    /**
     * How long before the end of a session the user can give feedback.
     */
    public static final long FEEDBACK_MILLIS_BEFORE_SESSION_END_MS = 15 * 60 * 1000l;

    /**
     * How long before the start of a session should livestream be open.
     */
    public static final long LIVESTREAM_BEFORE_SESSION_START_MS = 10 * TimeUtils.MINUTE;

    // Firebase result and status keys.
    public static final String RESERVED = "reserved";
    public static final String RESERVE_DENIED_SPACE = "reserve_denied_space";
    public static final String RESERVE_DENIED_CUTOFF = "reserve_denied_cutoff";
    public static final String RESERVE_DENIED_CLASH = "reserve_denied_clash";
    public static final String RESERVE_DENIED_FAILED = "reserve_denied_failed";
    public static final String RETURNED = "returned";
    public static final String RETURN_DENIED_CUTOFF = "return_denied_cutoff";
    public static final String RETURN_DENIED_FAILED = "return_denied_failed";
    public static final String RESERVE_STATUS_GRANTED = "granted";
    public static final String RESERVE_STATUS_WAITING = "waiting";
    public static final String RESERVE_STATUS_RETURNED = "returned";

    // Firebase node keys.
    public static final String FIREBASE_NODE_QUEUE = "queue";
    public static final String FIREBASE_NODE_SESSIONS = "sessions";
    public static final String FIREBASE_NODE_RESERVATIONS = "reservations";
    public static final String FIREBASE_NODE_STATUS = "status";
    public static final String FIREBASE_NODE_RESULTS = "results";
    public static final String FIREBASE_NODE_SEATS = "seats";
    public static final String FIREBASE_NODE_SEATS_AVAILABLE = "seats_available";

}
