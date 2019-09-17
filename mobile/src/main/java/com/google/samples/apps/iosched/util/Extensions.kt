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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.net.wifi.WifiConfiguration
import android.os.Handler
import android.text.StaticLayout
import android.util.TypedValue
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import androidx.annotation.DimenRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ViewDataBinding
import com.google.samples.apps.iosched.model.Theme
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

fun ObservableBoolean.hasSameValue(other: ObservableBoolean) = get() == other.get()

fun Int.isEven() = this % 2 == 0

fun View.isRtl() = layoutDirection == View.LAYOUT_DIRECTION_RTL

inline fun <T : ViewDataBinding> T.executeAfter(block: T.() -> Unit) {
    block()
    executePendingBindings()
}

/**
 * An extension to `postponeEnterTransition` which will resume after a timeout.
 */
fun Activity.postponeEnterTransition(timeout: Long) {
    postponeEnterTransition()
    window.decorView.postDelayed({ startPostponedEnterTransition() }, timeout)
}

/**
 * An extension to `postponeEnterTransition` which will resume after a timeout.
 */
fun DaggerFragment.postponeEnterTransition(timeout: Long) {
    postponeEnterTransition()
    Handler().postDelayed({ startPostponedEnterTransition() }, timeout)
}

/**
 * Calculated the widest line in a [StaticLayout].
 */
fun StaticLayout.textWidth(): Int {
    var width = 0f
    for (i in 0 until lineCount) {
        width = width.coerceAtLeast(getLineWidth(i))
    }
    return width.toInt()
}

/**
 * Linearly interpolate between two values.
 */
fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

/**
 * Alternative to Resources.getDimension() for values that are TYPE_FLOAT.
 */
fun Resources.getFloat(@DimenRes resId: Int): Float {
    val outValue = TypedValue()
    getValue(resId, outValue, true)
    return outValue.float
}

/**
 * Return the Wifi config wrapped in quotes.
 */
fun WifiConfiguration.quoteSsidAndPassword(): WifiConfiguration {
    return WifiConfiguration().apply {
        SSID = this@quoteSsidAndPassword.SSID.wrapInQuotes()
        preSharedKey = this@quoteSsidAndPassword.preSharedKey.wrapInQuotes()
    }
}

/**
 * Return the Wifi config without quotes.
 */
fun WifiConfiguration.unquoteSsidAndPassword(): WifiConfiguration {
    return WifiConfiguration().apply {
        SSID = this@unquoteSsidAndPassword.SSID.unwrapQuotes()
        preSharedKey = this@unquoteSsidAndPassword.preSharedKey.unwrapQuotes()
    }
}

fun String.wrapInQuotes(): String {
    var formattedConfigString: String = this
    if (!startsWith("\"")) {
        formattedConfigString = "\"$formattedConfigString"
    }
    if (!endsWith("\"")) {
        formattedConfigString = "$formattedConfigString\""
    }
    return formattedConfigString
}

fun String.unwrapQuotes(): String {
    var formattedConfigString: String = this
    if (formattedConfigString.startsWith("\"")) {
        if (formattedConfigString.length > 1) {
            formattedConfigString = formattedConfigString.substring(1)
        } else {
            formattedConfigString = ""
        }
    }
    if (formattedConfigString.endsWith("\"")) {
        if (formattedConfigString.length > 1) {
            formattedConfigString =
                formattedConfigString.substring(0, formattedConfigString.length - 1)
        } else {
            formattedConfigString = ""
        }
    }
    return formattedConfigString
}

fun View.doOnApplyWindowInsets(
    f: (
        View,
        insets: WindowInsetsCompat,
        initialPadding: ViewDimensions,
        initialMargin: ViewDimensions
    ) -> Unit
) {
    // Create a snapshot of the view's padding state
    val initialPadding = createStateForViewPadding(this)
    val initialMargin = createStateForViewMargin(this)
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        f(v, insets, initialPadding, initialMargin)
        insets
    }
    requestApplyInsetsWhenAttached()
}

/**
 * Call [View.requestApplyInsets] in a safe away. If we're attached it calls it straight-away.
 * If not it sets an [View.OnAttachStateChangeListener] and waits to be attached before calling
 * [View.requestApplyInsets].
 */
fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

/**
 * Flow transformation that ignores the first element emitted by the original Flow.
 */
fun <T> Flow<T>.ignoreFirst(): Flow<T> {
    var firstElement = true
    return transform { value ->
        if (firstElement) {
            firstElement = false
            return@transform
        } else {
            return@transform emit(value)
        }
    }
}

/**
 * Cancel the Job if it's active.
 */
fun Job?.cancelIfActive() {
    if (this?.isActive == true) {
        cancel()
    }
}

private fun createStateForViewPadding(view: View) = ViewDimensions(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom, view.paddingStart,
    view.paddingEnd
)

private fun createStateForViewMargin(view: View): ViewDimensions {
    return (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
        ViewDimensions(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin,
                it.marginStart, it.marginEnd)
    } ?: ViewDimensions()
}

data class ViewDimensions(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val start: Int = 0,
    val end: Int = 0
)

@SuppressLint("WrongConstant") // Suppressing warning on MODE_NIGHT_AUTO_BATTERY. b/128789886
fun updateForTheme(theme: Theme) = when (theme) {
    Theme.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    Theme.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    Theme.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    Theme.BATTERY_SAVER -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
}
