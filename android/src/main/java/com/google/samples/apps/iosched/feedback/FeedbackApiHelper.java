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

import com.google.samples.apps.iosched.BuildConfig;
import com.turbomanage.httpclient.BasicHttpClient;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * Sends feedback data to the server Feedback API.
 */
public class FeedbackApiHelper {

    private static final String TAG = makeLogTag(FeedbackApiHelper.class);
    
    private final String mUrl;

    private BasicHttpClient mHttpClient;

    public FeedbackApiHelper(BasicHttpClient httpClient, String url) {
        mHttpClient = httpClient;
        mUrl = url;
    }

    /**
     * Posts session feedback to the server. This method does network I/O and should run on
     * a background thread, do not call from the UI thread.
     *
     * @param sessionId The ID of the session that was reviewed.
     * @param questions
     * @return true if successful.
     */
    public boolean sendSessionToServer(String sessionId, HashMap<String, String> questions) {
        checkState(sessionId != null && !sessionId.isEmpty() && questions != null
                && questions.size() > 0, "Error posting session: some of the data is"
                + " invalid. SessionId " + sessionId + " Questions: " + questions);

        // TODO: Implement custom survey handling code here
        LOGE(TAG, "Survey handler not implemented!");
        return true;
    }

}
