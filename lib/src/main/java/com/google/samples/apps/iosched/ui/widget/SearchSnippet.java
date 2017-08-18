/*
 * Copyright 2015 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An extension to TextView which marks up search result snippets.
 *
 * It looks for search terms surrounded by curly brace tokens e.g. “blah {Android} blah” and marks
 * the search term up as bold.
 */
public class SearchSnippet extends TextView {

    private static final Pattern PATTERN_SEARCH_TERM = Pattern.compile("(\\{[^\\}]+\\})", Pattern.DOTALL);

    public SearchSnippet(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!TextUtils.isEmpty(text)) {
            Matcher matcher = PATTERN_SEARCH_TERM.matcher(text);
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            List<String> hits = new ArrayList<>();
            while (matcher.find()) {
                hits.add(matcher.group(1));
            }

            for (String hit : hits) {
                int start = ssb.toString().indexOf(hit);
                int end = start + hit.length();
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
                // delete the markup tokens
                ssb.delete(end - 1, end);
                ssb.delete(start, start + 1);
            }
            text = ssb;
        }
        super.setText(text, type);
    }
}
