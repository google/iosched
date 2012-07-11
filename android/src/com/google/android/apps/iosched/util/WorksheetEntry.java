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

import android.text.format.DateUtils;

import java.io.IOException;

import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.HREF;
import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.LINK;
import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.REL;
import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.TITLE;
import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.UPDATED;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

public class WorksheetEntry {
    private static final String REL_LISTFEED = "http://schemas.google.com/spreadsheets/2006#listfeed";

    private long mUpdated;
    private String mTitle;
    private String mListFeed;

    public long getUpdated() {
        return mUpdated;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getListFeed() {
        return mListFeed;
    }

    @Override
    public String toString() {
        return "title=" + mTitle + ", updated=" + mUpdated + " ("
                + DateUtils.getRelativeTimeSpanString(mUpdated) + ")";
    }

    public static WorksheetEntry fromParser(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        final int depth = parser.getDepth();
        final WorksheetEntry entry = new WorksheetEntry();

        String tag = null;
        int type;
        while (((type = parser.next()) != END_TAG ||
                parser.getDepth() > depth) && type != END_DOCUMENT) {
            if (type == START_TAG) {
                tag = parser.getName();
                if (LINK.equals(tag)) {
                    final String rel = parser.getAttributeValue(null, REL);
                    final String href = parser.getAttributeValue(null, HREF);
                    if (REL_LISTFEED.equals(rel)) {
                        entry.mListFeed = href;
                    }
                }
            } else if (type == END_TAG) {
                tag = null;
            } else if (type == TEXT) {
                final String text = parser.getText();
                if (TITLE.equals(tag)) {
                    entry.mTitle = text;
                } else if (UPDATED.equals(tag)) {
                    entry.mUpdated = ParserUtils.parseTime(text);
                }
            }
        }
        return entry;
    }
}
