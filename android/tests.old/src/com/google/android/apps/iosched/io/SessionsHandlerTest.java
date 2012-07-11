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

import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleProvider;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.provider.ScheduleContract.Tracks;
import com.google.android.apps.iosched.util.ParserUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.ProviderTestCase2;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class SessionsHandlerTest extends ProviderTestCase2<ScheduleProvider>  {

    public SessionsHandlerTest() {
        super(ScheduleProvider.class, ScheduleContract.CONTENT_AUTHORITY);
    }

    public void testLocalHandler() throws Exception {
        parseBlocks();
        parseTracks();
        parseRooms();

        final ContentResolver resolver = getMockContentResolver();
        final XmlPullParser parser = openAssetParser("local-sessions.xml");
        new LocalSessionsHandler().parseAndApply(parser, resolver);

        Cursor cursor = resolver.query(Sessions.CONTENT_URI, null, null, null,
                Sessions.DEFAULT_SORT);
        try {
            assertEquals(2, cursor.getCount());

            assertTrue(cursor.moveToNext());
            assertEquals("writingreal-timegamesforandroidredux", getString(cursor,
                    Sessions.SESSION_ID));
            assertEquals("Writing real-time games for Android redux", getString(cursor,
                    Sessions.TITLE));
            assertEquals(6, getInt(cursor, Sessions.ROOM_ID));
            assertEquals(2, getInt(cursor, Sessions.ROOM_FLOOR));
            assertEquals(ParserUtils.BLOCK_TYPE_OFFICE_HOURS,
                    getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1274270400000L, getLong(cursor, Sessions.BLOCK_START));
            assertEquals(1274295600000L, getLong(cursor, Sessions.BLOCK_END));

            assertTrue(cursor.moveToNext());
            assertEquals("beginners-guide-android", getString(cursor, Sessions.SESSION_ID));
            assertEquals("A beginner's guide to Android", getString(cursor, Sessions.TITLE));
            assertEquals(4, getInt(cursor, Sessions.ROOM_ID));
            assertEquals(2, getInt(cursor, Sessions.ROOM_FLOOR));
            assertEquals(ParserUtils.BLOCK_TYPE_OFFICE_HOURS,
                    getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1274291100000L, getLong(cursor, Sessions.BLOCK_START));
            assertEquals(1274294700000L, getLong(cursor, Sessions.BLOCK_END));

        } finally {
            cursor.close();
        }

        final Uri sessionTracks = Sessions.buildTracksDirUri("beginners-guide-android");
        cursor = resolver.query(sessionTracks, null, null, null, Tracks.DEFAULT_SORT);
        try {
            assertEquals(1, cursor.getCount());

            assertTrue(cursor.moveToNext());
            assertEquals("beginners-guide-android", getString(cursor, Sessions.SESSION_ID));
            assertEquals("android", getString(cursor, Tracks.TRACK_ID));
            assertEquals(-14002535, getInt(cursor, Tracks.TRACK_COLOR));

        } finally {
            cursor.close();
        }
    }

    public void testRemoteHandler() throws Exception {
        parseTracks();
        parseRooms();

        final ContentResolver resolver = getMockContentResolver();
        final XmlPullParser parser = openAssetParser("remote-sessions1.xml");
        new RemoteSessionsHandler().parseAndApply(parser, resolver);

        Cursor cursor = resolver.query(Sessions.CONTENT_URI, null, null, null,
                Sessions.DEFAULT_SORT);
        try {
            assertEquals(6, cursor.getCount());

            assertTrue(cursor.moveToNext());
            assertEquals("beginners-guide-android", getString(cursor, Sessions.SESSION_ID));
            assertEquals(6, getInt(cursor, Sessions.ROOM_ID));
            assertEquals(2, getInt(cursor, Sessions.ROOM_FLOOR));
            assertEquals(ParserUtils.BLOCK_TYPE_SESSION, getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1274291100000L, getLong(cursor, Sessions.BLOCK_START));
            assertEquals(1274294700000L, getLong(cursor, Sessions.BLOCK_END));
            assertEquals("http://www.google.com/moderator/#15/e=68f0&t=68f0.45", getString(cursor,
                    Sessions.MODERATOR_URL));

            assertTrue(cursor.moveToNext());
            assertEquals("writing-real-time-games-android", getString(cursor, Sessions.SESSION_ID));
            assertEquals(ParserUtils.BLOCK_TYPE_SESSION, getString(cursor, Sessions.BLOCK_TYPE));

            assertTrue(cursor.moveToLast());
            assertEquals("developing-restful-android-apps", getString(cursor, Sessions.SESSION_ID));
            assertEquals(ParserUtils.BLOCK_TYPE_SESSION, getString(cursor, Sessions.BLOCK_TYPE));

        } finally {
            cursor.close();
        }

        final Uri sessionTracks = Sessions.buildTracksDirUri("android-ui-design-patterns");
        cursor = resolver.query(sessionTracks, null, null, null, Tracks.DEFAULT_SORT);
        try {
            assertEquals(2, cursor.getCount());

            assertTrue(cursor.moveToNext());
            assertEquals("android-ui-design-patterns", getString(cursor, Sessions.SESSION_ID));
            assertEquals("android", getString(cursor, Tracks.TRACK_ID));
            assertEquals(-14002535, getInt(cursor, Tracks.TRACK_COLOR));

            assertTrue(cursor.moveToNext());
            assertEquals("android-ui-design-patterns", getString(cursor, Sessions.SESSION_ID));
            assertEquals("enterprise", getString(cursor, Tracks.TRACK_ID));
            assertEquals(-15750145, getInt(cursor, Tracks.TRACK_COLOR));

        } finally {
            cursor.close();
        }
    }

    public void testLocalRemoteUpdate() throws Exception {
        parseBlocks();
        parseTracks();
        parseRooms();

        // first, insert session data from local source
        final ContentResolver resolver = getMockContentResolver();
        final XmlPullParser parser = openAssetParser("local-sessions.xml");
        new LocalSessionsHandler().parseAndApply(parser, resolver);

        // now, star one of the sessions
        final Uri sessionUri = Sessions.buildSessionUri("beginners-guide-android");
        final ContentValues values = new ContentValues();
        values.put(Sessions.STARRED, 1);
        resolver.update(sessionUri, values, null, null);

        Cursor cursor = resolver.query(Sessions.CONTENT_URI, null, null, null,
                Sessions.DEFAULT_SORT);
        try {
            assertEquals(2, cursor.getCount());

            assertTrue(cursor.moveToNext());
            assertEquals("writingreal-timegamesforandroidredux", getString(cursor,
                    Sessions.SESSION_ID));
            assertEquals(ParserUtils.BLOCK_TYPE_OFFICE_HOURS,
                    getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(0, getInt(cursor, Sessions.STARRED));
            assertEquals(0L, getLong(cursor, Sessions.UPDATED));

            // make sure session is starred
            assertTrue(cursor.moveToNext());
            assertEquals("beginners-guide-android", getString(cursor, Sessions.SESSION_ID));
            assertEquals(4, getInt(cursor, Sessions.ROOM_ID));
            assertEquals(2, getInt(cursor, Sessions.ROOM_FLOOR));
            assertEquals(ParserUtils.BLOCK_TYPE_OFFICE_HOURS,
                    getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1274291100000L, getLong(cursor, Sessions.BLOCK_START));
            assertEquals(1274294700000L, getLong(cursor, Sessions.BLOCK_END));
            assertEquals(1, getInt(cursor, Sessions.STARRED));
            assertEquals(0L, getLong(cursor, Sessions.UPDATED));

        } finally {
            cursor.close();
        }

        // second, perform remote sync to pull in updates
        final XmlPullParser parser1 = openAssetParser("remote-sessions1.xml");
        new RemoteSessionsHandler().parseAndApply(parser1, resolver);

        cursor = resolver.query(Sessions.CONTENT_URI, null, null, null, Sessions.DEFAULT_SORT);
        try {
            // six sessions from remote sync, plus one original
            assertEquals(7, cursor.getCount());

            // original local-only session is still hanging around
            assertTrue(cursor.moveToNext());
            assertEquals("writingreal-timegamesforandroidredux", getString(cursor,
                    Sessions.SESSION_ID));
            assertEquals(ParserUtils.BLOCK_TYPE_OFFICE_HOURS,
                    getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(0, getInt(cursor, Sessions.STARRED));
            assertEquals(0L, getLong(cursor, Sessions.UPDATED));

            // existing session was updated with remote values, and has star
            assertTrue(cursor.moveToNext());
            assertEquals("beginners-guide-android", getString(cursor, Sessions.SESSION_ID));
            assertEquals(1, getInt(cursor, Sessions.STARRED));
            assertEquals(ParserUtils.BLOCK_TYPE_SESSION, getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1273186984000L, getLong(cursor, Sessions.UPDATED));

            // make sure session block was updated from remote
            assertTrue(cursor.moveToNext());
            assertEquals("writing-real-time-games-android", getString(cursor, Sessions.SESSION_ID));
            assertEquals(6, getInt(cursor, Sessions.ROOM_ID));
            assertEquals(2, getInt(cursor, Sessions.ROOM_FLOOR));
            assertEquals(ParserUtils.BLOCK_TYPE_SESSION, getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1274297400000L, getLong(cursor, Sessions.BLOCK_START));
            assertEquals(1274301000000L, getLong(cursor, Sessions.BLOCK_END));
            assertEquals("This session is a crash course in Android game development: everything "
                    + "you need to know to get started writing 2D and 3D games, as well as tips, "
                    + "tricks, and benchmarks to help your code reach optimal performance.  In "
                    + "addition, we'll discuss hot topics related to game development, including "
                    + "hardware differences across devices, using C++ to write Android games, "
                    + "and the traits of the most popular games on Market.", getString(cursor,
                    Sessions.ABSTRACT));
            assertEquals("Proficiency in Java and a solid grasp of Android's fundamental concepts",
                    getString(cursor, Sessions.REQUIREMENTS));
            assertEquals("http://www.google.com/moderator/#15/e=68f0&t=68f0.9B", getString(cursor,
                    Sessions.MODERATOR_URL));
            assertEquals(0, getInt(cursor, Sessions.STARRED));
            assertEquals(1273186984000L, getLong(cursor, Sessions.UPDATED));

            assertTrue(cursor.moveToLast());
            assertEquals("developing-restful-android-apps", getString(cursor, Sessions.SESSION_ID));
            assertEquals("301", getString(cursor, Sessions.TYPE));
            assertEquals(0, getInt(cursor, Sessions.STARRED));
            assertEquals(ParserUtils.BLOCK_TYPE_SESSION, getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1273186984000L, getLong(cursor, Sessions.UPDATED));

        } finally {
            cursor.close();
        }

        // third, perform another remote sync
        final XmlPullParser parser2 = openAssetParser("remote-sessions2.xml");
        new RemoteSessionsHandler().parseAndApply(parser2, resolver);

        cursor = resolver.query(Sessions.CONTENT_URI, null, null, null, Sessions.DEFAULT_SORT);
        try {
            // six sessions from remote sync, plus one original
            assertEquals(7, cursor.getCount());

            // original local-only session is still hanging around
            assertTrue(cursor.moveToNext());
            assertEquals("writingreal-timegamesforandroidredux", getString(cursor,
                    Sessions.SESSION_ID));
            assertEquals(ParserUtils.BLOCK_TYPE_OFFICE_HOURS,
                    getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(0, getInt(cursor, Sessions.STARRED));
            assertEquals(0L, getLong(cursor, Sessions.UPDATED));

            // existing session was updated with remote values, and has star
            assertTrue(cursor.moveToNext());
            assertEquals("beginners-guide-android", getString(cursor, Sessions.SESSION_ID));
            assertEquals(ParserUtils.BLOCK_TYPE_SESSION, getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1, getInt(cursor, Sessions.STARRED));
            assertEquals(1273186984000L, getLong(cursor, Sessions.UPDATED));

            // existing session was updated with remote values, and has star
            assertTrue(cursor.moveToNext());
            assertEquals("writing-real-time-games-android", getString(cursor, Sessions.SESSION_ID));
            assertEquals("Proficiency in Java and Python and Ruby and Scheme and "
                    + "Bash and Ada and a solid grasp of Android's fundamental concepts",
                    getString(cursor, Sessions.REQUIREMENTS));
            assertEquals(0, getInt(cursor, Sessions.STARRED));
            assertEquals(ParserUtils.BLOCK_TYPE_SESSION, getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1273532584000L, getLong(cursor, Sessions.UPDATED));

            // last session should remain unchanged, since updated flag didn't
            // get touched. the remote spreadsheet said "102401", but we should
            // still have "301".
            assertTrue(cursor.moveToLast());
            assertEquals("developing-restful-android-apps", getString(cursor, Sessions.SESSION_ID));
            assertEquals("301", getString(cursor, Sessions.TYPE));
            assertEquals(ParserUtils.BLOCK_TYPE_SESSION, getString(cursor, Sessions.BLOCK_TYPE));
            assertEquals(1273186984000L, getLong(cursor, Sessions.UPDATED));

        } finally {
            cursor.close();
        }
    }

    private void parseBlocks() throws Exception {
        final XmlPullParser parser = openAssetParser("local-blocks.xml");
        new LocalBlocksHandler().parseAndApply(parser, getMockContentResolver());
    }

    private void parseTracks() throws Exception {
        final XmlPullParser parser = openAssetParser("local-tracks.xml");
        new LocalTracksHandler().parseAndApply(parser, getMockContentResolver());
    }

    private void parseRooms() throws Exception {
        final XmlPullParser parser = openAssetParser("local-rooms.xml");
        new LocalRoomsHandler().parseAndApply(parser, getMockContentResolver());
    }

    private String getString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndex(column));
    }

    private long getInt(Cursor cursor, String column) {
        return cursor.getInt(cursor.getColumnIndex(column));
    }

    private long getLong(Cursor cursor, String column) {
        return cursor.getLong(cursor.getColumnIndex(column));
    }

    private XmlPullParser openAssetParser(String assetName) throws XmlPullParserException,
            IOException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Context testContext = getTestContext(this);
        return ParserUtils.newPullParser(testContext.getResources().getAssets().open(assetName));
    }

    /**
     * Exposes method {@code getTestContext()} in {@link AndroidTestCase}, which
     * is hidden for now. Useful for obtaining access to the test assets.
     */
    public static Context getTestContext(AndroidTestCase testCase) throws IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        return (Context) AndroidTestCase.class.getMethod("getTestContext").invoke(testCase);
    }

}
