/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.ui.feed

import android.os.Handler
import androidx.lifecycle.LiveData
import com.google.samples.apps.iosched.shared.di.MainThreadHandler
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

/** A class that controls the [@FeedHeader] */
class FeedHeaderLiveData @Inject constructor() : LiveData<FeedHeader>() {
    private val conferenceStart = TimeUtils.ConferenceDays.first().start.plusHours(3L)
    private var feedHeader = FeedHeader(timerVisible = false, moment = null)
    private val updater = Runnable { update() }

    @Inject
    @MainThreadHandler
    lateinit var handler: Handler

    override fun onActive() {
        super.onActive()
        value = feedHeader
        update()
    }

    override fun onInactive() {
        super.onInactive()
        handler.removeCallbacks(updater)
    }

    private fun update() {
        var timeUntilConf = Duration.between(ZonedDateTime.now(), conferenceStart)

        val newFeedHeader = when (timeUntilConf.isNegative) {
            true -> feedHeader.copy(timerVisible = false)
            false -> feedHeader.copy(timerVisible = true)
        }

        // TODO: Get the moments and select current moment

        // Trigger update only when the value gets changed
        if (newFeedHeader != feedHeader) {
            feedHeader = newFeedHeader
            value = newFeedHeader
        }

        if (!timeUntilConf.isNegative) {
            // Only set the future call if the time is positive
            handler.postDelayed(updater, timeUntilConf.toMillis())
        }
    }
}