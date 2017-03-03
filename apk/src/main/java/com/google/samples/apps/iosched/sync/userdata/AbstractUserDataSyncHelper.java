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

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.gcm.ServerUtilities;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContract.MySchedule;
import com.google.samples.apps.iosched.provider.ScheduleContract.MyFeedbackSubmitted;
import com.google.samples.apps.iosched.provider.ScheduleContract.MyViewedVideos;
import com.google.samples.apps.iosched.provider.ScheduleContractHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;


/**
 * Helper class that syncs user data in a Drive's AppData folder.
 *
 * Protocode:
 *
 *   // when user clicks on "star":
 *   session UI: run updateSession()
 *   this.updateSession():
 *     send addstar/removestar to contentProvider
 *     send broadcast to update any dependent UI
 *     save user actions as pending in shared settings_prefs
 *
 *   // on sync
 *   syncadapter: call this.sync()
 *   this.sync():
 *     fetch remote content
 *     if pending actions:
 *       apply to content and update remote
 *     if modified content != last synced content:
 *       update contentProvider
 *       send broadcast to update any dependent UI
 *
 *
 */
public abstract class AbstractUserDataSyncHelper {
    private static final String TAG = makeLogTag(AbstractUserDataSyncHelper.class);

    protected Context mContext;
    protected String mAccountName;
    protected int mIoExceptions = 0;

    public AbstractUserDataSyncHelper(Context context, String accountName) {
        this.mContext = context;
        this.mAccountName = accountName;
    }

    protected abstract boolean syncImpl(List<UserAction> actions, boolean hasPendingLocalData);

    /**
     * Create a copy of current pending actions and delegate the
     * proper sync'ing to the concrete subclass on the method syncImpl.
     */
    public boolean sync() {
        // Although we have a dirty flag per item, we need all schedule/viewed videos to sync,
        // because it's all sync'ed at once to a file on AppData folder. We only use the dirty flag
        // to decide if the local content was changed or not. If it was, we replace the remote
        // content.
        boolean hasPendingLocalData = false;
        ArrayList<UserAction> actions = new ArrayList<>();

        // Get schedule data pending sync.
        Cursor scheduleData = mContext.getContentResolver().query(
                MySchedule.buildMyScheduleUri(mAccountName),
                UserDataQueryEnum.MY_SCHEDULE.getProjection(), null, null, null);

        if (scheduleData != null) {
            while (scheduleData.moveToNext()) {
                UserAction userAction = new UserAction();
                userAction.sessionId = scheduleData.getString(
                        scheduleData.getColumnIndex(MySchedule.SESSION_ID));
                Integer inSchedule = scheduleData.getInt(
                        scheduleData.getColumnIndex(MySchedule.MY_SCHEDULE_IN_SCHEDULE));
                if (inSchedule == 0) {
                    userAction.type = UserAction.TYPE.REMOVE_STAR;
                } else {
                    userAction.type = UserAction.TYPE.ADD_STAR;
                }
                userAction.requiresSync = scheduleData.getInt(
                        scheduleData.getColumnIndex(MySchedule.MY_SCHEDULE_DIRTY_FLAG)) == 1;
                userAction.timestamp = scheduleData.getLong(
                        scheduleData.getColumnIndex(MySchedule.MY_SCHEDULE_TIMESTAMP));
                actions.add(userAction);
                if (!hasPendingLocalData && userAction.requiresSync) {
                    hasPendingLocalData = true;
                }
            }
            scheduleData.close();
        }

        // Get video viewed data pending sync.
        Cursor videoViewed = mContext.getContentResolver().query(
                ScheduleContract.MyViewedVideos.buildMyViewedVideosUri(mAccountName),
                UserDataQueryEnum.MY_VIEWED_VIDEO.getProjection(), null, null, null);

        if (videoViewed != null) {
            while (videoViewed.moveToNext()) {
                UserAction userAction = new UserAction();
                userAction.videoId = videoViewed.getString(
                        videoViewed.getColumnIndex(MyViewedVideos.VIDEO_ID));
                userAction.type = UserAction.TYPE.VIEW_VIDEO;
                userAction.requiresSync = videoViewed.getInt(
                        videoViewed.getColumnIndex(
                                MyViewedVideos.MY_VIEWED_VIDEOS_DIRTY_FLAG)) == 1;
                actions.add(userAction);
                if (!hasPendingLocalData && userAction.requiresSync) {
                    hasPendingLocalData = true;
                }
            }
            videoViewed.close();
        }

        // Get feedback submitted data pending sync.
        Cursor feedbackSubmitted = mContext.getContentResolver().query(
                MyFeedbackSubmitted.buildMyFeedbackSubmittedUri(mAccountName),
                UserDataQueryEnum.MY_FEEDBACK_SUBMITTED.getProjection(), null, null, null);

        if (feedbackSubmitted != null) {
            while (feedbackSubmitted.moveToNext()) {
                UserAction userAction = new UserAction();
                userAction.sessionId = feedbackSubmitted.getString(
                        feedbackSubmitted.getColumnIndex(MyFeedbackSubmitted.SESSION_ID));
                userAction.type = UserAction.TYPE.VIEW_VIDEO;
                userAction.requiresSync = feedbackSubmitted.getInt(
                        feedbackSubmitted.getColumnIndex(
                                MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_DIRTY_FLAG)) == 1;
                actions.add(userAction);
                if (!hasPendingLocalData && userAction.requiresSync) {
                    hasPendingLocalData = true;
                }
            }
            feedbackSubmitted.close();
        }


        Log.d(TAG, "Starting User Data sync. hasPendingData = " + hasPendingLocalData);

        boolean dataChanged = syncImpl(actions, hasPendingLocalData);

        if (hasPendingLocalData) {
            resetDirtyFlag(actions);

            // Notify other devices via GCM.
            ServerUtilities.notifyUserDataChanged(mContext);
        }
        if (dataChanged) {
            LOGD(TAG, "Notifying changes on paths related to user data on Content Resolver.");
            ContentResolver resolver = mContext.getContentResolver();
            for (String path : ScheduleContract.USER_DATA_RELATED_PATHS) {
                Uri uri = ScheduleContract.BASE_CONTENT_URI.buildUpon().appendPath(path).build();
                resolver.notifyChange(uri, null);
            }
            mContext.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(mContext, false));
        }
        return dataChanged;
    }

    private void resetDirtyFlag(ArrayList<UserAction> actions) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (UserAction action : actions) {

            Uri baseUri;
            String with;
            String[] withSelectionValue;
            String dirtyField;

            if (action.type == UserAction.TYPE.VIEW_VIDEO) {
                baseUri = MyViewedVideos.buildMyViewedVideosUri(mAccountName);
                with = MyViewedVideos.VIDEO_ID + "=?";
                withSelectionValue = new String[]{action.videoId};
                dirtyField = MyViewedVideos.MY_VIEWED_VIDEOS_DIRTY_FLAG;
            } else if (action.type == UserAction.TYPE.SUBMIT_FEEDBACK) {
                baseUri = MyFeedbackSubmitted.buildMyFeedbackSubmittedUri(mAccountName);
                with = MyFeedbackSubmitted.SESSION_ID + "=?";
                withSelectionValue = new String[]{action.sessionId};
                dirtyField = MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_DIRTY_FLAG;
            } else {
                baseUri = MySchedule.buildMyScheduleUri(mAccountName);
                with = MySchedule.SESSION_ID + "=? AND "
                        + MySchedule.MY_SCHEDULE_IN_SCHEDULE + "=?";
                withSelectionValue = new String[]{action.sessionId,
                        action.type == UserAction.TYPE.ADD_STAR ? "1" : "0"};
                dirtyField = MySchedule.MY_SCHEDULE_DIRTY_FLAG;
            }

            ContentProviderOperation op = ContentProviderOperation.newUpdate(
                    ScheduleContractHelper.setUriAsCalledFromSyncAdapter(baseUri))
                    .withSelection(with, withSelectionValue)
                    .withValue(dirtyField, 0)
                    .build();
            LOGD(TAG, op.toString());
            ops.add(op);
        }
        try {
            ContentProviderResult[] result = mContext.getContentResolver().applyBatch(
                    ScheduleContract.CONTENT_AUTHORITY, ops);
            LOGD(TAG, "Result of cleaning dirty flags is "+ Arrays.toString(result));
        } catch (Exception ex) {
            LOGW(TAG, "Could not update dirty flags. Ignoring.", ex);
        }
    }

    public void incrementIoExceptions() {
        mIoExceptions++;
    }

    public int getIoExcpetions() {
        return mIoExceptions;
    }

    private enum UserDataQueryEnum implements QueryEnum {
        MY_SCHEDULE(0, new String[]{MySchedule.SESSION_ID, MySchedule.MY_SCHEDULE_IN_SCHEDULE,
                MySchedule.MY_SCHEDULE_DIRTY_FLAG, MySchedule.MY_SCHEDULE_TIMESTAMP}),

        MY_FEEDBACK_SUBMITTED(0, new String[]{MyFeedbackSubmitted.SESSION_ID,
                MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_DIRTY_FLAG}),

        MY_VIEWED_VIDEO(0, new String[]{MyViewedVideos.VIDEO_ID,
                MyViewedVideos.MY_VIEWED_VIDEOS_DIRTY_FLAG});

        private int id;

        private String[] projection;

        UserDataQueryEnum(int id, String[] projection) {
            this.id = id;
            this.projection = projection;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String[] getProjection() {
            return projection;
        }

    }
}
