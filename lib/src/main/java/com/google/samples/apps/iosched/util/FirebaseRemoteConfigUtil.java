/*
 * Copyright (c) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.samples.apps.iosched.util;

import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.URLSpan;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.apache.commons.lang3.StringEscapeUtils;

public class FirebaseRemoteConfigUtil {

    public static Spannable getRemoteConfigSpannable(String key) {
        Spannable spannable = new SpannableString(Html.fromHtml(
                StringEscapeUtils.unescapeJava(
                        FirebaseRemoteConfig.getInstance().getString(key))));
        stripUnderlines(spannable);
        return spannable;
    }

    public static void stripUnderlines(Spannable spannable) {
        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);
            span = new URLSpan(span.getURL()) {
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            spannable.setSpan(span, start, end, 0);
        }
    }
}
