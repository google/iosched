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

package com.google.samples.apps.iosched.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.postDelayed
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.TimeUtils
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

class CountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val compositions = CompositionSet()

    private val root: View = LayoutInflater.from(context).inflate(R.layout.countdown, this, true)
    private var days1 by AnimateDigitDelegate(compositions) {
        root.findViewById(R.id.countdown_days_1)
    }
    private var days2 by AnimateDigitDelegate(compositions) {
        root.findViewById(R.id.countdown_days_2)
    }
    private var hours1 by AnimateDigitDelegate(compositions) {
        root.findViewById(R.id.countdown_hours_1)
    }
    private var hours2 by AnimateDigitDelegate(compositions) {
        root.findViewById(R.id.countdown_hours_2)
    }
    private var mins1 by AnimateDigitDelegate(compositions) {
        root.findViewById(R.id.countdown_mins_1)
    }
    private var mins2 by AnimateDigitDelegate(compositions) {
        root.findViewById(R.id.countdown_mins_2)
    }
    private var secs1 by AnimateDigitDelegate(compositions) {
        root.findViewById(R.id.countdown_secs_1)
    }
    private var secs2 by AnimateDigitDelegate(compositions) {
        root.findViewById(R.id.countdown_secs_2)
    }

    private val conferenceStart = TimeUtils.ConferenceDays.first().start.plusHours(3L)

    data class Countdown(
        val days: Int,
        val hours: Int,
        val minutes: Int,
        val seconds: Int
    ) {
        companion object {
            fun until(time: ZonedDateTime): Countdown? {
                var duration = Duration.between(ZonedDateTime.now(), time)
                if (duration.isNegative) {
                    return null
                }
                val days = duration.toDays()
                duration = duration.minusDays(days)
                val hours = duration.toHours()
                duration = duration.minusHours(hours)
                val mins = duration.toMinutes()
                duration = duration.minusMinutes(mins)
                val secs = duration.seconds
                return Countdown(days.toInt(), hours.toInt(), mins.toInt(), secs.toInt())
            }
        }
    }

    init {
        isFocusableInTouchMode = true
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun dispatchPopulateAccessibilityEvent(
                host: View?,
                event: AccessibilityEvent?
            ): Boolean {
                return if (event != null) {
                    val countdown = Countdown.until(conferenceStart)
                    if (countdown != null) {
                        val res = context.resources
                        event.text.add(res.getQuantityString(
                            R.plurals.duration_days, countdown.days, countdown.days))
                        event.text.add(res.getQuantityString(
                            R.plurals.duration_hours, countdown.hours, countdown.hours))
                        event.text.add(res.getQuantityString(
                            R.plurals.duration_minutes, countdown.minutes, countdown.minutes))
                        event.text.add(res.getQuantityString(
                            R.plurals.duration_seconds, countdown.seconds, countdown.seconds))
                    }
                    true
                } else {
                    super.dispatchPopulateAccessibilityEvent(host, event)
                }
            }
        })
    }

    private val updateTime: Runnable = object : Runnable {

        override fun run() {
            val countdown = Countdown.until(conferenceStart) ?: return

            compositions.doOnReady {
                days1 = (countdown.days / 10)
                days2 = (countdown.days % 10)

                hours1 = (countdown.hours / 10)
                hours2 = (countdown.hours % 10)

                mins1 = (countdown.minutes / 10)
                mins2 = (countdown.minutes % 10)

                secs1 = (countdown.seconds / 10)
                secs2 = (countdown.seconds % 10)

                handler?.postDelayed(this, 1_000L) // Run self every second
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Timber.d("Starting countdown")
        handler?.post(updateTime)
        compositions.load(context)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Timber.d("Stopping countdown")
        handler?.removeCallbacks(updateTime)
        compositions.clear()
    }

    /**
     * A delegate who upon receiving a new value, runs animations on a view obtained from
     * [viewProvider]
     */
    private class AnimateDigitDelegate(
        private val compositions: CompositionSet,
        private val viewProvider: () -> LottieAnimationView
    ) : ObservableProperty<Int>(-1) {
        override fun afterChange(property: KProperty<*>, oldValue: Int, newValue: Int) {
            // Sanity check, `newValue` should always be in range [0–9]
            if (newValue < 0 || newValue > 9) {
                Timber.e("Trying to animate to digit: $newValue")
                return
            }

            if (oldValue != newValue) {
                val view = viewProvider()
                if (oldValue != -1) {
                    // Animate out the prev digit i.e play the second half of it's comp
                    compositions[oldValue]?.let { view.setComposition(it) }
                    view.setMinAndMaxProgress(0.5f, 1f)
                    // Some issues scheduling & playing 2 * 500ms comps every 1s. Speed up the
                    // outward anim slightly to give us some headroom ¯\_(ツ)_/¯
                    view.speed = 1.1f
                    view.playAnimation()

                    view.postDelayed(500L) {
                        compositions[newValue]?.let { view.setComposition(it) }
                        view.setMinAndMaxProgress(0f, 0.5f)
                        view.speed = 1f
                        view.playAnimation()
                    }
                } else {
                    // Initial show, just animate in the desired digit
                    compositions[newValue]?.let { view.setComposition(it) }
                    view.setMinAndMaxProgress(0f, 0.5f)
                    view.playAnimation()
                }
            }
        }
    }
}

private class CompositionSet {

    private val _compositions = arrayOfNulls<LottieComposition?>(10)

    private var doOnReadyCallback: (() -> Unit)? = null

    val ready: Boolean
        get() = _compositions.all { it != null }

    fun load(context: Context) {
        for (i in 0..9) {
            LottieCompositionFactory.fromAsset(context, "anim/$i.json").addListener { composition ->
                _compositions[i] = composition
                if (ready) {
                    doOnReadyCallback?.let {
                        it()
                        doOnReadyCallback = null
                    }
                }
            }
        }
    }

    fun doOnReady(body: () -> Unit) {
        if (ready) {
            body()
        } else {
            doOnReadyCallback = body
        }
    }

    fun clear() {
        for (i in 0..9) {
            _compositions[i] = null
        }
    }

    operator fun get(i: Int): LottieComposition? {
        return _compositions[i]
    }
}
