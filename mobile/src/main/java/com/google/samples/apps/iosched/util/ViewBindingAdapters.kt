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
import android.net.Uri
import android.support.v4.view.ViewPager
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.samples.apps.iosched.R
import timber.log.Timber

@BindingAdapter("invisibleUnless")
fun invisibleUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) VISIBLE else INVISIBLE
}

@BindingAdapter("goneUnless")
fun goneUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) VISIBLE else GONE
}

@BindingAdapter("pageMargin")
fun pageMargin(viewPager: ViewPager, pageMargin: Float) {
    viewPager.pageMargin = pageMargin.toInt()
}

@BindingAdapter("clipToCircle")
fun clipToCircle(view: View, circleSize: Float) {
    view.clipToOutline = true
    view.outlineProvider = CircularOutlineProvider(circleSize.toInt())
}

@BindingAdapter("imageUrl")
fun imageUrl(imageView: ImageView, imageUrl: Uri?) {
    when (imageUrl) {
        null -> {
            Timber.d("Unsetting image url")
            // TODO: b/74393872 Use an actual placeholder.
            Glide.with(imageView)
                .load(R.drawable.tag_filled)
                .into(imageView)
        }
        else -> {
            Glide.with(imageView)
                .load(imageUrl)
                .into(imageView)
        }
    }
}
