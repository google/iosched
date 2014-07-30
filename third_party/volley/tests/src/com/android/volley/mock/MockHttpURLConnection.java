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

package com.android.volley.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MockHttpURLConnection extends HttpURLConnection {

    private boolean mDoOutput;
    private String mRequestMethod;
    private OutputStream mOutputStream;

    public MockHttpURLConnection() throws MalformedURLException {
        super(new URL("http://foo.com"));
        mDoOutput = false;
        mRequestMethod = "GET";
        mOutputStream = new ByteArrayOutputStream();
    }

    @Override
    public void setDoOutput(boolean flag) {
        mDoOutput = flag;
    }

    @Override
    public boolean getDoOutput() {
        return mDoOutput;
    }

    @Override
    public void setRequestMethod(String method) {
        mRequestMethod = method;
    }

    @Override
    public String getRequestMethod() {
        return mRequestMethod;
    }

    @Override
    public OutputStream getOutputStream() {
        return mOutputStream;
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() throws IOException {
    }

}
