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
package com.google.samples.apps.iosched.sync.userdata.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.sync.userdata.UserAction;

import java.util.*;

/**
 * Helper class to handle the format of the User Data that is stored into AppData.
 * TODO: Refactor. Class mixes util methods, Pojos and business logic. See b/27809362.
 */
public class UserDataHelper {
    /**
     * Returns a JSON string representation of the given UserData object.
     */
    static public String toJsonString(UserData userData) {
        return new Gson().toJson(userData);
    }

    /**
     * Returns the JSON string representation of the given UserData object as a byte array.
     */
    static public byte[] toByteArray(UserData userData) {
        return toJsonString(userData).getBytes(Charsets.UTF_8);
    }

    /**
     * Deserializes the UserData given as a JSON string into a {@link UserData} object.
     * TODO: put this in UserData.
     */
    static public UserData fromString(String str) {
        if (str == null || str.isEmpty()) {
            return new UserData();
        }
        return new Gson().fromJson(str, UserData.class);
    }

    /**
     * Creates a UserData object from the given List of user actions.
     */
    static public UserData getUserData(List<UserAction> actions) {
        UserData userData = new UserData();
        if (actions != null) {
            for (UserAction action : actions) {
                if (action.type == UserAction.TYPE.ADD_STAR) {
                    if(userData.getStarredSessions() == null) {
                        // TODO: Make this part of setter. Create lazily.
                        userData.setStarredSessions(new HashMap<String, UserData.StarredSession>());
                    }
                    userData.getStarredSessions().put(action.sessionId,
                            new UserData.StarredSession(true, action.timestamp));
                } else if (action.type == UserAction.TYPE.VIEW_VIDEO) {
                    if(userData.getViewedVideoIds() == null) {
                        userData.setViewedVideoIds(new HashSet<String>());
                    }
                    userData.getViewedVideoIds().add(action.videoId);
                } else if (action.type == UserAction.TYPE.SUBMIT_FEEDBACK) {
                    if(userData.getFeedbackSubmittedSessionIds() == null) {
                        userData.setFeedbackSubmittedSessionIds(new HashSet<String>());
                    }
                    userData.getFeedbackSubmittedSessionIds().add(action.sessionId);
                } else if (action.type == UserAction.TYPE.REMOVE_STAR) {
                    userData.getStarredSessions().put(action.sessionId,
                            new UserData.StarredSession(false, action.timestamp));
                }
            }
        }
        return userData;
    }

    /**
     * Reads the data from the {@code column} of the content's {@code queryUri} and returns it as an
     * Array.
     */
    static private Set<String> getColumnContentAsArray(Context context, Uri queryUri,
            String column){
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[]{column}, null, null, null);
        Set<String> columnValues = new HashSet<>();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    columnValues.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return columnValues;
    }

    /**
     * Reads the data from columns of the content's {@code queryUri} and returns it as a Map.
     */
    static private Map<String, UserData.StarredSession> getColumnContentAsMap(Context context,
            Uri queryUri,
            String sessionIdColumn, String inScheduleColumn, String timestampColumn) {
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[]{sessionIdColumn, inScheduleColumn, timestampColumn}, null, null, null);
        Map<String, UserData.StarredSession> sessionValues = new HashMap<>();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sessionValues.put(cursor.getString(cursor.getColumnIndex(sessionIdColumn)),
                            new UserData.StarredSession(true,
                                    cursor.getLong(cursor.getColumnIndex(timestampColumn))));
                } while (cursor.moveToNext());
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return sessionValues;
    }

    /**
     * Returns the User Data that's on the device's local DB.
     */
    static public UserData getLocalUserData(Context context) {
        UserData userData = new UserData();

        userData.setStarredSessions(getColumnContentAsMap(context,
                ScheduleContract.MySchedule.CONTENT_URI,
                ScheduleContract.MySchedule.SESSION_ID,
                ScheduleContract.MySchedule.MY_SCHEDULE_IN_SCHEDULE,
                ScheduleContract.MySchedule.MY_SCHEDULE_TIMESTAMP));

        // Get Viewed Videos.
        userData.setViewedVideoIds(getColumnContentAsArray(context,
                ScheduleContract.MyViewedVideos.CONTENT_URI,
                ScheduleContract.MyViewedVideos.VIDEO_ID));

        // Get Feedback Submitted Sessions.
        userData.setFeedbackSubmittedSessionIds(getColumnContentAsArray(context,
                ScheduleContract.MyFeedbackSubmitted.CONTENT_URI,
                ScheduleContract.MyFeedbackSubmitted.SESSION_ID));

        return userData;
    }

    /**
     * Writes the given user data into the device's local DB.
     */
    static public void setLocalUserData(Context context, UserData userData, String accountName) {
        // TODO: throw if null. Callers should ensure the data is not null. See b/27809502.
        if (userData == null) {
            return;
        }

        // first clear all stars.
        context.getContentResolver().delete(ScheduleContract.MySchedule.CONTENT_URI,
                ScheduleContract.MySchedule.MY_SCHEDULE_ACCOUNT_NAME +" = ?",
                new String[]{accountName});

        // Now add the ones in sessionIds.
        ArrayList<UserAction> actions = new ArrayList<>();
        if (userData.getStarredSessions() != null) {
            for (Map.Entry<String, UserData.StarredSession> entry : userData.getStarredSessions().entrySet()) {
                UserAction action = new UserAction();
                action.type = entry.getValue().isInSchedule() ? UserAction.TYPE.ADD_STAR:
                        UserAction.TYPE.REMOVE_STAR;
                action.sessionId = entry.getKey();
                action.timestamp = entry.getValue().getTimestamp();
                actions.add(action);
            }
        }

        // first clear all viewed videos.
        context.getContentResolver().delete(ScheduleContract.MyViewedVideos.CONTENT_URI,
                ScheduleContract.MyViewedVideos.MY_VIEWED_VIDEOS_ACCOUNT_NAME +" = ?",
                new String[]{accountName});

        // Now add the viewed videos.
        if (userData.getViewedVideoIds() != null) {
            for (String videoId : userData.getViewedVideoIds()) {
                UserAction action = new UserAction();
                action.type = UserAction.TYPE.VIEW_VIDEO;
                action.videoId = videoId;
                actions.add(action);
            }
        }

        // first clear all feedback submitted videos.
        context.getContentResolver().delete(ScheduleContract.MyFeedbackSubmitted.CONTENT_URI,
                ScheduleContract.MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME +" = ?",
                new String[]{accountName});

        // Now add the feedback submitted videos.
        if (userData.getFeedbackSubmittedSessionIds() != null) {
            for (String sessionId : userData.getFeedbackSubmittedSessionIds()) {
                UserAction action = new UserAction();
                action.type = UserAction.TYPE.SUBMIT_FEEDBACK;
                action.sessionId = sessionId;
                actions.add(action);
            }
        }

        UserActionHelper.updateContentProvider(context, actions, accountName);
    }
}