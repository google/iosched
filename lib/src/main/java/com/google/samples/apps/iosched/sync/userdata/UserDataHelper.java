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
package com.google.samples.apps.iosched.sync.userdata;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.IOUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.samples.apps.iosched.provider.ScheduleContract.MyFeedbackSubmitted;
import static com.google.samples.apps.iosched.provider.ScheduleContract.MyReservations;
import static com.google.samples.apps.iosched.provider.ScheduleContract.MySchedule;
import static com.google.samples.apps.iosched.provider.ScheduleContract.MyViewedVideos;
import static com.google.samples.apps.iosched.util.LogUtils.LOGV;

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
        return toJsonString(userData).getBytes(IOUtils.CHARSET_UTF8);
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
                MySchedule.CONTENT_URI,
                MySchedule.SESSION_ID,
                MySchedule.MY_SCHEDULE_IN_SCHEDULE,
                MySchedule.MY_SCHEDULE_TIMESTAMP));

        // Get Viewed Videos.
        userData.setViewedVideoIds(getColumnContentAsArray(context,
                MyViewedVideos.CONTENT_URI,
                MyViewedVideos.VIDEO_ID));

        // Get Feedback Submitted Sessions.
        userData.setFeedbackSubmittedSessionIds(getColumnContentAsArray(context,
                MyFeedbackSubmitted.CONTENT_URI,
                MyFeedbackSubmitted.SESSION_ID));

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
        context.getContentResolver().delete(MySchedule.CONTENT_URI,
                MySchedule.MY_SCHEDULE_ACCOUNT_NAME +" = ?",
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

        // first clear all feedback submitted sessions.
        context.getContentResolver().delete(ScheduleContract.MyFeedbackSubmitted.CONTENT_URI,
                ScheduleContract.MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME +" = ?",
                new String[]{accountName});

        // Now add the feedback submitted sessions.
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

    public static void clearUserDataOnSignOut(final Context context) {
        String accountName = AccountUtils.getActiveAccountName(context);
        AsyncQueryHandler handler = new ClearDataAsyncQueryHandler(context);

        handler.startDelete(ClearDataAsyncQueryHandler.TOKEN_MY_SCHEDULE, null,
                MySchedule.buildMyScheduleUri(accountName), null, null);

        handler.startDelete(2, null, MyFeedbackSubmitted.buildMyFeedbackSubmittedUri(accountName),
                null, null);

        handler.startDelete(3, null, MyReservations.buildMyReservationUri(accountName), null, null);
    }

    private static class ClearDataAsyncQueryHandler extends AsyncQueryHandler {
        private final WeakReference<Context> mReference;
        static final int TOKEN_MY_SCHEDULE = 1;

        private ClearDataAsyncQueryHandler(Context context) {
            super(context.getContentResolver());
            mReference = new WeakReference<>(context);
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            if (token == TOKEN_MY_SCHEDULE && result > 0) {
                // When items are deleted from MY_SCHEDULE trigger a notifyChange
                // so that any current loaders are updated.
                // TODO: Ideally the MyIo UI should observe individual changes to
                // the user URIs instead of a composite URI.
                Context context = mReference.get();
                if (context == null) {
                    return;
                }
                context.getContentResolver()
                        .notifyChange(ScheduleContract.Sessions.CONTENT_MY_SCHEDULE_URI, null);
            }
        }
    }
}