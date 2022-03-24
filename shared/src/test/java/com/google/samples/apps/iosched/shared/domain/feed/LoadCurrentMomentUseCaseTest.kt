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

package com.google.samples.apps.iosched.shared.domain.feed

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.shared.data.feed.DefaultFeedRepository
import com.google.samples.apps.iosched.shared.data.feed.FeedRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.threeten.bp.Instant

/**
 * Unit tests for [LoadCurrentMomentUseCase]
 */
class LoadCurrentMomentUseCaseTest {

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun timeDuringMoment_loadsMoment() = runTest {
        val time = TestData.TestConferenceDays.first().start.plusHours(3).toInstant()
        val moment = loadMomentForTime(time)
        assertEquals(moment, TestData.moment1)
    }

    @Test
    fun timeBeforeFirstMoment_loadsNull() = runTest {
        val time = TestData.TestConferenceDays.first().start.minusMinutes(1).toInstant()
        val moment = loadMomentForTime(time)
        assertNull(moment)
    }

    @Test
    fun timeAfterLastMoment_loadsNull() = runTest {
        val time = TestData.TestConferenceDays.last().end.plusMinutes(1).toInstant()
        val moment = loadMomentForTime(time)
        assertNull(moment)
    }

    @Test
    fun loadsError() = runTest {
        val useCase =
            LoadCurrentMomentUseCase(unsuccessfulFeedRepository, coroutineRule.testDispatcher)

        // Time doesn't matter
        val result = useCase(Instant.now())

        assertTrue(result is Result.Error)
    }

    private suspend fun loadMomentForTime(time: Instant): Moment? {
        // Build use case with the test data
        val useCase =
            LoadCurrentMomentUseCase(successfulFeedRepository, coroutineRule.testDispatcher)

        // Execute the use case
        val result = useCase(time)

        // Verify successful execution
        assertTrue(result is Result.Success)

        // Previous assert ensures this is actually a success
        return result.successOr(null)
    }
}

private val successfulFeedRepository = DefaultFeedRepository(
    TestAnnouncementDataSource, TestMomentDataSource
)

private val unsuccessfulFeedRepository = object : FeedRepository {

    override fun getAnnouncements(): List<Announcement> {
        throw Exception("Error!")
    }

    override fun getMoments(): List<Moment> {
        throw Exception("Error!")
    }
}
