/*
 * Copyright (c) 2017 Google Inc.
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

package com.google.samples.apps.iosched.sync.userdata;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents user data stored locally in a SQLite database.
 */
public class UserDataModel {
    /** JSON Attribute name of the starred sessions values. */
    private static final String JSON_ATTRIBUTE_STARRED_SESSIONS = "starred_sessions";

    /** JSON Attribute name of the reserved sessions values. */
    private static final String JSON_ATTRIBUTE_RESERVED_SESSIONS = "reserved_sessions";

    /** JSON Attribute name of the feedback submitted for sessions values. */
    private static final String JSON_ATTRIBUTE_FEEDBACK_SUBMITTED_SESSIONS =
            "feedback_submitted_sessions";

    @SerializedName(JSON_ATTRIBUTE_STARRED_SESSIONS)
    private Map<String, StarredSession> mStarredSessions = new HashMap<>();

    @SerializedName(JSON_ATTRIBUTE_RESERVED_SESSIONS)
    private Map<String, ReservedSession> mReservedSessions = new HashMap<>();

    @SerializedName(JSON_ATTRIBUTE_FEEDBACK_SUBMITTED_SESSIONS)
    private  Set<String> mFeedbackSubmittedSessionIds = new HashSet<>();

    /**
     * Deserializes the UserDataModel given as a JSON string into a {@link UserDataModel} object.
     */
    public static UserDataModel fromString(String str) {
        if (str == null || str.isEmpty()) {
            return new UserDataModel();
        }
        return new Gson().fromJson(str, UserDataModel.class);
    }

    Map<String, StarredSession> getStarredSessions() {
        return mStarredSessions;
    }

    Map<String, ReservedSession> getReservedSessions() {
        return mReservedSessions;
    }

    Set<String> getFeedbackSubmittedSessionIds() {
        return mFeedbackSubmittedSessionIds;
    }

    void setStarredSessions(final Map<String, StarredSession> starredSessions) {
        getStarredSessions().putAll(starredSessions);
    }

    void setReservedSessions(final Map<String, ReservedSession> reservedSessions) {
        getReservedSessions().putAll(reservedSessions);
    }

    void setFeedbackSubmittedSessionIds(final Set<String> feedbackSubmittedSessionIds) {
        getFeedbackSubmittedSessionIds().addAll(feedbackSubmittedSessionIds);
    }

    /**
     * Builds and returns an instance with the data of {@code other}.
     */
    private static UserDataModel fromOther(UserDataModel other) {
        UserDataModel newObj = new UserDataModel();
        newObj.setStarredSessions(other.getStarredSessions());
        newObj.setFeedbackSubmittedSessionIds(other.getFeedbackSubmittedSessionIds());
        newObj.setReservedSessions(other.getReservedSessions());
        return newObj;
    }

    /**
     * Returns a new instance that reconciles differences between local and remote representations
     * of user data.
     */
    static UserDataModel reconciledUserData(UserDataModel local, UserDataModel remote) {
        UserDataModel localCopy = UserDataModel.fromOther(local);

        Set<String> localStarredSessionIds = localCopy.getStarredSessions().keySet();
        for (Map.Entry<String, StarredSession> remoteEntry:
                remote.getStarredSessions().entrySet()) {
            String starredSessionId = remoteEntry.getKey();
            if (localStarredSessionIds.contains(starredSessionId)) {
                // If remote is more recent, it should override local.
                if (remoteEntry.getValue().timestamp >
                        localCopy.getStarredSessions().get(starredSessionId).timestamp) {
                    localCopy.getStarredSessions().put(starredSessionId, remoteEntry.getValue());
                }
            } else {
                // Some other client has modified bookmark data.
                localCopy.getStarredSessions().put(remoteEntry.getKey(), remoteEntry.getValue());
            }
        }

        Set<String> localReservedSessionIds = localCopy.getReservedSessions().keySet();
        for (Map.Entry<String, ReservedSession> remoteEntry:
                remote.getReservedSessions().entrySet()) {
            String reservedSessionId = remoteEntry.getKey();
            if (localReservedSessionIds.contains(reservedSessionId)) {
                if (remoteEntry.getValue().timestamp >
                        localCopy.getReservedSessions().get(reservedSessionId).timestamp) {
                    localCopy.getReservedSessions().put(reservedSessionId, remoteEntry.getValue());
                }
            } else {
                localCopy.getReservedSessions().put(reservedSessionId, remoteEntry.getValue());
            }
        }

        localCopy.getFeedbackSubmittedSessionIds().addAll(remote.getFeedbackSubmittedSessionIds());

        return localCopy;
    }

    /**
     * Returns a JSON string representation of this object.
     */
    String toJsonString() {
        return new Gson().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDataModel that = (UserDataModel) o;
        return Objects.equals(mStarredSessions, that.mStarredSessions) &&
                Objects.equals(mReservedSessions, that.mReservedSessions) &&
                Objects.equals(mFeedbackSubmittedSessionIds, that.mFeedbackSubmittedSessionIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStarredSessions, mReservedSessions, mFeedbackSubmittedSessionIds);
    }

    /**
     * Used to track whether a session is currently in a user's schedule and the timestamp for when
     * it was last added or removed.
     */
    static class StarredSession {
        public boolean inSchedule;
        public long timestamp;

        StarredSession(final boolean inSchedule, final long timestamp) {
            this.inSchedule = inSchedule;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StarredSession that = (StarredSession) o;
            return inSchedule == that.inSchedule &&
                    timestamp == that.timestamp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(inSchedule, timestamp);
        }
    }

    /**
     * Used to track whether the user has a reservation or is on the waitlist for the
     * session, as well as the time that the reservation/waitlist status was changed.
     */
    static class ReservedSession {
        public int status;
        public long timestamp;

        ReservedSession(final int status, final long timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReservedSession that = (ReservedSession) o;
            return status == that.status &&
                    timestamp == that.timestamp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, timestamp);
        }
    }
}