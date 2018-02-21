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

package com.google.samples.apps.iosched.util

import android.databinding.BindingAdapter
import android.graphics.Color.TRANSPARENT
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemSessionTagBinding
import com.google.samples.apps.iosched.shared.model.Tag

@BindingAdapter("invisibleUnless")
fun invisibleUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) VISIBLE else INVISIBLE
}

@BindingAdapter("goneUnless")
fun goneUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) VISIBLE else GONE
}

@BindingAdapter("sessionTags")
fun sessionTags(container: LinearLayout, sessionTags: List<Tag>) {
    container.removeAllViews()
    val inflater = LayoutInflater.from(container.context)
    sessionTags.forEach { container.addView(createSessionTagButton(inflater, container, it)) }
}

private fun createSessionTagButton(
    inflater: LayoutInflater,
    container: ViewGroup,
    sessionTag: Tag
): Button {
    val tagBinding = ItemSessionTagBinding.inflate(inflater, container, false).apply {
        tag = sessionTag
    }
    return tagBinding.tagButton
}

@BindingAdapter("tagTint")
fun tagTint(textView: TextView, color: Int) {
    val tintColor = if (color != TRANSPARENT) {
        color
    } else {
        ContextCompat.getColor(textView.context, R.color.default_tag_color)
    }
    textView.compoundDrawablesRelative[0]?.setTint(tintColor)
}
