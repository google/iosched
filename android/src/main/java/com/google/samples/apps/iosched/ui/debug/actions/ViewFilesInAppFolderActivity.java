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
package com.google.samples.apps.iosched.ui.debug.actions;

import android.os.Bundle;
import android.widget.TextView;

import com.google.samples.apps.iosched.sync.userdata.gms.ApiClientAsyncTask;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.android.gms.drive.*;
import com.google.api.client.util.Charsets;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 *
 * Simple debug activity that lists all files currently in Drive AppFolder and their contents
 *
 */
public class ViewFilesInAppFolderActivity extends BaseActivity {

    private static final String TAG = makeLogTag(ViewFilesInAppFolderActivity.class);

    private TextView mLogArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogArea = new TextView(this);

        ApiClientAsyncTask<Void, Void, String> task = new ApiClientAsyncTask<Void, Void, String>(this) {
            @Override
            protected String doInBackgroundConnected(Void[] params) {
                StringBuffer result = new StringBuffer();
                MetadataBuffer buffer = Drive.DriveApi.getAppFolder(getGoogleApiClient())
                        .listChildren(getGoogleApiClient()).await().getMetadataBuffer();

                result.append("found " + buffer.getCount() + " files:\n");
                for (Metadata m: buffer) {
                    DriveId id = m.getDriveId();
                    DriveFile file = Drive.DriveApi.getFile(getGoogleApiClient(), id);

                    Contents contents = file.openContents( getGoogleApiClient(),
                            DriveFile.MODE_READ_ONLY, null).await().getContents();

                    FileInputStream is = new FileInputStream(contents.getParcelFileDescriptor().getFileDescriptor());
                    try {
                        BufferedReader bf = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
                        String line=null; StringBuffer sb=new StringBuffer();
                        while ((line=bf.readLine()) != null ) {
                            sb.append(line);
                        }
                        file.discardContents(getGoogleApiClient(), contents);
                        result.append("*** " + m.getTitle() + "/" + id + "/" + m.getFileSize() + "B:\n   [" + sb.toString() + "]\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                buffer.close();
                return result.toString();
            }

            @Override
            protected void onPostExecute(String s) {
                if (mLogArea != null) {
                    mLogArea.append(s);
                }
            }
        };
        task.execute();

    }

}
