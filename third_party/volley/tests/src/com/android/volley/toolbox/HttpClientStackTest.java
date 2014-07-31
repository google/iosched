/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.volley.Request.Method;
import com.android.volley.mock.TestRequest;
import com.android.volley.toolbox.HttpClientStack.HttpPatch;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

@SmallTest
public class HttpClientStackTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
    }

    public void testCreateDeprecatedGetRequest() throws Exception {
        TestRequest.DeprecatedGet request = new TestRequest.DeprecatedGet();
        assertEquals(request.getMethod(), Method.DEPRECATED_GET_OR_POST);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpGet);
    }

    public void testCreateDeprecatedPostRequest() throws Exception {
        TestRequest.DeprecatedPost request = new TestRequest.DeprecatedPost();
        assertEquals(request.getMethod(), Method.DEPRECATED_GET_OR_POST);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpPost);
    }

    public void testCreateGetRequest() throws Exception {
        TestRequest.Get request = new TestRequest.Get();
        assertEquals(request.getMethod(), Method.GET);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpGet);
    }

    public void testCreatePostRequest() throws Exception {
        TestRequest.Post request = new TestRequest.Post();
        assertEquals(request.getMethod(), Method.POST);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpPost);
    }

    public void testCreatePostRequestWithBody() throws Exception {
        TestRequest.PostWithBody request = new TestRequest.PostWithBody();
        assertEquals(request.getMethod(), Method.POST);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpPost);
    }

    public void testCreatePutRequest() throws Exception {
        TestRequest.Put request = new TestRequest.Put();
        assertEquals(request.getMethod(), Method.PUT);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpPut);
    }

    public void testCreatePutRequestWithBody() throws Exception {
        TestRequest.PutWithBody request = new TestRequest.PutWithBody();
        assertEquals(request.getMethod(), Method.PUT);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpPut);
    }

    public void testCreateDeleteRequest() throws Exception {
        TestRequest.Delete request = new TestRequest.Delete();
        assertEquals(request.getMethod(), Method.DELETE);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpDelete);
    }

    public void testCreateHeadRequest() throws Exception {
        TestRequest.Head request = new TestRequest.Head();
        assertEquals(request.getMethod(), Method.HEAD);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpHead);
    }

    public void testCreateOptionsRequest() throws Exception {
        TestRequest.Options request = new TestRequest.Options();
        assertEquals(request.getMethod(), Method.OPTIONS);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpOptions);
    }

    public void testCreateTraceRequest() throws Exception {
        TestRequest.Trace request = new TestRequest.Trace();
        assertEquals(request.getMethod(), Method.TRACE);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpTrace);
    }

    public void testCreatePatchRequest() throws Exception {
        TestRequest.Patch request = new TestRequest.Patch();
        assertEquals(request.getMethod(), Method.PATCH);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpPatch);
    }

    public void testCreatePatchRequestWithBody() throws Exception {
        TestRequest.PatchWithBody request = new TestRequest.PatchWithBody();
        assertEquals(request.getMethod(), Method.PATCH);

        HttpUriRequest httpRequest = HttpClientStack.createHttpRequest(request, null);
        assertTrue(httpRequest instanceof HttpPatch);
    }
}
