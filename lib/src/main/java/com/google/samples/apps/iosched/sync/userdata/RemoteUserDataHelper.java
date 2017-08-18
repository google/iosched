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

package com.google.samples.apps.iosched.sync.userdata;

import android.content.Context;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.ArrayMap;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.rpc.userdata.Userdata;
import com.google.samples.apps.iosched.rpc.userdata.model.JsonMap;
import com.google.samples.apps.iosched.rpc.userdata.model.UserData;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to process user data received from a remote endpoint.
 */
class RemoteUserDataHelper {

    // Fields used to process JSON received from the user data endppoint.
    private static final String SESSION_ID = "sessionID";
    private static final String IN_SCHEDULE = "inSchedule";
    private static final String TIMESTAMP_UTC = "timestampUTC";
    private static final String STATUS = "status";

    // Tracks the reserved status of a session as reported by the user data remote endpoint.
    public enum Status {
        WAITLISTED,
        DELETED,
        RESERVED
    }

    /**
     * Returns a {@link Userdata} object that can be used to access the user data endpoint.
     * @param context {@link Context}.
     */
    static Userdata getUserdataHandler(Context context) {
        GoogleAccountCredential credential =
                GoogleAccountCredential
                        .usingOAuth2(context, Arrays.asList(AccountUtils.AUTH_SCOPES));
        credential.setSelectedAccount(AccountUtils.getActiveAccount(context));
        return new Userdata.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), credential).build();
    }

    /**
     * Returns a user's bookmarked/starred sessions stored in the user data endpoint.
     *
     * @param remoteUserDataJson A {@link UserData} object representing the user's data.
     */
    static Map<String, UserDataModel.StarredSession> getRemoteBookmarkedSessions(
            UserData remoteUserDataJson) {

        Map<String, UserDataModel.StarredSession> result = new HashMap<>();
        JsonMap jsonMap = remoteUserDataJson.getBookmarkedSessions();

        if (jsonMap != null) {
            for (Object o : jsonMap.values()) {
                String sessionID = String.valueOf(((ArrayMap) o).get(SESSION_ID));
                boolean inSchedule =
                        Boolean.parseBoolean(String.valueOf(((ArrayMap) o).get(IN_SCHEDULE)));
                long timestampUTC = Long.parseLong(String.valueOf(((ArrayMap) o).get(TIMESTAMP_UTC)));
                result.put(sessionID, new UserDataModel.StarredSession(inSchedule, timestampUTC));
            }
        }
        return result;
    }

    /**
     * Returns a user's reserved/waitlisted sessions stored in the user data endpoint.
     *
     * @param remoteUserDataJson A {@link UserData} object representing the user's data.
     */
    static Map<String, UserDataModel.ReservedSession> getRemoteReservedSessions(
            UserData remoteUserDataJson) {

        Map<String, UserDataModel.ReservedSession> result = new HashMap<>();
        JsonMap jsonMap = remoteUserDataJson.getReservedSessions();
        if (jsonMap != null) {
            for (Object o : jsonMap.values()) {
                String sessionID = String.valueOf(((ArrayMap) o).get(SESSION_ID));

                Status status = Status.valueOf(String.valueOf(((ArrayMap) o).get(STATUS)));

                int valueToInsert;

                switch (status) {
                    case RESERVED:
                        valueToInsert = ScheduleContract.MyReservations.RESERVATION_STATUS_RESERVED;
                        break;
                    case WAITLISTED:
                        valueToInsert = ScheduleContract.MyReservations
                                .RESERVATION_STATUS_WAITLISTED;
                        break;
                    default:
                        valueToInsert = ScheduleContract.MyReservations
                                .RESERVATION_STATUS_UNRESERVED;
                }

                long timestampUTC = Long.parseLong(String.valueOf(((ArrayMap) o).get(
                        TIMESTAMP_UTC)));
                result.put(sessionID, new UserDataModel.ReservedSession(valueToInsert,
                        timestampUTC));
            }
        }
        return result;
    }

    /**
     * Returns a user's reviewed sessions stored in the user data endpoint.
     *
     * @param remoteUserDataJson A {@link UserData} object representing the user's data.
     */
    static Set<String> getRemoteReviewedSessions(UserData remoteUserDataJson) {
        List<String> reviewedSessions = remoteUserDataJson.getReviewedSessions();
        Set<String> result = new HashSet<>();
        if (reviewedSessions != null) {
            for (Object o : reviewedSessions) {
                String sessionID = String.valueOf(((ArrayMap) o).get(SESSION_ID));
                result.add(sessionID);
            }
        }
        return result;
    }

    /**
     * Builds and returns a {@link UserData} object from a {@link UserDataModel}. The returned
     * object can be used to write data to the user data endpoint.
     *
     * @param model Data from the user data endpoint represented as a {@link UserDataModel}.
     */
    static UserData asUserData(UserDataModel model) {
        UserData userData = new UserData();
        JsonMap bookmarkedSessionsJsonMap = new JsonMap();

        for (final Map.Entry<String, UserDataModel.StarredSession> entry:
                model.getStarredSessions().entrySet()) {
            ArrayMap<String, String> arrayMap = new ArrayMap<>();
            arrayMap.put(SESSION_ID, entry.getKey());
            arrayMap.put(IN_SCHEDULE, String.valueOf(entry.getValue().inSchedule));
            arrayMap.put(TIMESTAMP_UTC, String.valueOf(entry.getValue().timestamp));
            bookmarkedSessionsJsonMap.set(entry.getKey(), arrayMap);
        }
        userData.setBookmarkedSessions(bookmarkedSessionsJsonMap);

        List<String> reviewSessionsList = new ArrayList<>();
        for (String reviewedSessionId: model.getFeedbackSubmittedSessionIds()) {
            reviewSessionsList.add(reviewedSessionId);
        }
        userData.setReviewedSessions(reviewSessionsList);

        return userData;
    }
}