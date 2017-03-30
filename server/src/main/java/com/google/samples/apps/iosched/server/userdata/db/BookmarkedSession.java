/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.server.userdata.db;

/**
 * A bookmarked session. BookmarkedSessions cannot actually be deleted -- though they
 * can be flagged as such (by setting inSchedule=false).
 *
 * The timestamp is used to represent the time the session was added/removed from the user's
 * schedule, in millis (UTC). This is used to resolve sync conflicts. Care must be taken to
 * ensure the timestamp is accurate, even if the user's clock is not.
 */
public class BookmarkedSession {
    /** CMS ID of the session which is being represented */
    public String sessionID;
    /** Whether this session is in the user's collection or not. (This is a proxy for deleting
     * a session, while retaining information about it for sync purposes.)
     */
    public boolean inSchedule;
    /**
     * Time the session was added/removed from the user's collection, in millis (UTC).
     */
    public long timestampUTC;

    public BookmarkedSession() {}

    public BookmarkedSession(String sessionID, boolean inSchedule, long timestampUTC) {
        this.sessionID = sessionID;
        this.inSchedule = inSchedule;
        this.timestampUTC = timestampUTC;
    }
}
