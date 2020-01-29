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

package com.google.samples.apps.iosched.util

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.shared.domain.internal.IOSchedHandler
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.time.FixedTimeExecutorRule
import com.google.samples.apps.iosched.util.ConferenceState.ENDED
import com.google.samples.apps.iosched.util.ConferenceState.STARTED
import com.google.samples.apps.iosched.util.ConferenceState.UPCOMING
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.threeten.bp.Instant

/**
 * Unit tests for [ConferenceStateLiveData]
 */
class ConferenceStateLiveDataTest {
    companion object {
        val INSTANT_BEFORE_CONFERENCE_START: Instant =
            TestData.TestConferenceDays[0].start.toInstant()
        // Conference actually starts after 3 hours of day0 start time
        val INSTANT_DURING_CONFERENCE: Instant =
            TestData.TestConferenceDays[0].start.plusHours(4).toInstant()
        val INSTANT_AFTER_CONFERENCE_END: Instant =
            TestData.TestConferenceDays[2].end.plusSeconds(1).toInstant()
    }

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    @get:Rule var fixedTimeExecutorRule = FixedTimeExecutorRule()

    private val fakeHandler = object : IOSchedHandler {
        override fun post(runnable: Runnable) = true
        override fun postDelayed(runnable: Runnable, millis: Long) = true
        override fun removeCallbacks(runnable: Runnable) {}
    }

    @Test
    fun testUpcomingState() {
        val conferenceStateLiveData =
            createConferenceStateLiveData(
                iOSchedHandler = fakeHandler,
                instant = INSTANT_BEFORE_CONFERENCE_START
            )
        assertThat(
            LiveDataTestUtil.getValue(conferenceStateLiveData), `is`
                (equalTo(UPCOMING))
        )
    }

    @Test
    fun testStartedState() {
        val conferenceStateLiveData =
            createConferenceStateLiveData(
                iOSchedHandler = fakeHandler,
                instant = INSTANT_DURING_CONFERENCE
            )
        assertThat(
            LiveDataTestUtil.getValue(conferenceStateLiveData), `is`
                (equalTo(STARTED))
        )
    }

    @Test
    fun testEndedState() {
        val conferenceStateLiveData =
            createConferenceStateLiveData(
                iOSchedHandler = fakeHandler,
                instant = INSTANT_AFTER_CONFERENCE_END
            )
        assertThat(
            LiveDataTestUtil.getValue(conferenceStateLiveData), `is`
                (equalTo(ENDED))
        )
    }

    @Test
    fun testStateTransition() {
        val mockTimeProvider = mock<TimeProvider> {
            on { now() }.doReturn(INSTANT_BEFORE_CONFERENCE_START)
        }

        val observer = mock<Observer<ConferenceState>> {
        }

        val handlerWithTransitionVerification = object : IOSchedHandler {
            private var numberOfTimes = 0

            override fun post(runnable: Runnable) = true

            override fun postDelayed(runnable: Runnable, millis: Long): Boolean {
                if (numberOfTimes == 0) {
                    verify(observer).onChanged(UPCOMING)
                    // Advance time beyond the start of conference.
                    `when`(mockTimeProvider.now()).thenReturn(INSTANT_DURING_CONFERENCE)
                    numberOfTimes++
                    runnable.run()
                }
                if (numberOfTimes == 1) {
                    verify(observer).onChanged(STARTED)
                    // Advance time just after the end of conference.
                    `when`(mockTimeProvider.now()).thenReturn(INSTANT_AFTER_CONFERENCE_END)
                    numberOfTimes++
                    runnable.run()
                }
                if (numberOfTimes == 2) {
                    verify(observer).onChanged(ENDED)
                }
                return true
            }

            override fun removeCallbacks(runnable: Runnable) {}
        }

        val conferenceStateLiveData =
            createConferenceStateLiveData(
                iOSchedHandler = handlerWithTransitionVerification,
                timeProvider = mockTimeProvider
            )
        conferenceStateLiveData.observeForever(observer)
    }

    private fun createConferenceStateLiveData(
        iOSchedHandler: IOSchedHandler,
        instant: Instant = Instant.now(),
        timeProvider: TimeProvider = mock {
            on { now() }.doReturn(instant)
        }
    ): ConferenceStateLiveData {
        return ConferenceStateLiveData(iOSchedHandler, timeProvider)
    }
}
