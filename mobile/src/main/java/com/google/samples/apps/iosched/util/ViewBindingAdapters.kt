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

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.widget.CustomSwipeRefreshLayout
import timber.log.Timber

@BindingAdapter("invisibleUnless")
fun invisibleUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) VISIBLE else INVISIBLE
}

@BindingAdapter("goneUnless")
fun goneUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) VISIBLE else GONE
}

@BindingAdapter("fabVisibility")
fun fabVisibility(fab: FloatingActionButton, visible: Boolean) {
    if (visible) fab.show() else fab.hide()
}

@BindingAdapter("pageMargin")
fun pageMargin(viewPager: ViewPager, pageMargin: Float) {
    viewPager.pageMargin = pageMargin.toInt()
}

@BindingAdapter("clipToCircle")
fun clipToCircle(view: View, clip: Boolean) {
    view.clipToOutline = clip
    view.outlineProvider = if (clip) CircularOutlineProvider else null
}

@BindingAdapter(value = ["imageUri", "placeholder"], requireAll = false)
fun imageUri(imageView: ImageView, imageUri: Uri?, placeholder: Drawable?) {
    val placeholderDrawable = placeholder ?: AppCompatResources.getDrawable(
        imageView.context, R.drawable.generic_placeholder
    )
    when (imageUri) {
        null -> {
            Timber.d("Unsetting image url")
            Glide.with(imageView)
                .load(placeholderDrawable)
                .into(imageView)
        }
        else -> {
            Glide.with(imageView)
                .load(imageUri)
                .apply(RequestOptions().placeholder(placeholderDrawable))
                .into(imageView)
        }
    }
}

@BindingAdapter(value = ["imageUrl", "placeholder"], requireAll = false)
fun imageUrl(imageView: ImageView, imageUrl: String?, placeholder: Drawable?) {
    imageUri(imageView, imageUrl?.toUri(), placeholder)
}

/**
 * Sets the colors of the [CustomSwipeRefreshLayout] loading indicator.
 */
@BindingAdapter("swipeRefreshColors")
fun setSwipeRefreshColors(swipeRefreshLayout: CustomSwipeRefreshLayout, colorResIds: IntArray) {
    swipeRefreshLayout.setColorSchemeColors(*colorResIds)
}

@BindingAdapter("noopInsets")
fun noopApplyWindowInsets(view: View, enabled: Boolean) {
    if (enabled) {
        view.setOnApplyWindowInsetsListener(NoopWindowInsetsListener)
        view.requestApplyInsetsWhenAttached()
    } else {
        view.setOnApplyWindowInsetsListener(null)
    }
}

@BindingAdapter(
        "paddingLeftSystemWindowInsets",
        "paddingTopSystemWindowInsets",
        "paddingRightSystemWindowInsets",
        "paddingBottomSystemWindowInsets",
        "paddingLeftGestureInsets",
        "paddingTopGestureInsets",
        "paddingRightGestureInsets",
        "paddingBottomGestureInsets",
        "marginLeftSystemWindowInsets",
        "marginTopSystemWindowInsets",
        "marginRightSystemWindowInsets",
        "marginBottomSystemWindowInsets",
        "marginLeftGestureInsets",
        "marginTopGestureInsets",
        "marginRightGestureInsets",
        "marginBottomGestureInsets",
        requireAll = false
)
fun applySystemWindows(
    view: View,
    padSystemWindowLeft: Boolean,
    padSystemWindowTop: Boolean,
    padSystemWindowRight: Boolean,
    padSystemWindowBottom: Boolean,
    padGestureLeft: Boolean,
    padGestureTop: Boolean,
    padGestureRight: Boolean,
    padGestureBottom: Boolean,
    marginSystemWindowLeft: Boolean,
    marginSystemWindowTop: Boolean,
    marginSystemWindowRight: Boolean,
    marginSystemWindowBottom: Boolean,
    marginGestureLeft: Boolean,
    marginGestureTop: Boolean,
    marginGestureRight: Boolean,
    marginGestureBottom: Boolean
) {
    require(((padSystemWindowLeft && padGestureLeft) ||
            (padSystemWindowTop && padGestureTop) ||
            (padSystemWindowRight && padGestureRight) ||
            (padSystemWindowBottom && padGestureBottom) ||
            (marginSystemWindowLeft && marginGestureLeft) ||
            (marginSystemWindowTop && marginGestureTop) ||
            (marginSystemWindowRight && marginGestureRight) ||
            (marginSystemWindowBottom && marginGestureBottom)).not()) {
        "Invalid parameters. Can not request system window and gesture inset handling" +
                " for the same dimension"
    }

    view.doOnApplyWindowInsets { v, insets, initialPadding, initialMargin ->
        // Padding handling

        val paddingLeft = when {
            padGestureLeft -> insets.systemGestureInsets.left
            padSystemWindowLeft -> insets.systemWindowInsetLeft
            else -> 0
        }
        val paddingTop = when {
            padGestureTop -> insets.systemGestureInsets.top
            padSystemWindowTop -> insets.systemWindowInsetTop
            else -> 0
        }
        val paddingRight = when {
            padGestureRight -> insets.systemGestureInsets.right
            padSystemWindowRight -> insets.systemWindowInsetRight
            else -> 0
        }
        val paddingBottom = when {
            padGestureBottom -> insets.systemGestureInsets.bottom
            padSystemWindowBottom -> insets.systemWindowInsetBottom
            else -> 0
        }
        v.setPadding(
                initialPadding.left + paddingLeft,
                initialPadding.top + paddingTop,
                initialPadding.right + paddingRight,
                initialPadding.bottom + paddingBottom
        )

        // Margin handling
        val marginInsetRequested = marginSystemWindowLeft || marginGestureLeft ||
                marginSystemWindowTop || marginGestureTop || marginSystemWindowRight ||
                marginGestureRight || marginSystemWindowBottom || marginGestureBottom
        require(!(marginInsetRequested && v.layoutParams !is ViewGroup.MarginLayoutParams)) {
            "Margin inset handling requested but view LayoutParams do not extend MarginLayoutParams"
        }
        val marginLeft = when {
            marginGestureLeft -> insets.systemGestureInsets.left
            marginSystemWindowLeft -> insets.systemWindowInsetLeft
            else -> 0
        }
        val marginTop = when {
            marginGestureTop -> insets.systemGestureInsets.top
            marginSystemWindowTop -> insets.systemWindowInsetTop
            else -> 0
        }
        val marginRight = when {
            marginGestureRight -> insets.systemGestureInsets.right
            marginSystemWindowRight -> insets.systemWindowInsetRight
            else -> 0
        }
        val marginBottom = when {
            marginGestureBottom -> insets.systemGestureInsets.bottom
            marginSystemWindowBottom -> insets.systemWindowInsetBottom
            else -> 0
        }
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = initialMargin.left + marginLeft
            topMargin = initialMargin.top + marginTop
            rightMargin = initialMargin.right + marginRight
            bottomMargin = initialMargin.bottom + marginBottom
        }
    }
}