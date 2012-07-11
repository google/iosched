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

package com.google.android.apps.iosched.util;

import com.google.android.apps.iosched.io.XmlHandler;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.provider.ScheduleContract.SyncColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Tracks;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Various utility methods used by {@link XmlHandler} implementations.
 */
public class ParserUtils {
    // TODO: consider refactor to HandlerUtils?

    // TODO: localize this string at some point
    public static final String BLOCK_TITLE_BREAKOUT_SESSIONS = "Breakout sessions";

    public static final String BLOCK_TYPE_FOOD = "food";
    public static final String BLOCK_TYPE_SESSION = "session";
    public static final String BLOCK_TYPE_OFFICE_HOURS = "officehours";

    // TODO: factor this out into a separate data file.
    public static final Set<String> LOCAL_TRACK_IDS = Sets.newHashSet(
            "accessibility", "android", "appengine", "chrome", "commerce", "developertools",
            "gamedevelopment", "geo", "googleapis", "googleapps", "googletv", "techtalk",
            "webgames", "youtube");

    /** Used to sanitize a string to be {@link Uri} safe. */
    private static final Pattern sSanitizePattern = Pattern.compile("[^a-z0-9-_]");
    private static final Pattern sParenPattern = Pattern.compile("\\(.*?\\)");

    /** Used to split a comma-separated string. */
    private static final Pattern sCommaPattern = Pattern.compile("\\s*,\\s*");

    private static Time sTime = new Time();
    private static XmlPullParserFactory sFactory;

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input) {
        return sanitizeId(input, false);
    }

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input, boolean stripParen) {
        if (input == null) return null;
        if (stripParen) {
            // Strip out all parenthetical statements when requested.
            input = sParenPattern.matcher(input).replaceAll("");
        }
        return sSanitizePattern.matcher(input.toLowerCase()).replaceAll("");
    }

    /**
     * Split the given comma-separated string, returning all values.
     */
    public static String[] splitComma(CharSequence input) {
        if (input == null) return new String[0];
        return sCommaPattern.split(input);
    }

    /**
     * Build and return a new {@link XmlPullParser} with the given
     * {@link InputStream} assigned to it.
     */
    public static XmlPullParser newPullParser(InputStream input) throws XmlPullParserException {
        if (sFactory == null) {
            sFactory = XmlPullParserFactory.newInstance();
        }
        final XmlPullParser parser = sFactory.newPullParser();
        parser.setInput(input, null);
        return parser;
    }

    /**
     * Parse the given string as a RFC 3339 timestamp, returning the value as
     * milliseconds since the epoch.
     */
    public static long parseTime(String time) {
        sTime.parse3339(time);
        return sTime.toMillis(false);
    }

    /**
     * Return a {@link Blocks#BLOCK_ID} matching the requested arguments.
     */
    public static String findBlock(String title, long startTime, long endTime) {
        // TODO: in future we might check provider if block exists
        return Blocks.generateBlockId(startTime, endTime);
    }

    /**
     * Return a {@link Blocks#BLOCK_ID} matching the requested arguments,
     * inserting a new {@link Blocks} entry as a
     * {@link ContentProviderOperation} when none already exists.
     */
    public static String findOrCreateBlock(String title, String type, long startTime, long endTime,
            ArrayList<ContentProviderOperation> batch, ContentResolver resolver) {
        // TODO: check for existence instead of always blindly creating. it's
        // okay for now since the database replaces on conflict.
        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Blocks.CONTENT_URI);
        final String blockId = Blocks.generateBlockId(startTime, endTime);
        builder.withValue(Blocks.BLOCK_ID, blockId);
        builder.withValue(Blocks.BLOCK_TITLE, title);
        builder.withValue(Blocks.BLOCK_START, startTime);
        builder.withValue(Blocks.BLOCK_END, endTime);
        builder.withValue(Blocks.BLOCK_TYPE, type);
        batch.add(builder.build());
        return blockId;
    }

    /**
     * Query and return the {@link SyncColumns#UPDATED} time for the requested
     * {@link Uri}. Expects the {@link Uri} to reference a single item.
     */
    public static long queryItemUpdated(Uri uri, ContentResolver resolver) {
        final String[] projection = { SyncColumns.UPDATED };
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            } else {
                return ScheduleContract.UPDATED_NEVER;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Query and return the newest {@link SyncColumns#UPDATED} time for all
     * entries under the requested {@link Uri}. Expects the {@link Uri} to
     * reference a directory of several items.
     */
    public static long queryDirUpdated(Uri uri, ContentResolver resolver) {
        final String[] projection = { "MAX(" + SyncColumns.UPDATED + ")" };
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
            cursor.moveToFirst();
            return cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }

    /**
     * Translate an incoming {@link Tracks#TRACK_ID}, usually passing directly
     * through, but returning a different value when a local alias is defined.
     */
    public static String translateTrackIdAlias(String trackId) {
        //if ("gwt".equals(trackId)) {
        //    return "googlewebtoolkit";
        //} else {
        return trackId;
        //}
    }

    /**
     * Translate a possibly locally aliased {@link Tracks#TRACK_ID} to its real value;
     * this usually is a pass-through.
     */
    public static String translateTrackIdAliasInverse(String trackId) {
        //if ("googlewebtoolkit".equals(trackId)) {
        //    return "gwt";
        //} else {
        return trackId;
        //}
    }

    /** XML tag constants used by the Atom standard. */
    public interface AtomTags {
        String ENTRY = "entry";
        String UPDATED = "updated";
        String TITLE = "title";
        String LINK = "link";
        String CONTENT = "content";

        String REL = "rel";
        String HREF = "href";
    }
}
