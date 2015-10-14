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

package com.google.samples.apps.iosched.feedback;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.samples.apps.iosched.service.SessionAlarmService;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A {@link WearableListenerService} service to receive the session feedback from the wearable
 * device and handle dismissal of notifications by deleting the associated Data Items.
 */
public class FeedbackWearableListenerService extends WearableListenerService
        {

    private static final String TAG = makeLogTag(FeedbackWearableListenerService.class);

    public static final String PATH_RESPONSE = "/iowear/response";

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (SessionAlarmService.ACTION_NOTIFICATION_DISMISSAL.equals(action)) {
                String sessionId = intent.getStringExtra(SessionAlarmService.KEY_SESSION_ID);
                LOGD(TAG, "onStartCommand(): Action = ACTION_NOTIFICATION_DISMISSAL Session: "
                        + sessionId);
                dismissWearableNotification(sessionId);
            }
        }
        return Service.START_NOT_STICKY;
    }

    /**
     * Removes the Data Item that was used to create a notification on the watch. By deleting the
     * data item, a {@link WearableListenerService} on the watch will be notified and the
     * notification on the watch will be removed.
     * <p/>
     * Since connection to the Google API client is asynchronous, we spawn a thread and wait for
     * the connection to be established before attempting to use the Google API client.
     *
     * @param sessionId The Session ID of the notification that should be removed
     */
    private void dismissWearableNotification(final String sessionId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.blockingConnect(
                            FeedbackConstants.GOOGLE_API_CLIENT_CONNECTION_TIMEOUT_S, TimeUnit.SECONDS);
                }
                if (!mGoogleApiClient.isConnected()) {
                    Log.e(TAG, "Failed to connect to mGoogleApiClient within "
                            + FeedbackConstants.GOOGLE_API_CLIENT_CONNECTION_TIMEOUT_S + " seconds");
                    return;
                }
                LOGD(TAG, "dismissWearableNotification(): Attempting to dismiss wearable "
                        + "notification");
                PutDataMapRequest putDataMapRequest = PutDataMapRequest
                        .create(FeedbackHelper.getFeedbackDataPathForWear(sessionId));
                if (mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.deleteDataItems(mGoogleApiClient, putDataMapRequest.getUri())
                            .setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                                @Override
                                public void onResult(
                                        DataApi.DeleteDataItemsResult deleteDataItemsResult) {
                                    if (!deleteDataItemsResult.getStatus().isSuccess()) {
                                        LOGD(TAG, "dismissWearableNotification(): failed to delete"
                                                + " the data item");
                                    }
                                }
                            });
                } else {
                    Log.e(TAG, "dismissWearableNotification()): No Google API Client connection");
                }
            }
        }).start();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged: " + dataEvents + " for " + getPackageName());

        for (DataEvent event : dataEvents) {
            LOGD(TAG, "Uri is: " + event.getDataItem().getUri());
            DataMapItem mapItem = DataMapItem.fromDataItem(event.getDataItem());
            String path = event.getDataItem().getUri().getPath();
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                if (PATH_RESPONSE.equals(path)) {
                    // we have a response
                    DataMap data = mapItem.getDataMap();
                    saveFeedback(data);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                if (path.startsWith(SessionAlarmService.PATH_FEEDBACK)) {
                    Uri uri = event.getDataItem().getUri();
                    dismissLocalNotification(uri.getLastPathSegment());
                }
            }
        }
    }

    /**
     * Dismisses the local notification for the given session
     */
    private void dismissLocalNotification(String sessionId) {
        LOGD(TAG, "dismissLocalNotification: sessionId=" + sessionId);
        NotificationManagerCompat.from(this)
                .cancel(sessionId, SessionAlarmService.FEEDBACK_NOTIFICATION_ID);
    }

    /**
     * This converts the {@code data} from the wearable app and persists it.
     *
     * @return true if successfully persisted
     */
    private boolean saveFeedback(DataMap data) {
        SessionFeedbackModel.SessionFeedbackData feedback =
                FeedbackHelper.convertDataMapToFeedbackData(data);
        if (feedback != null) {
            LOGD(TAG, "Feedback answers received from wear: " + feedback.toString());
            FeedbackHelper feedbackHelper = new FeedbackHelper(this);
            feedbackHelper.saveSessionFeedback(feedback);
            return true;
        }
        return false;
    }
}
