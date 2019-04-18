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

package com.google.samples.apps.iosched.shared.usecases.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.shared.data.feed.FeedRepository
import com.google.samples.apps.iosched.shared.domain.feed.LoadAnnouncementsUseCase
import com.google.samples.apps.iosched.shared.domain.feed.TestMomentDataSource
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import org.junit.Assert
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

    @get:Rule
    val syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun feedItemsLoadedSuccessfully() {
        val useCase = LoadAnnouncementsUseCase(
            SuccessfulFeedRepository,
            FixedTimeProvider(TimeUtils.ConferenceDays[2].end.toInstant()))
        val resultLivedata = MutableLiveData<Result<List<Announcement>>>()

        useCase(Unit, resultLivedata)

        val result = LiveDataTestUtil.getValue(resultLivedata)
        Assert.assertEquals(result, Result.Success(TestAnnouncementDataSource.feedItems))
    }

    @Test
    fun feedItemsLoadedUnsuccessfully() {
        val useCase = LoadAnnouncementsUseCase(
            UnsuccessfulFeedRepository,
            FixedTimeProvider(TimeUtils.ConferenceDays[2].end.toInstant()))
        val resultLivedata = MutableLiveData<Result<List<Announcement>>>()

        useCase(Unit, resultLivedata)

        val result = LiveDataTestUtil.getValue(resultLivedata)
        assertTrue(result is Result.Error)
    }

    @Test
    fun feedItemsLoaded_filteredByTimestamp() {
        val useCase = LoadAnnouncementsUseCase(
            SuccessfulFeedRepository,
            FixedTimeProvider(TimeUtils.ConferenceDays[0].end.plusHours(1).toInstant()))
        val resultLivedata = MutableLiveData<Result<List<Announcement>>>()

        useCase(Unit, resultLivedata)

        val result = LiveDataTestUtil.getValue(resultLivedata)
        Assert.assertEquals(result, Result.Success(
            listOf(TestAnnouncementDataSource.feedItems[0],
                TestAnnouncementDataSource.feedItems[1])))
    }
}

val SuccessfulFeedRepository = object : FeedRepository {

    override fun getAnnouncements(): List<Announcement> =
        TestAnnouncementDataSource.getAnnouncements()

    override fun getMoments(): List<Moment> = TestMomentDataSource.getMoments()
}

val UnsuccessfulFeedRepository = object : FeedRepository {

    override fun getAnnouncements(): List<Announcement> {
        throw Exception("Error!")
    }

    override fun getMoments(): List<Moment> {
        throw Exception("Error!")
    }
}

/**
 * Fix the TimeProvider to a fixed time
 * TODO: Better to share this class with the one in the mobile module
 */
private class FixedTimeProvider(var instant: Instant) : TimeProvider {

    override fun now(): Instant {
        return instant
    }
}
