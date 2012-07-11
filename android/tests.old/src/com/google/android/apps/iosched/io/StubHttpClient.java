/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.iosched.io;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Stub {@link HttpClient} that will provide a single {@link HttpResponse} to
 * any incoming {@link HttpRequest}. This single response can be set using
 * {@link #setResponse(HttpResponse)}.
 */
class StubHttpClient extends DefaultHttpClient {
    private static final String TAG = "StubHttpClient";

    private HttpResponse mResponse;
    private HttpRequest mLastRequest;

    public StubHttpClient() {
        resetState();
    }

    /**
     * Set the default {@link HttpResponse} to always return for any
     * {@link #execute(HttpUriRequest)} calls.
     */
    public void setResponse(HttpResponse currentResponse) {
        mResponse = currentResponse;
    }

    /**
     * Set the default {@link HttpResponse} to always return for any
     * {@link #execute(HttpUriRequest)} calls. This is a shortcut instead of
     * calling {@link #buildResponse(int, String, AndroidTestCase)}.
     */
    public void setResponse(int statusCode, String assetName, AndroidTestCase testCase)
            throws Exception {
        setResponse(buildResponse(statusCode, assetName, testCase));
    }

    /**
     * Return the last {@link HttpRequest} that was requested through
     * {@link #execute(HttpUriRequest)}, exposed for testing purposes.
     */
    public HttpRequest getLastRequest() {
        return mLastRequest;
    }

    /**
     * Reset any internal state, usually so this heavy {@link HttpClient} can be
     * reused across tests.
     */
    public void resetState() {
        mResponse = buildInternalServerError();
    }

    private static HttpResponse buildInternalServerError() {
        final StatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_INTERNAL_SERVER_ERROR, null);
        return new BasicHttpResponse(status);
    }

    /**
     * Build a stub {@link HttpResponse}, probably for use with
     * {@link #setResponse(HttpResponse)}.
     *
     * @param statusCode {@link HttpStatus} code to use.
     * @param assetName Name of asset that should be included as a single
     *            {@link HttpEntity} to be included in the response, loaded
     *            through {@link AssetManager}.
     */
    public static HttpResponse buildResponse(int statusCode, String assetName,
            AndroidTestCase testCase) throws Exception {
        final Context testContext = getTestContext(testCase);
        return buildResponse(statusCode, assetName, testContext);
    }

    /**
     * Exposes method {@code getTestContext()} in {@link AndroidTestCase}, which
     * is hidden for now. Useful for obtaining access to the test assets.
     */
    public static Context getTestContext(AndroidTestCase testCase) throws IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        return (Context) AndroidTestCase.class.getMethod("getTestContext").invoke(testCase);
    }

    /**
     * Build a stub {@link HttpResponse}, probably for use with
     * {@link #setResponse(HttpResponse)}.
     *
     * @param statusCode {@link HttpStatus} code to use.
     * @param assetName Name of asset that should be included as a single
     *            {@link HttpEntity} to be included in the response, loaded
     *            through {@link AssetManager}.
     */
    public static HttpResponse buildResponse(int statusCode, String assetName, Context context)
            throws IOException {
        final StatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, null);
        final HttpResponse response = new BasicHttpResponse(status);
        if (assetName != null) {
            final InputStream entity = context.getAssets().open(assetName);
            response.setEntity(new InputStreamEntity(entity, entity.available()));
        }
        return response;
    }

    /** {@inheritDoc} */
    @Override
    protected RequestDirector createClientRequestDirector(
            final HttpRequestExecutor requestExec,
            final ClientConnectionManager conman,
            final ConnectionReuseStrategy reustrat,
            final ConnectionKeepAliveStrategy kastrat,
            final HttpRoutePlanner rouplan,
            final HttpProcessor httpProcessor,
            final HttpRequestRetryHandler retryHandler,
            final RedirectHandler redirectHandler,
            final AuthenticationHandler targetAuthHandler,
            final AuthenticationHandler proxyAuthHandler,
            final UserTokenHandler stateHandler,
            final HttpParams params) {
        return new RequestDirector() {
            /** {@inheritDoc} */
            public HttpResponse execute(HttpHost target, HttpRequest request,
                    HttpContext context) throws HttpException, IOException {
                Log.d(TAG, "Intercepted: " + request.getRequestLine().toString());
                mLastRequest = request;
                return mResponse;
            }
        };
    }
}
