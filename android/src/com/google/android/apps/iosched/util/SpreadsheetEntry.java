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

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.CONTENT;
import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.UPDATED;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

public class SpreadsheetEntry extends HashMap<String, String> {

    private static final Pattern sContentPattern = Pattern.compile(
            "(?:^|, )([_a-zA-Z0-9]+): (.*?)(?=\\s*$|, [_a-zA-Z0-9]+: )", Pattern.DOTALL);

    private static Matcher sContentMatcher;

    private static Matcher getContentMatcher(CharSequence input) {
        if (sContentMatcher == null) {
            sContentMatcher = sContentPattern.matcher(input);
        } else {
            sContentMatcher.reset(input);
        }
        return sContentMatcher;
    }

    private long mUpdated;

    public long getUpdated() {
        return mUpdated;
    }

    public static SpreadsheetEntry fromParser(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        final int depth = parser.getDepth();
        final SpreadsheetEntry entry = new SpreadsheetEntry();

        String tag = null;
        int type;
        while (((type = parser.next()) != END_TAG ||
                parser.getDepth() > depth) && type != END_DOCUMENT) {
            if (type == START_TAG) {
                tag = parser.getName();
            } else if (type == END_TAG) {
                tag = null;
            } else if (type == TEXT) {
                if (UPDATED.equals(tag)) {
                    final String text = parser.getText();
                    entry.mUpdated = ParserUtils.parseTime(text);
                } else if (CONTENT.equals(tag)) {
                    final String text = parser.getText();
                    final Matcher matcher = getContentMatcher(text);
                    while (matcher.find()) {
                        final String key = matcher.group(1);
                        final String value = matcher.group(2).trim();
                        entry.put(key, value);
                    }
                }
            }
        }
        return entry;
    }
}
