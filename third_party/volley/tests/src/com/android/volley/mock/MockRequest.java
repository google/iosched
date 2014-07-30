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

package com.android.volley.mock;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.utils.CacheTestUtils;

import java.util.HashMap;
import java.util.Map;

public class MockRequest extends Request<byte[]> {
    public MockRequest() {
        super(Request.Method.GET, "http://foo.com", null);
    }

    public MockRequest(String url, ErrorListener listener) {
        super(Request.Method.GET, url, listener);
    }

    private Map<String, String> mPostParams = new HashMap<String, String>();

    public void setPostParams(Map<String, String> postParams) {
        mPostParams = postParams;
    }

    @Override
    public Map<String, String> getPostParams() {
        return mPostParams;
    }

    private String mCacheKey = super.getCacheKey();

    public void setCacheKey(String cacheKey) {
        mCacheKey = cacheKey;
    }

    @Override
    public String getCacheKey() {
        return mCacheKey;
    }

    public boolean deliverResponse_called = false;
    public boolean parseResponse_called = false;

    @Override
    protected void deliverResponse(byte[] response) {
        deliverResponse_called = true;
    }

    public boolean deliverError_called = false;

    @Override
    public void deliverError(VolleyError error) {
        super.deliverError(error);
        deliverError_called = true;
    }

    public boolean cancel_called = false;

    @Override
    public void cancel() {
        cancel_called = true;
        super.cancel();
    }

    private Priority mPriority = super.getPriority();

    public void setPriority(Priority priority) {
        mPriority = priority;
    }

    @Override
    public Priority getPriority() {
        return mPriority;
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        parseResponse_called = true;
        return Response.success(response.data, CacheTestUtils.makeRandomCacheEntry(response.data));
    }

}
