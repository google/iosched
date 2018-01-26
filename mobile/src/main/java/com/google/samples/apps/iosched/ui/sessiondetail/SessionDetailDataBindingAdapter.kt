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

package com.google.samples.apps.iosched.ui.sessiondetail

import android.databinding.BindingAdapter
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.model.Tag

@Suppress("unused")
@BindingAdapter("app:sessionTags")
fun sessionTags(layout: LinearLayout, sessionTags: List<Tag>) {
    val inf = LayoutInflater.from(layout.context)
    layout.removeAllViews()
    sessionTags.forEach { layout.addView(createSessionTagButton(inf, layout, it)) }
}

private fun createSessionTagButton(
        inflater: LayoutInflater,
        container: ViewGroup,
        tag: Tag
): Button {
    return (inflater.inflate(R.layout.item_session_tag, container, false) as Button).apply {
        text = tag.name
        background.setColorFilter(Color.parseColor(tag.color), PorterDuff.Mode.SRC_ATOP)
    }
}