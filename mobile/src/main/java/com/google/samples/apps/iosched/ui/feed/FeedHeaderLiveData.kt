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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.model.Theme
import com.google.samples.apps.iosched.shared.di.MainThreadHandler
import com.google.samples.apps.iosched.shared.domain.feed.LoadMomentsUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

/** A class that controls the [@FeedHeader] */
class FeedHeaderLiveData @Inject constructor(
    private val loadMomentsUseCase: LoadMomentsUseCase
) : MediatorLiveData<FeedHeader>() {
    private val conferenceStart = TimeUtils.ConferenceDays.first().start.plusHours(3L)
    private var feedHeader =
        FeedHeader(timerVisible = false, moment = null, timeZoneId = ZoneId.systemDefault(),
            theme = Theme.SYSTEM)
    private val updater = Runnable {
        update(
            momentsResult.value.let {
                if (it is Success) it.data else emptyList()
            }
        )
    }
    private val momentsResult = MutableLiveData<Result<List<Moment>>>()

    @Inject
    @MainThreadHandler
    lateinit var handler: Handler

    override fun onActive() {
        super.onActive()
        value = feedHeader
        addSource(momentsResult) {
            update(if (it is Success) it.data else emptyList())
        }
        loadMomentsUseCase(Unit, momentsResult)
    }

    override fun onInactive() {
        super.onInactive()
        removeSource(momentsResult)
        handler.removeCallbacks(updater)
    }

    private fun filterCurrentMoment(input: List<Moment>): Moment? {
        val now = ZonedDateTime.now()
        return input
            .filter { it.startTime <= now && now < it.endTime }
            .let { it.firstOrNull() }
    }

    private fun update(moments: List<Moment>) {
        val currentMoment = filterCurrentMoment(moments)
        val timeUntilConf = Duration.between(ZonedDateTime.now(), conferenceStart)

        val newFeedHeader =
            feedHeader.copy(timerVisible = !timeUntilConf.isNegative, moment = currentMoment)

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