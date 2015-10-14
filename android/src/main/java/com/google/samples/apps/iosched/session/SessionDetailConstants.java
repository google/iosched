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

public class SessionDetailConstants {

    /**
     * How long before a session "This session starts in N minutes." is displayed.
     */
    public static final long HINT_TIME_BEFORE_SESSION_MIN = 60l;

    /**
     * Every 10 seconds, the time sensitive views of {@link SessionDetailFragment} are updated.
     * Those are related to live streaming, feedback, and information about how soon the session starts.
     */
    public static final int TIME_HINT_UPDATE_INTERVAL = 10000;

    /**
     * How long before the end of a session the user can give feedback.
     */
    public static final long FEEDBACK_MILLIS_BEFORE_SESSION_END_MS = 15 * 60 * 1000l;

    /**
     * The name of the shared transition shown when loading {@link SessionDetailFragment}.
     */
    public static final String TRANSITION_NAME_PHOTO = "photo";

}
