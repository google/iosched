/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.volley.toolbox;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.mock.MockHttpStack;

import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.HashMap;
import java.util.Map;

@SmallTest
public class BasicNetworkTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
    }

    public void testHeadersAndPostParams() throws Exception {
        MockHttpStack mockHttpStack = new MockHttpStack();
        BasicHttpResponse fakeResponse = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1),
                200, "OK");
        fakeResponse.setEntity(new StringEntity("foobar"));
        mockHttpStack.setResponseToReturn(fakeResponse);
        BasicNetwork httpNetwork = new BasicNetwork(mockHttpStack);
        Request<String> request = new Request<String>(Request.Method.GET, "http://foo", null) {

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                return null;
            }

            @Override
            protected void deliverResponse(String response) {
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> result = new HashMap<String, String>();
                result.put("requestheader", "foo");
                return result;
            }

            @Override
            public Map<String, String> getParams() {
                Map<String, String> result = new HashMap<String, String>();
                result.put("requestpost", "foo");
                return result;
            }
        };
        httpNetwork.performRequest(request);
        assertEquals("foo", mockHttpStack.getLastHeaders().get("requestheader"));
        assertEquals("requestpost=foo&", new String(mockHttpStack.getLastPostBody()));
    }
}
