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

package com.google.samples.apps.iosched.service;

import com.google.android.gms.common.ConnectionResult;
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
import com.google.samples.apps.iosched.util.FeedbackUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A {@link com.google.android.gms.wearable.WearableListenerService} service to receive the session
 * feedback from the wearable device and handle dismissal of notifications by deleting the
 * associated Data Items.
 */
public class FeedbackListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = makeLogTag(FeedbackListenerService.class);
    public static final String PATH_RESPONSE = "/iowear/response";
    private GoogleApiClient mGoogleApiClient;
    private boolean mConnected = false;
    private static final long TIMEOUT_S = 10; // how long wait for APi Client connection, in seconds

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
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
     * data item, a {@link com.google.android.gms.wearable.WearableListenerService} on the watch
     * will be notified and the notification on the watch will be removed.
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
                if (!mConnected) {
                    mGoogleApiClient.blockingConnect(TIMEOUT_S, TimeUnit.SECONDS);
                }
                if (!mConnected) {
                    Log.e(TAG, "Failed to connect to mGoogleApiClient within " + TIMEOUT_S
                            + " seconds");
                    return;
                }
                LOGD(TAG, "dismissWearableNotification(): Attempting to dismiss wearable "
                        + "notification");
                PutDataMapRequest putDataMapRequest = PutDataMapRequest
                        .create(FeedbackUtils.getFeedbackPath(sessionId));
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
                    String jsonString = data.getString("response");
                    if (TextUtils.isEmpty(jsonString)) {
                        return;
                    }
                    LOGD(TAG, "jsonString is: " + jsonString);
                    saveFeedback(jsonString);
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
     * Persisting the feedback in the database. The input is the JSON string that represents the
     * response from the user on the paired wear device. The format of a typical response is:
     * <pre>[{"s":"sessionId-1234"},{"q":1,"a":2},{"q":0,"a":1},{"q":3,"a":1},{"q":2,"a":1}]</pre>
     */
    private void saveFeedback(String jsonString) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            if (null != jsonArray) {
                JSONObject sessionObj = (JSONObject) jsonArray.get(0);
                String sessionId = sessionObj.getString("s");
                StringBuffer result = new StringBuffer("Session Id: " + sessionId + "\n");
                int[] answers = new int[4];
                for (int i = 0; i < answers.length; i++) {
                    answers[i] = -1;
                }
                for (int i = 1; i < jsonArray.length(); i++) {
                    JSONObject answerObj = (JSONObject) jsonArray.get(i);
                    int question = answerObj.getInt("q");
                    int answer = answerObj.getInt("a") + 1;
                    answers[question] = answer;
                    result.append("Question: " + question + " ---> Answer: " + answer + "\n");
                }
                LOGD(TAG, "Feedback answers received from the wear: " + result.toString());
                FeedbackUtils.saveSessionFeedback(this, sessionId, answers[0], answers[1],
                        answers[2], answers[3], null);
            }

        } catch (JSONException e) {
            LOGE(TAG, "Failed to parse the json received from the wear", e);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        mConnected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to the Google API client");
        mConnected = false;
    }
}
