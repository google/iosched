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

package com.google.samples.apps.iosched.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.view.postDelayed
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieAnimationView.CacheStrategy.Strong
import com.google.samples.apps.iosched.databinding.FragmentCountdownBinding
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class CountdownFragment : Fragment() {

    private val handler = Handler()
    private lateinit var binding: FragmentCountdownBinding
    private var days1 by AnimateDigitDelegate { binding.countdownDays1 }
    private var days2 by AnimateDigitDelegate { binding.countdownDays2 }
    private var hours1 by AnimateDigitDelegate { binding.countdownHours1 }
    private var hours2 by AnimateDigitDelegate { binding.countdownHours2 }
    private var mins1 by AnimateDigitDelegate { binding.countdownMins1 }
    private var mins2 by AnimateDigitDelegate { binding.countdownMins2 }
    private var secs1 by AnimateDigitDelegate { binding.countdownSecs1 }
    private var secs2 by AnimateDigitDelegate { binding.countdownSecs2 }

    private val updateTime: Runnable = object : Runnable {

        // todo: verify Keynote start time
        private val conferenceStart = TimeUtils.ConferenceDay.DAY_1.start.plusHours(3L)

        override fun run() {
            var timeUntilConf = Duration.between(ZonedDateTime.now(), conferenceStart)

            val days = timeUntilConf.toDays()
            days1 = (days / 10).toInt()
            days2 = (days % 10).toInt()
            timeUntilConf = timeUntilConf.minusDays(days)

            val hours = timeUntilConf.toHours()
            hours1 = (hours / 10).toInt()
            hours2 = (hours % 10).toInt()
            timeUntilConf = timeUntilConf.minusHours(hours)

            val mins = timeUntilConf.toMinutes()
            mins1 = (mins / 10).toInt()
            mins2 = (mins % 10).toInt()
            timeUntilConf = timeUntilConf.minusMinutes(mins)

            val secs = timeUntilConf.seconds
            secs1 = (secs / 10).toInt()
            secs2 = (secs % 10).toInt()

            handler.postDelayed(this, 1_000L) // Run self every second
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCountdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Timber.d("Starting countdown")
        handler.post(updateTime)
    }

    override fun onDetach() {
        super.onDetach()
        Timber.d("Stopping countdown")
        handler.removeCallbacks(updateTime)
    }
}

/**
 * A delegate who upon receiving a new value, runs animations on a view obtained from [viewProvider]
 */
class AnimateDigitDelegate(
    private val viewProvider: () -> LottieAnimationView
) : ObservableProperty<Int>(-1) {
    override fun afterChange(property: KProperty<*>, oldValue: Int, newValue: Int) {
        if (oldValue != newValue) {
            val view = viewProvider()
            if (oldValue != -1) {
                // Animate out the prev digit i.e play the second half of it's comp
                view.setAnimation("anim/$oldValue.json", Strong)
                view.setMinAndMaxProgress(0.5f, 1f)
                // Some issues scheduling & playing 2 * 500ms comps every 1s. Speed up the
                // outward anim slightly to give us some headroom ¯\_(ツ)_/¯
                view.speed = 1.1f
                view.playAnimation()

                view.postDelayed(500L) {
                    view.setAnimation("anim/$newValue.json", Strong)
                    view.setMinAndMaxProgress(0f, 0.5f)
                    view.speed = 1f
                    view.playAnimation()
                }
            } else {
                // Initial show, just animate in the desired digit
                view.setAnimation("anim/$newValue.json", Strong)
                view.setMinAndMaxProgress(0f, 0.5f)
                view.playAnimation()
            }
        }
    }
}
