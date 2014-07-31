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

import android.test.suitebuilder.annotation.SmallTest;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

@SmallTest
public class HttpHeaderParserTest extends TestCase {

    private static long ONE_MINUTE_MILLIS = 1000L * 60;
    private static long ONE_HOUR_MILLIS = 1000L * 60 * 60;

    private NetworkResponse response;
    private Map<String, String> headers;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        headers = new HashMap<String, String>();
        response = new NetworkResponse(0, null, headers, false);
    }

    public void testParseCacheHeaders_noHeaders() {
        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);

        assertNotNull(entry);
        assertNull(entry.etag);
        assertEquals(0, entry.serverDate);
        assertEquals(0, entry.ttl);
        assertEquals(0, entry.softTtl);
    }

    public void testParseCacheHeaders_headersSet() {
        headers.put("MyCustomHeader", "42");

        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);

        assertNotNull(entry);
        assertNotNull(entry.responseHeaders);
        assertEquals(1, entry.responseHeaders.size());
        assertEquals("42", entry.responseHeaders.get("MyCustomHeader"));
    }

    public void testParseCacheHeaders_etag() {
        headers.put("ETag", "Yow!");

        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);

        assertNotNull(entry);
        assertEquals("Yow!", entry.etag);
    }

    public void testParseCacheHeaders_normalExpire() {
        long now = System.currentTimeMillis();
        headers.put("Date", rfc1123Date(now));
        headers.put("Expires", rfc1123Date(now + ONE_HOUR_MILLIS));

        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);

        assertNotNull(entry);
        assertNull(entry.etag);
        assertEqualsWithin(entry.serverDate, now, ONE_MINUTE_MILLIS);
        assertTrue(entry.softTtl >= (now + ONE_HOUR_MILLIS));
        assertTrue(entry.ttl == entry.softTtl);
    }

    public void testParseCacheHeaders_expiresInPast() {
        long now = System.currentTimeMillis();
        headers.put("Date", rfc1123Date(now));
        headers.put("Expires", rfc1123Date(now - ONE_HOUR_MILLIS));

        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);

        assertNotNull(entry);
        assertNull(entry.etag);
        assertEqualsWithin(entry.serverDate, now, ONE_MINUTE_MILLIS);
        assertEquals(0, entry.ttl);
        assertEquals(0, entry.softTtl);
    }

    public void testParseCacheHeaders_serverRelative() {

        long now = System.currentTimeMillis();
        // Set "current" date as one hour in the future
        headers.put("Date", rfc1123Date(now + ONE_HOUR_MILLIS));
        // TTL four hours in the future, so should be three hours from now
        headers.put("Expires", rfc1123Date(now + 4 * ONE_HOUR_MILLIS));

        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);

        assertEqualsWithin(now + 3 * ONE_HOUR_MILLIS, entry.ttl, ONE_MINUTE_MILLIS);
        assertEquals(entry.softTtl, entry.ttl);
    }

    public void testParseCacheHeaders_cacheControlOverridesExpires() {
        long now = System.currentTimeMillis();
        headers.put("Date", rfc1123Date(now));
        headers.put("Expires", rfc1123Date(now + ONE_HOUR_MILLIS));
        headers.put("Cache-Control", "public, max-age=86400");

        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);

        assertNotNull(entry);
        assertNull(entry.etag);
        assertEqualsWithin(now + 24 * ONE_HOUR_MILLIS, entry.ttl, ONE_MINUTE_MILLIS);
        assertEquals(entry.softTtl, entry.ttl);
    }

    public void testParseCacheHeaders_cacheControlNoCache() {
        long now = System.currentTimeMillis();
        headers.put("Date", rfc1123Date(now));
        headers.put("Expires", rfc1123Date(now + ONE_HOUR_MILLIS));
        headers.put("Cache-Control", "no-cache");

        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);

        assertNull(entry);
    }

    public void testParseCacheHeaders_cacheControlMustRevalidate() {
        long now = System.currentTimeMillis();
        headers.put("Date", rfc1123Date(now));
        headers.put("Expires", rfc1123Date(now + ONE_HOUR_MILLIS));
        headers.put("Cache-Control", "must-revalidate");

        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);

        assertNotNull(entry);
        assertNull(entry.etag);
        assertEqualsWithin(now, entry.ttl, ONE_MINUTE_MILLIS);
        assertEquals(entry.softTtl, entry.ttl);
    }

    private void assertEqualsWithin(long expected, long value, long fudgeFactor) {
        long diff = Math.abs(expected - value);
        assertTrue(diff < fudgeFactor);
    }

    private static String rfc1123Date(long millis) {
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        return df.format(new Date(millis));
    }

    // --------------------------

    public void testParseCharset() {
        // Like the ones we usually see
        headers.put("Content-Type", "text/plain; charset=utf-8");
        assertEquals("utf-8", HttpHeaderParser.parseCharset(headers));

        // Extra whitespace
        headers.put("Content-Type", "text/plain;    charset=utf-8 ");
        assertEquals("utf-8", HttpHeaderParser.parseCharset(headers));

        // Extra parameters
        headers.put("Content-Type", "text/plain; charset=utf-8; frozzle=bar");
        assertEquals("utf-8", HttpHeaderParser.parseCharset(headers));

        // No Content-Type header
        headers.clear();
        assertEquals("ISO-8859-1", HttpHeaderParser.parseCharset(headers));

        // Empty value
        headers.put("Content-Type", "text/plain; charset=");
        assertEquals("ISO-8859-1", HttpHeaderParser.parseCharset(headers));

        // None specified
        headers.put("Content-Type", "text/plain");
        assertEquals("ISO-8859-1", HttpHeaderParser.parseCharset(headers));

        // None specified, extra semicolon
        headers.put("Content-Type", "text/plain;");
        assertEquals("ISO-8859-1", HttpHeaderParser.parseCharset(headers));
    }
}
