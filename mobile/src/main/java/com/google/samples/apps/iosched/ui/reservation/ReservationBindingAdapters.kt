/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.ui.reservation

import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.databinding.BindingAdapter
import com.google.samples.apps.iosched.R

@BindingAdapter("fromTitle", "toTitle", requireAll = true)
fun makeSessionTitlesBold(textView: TextView, fromTitle: String, toTitle: String) {
    var text: CharSequence =
        textView.context.getString(R.string.swap_reservation_content, fromTitle, toTitle)
    text = makeTextBold(text, fromTitle)
    textView.text = makeTextBold(text, toTitle)
}

@BindingAdapter("sessionTitle", requireAll = true)
fun makeSessionTitleBold(textView: TextView, sessionTitle: String) {
    val text: CharSequence =
        textView.context.getString(R.string.remove_reservation_content, sessionTitle)
    textView.text = makeTextBold(text, sessionTitle)
}

private fun makeTextBold(text: CharSequence, boldText: String): CharSequence {
    val from = text.indexOf(boldText)
    val end = from + boldText.length
    return buildSpannedString {
        append(text.subSequence(0, from))
        bold {
            append(boldText)
        }
        append(text.subSequence(end, text.length))
    }
}
