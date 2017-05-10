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

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.samples.apps.iosched.rpc.feedback.Feedback;
import com.google.samples.apps.iosched.rpc.feedback.model.Rating;
import com.google.samples.apps.iosched.rpc.feedback.model.SessionFeedback;
import com.turbomanage.httpclient.BasicHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;
import static com.google.samples.apps.iosched.util.PreconditionUtils.checkState;

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
     * @param questions Map where the keys are question codes and values are user responses.
     * @return true if successful.
     */
    boolean sendSessionToServer(String sessionId, final HashMap<Integer, Integer> questions) {
        checkState(sessionId != null && !sessionId.isEmpty() && questions != null
                && questions.size() > 0, "Error posting session: some of the data is"
                + " invalid. SessionId " + sessionId + " Questions: " + questions);

        Feedback feedbackHandler = new Feedback.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), null).build();

        SessionFeedback sessionFeedback = new SessionFeedback();
        sessionFeedback.setSessionId(sessionId);

        ArrayList<Rating> ratings = new ArrayList<>();

        for (Integer key: questions.keySet()) {
            Rating rating = new Rating();
            rating.setQuestion(key);
            rating.setAnswer(questions.get(key));
            ratings.add(rating);
        }

        sessionFeedback.setRatings(ratings);

        try {
            feedbackHandler.sendSessionFeedback(sessionFeedback).execute();
        } catch (IOException e) {
            LOGW(TAG, "Could not submit session feedback: " + e);
            return false;
        }
        return true;
    }
}
