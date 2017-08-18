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
 * A reserved (or waitlisted) session. ReservedSessions cannot actually be deleted -- though
 * they can be flagged as such (by setting status=DELETED).
 *
 * The timestamp is used to represent the time the session was added/removed from the user's
 * schedule, in millis (UTC). This is used to resolve sync conflicts. Care must be taken to
 * ensure the timestamp is accurate, even if the user's clock is not.
 */
public class ReservedSession {
    /**
     * Reservation status. WAITLISTED means the user is in the waiting list for this
     * session. RESERVED means the user holds a valid reservation. DELETED means the
     * user no longer holds any claim to this session (data preserved for sync purposes).
     */
    public enum Status {
        WAITLISTED, DELETED, RESERVED
    }

    /** CMS ID of the session which is being represented */
    public String sessionID;

    /**
     * Whether this session is in the user's collection, and if so whether it's
     * WAITLISTED or RESERVED.
     */
    public Status status;

    /**
     * Time the session was added/removed from the user's collection, in millis (UTC).
     */
    public long timestampUTC;

    public ReservedSession() {}

    public ReservedSession(String sessionID, Status status, long timestampUTC) {
        this.sessionID = sessionID;
        this.status = status;
        this.timestampUTC = timestampUTC;
    }
}
