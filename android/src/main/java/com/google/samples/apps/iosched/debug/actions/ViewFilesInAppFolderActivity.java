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
package com.google.samples.apps.iosched.debug.actions;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.sync.userdata.gms.DriveHelper;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.io.IOException;

import static com.google.android.gms.common.api.GoogleApiClient.*;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Simple debug activity that lists all files currently in Google Drive AppFolder only
 * with their content.
 */
public class ViewFilesInAppFolderActivity extends BaseActivity
        implements ConnectionCallbacks, OnConnectionFailedListener {

    private static final String TAG = makeLogTag(ViewFilesInAppFolderActivity.class);
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1001;
    private static final String EOL = "\n";

    private TextView mLogArea;
    private ProgressDialog mProgressDialog;

    private GoogleApiClient mGoogleApiClient;
    private FetchDataTask mFetchDataTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_action_showdrivefiles);
        mLogArea = (TextView) findViewById(R.id.logArea);

        mProgressDialog = new ProgressDialog(this);
        mGoogleApiClient = new Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .setAccountName(AccountUtils.getActiveAccountName(this))
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mFetchDataTask != null) {
            mFetchDataTask.cancel(true);
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mFetchDataTask != null) {
            mFetchDataTask.cancel(true);
        }
        mFetchDataTask = new FetchDataTask();
        mFetchDataTask.execute();
    }

    @Override
    public void onConnectionSuspended(int status) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    private class FetchDataTask extends AsyncTask<Void, Void, String> {

        protected void onPreExecute() {
            if (!mProgressDialog.isShowing()) {
                mProgressDialog.show();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            DriveHelper helper = new DriveHelper(mGoogleApiClient);
            StringBuilder result = new StringBuilder();
            MetadataBuffer buffer = Drive.DriveApi.getAppFolder(mGoogleApiClient)
                    .listChildren(mGoogleApiClient).await().getMetadataBuffer();
            try {
                result.append("found ").append(buffer.getCount())
                        .append(" file(s):").append(EOL)
                        .append("----------").append(EOL);

                for (Metadata m : buffer) {
                    DriveId id = m.getDriveId();
                    result.append("Name: ").append(m.getTitle()).append(EOL);
                    result.append("MimeType: ").append(m.getMimeType()).append(EOL);
                    result.append(id.encodeToString()).append(EOL);
                    result.append("LastModified: ").append(m.getModifiedDate().getTime())
                            .append(EOL);
                    String content = helper.getContentsFromDrive(id);
                    result.append("--------").append(EOL)
                            .append(content).append(EOL);
                    result.append("--------");
                }
            } catch (IOException io) {
                result.append("Exception fetching content").append(EOL);
            } finally {
                buffer.close();
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String content) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mLogArea.setText(content);
        }
    }
}
