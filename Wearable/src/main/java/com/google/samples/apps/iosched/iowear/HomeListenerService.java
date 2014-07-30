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

package com.google.samples.apps.iosched.iowear;

import static com.google.samples.apps.iosched.iowear.utils.Utils.LOGD;
import static com.google.samples.apps.iosched.iowear.utils.Utils.makeLogTag;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.samples.apps.iosched.R;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A {@link com.google.android.gms.wearable.WearableListenerService} service that is invoked upon
 * receiving a DataItem from the handset for session feedback notifications, or the dismissal of a
 * notification. Handset application creates a Data Item that will then trigger the invocation of
 * this service. That will result in creation of a wearable notification. Similarly, when a
 * notification is dismissed, this service will be invoked to delete the associated data item.
 */
public class HomeListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = makeLogTag("HomeListenerService");
    public static final String KEY_SESSION_ID = "session-id";
    public static final String KEY_SESSION_NAME = "session-name";
    public static final String KEY_SPEAKER_NAME = "speaker-name";
    public static final String KEY_SESSION_ROOM = "session-room";
    public static final String KEY_NOTIFICATION_ID = "notification-id";
    public final static String ACTION_DISMISS
            = "com.google.devrel.io.android.iowear.ACTION_DISMISS";
    public final static String ACTION_START_FEEDBACK
            = "com.google.devrel.io.android.iowear.ACTION_FEEDBACK";
    public static final String PATH_FEEDBACK = "/iowear/feedback";
    public static final String PATH_RESPONSE = "/iowear/response";

    public final static int NOTIFICATION_ID = 10;
    private static int notificationCounter = 1;

    private GoogleApiClient mGoogleApiClient;
    private boolean mConnected = false;
    private final static long TIMEOUT_S = 10; // how long to wait for GoogleApi Client connection

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
        LOGD(TAG, "onStartCommand");
        if (null != intent) {
            String action = intent.getAction();
            if (ACTION_DISMISS.equals(action)) {
                String sessionId = intent.getStringExtra(KEY_SESSION_ID);
                LOGD(TAG, "onStartCommand(): Action: ACTION_DISMISS Session: " + sessionId);
                dismissPhoneNotification(sessionId);
            } else if (ACTION_START_FEEDBACK.equals(action)) {
                Intent feedbackIntent = new Intent(this, PagerActivity.class);
                String sessionId = intent.getStringExtra(HomeListenerService.KEY_SESSION_ID);
                LOGD(TAG, "Received session id from data layer: " + sessionId);
                feedbackIntent.putExtra(HomeListenerService.KEY_SESSION_ID, sessionId);
                feedbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(feedbackIntent);
                NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
            }
        }
        return Service.START_NOT_STICKY;
    }

    /**
     * Removes the Data Item that was used to create a notification on the watch. By deleting the
     * data item, a {@link com.google.android.gms.wearable.WearableListenerService} on the watch
     * will be notified and the notification on the watch will be removed.
     * <p/>
     * Since connection to the Google API client is asynchronous, we spawn a thread nd put it to
     * sleep waiting for the connection to be established before attempting to use the Google API
     * client.
     *
     * @param sessionId The Session ID` of the notification that should be removed
     */
    private void dismissPhoneNotification(final String sessionId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mConnected) {
                    mGoogleApiClient.blockingConnect(TIMEOUT_S, TimeUnit.SECONDS);
                }
                if (!mConnected) {
                    Log.e(TAG, "dismissPhoneNotification(): Received an interrupt exception");
                    return;
                }
                PutDataMapRequest putDataMapRequest = PutDataMapRequest
                        .create(getFeedbackPath(sessionId));
                LOGD(TAG, "dismissPhoneNotification(): Dismiss wearable notification");
                if (mGoogleApiClient.isConnected()) {
                    LOGD(TAG, "Deleting Uri is: " + putDataMapRequest.getUri().toString());
                    Wearable.DataApi.deleteDataItems(mGoogleApiClient, putDataMapRequest.getUri())
                            .setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                                @Override
                                public void onResult(
                                        DataApi.DeleteDataItemsResult deleteDataItemsResult) {
                                    if (!deleteDataItemsResult.getStatus().isSuccess()) {
                                        LOGD(TAG, "dismissPhoneNotification(): failed to delete"
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

    /**
     * Returns the path for a feedback with the given session.
     */
    private String getFeedbackPath(String sessionId) {
        return PATH_FEEDBACK + "/" + sessionId;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged: " + dataEvents + " for " + getPackageName());

        for (DataEvent event : dataEvents) {
            Uri uri = event.getDataItem().getUri();
            LOGD(TAG, "onDataChanged(): Received a data item change with uri: " + uri.toString());
            if (event.getType() == DataEvent.TYPE_DELETED) {
                if (uri.getPath().startsWith(PATH_FEEDBACK)) {
                    dismissLocalNotification(uri.getLastPathSegment());
                }
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                if (uri.getPath().startsWith(PATH_FEEDBACK)) {
                    setupNotification(event.getDataItem());
                }
            }
        }
    }

    /**
     * Builds notification for wear based on the data in the Data Item that is passed in.
     */
    private void setupNotification(DataItem dataItem) {
        LOGD(TAG, "setupNotification(): DataItem=" + dataItem.getUri());
        PutDataMapRequest putDataMapRequest = PutDataMapRequest
                .createFromDataMapItem(DataMapItem.fromDataItem(dataItem));
        final DataMap dataMap = putDataMapRequest.getDataMap();
        String sessionId = dataMap.getString(KEY_SESSION_ID);
        String sessionRoom = dataMap.getString(KEY_SESSION_ROOM);
        String sessionName = dataMap.getString(KEY_SESSION_NAME);
        String speakers = dataMap.getString(KEY_SPEAKER_NAME);

        int notificationId = (int) new Date().getTime();
        Intent intent = new Intent(ACTION_DISMISS);
        intent.putExtra(KEY_SESSION_ID, dataMap.getString(KEY_SESSION_ID));

        // need to be notified if user dismisses teh notification so we can dismiss the
        // corresponding notification on teh paired handset.
        PendingIntent deleteIntent = PendingIntent
                .getService(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent showCardIntent = showCardIntent(sessionId, sessionRoom, sessionName,
                speakers, notificationId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.rate_this_session))
                .setDeleteIntent(deleteIntent)
                .setContentText(sessionName)
                .extend(new NotificationCompat.WearableExtender()
                        .setDisplayIntent(showCardIntent));

        NotificationManagerCompat.from(this)
                .notify(sessionId, NOTIFICATION_ID, builder.build());
    }

    private PendingIntent showCardIntent(String sessionId, String sessionRoom,
            String sessionName, String speakers, int notificationId) {
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.putExtra(KEY_SESSION_ID, sessionId);
        intent.putExtra(KEY_SESSION_ROOM, sessionRoom);
        intent.putExtra(KEY_SESSION_NAME, sessionName);
        intent.putExtra(KEY_SPEAKER_NAME, speakers);
        intent.putExtra(KEY_NOTIFICATION_ID, notificationId);
        return PendingIntent.getActivity(this, notificationCounter++, intent, 0);
    }

    /**
     * Dismisses the local notification for the given session id
     */
    private void dismissLocalNotification(String sessionId) {
        Log.v(TAG, "dismissLocalNotification: sessionId=" + sessionId);

        NotificationManagerCompat.from(this)
                .cancel(sessionId, HomeListenerService.NOTIFICATION_ID);
    }

    @Override
    public void onConnected(Bundle bundle) {
        LOGD(TAG, "Connected to Google API Client");
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
