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
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.threeten.bp.Instant

/**
 * Unit tests for [LoadAnnouncementsUseCase]
 */
class LoadAnnouncementsUseCaseTest {

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun announcementsLoadedSuccessfully() = coroutineRule.runBlockingTest {
        val useCase =
            LoadAnnouncementsUseCase(successfulFeedRepository, coroutineRule.testDispatcher)

        // Load all items
        val time = TestData.TestConferenceDays.last().end.toInstant()
        val result = useCase(time)

        assertEquals(result, Result.Success(TestData.announcements))
    }

    @Test
    fun announcementsLoadedUnsuccessfully() = coroutineRule.runBlockingTest {
        val useCase =
            LoadAnnouncementsUseCase(unsuccessfulFeedRepository, coroutineRule.testDispatcher)

        // Time doesn't matter
        val result = useCase(Instant.now())

        assertTrue(result is Result.Error)
    }

    @Test
    fun announcementsLoaded_filteredByTimestamp() = coroutineRule.runBlockingTest {
        val useCase =
            LoadAnnouncementsUseCase(successfulFeedRepository, coroutineRule.testDispatcher)

        // Load only the first day's items
        val time = TestData.TestConferenceDays.first().end.plusMinutes(1).toInstant()
        val result = useCase(time)

        assertEquals(result, Result.Success(TestData.announcements.subList(0, 2)))
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
