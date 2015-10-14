/*
 * Copyright 2015 Google Inc. All rights reserved.
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

import com.google.samples.apps.iosched.util.LogUtils;

import com.turbomanage.httpclient.BasicHttpClient;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.test.suitebuilder.annotation.SmallTest;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class FeedbackApiHelperTest {

    private static final String FAKE_URL = "my/fake/url";

    private static final String FAKE_SESSION_ID = "123";

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Mock
    private BasicHttpClient mMockHttpClient;

    @Mock
    private HttpResponse mMockHttpResponse;

    @Mock
    private ParameterMap mMockParameterMap;

    private FeedbackApiHelper mFeedbackApiHelper;

    @Before
    public void createFeedbackHelper() {
        mFeedbackApiHelper = new FeedbackApiHelper(mMockHttpClient, FAKE_URL);
    }

    @Before
    public void disableLogging() {
        LogUtils.LOGGING_ENABLED = false;
    }

    @Test
    public void sendSessionToServer_SuccessfulConnection_ReturnsTrue() {
        // Given a list of questions and successful http connection
        HashMap<String, String> questions = new HashMap<String, String>();
        questions.put("Q10", "b2ce21ff-2cbe-e411-b87f-00155d5066d7");
        questions.put("Q20", "9fce21ff-2cbe-e411-b87f-00155d5066d7");
        questions.put("Q30", "a5ce21ff-2cbe-e411-b87f-00155d5066d7");
        questions.put("Q40", "acce21ff-2cbe-e411-b87f-00155d5066d7");

        initWithStubbedSuccessfulConnection();

        // When ran with a valid session id
        boolean success = mFeedbackApiHelper.sendSessionToServer(FAKE_SESSION_ID, questions);

        // Then true is returned
        assertTrue(success);
    }

    @Test
    public void sendSessionToServer_NullSessionId_ThrowsISE() {
        // Expected
        mThrown.expect(IllegalStateException.class);

        // When ran with a null session id
        boolean success = mFeedbackApiHelper.sendSessionToServer(null, new HashMap<String, String>());

        // Then ISE is thrown
    }

    @Test
    public void sendSessionToServer_EmptySessionId_ThrowsISE() {
        // Expected
        mThrown.expect(IllegalStateException.class);

        // When ran with an empty session id
        boolean success = mFeedbackApiHelper.sendSessionToServer("", new HashMap<String, String>());

        // Then ISE is thrown
    }

    @Test
    public void sendSessionToServer_NullQuestions_ThrowsISE() {
        // Expected
        mThrown.expect(IllegalStateException.class);

        // When ran with a valid session id and null list of questions
        boolean success = mFeedbackApiHelper.sendSessionToServer(FAKE_SESSION_ID, null);

        // Then ISE is thrown
    }

    @Test
    public void sendSessionToServer_ZeroQuestions_ThrowsISE() {
        // Expected
        mThrown.expect(IllegalStateException.class);

        // When ran with a valid session id and empty list of questions
        boolean success = mFeedbackApiHelper.sendSessionToServer(FAKE_SESSION_ID,
                new HashMap<String, String>());

        // Then ISE is thrown
    }

    private void initWithStubbedSuccessfulConnection(){
        when(mMockHttpClient.addHeader(anyString(), anyString())).thenReturn(mMockHttpClient);
        when(mMockHttpClient.newParams()).thenReturn(mMockParameterMap);
        when(mMockParameterMap.add(anyString(),anyString())).thenReturn(mMockParameterMap);
        when(mMockHttpClient.get(eq(FAKE_URL), any(ParameterMap.class))).thenReturn(
                mMockHttpResponse);
        when(mMockHttpResponse.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
    }

    private void initWithStubbedUnsuccessfulConnection(){
        when(mMockHttpClient.addHeader(anyString(), anyString())).thenReturn(mMockHttpClient);
        when(mMockHttpClient.newParams()).thenReturn(mMockParameterMap);
        when(mMockParameterMap.add(anyString(),anyString())).thenReturn(mMockParameterMap);
        when(mMockHttpClient.get(eq(FAKE_URL), any(ParameterMap.class))).thenReturn(null);
    }

}
