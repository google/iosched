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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContractHelper;

import java.util.ArrayList;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Helper class to handle the format of the user actions done on the device.
 */
class UserActionHelper {
    private static final String TAG = makeLogTag(UserActionHelper.class);

    /**
     * Update content providers as a batch command based on the given list of User Actions.
     */
    static void updateContentProvider(Context context, List<UserAction> userActions,
            String account) {
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (UserAction action : userActions) {
            batch.add(createUpdateOperation(action, account));
        }
        try {
            context.getContentResolver().applyBatch(ScheduleContract.CONTENT_AUTHORITY, batch);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Could not apply operations", e);
        }
    }

    /**
     * Creates the correct content provider update operation depending on the type of the user
     * action.
     */
    static private ContentProviderOperation createUpdateOperation(UserAction action,
            String account) {
        if (action.type == UserAction.TYPE.ADD_STAR || action.type == UserAction.TYPE.REMOVE_STAR) {
            return ContentProviderOperation
                    .newInsert(
                            ScheduleContractHelper.addOverrideAccountName(
                                    ScheduleContract.MySchedule.CONTENT_URI, account))
                    .withValue(ScheduleContract.MySchedule.MY_SCHEDULE_DIRTY_FLAG, "0")
                    .withValue(ScheduleContract.MySchedule.SESSION_ID, action.sessionId)
                    .withValue(ScheduleContract.MySchedule.MY_SCHEDULE_TIMESTAMP,
                            action.timestamp)
                    .withValue(ScheduleContract.MySchedule.MY_SCHEDULE_IN_SCHEDULE,
                            action.type == UserAction.TYPE.ADD_STAR ? 1 : 0)
                    .build();
        } else if (action.type == UserAction.TYPE.SUBMIT_FEEDBACK) {
            return ContentProviderOperation
                    .newInsert(
                            ScheduleContractHelper.addOverrideAccountName(
                                    ScheduleContract.MyFeedbackSubmitted.CONTENT_URI, account))
                    .withValue(ScheduleContract.MyFeedbackSubmitted
                            .MY_FEEDBACK_SUBMITTED_DIRTY_FLAG, "0")
                    .withValue(ScheduleContract.MyFeedbackSubmitted.SESSION_ID, action.sessionId)
                    .build();
        } else if (action.type == UserAction.TYPE.RESERVE ||
                action.type == UserAction.TYPE.WAITLIST ||
                action.type == UserAction.TYPE.UNRESERVE) {

            int reservationStatus;
            switch (action.type) {
                case RESERVE:
                    reservationStatus = ScheduleContract.MyReservations.RESERVATION_STATUS_RESERVED;
                    break;
                case WAITLIST:
                    reservationStatus =
                            ScheduleContract.MyReservations.RESERVATION_STATUS_WAITLISTED;
                    break;
                default:
                    reservationStatus =
                            ScheduleContract.MyReservations.RESERVATION_STATUS_UNRESERVED;
            }

            return ContentProviderOperation
                    .newInsert(
                            ScheduleContractHelper.addOverrideAccountName(
                                    ScheduleContract.MyReservations.CONTENT_URI, account))
                    .withValue(ScheduleContract.MyReservations.MY_RESERVATION_STATUS,
                            reservationStatus)
                    .withValue(ScheduleContract.MyReservations.MY_RESERVATION_TIMESTAMP,
                            action.timestamp)
                    .withValue(ScheduleContract.MyReservations.SESSION_ID, action.sessionId)
                    .build();
        } else {
            return ContentProviderOperation
                    .newDelete(
                            ScheduleContractHelper.addOverrideAccountName(
                                    ScheduleContract.MySchedule.CONTENT_URI, account))
                    .withSelection(
                            ScheduleContract.MySchedule.SESSION_ID + " = ? AND " +
                                    ScheduleContract.MySchedule.MY_SCHEDULE_ACCOUNT_NAME + " = ? ",
                            new String[]{action.sessionId, account}
                    )
                    .build();
        }
    }
}
