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

package com.google.samples.apps.iosched.ui.sessioncommon

import android.content.Context
import android.databinding.BindingAdapter
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.util.StateSet
import android.view.View
import android.widget.TextView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.model.Tag

@BindingAdapter("sessionTags")
fun sessionTags(recyclerView: RecyclerView, sessionTags: List<Tag>?) {
    recyclerView.adapter = (recyclerView.adapter as? TagAdapter
        ?: TagAdapter()).apply {
        tags = sessionTags ?: emptyList()
    }
}

@BindingAdapter("tagTint")
fun tagTint(textView: TextView, color: Int) {
    // Tint the colored dot
    textView.compoundDrawablesRelative[0]?.setTint(
        tagTintOrDefault(
            color,
            textView.context
        )
    )
}

/**
 * Creates a tag background with checkable state. When checked, the tag becomes filled and shows a
 * clear ('X') icon in place of the dot.
 */
@BindingAdapter("tagColor")
fun tagColor(textView: TextView, color: Int) {
    val tintColor =
        tagTintOrDefault(color, textView.context)

    // We can't define these with XML <selector>s because we want to tint only one state in each,
    // and StateListDrawable does not give us a way to extract any of its contained Drawables.
    // We also can't tint using an XML color <selector> because there's no way to specify *no* tint
    // for a state (different from transparent tint, which makes the drawable invisible).
    val dotOrClear = StateListDrawable().apply {
        // clear icon when checked
        addState(
            intArrayOf(android.R.attr.state_checked),
            drawableCompat(
                textView,
                R.drawable.tag_clear
            )
        )
        // colored dot by default
        addState(StateSet.WILD_CARD,
            drawableCompat(
                textView,
                R.drawable.tag_dot
            )?.apply { setTint(tintColor) })
    }
    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(dotOrClear, null, null, null)

    val tagBg = StateListDrawable().apply {
        // filled chip when checked
        addState(intArrayOf(android.R.attr.state_checked),
            drawableCompat(
                textView,
                R.drawable.tag_filled
            )?.apply { setTint(tintColor) })
        // outline by default
        addState(
            StateSet.WILD_CARD,
            drawableCompat(
                textView,
                R.drawable.tag_outline
            )
        )
    }
    ((textView.background as InsetDrawable).drawable as RippleDrawable)
        .setDrawableByLayerId(R.id.tag_fill, tagBg)
}

fun tagTintOrDefault(color: Int, context: Context): Int {
    return if (color != TRANSPARENT) {
        color
    } else {
        ContextCompat.getColor(context, R.color.default_tag_color)
    }
}

fun drawableCompat(view: View, id: Int) = AppCompatResources.getDrawable(view.context, id)
