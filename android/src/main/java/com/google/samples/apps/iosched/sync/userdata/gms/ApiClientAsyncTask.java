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
package com.google.samples.apps.iosched.sync.userdata.gms;

import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.CountDownLatch;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * An AsyncTask that maintains a connected client.
 */
public abstract class ApiClientAsyncTask<Params, Progress, Result>
        extends AsyncTask<Params, Progress, Result> {

    private static final String TAG = makeLogTag(ApiClientAsyncTask.class);
    private static final int REQUEST_CODE_RESOLUTION = 1;

    private GoogleApiClient mClient;
    private Context mContext;
    private String lastUsedAccountName;

    public ApiClientAsyncTask(Context context) {
        this.mContext = context;
    }

    @Override
    protected final Result doInBackground(Params... params) {
        Log.d(TAG, "doInBackground of ApiClientAsyncTask");

        getGoogleApiClient();

        final CountDownLatch latch = new CountDownLatch(1);
        mClient.registerConnectionCallbacks(new ConnectionCallbacks() {
            @Override
            public void onConnectionSuspended(int cause) {
            }

            @Override
            public void onConnected(Bundle arg0) {
                Log.d(TAG, "ApiClientAsyncTask onConnected");
                latch.countDown();
            }
        });
        mClient.registerConnectionFailedListener(new OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult result) {
                Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
                if (!result.hasResolution()) {
                    // show the localized error dialog.
                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                            (Activity) ApiClientAsyncTask.this.getContext(), 0).show();
                    return;
                }
                try {
                    result.startResolutionForResult((Activity) ApiClientAsyncTask.this.getContext(), REQUEST_CODE_RESOLUTION);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Exception while starting resolution activity", e);
                }
                latch.countDown();
            }
        });
        mClient.connect();
        try {
            latch.await();
        } catch (InterruptedException e) {
            return null;
        }
        if (!mClient.isConnected()) {
            return null;
        }
        try {
            return doInBackgroundConnected(params);
        } catch (RuntimeException e) {
            Log.e(TAG, "ApiClientAsyncTask exception on doInBackgroundConnected!", e);
            throw e;
        } finally {
            mClient.disconnect();
        }
    }

    /**
     * Override this method to perform a computation on a background thread, while the client is
     * connected.
     */
    protected abstract Result doInBackgroundConnected(Params... params);

    /**
     * Gets the GoogleApliClient owned by this async task.
     */
    protected GoogleApiClient getGoogleApiClient() {
        String currentAccountName = AccountUtils.getActiveAccountName(mContext);
        if (lastUsedAccountName != null &&
                !lastUsedAccountName.equals(currentAccountName)) {
            if (mClient != null && mClient.isConnected()) {
                mClient.disconnect();
            }
            mClient = null;
            lastUsedAccountName = currentAccountName;
        }
        if (mClient == null) {
            GoogleApiClient.Builder builder = new GoogleApiClient.Builder(mContext)
                    .addApi(Drive.API)
                    .setAccountName(currentAccountName)
                    .addScope(Drive.SCOPE_APPFOLDER);
            mClient = builder.build();
        }
        mClient.connect();
        return mClient;
    }

    public Context getContext() {
        return mContext;
    }
}