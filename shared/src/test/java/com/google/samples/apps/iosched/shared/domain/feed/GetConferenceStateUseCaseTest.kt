/*
 * Copyright 2020 Google LLC
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

package com.google.samples.apps.iosched.shared.domain.feed

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.google.samples.apps.iosched.shared.domain.feed.ConferenceState.ENDED
import com.google.samples.apps.iosched.shared.domain.feed.ConferenceState.STARTED
import com.google.samples.apps.iosched.shared.domain.feed.ConferenceState.UPCOMING
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import java.util.concurrent.TimeUnit
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.threeten.bp.Instant

private val INSTANT_BEFORE_CONFERENCE_START: Instant =
    TestData.TestConferenceDays[0].start.toInstant()
// Conference actually starts after 3 hours of day0 start time
private val INSTANT_DURING_CONFERENCE: Instant =
    TestData.TestConferenceDays[0].start.plusHours(4).toInstant()
private val INSTANT_AFTER_CONFERENCE_END: Instant =
    TestData.TestConferenceDays[2].end.plusSeconds(1).toInstant()

/**
 * Unit tests for [GetConferenceStateUseCase]
 */
class GetConferenceStateUseCaseTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val testDispatcher = coroutineRule.testDispatcher

    @Test
    fun testUpcomingState() = testDispatcher.runBlockingTest {
        val getConferenceStateUseCase =
            createGetConferenceStateUseCase(
                instant = INSTANT_BEFORE_CONFERENCE_START
            )

        val currentState = getConferenceStateUseCase(Unit).first()

        assertEquals(
            Result.Success(UPCOMING),
            currentState
        )
    }

    @Test
    fun testStartedState() = testDispatcher.runBlockingTest {
        val getConferenceStateUseCase =
            createGetConferenceStateUseCase(
                instant = INSTANT_DURING_CONFERENCE
            )

        val currentState = getConferenceStateUseCase(Unit).first()

        assertEquals(
            Result.Success(STARTED),
            currentState
        )
    }

    @Test
    fun testEndedState() = testDispatcher.runBlockingTest {
        val getConferenceStateUseCase =
            createGetConferenceStateUseCase(
                instant = INSTANT_AFTER_CONFERENCE_END
            )

        val currentState = getConferenceStateUseCase(Unit).first()

        assertEquals(
            Result.Success(ENDED),
            currentState
        )
    }

    @Test
    fun testStateTransition() = testDispatcher.runBlockingTest {
        val mockTimeProvider = mock<TimeProvider> {
            on { now() }.doReturn(
                INSTANT_BEFORE_CONFERENCE_START
            )
        }
        val conferenceStateUseCase =
            createGetConferenceStateUseCase(
                timeProvider = mockTimeProvider
            )

        val observer = mock<Observer<Result<ConferenceState>>> {
        }

        // Act
        conferenceStateUseCase(Unit).asLiveData().observeForever(observer)

        verify(observer).onChanged(Result.Success(UPCOMING))

        // Arrange for STARTED state
        `when`(mockTimeProvider.now()).thenReturn(
            INSTANT_DURING_CONFERENCE
        )
        testDispatcher.advanceTimeBy(TimeUnit.DAYS.toMillis(1))

        verify(observer).onChanged(Result.Success(STARTED))

        // Arrange for ENDED state
        `when`(mockTimeProvider.now()).thenReturn(
            INSTANT_AFTER_CONFERENCE_END
        )
        testDispatcher.advanceTimeBy(TimeUnit.DAYS.toMillis(4))

        verify(observer).onChanged(Result.Success(ENDED))
    }

    private fun createGetConferenceStateUseCase(
        instant: Instant = Instant.now(),
        timeProvider: TimeProvider = mock {
            on { now() }.doReturn(instant)
        }
    ): GetConferenceStateUseCase {
        return GetConferenceStateUseCase(testDispatcher, timeProvider)
    }
}
