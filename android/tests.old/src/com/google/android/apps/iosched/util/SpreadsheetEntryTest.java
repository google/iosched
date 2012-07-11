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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class SpreadsheetEntryTest extends AndroidTestCase {

    public void testParseNormal() throws Exception {
        final XmlPullParser parser = openAssetParser("spreadsheet-normal.xml");
        final SpreadsheetEntry entry = SpreadsheetEntry.fromParser(parser);

        assertEquals("unexpected columns", 19, entry.size());
        assertEquals("Wednesday May 19", entry.get("sessiondate"));
        assertEquals("10:45am-11:45am", entry.get("sessiontime"));
        assertEquals("6", entry.get("room"));
        assertEquals("Android", entry.get("product"));
        assertEquals("Android", entry.get("track"));
        assertEquals("101", entry.get("sessiontype"));
        assertEquals("A beginner's guide to Android", entry.get("sessiontitle"));
        assertEquals("Android, Mobile, Java", entry.get("tags"));
        assertEquals("Reto Meier", entry.get("sessionspeakers"));
        assertEquals("retomeier", entry.get("speakers"));
        assertEquals("This session will introduce some of the basic concepts involved in "
                + "Android development. Starting with an overview of the SDK APIs available "
                + "to developers, we will work through some simple code examples that "
                + "explore some of the more common user features including using sensors, "
                + "maps, and geolocation.", entry.get("sessionabstract"));
        assertEquals("Proficiency in Java and a basic understanding of embedded "
                + "environments like mobile phones", entry.get("sessionrequirements"));
        assertEquals("beginners-guide-android", entry.get("sessionlink"));
        assertEquals("#android1", entry.get("sessionhashtag"));
        assertEquals("http://www.google.com/moderator/#15/e=68f0&t=68f0.45",
                entry.get("moderatorlink"));
        assertEquals(
                "https://wave.google.com/wave/#restored:wave:googlewave.com!w%252B-Xhdu7ZkBHw",
                entry.get("wavelink"));
        assertEquals("https://wave.google.com/wave/#restored:wave:googlewave.com",
                entry.get("_e8rn7"));
        assertEquals("w%252B-Xhdu7ZkBHw", entry.get("_dmair"));
        assertEquals("w+-Xhdu7ZkBHw", entry.get("waveid"));
    }

    public void testParseEmpty() throws Exception {
        final XmlPullParser parser = openAssetParser("spreadsheet-empty.xml");
        final SpreadsheetEntry entry = SpreadsheetEntry.fromParser(parser);

        assertEquals("unexpected columns", 0, entry.size());
    }

    public void testParseEmptyField() throws Exception {
        final XmlPullParser parser = openAssetParser("spreadsheet-emptyfield.xml");
        final SpreadsheetEntry entry = SpreadsheetEntry.fromParser(parser);

        assertEquals("unexpected columns", 3, entry.size());
        assertEquals("Wednesday May 19", entry.get("sessiondate"));
        assertEquals("", entry.get("sessiontime"));
        assertEquals("6", entry.get("room"));

    }

    public void testParseSingle() throws Exception {
        final XmlPullParser parser = openAssetParser("spreadsheet-single.xml");
        final SpreadsheetEntry entry = SpreadsheetEntry.fromParser(parser);

        assertEquals("unexpected columns", 1, entry.size());
        assertEquals("Wednesday May 19", entry.get("sessiondate"));
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
