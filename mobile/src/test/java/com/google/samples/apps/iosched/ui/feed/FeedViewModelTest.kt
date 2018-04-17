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

package com.google.samples.apps.iosched.ui.feed

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.shared.data.feed.DefaultFeedRepository
import com.google.samples.apps.iosched.shared.domain.feed.LoadFeedUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [FeedViewModel]
 */
class FeedViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testDataIsLoaded_ObservablesUpdated() {
        // Create a test use case with test data
        val testData = TestData.feed
        val repository = DefaultFeedRepository(TestFeedDataSource)
        val loadFeedUseCase = LoadFeedUseCase(repository)

        // Create ViewModel with the use case and load the feed
        val viewModel = FeedViewModel(loadFeedUseCase)
        viewModel.loadFeed()
        val feedObservable = LiveDataTestUtil.getValue(viewModel.feed)

        // Check that data was loaded correctly
        assertThat(feedObservable?.size, `is`(equalTo(testData.size)))
        for ((index, item) in testData.withIndex()) {
            val actual = feedObservable?.get(index)
            assertThat(actual, `is`(equalTo(item)))
        }

        assertThat("Once feed items are loaded, isLoading should be false",
                LiveDataTestUtil.getValue(viewModel.isLoading),
                `is`(false))
    }

    @Test
    fun testDataIsLoaded_Fails() {
        // Create a test use case with test repository that returns an error
        val loadFeedUseCase = FailingUseCase

        // Create ViewModel with the use case
        val viewModel = FeedViewModel(loadFeedUseCase)
        viewModel.loadFeed()

        // Verify that an error was caught
        val errorMessage = LiveDataTestUtil.getValue(viewModel.errorMessage)
        assertTrue(errorMessage?.peekContent()?.isNotEmpty() ?: false)

    }

    /**
     * Use case that always returns an error when executed.
     */
    object FailingUseCase : LoadFeedUseCase(DefaultFeedRepository(TestFeedDataSource)) {
        override fun execute(parameters: Unit) {
            result.postValue(Result.Error(Exception("Error!")))
        }
    }

}