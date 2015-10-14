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

import com.google.android.gms.wearable.DataMap;

import com.google.samples.apps.iosched.feedback.SessionFeedbackModel.SessionFeedbackData;
import com.google.samples.apps.iosched.util.LogUtils;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.test.suitebuilder.annotation.SmallTest;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class FeedbackHelperTest {

    private final static String DATA_MAP_KEY = "response";

    private final static String INVALID_JSON = "fef}";

    private final static String VALID_JSON_SESSION_ID = "sessionId-1234";

    private final static int VALID_JSON_SESSION_RATING = 0;

    private final static int VALID_JSON_Q1_ANSWER = 2;

    private final static int VALID_JSON_Q2_ANSWER = 1;

    private final static int VALID_JSON_Q3_ANSWER = 3;

    private final static String VALID_JSON = "[{\"s\":\"" + VALID_JSON_SESSION_ID + "\"},"
            + "{\"q\":1,\"a\":" + VALID_JSON_Q1_ANSWER + "},{\"q\":0,\"a\":"
            + VALID_JSON_SESSION_RATING +"},{\"q\":3,\"a\":" +VALID_JSON_Q3_ANSWER + "},"
            + "{\"q\":2,\"a\":"+ VALID_JSON_Q2_ANSWER + "}]";

    @Mock
    private DataMap mMockDataMap;

    @Before
    public void disableLogging(){
        LogUtils.LOGGING_ENABLED = false;
    }

    @Test
    @Ignore("Will be fixed with build tools 1.3")
    public void convertDataMapToFeedbackData_ValidData_ReturnsCorrectObject() throws JSONException {
        // This test fails because of testCompile org.json is ignored. This will be fixed in 1.3.
        // Given a mock DataMap with valid JSON
        when(mMockDataMap.getString(DATA_MAP_KEY)).thenReturn(VALID_JSON);

        // When run with invalid JSON
        SessionFeedbackData data = FeedbackHelper.convertDataMapToFeedbackData(mMockDataMap);

        // Then returned data matches JSON data
        assertThat(data.sessionId, is(VALID_JSON_SESSION_ID));
        assertThat(data.sessionRating, is(VALID_JSON_SESSION_RATING));
        assertThat(data.sessionRelevantAnswer, is(VALID_JSON_Q1_ANSWER));
        assertThat(data.contentAnswer, is(VALID_JSON_Q2_ANSWER));
        assertThat(data.speakerAnswer, is(VALID_JSON_Q3_ANSWER));
        assertThat(data.comments, is(nullValue()));
    }

    @Test
    public void convertDataMapToFeedbackData_InvalidJSON_ReturnsNullObject() {
        // Given a mock DataMap with invalid JSON
        when(mMockDataMap.getString(DATA_MAP_KEY)).thenReturn(INVALID_JSON);

        // When run with invalid JSON
        SessionFeedbackData data = FeedbackHelper.convertDataMapToFeedbackData(mMockDataMap);

        // Then null data is returned
        assertThat(data, is(nullValue()));
    }

    @Test
    public void convertDataMapToFeedbackData_NullData_ReturnsNullObject() {
        // When run with null data
        SessionFeedbackData data = FeedbackHelper.convertDataMapToFeedbackData(null);

        // Then null data is returned
        assertThat(data, is(nullValue()));
    }
}
