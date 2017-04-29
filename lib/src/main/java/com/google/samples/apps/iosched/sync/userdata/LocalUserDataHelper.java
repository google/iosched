/*
 * Copyright 2016 Google Inc. All rights reserved.
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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import com.google.api.client.util.Charsets;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.AccountUtils;

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
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Helper class to process user data stored in the local SQLite db.
 */
public class LocalUserDataHelper {

    private static final String TAG = makeLogTag("LocalUserDataHelper");

    /**
     * Returns the JSON string representation of the given UserData object as a byte array.
     */
    public static byte[] toByteArray(UserDataModel userDataModel) {
        return userDataModel.toJsonString().getBytes(Charsets.UTF_8);
    }

    /**
     * Reads the data from the {@code column} of the content's {@code queryUri} and returns it as an
     * Array.
     */
    static private Set<String> getFeedbackSubmittedSessions(Context context, Uri queryUri,
            String column) {
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
    static private Map<String, UserDataModel.StarredSession> getStarredSessions(
            Context context, Uri queryUri, String sessionIdColumn, String inScheduleColumn,
            String timestampColumn) {
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[]{sessionIdColumn, inScheduleColumn, timestampColumn}, null, null, null);
        Map<String, UserDataModel.StarredSession> sessionValues = new HashMap<>();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sessionValues.put(cursor.getString(cursor.getColumnIndex(sessionIdColumn)),
                            new UserDataModel.StarredSession(true,
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
     * Reads the data from columns of the content's {@code queryUri} and returns it as a Map.
     */
    static private Map<String, UserDataModel.ReservedSession> getReservedSessions(
            Context context, Uri queryUri, String sessionIdColumn, String statusColumn,
            String timestampColumn) {
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[]{sessionIdColumn, statusColumn, timestampColumn}, null, null, null);
        Map<String, UserDataModel.ReservedSession> sessionValues = new HashMap<>();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sessionValues.put(cursor.getString(cursor.getColumnIndex(sessionIdColumn)),
                            new UserDataModel.ReservedSession(cursor.getInt(
                                    cursor.getColumnIndex(statusColumn)),
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

    static UserDataModel getUserData(List<UserAction> actions) {
        UserDataModel userDataModel = new UserDataModel();
        if (actions != null) {
            for (UserAction action : actions) {
                if (action.type == UserAction.TYPE.ADD_STAR) {
                    userDataModel.getStarredSessions().put(action.sessionId,
                            new UserDataModel.StarredSession(true, action.timestamp));
                } else if (action.type == UserAction.TYPE.REMOVE_STAR) {
                    userDataModel.getStarredSessions().put(action.sessionId,
                            new UserDataModel.StarredSession(false, action.timestamp));
                } else if (action.type == UserAction.TYPE.SUBMIT_FEEDBACK) {
                    userDataModel.getFeedbackSubmittedSessionIds().add(action.sessionId);
                }
            }
        }

        return userDataModel;
    }

    /**
     * Returns the user data that's on the device's local DB.
     */
    public static UserDataModel getLocalUserData(Context context) {
        UserDataModel userDataModel = new UserDataModel();

        userDataModel.setStarredSessions(getStarredSessions(context,
                MySchedule.CONTENT_URI,
                MySchedule.SESSION_ID,
                MySchedule.MY_SCHEDULE_IN_SCHEDULE,
                MySchedule.MY_SCHEDULE_TIMESTAMP));

        userDataModel.setReservedSessions(getReservedSessions(context,
                MyReservations.CONTENT_URI,
                MyReservations.SESSION_ID,
                MyReservations.MY_RESERVATION_STATUS,
                MyReservations.MY_RESERVATION_TIMESTAMP));

        userDataModel.setFeedbackSubmittedSessionIds(getFeedbackSubmittedSessions(context,
                MyFeedbackSubmitted.CONTENT_URI,
                MyFeedbackSubmitted.SESSION_ID));

        return userDataModel;
    }

    /**
     * Writes the given user data into the device's local DB.
     */
    static void setLocalUserData(Context context, UserDataModel userDataModel,
            String accountName) {
        // TODO: throw if null. Callers should ensure the data is not null. See b/27809502.
        if (userDataModel == null) {
            return;
        }

        ArrayList<UserAction> actions = new ArrayList<>();

        // first clear all stars.
        context.getContentResolver().delete(MySchedule.CONTENT_URI,
                MySchedule.MY_SCHEDULE_ACCOUNT_NAME + " = ?",
                new String[]{accountName});

        // Now add the ones in sessionIds.
        if (userDataModel.getStarredSessions() != null) {
            for (Map.Entry<String, UserDataModel.StarredSession> entry :
                    userDataModel.getStarredSessions().entrySet()) {
                UserAction action = new UserAction();
                action.type = entry.getValue().inSchedule ? UserAction.TYPE.ADD_STAR :
                        UserAction.TYPE.REMOVE_STAR;
                action.sessionId = entry.getKey();
                action.timestamp = entry.getValue().timestamp;
                actions.add(action);
            }
        }

        // First clear all reservations.
        context.getContentResolver().delete(MySchedule.CONTENT_URI,
                MyReservations.MY_RESERVATION_ACCOUNT_NAME + " = ?",
                new String[]{accountName});

        // Now add the reserved sessions.
        if (userDataModel.getReservedSessions() != null) {
            for (Map.Entry<String, UserDataModel.ReservedSession> entry :
                    userDataModel.getReservedSessions().entrySet()) {
                UserAction action = new UserAction();
                UserDataModel.ReservedSession value = entry.getValue();
                switch (value.status) {
                    case ScheduleContract.MyReservations.RESERVATION_STATUS_RESERVED:
                        action.type = UserAction.TYPE.RESERVE;
                        break;
                    case MyReservations.RESERVATION_STATUS_WAITLISTED:
                        action.type = UserAction.TYPE.WAITLIST;
                        break;
                    default:
                        action.type = UserAction.TYPE.UNRESERVE;
                }
                action.sessionId = entry.getKey();
                action.timestamp = value.timestamp;
                actions.add(action);
            }
        }

        // First clear all feedback submitted sessions.
        context.getContentResolver().delete(ScheduleContract.MyFeedbackSubmitted.CONTENT_URI,
                ScheduleContract.MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME + " = ?",
                new String[]{accountName});

        // Now add the feedback submitted sessions.
        if (userDataModel.getFeedbackSubmittedSessionIds() != null) {
            for (String sessionId : userDataModel.getFeedbackSubmittedSessionIds()) {
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
        ClearDataAsyncHandler handler = new ClearDataAsyncHandler(context, accountName);
        handler.clearUserData();
    }

    private static final class ClearDataAsyncHandler extends Handler {
        private static final int MSG_CLEAR_USERDATA = 1;

        private static Looper sLooper;

        static {
            HandlerThread thread = new HandlerThread("ClearDataHandlerThread");
            thread.start();
            sLooper = thread.getLooper();
        }

        private final WeakReference<Context> mReference;
        private final String mAccountName;

        private ClearDataAsyncHandler(Context context, String accountName) {
            super(sLooper);
            mReference = new WeakReference<>(context);
            mAccountName = accountName;
        }

        public void clearUserData() {
            Message.obtain(this, MSG_CLEAR_USERDATA).sendToTarget();
        }

        @Override
        public void handleMessage(Message msg) {
            if (MSG_CLEAR_USERDATA != msg.what) {
                return;
            }

            Context context = mReference.get();
            if (context == null) {
                return;
            }

            ArrayList<ContentProviderOperation> ops = new ArrayList<>(3);
            ops.add(ContentProviderOperation.newDelete(MySchedule.buildMyScheduleUri(mAccountName))
                    .build());
            ops.add(ContentProviderOperation.newDelete(
                    MyFeedbackSubmitted.buildMyFeedbackSubmittedUri(mAccountName)).build());
            ops.add(ContentProviderOperation.newDelete(
                    MyReservations.buildMyReservationUri(mAccountName)).build());
            try {
                context.getContentResolver().applyBatch(ScheduleContract.CONTENT_AUTHORITY, ops);
            } catch (RemoteException | OperationApplicationException e) {
                LOGE(TAG, "Error clearing user data", e);
            }

            // force any current loaders to reload their data
            context.getContentResolver().notifyChange(ScheduleContract.BASE_CONTENT_URI, null);
        }
    }
}
