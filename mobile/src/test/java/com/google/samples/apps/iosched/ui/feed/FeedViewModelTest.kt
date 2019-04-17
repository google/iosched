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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.feed.DefaultFeedRepository
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.feed.LoadAnnouncementsUseCase
import com.google.samples.apps.iosched.shared.domain.feed.LoadMomentsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeThemedActivityDelegate
import com.google.samples.apps.iosched.test.util.time.FixedTimeProvider
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito

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

    private val mockHandler = Mockito.mock(Handler::class.java)

    @Before
    fun setup() {
        Mockito.`when`(mockHandler.postDelayed(any(Runnable::class.java), anyLong()))
            .thenAnswer { invocation ->
                // Don't call run() on the argument as it will lead to stackOverFlow ;)
                true
            }
    }

    @Test
    fun testDataIsLoaded_ObservablesUpdated() {
        // Create a test use case with test data
        val testData = TestData.feed

        // Create ViewModel with the use case and load the feed
        val viewModel = createFeedViewModel()
        val feedObservable = LiveDataTestUtil.getValue(viewModel.feed)

        // Check that data was loaded correctly
        // Adding '+ 3' as the items will be preceded by 1. The FeedHeader 2. Sessions 3. Announcements header
        assertThat(feedObservable?.size, `is`(equalTo(testData.size + 3)))
        for ((index, item) in testData.withIndex()) {
            val actual = feedObservable?.get(index + 3)
            assertThat(actual, `is`(equalTo(item)))
        }
    }

    @Test
    fun testDataIsLoaded_Fails() {
        // Create a test use case with test repository that returns an error
        val loadFeedUseCase = FailingUseCase

        // Create ViewModel with the use case
        val viewModel = createFeedViewModel(loadAnnouncementUseCase = loadFeedUseCase)

        // Verify that an error was caught
        val errorMessage = LiveDataTestUtil.getValue(viewModel.errorMessage)
        assertTrue(errorMessage?.peekContent()?.isNotEmpty() ?: false)
    }

    /**
     * Use case that always returns an error when executed.
     */
    object FailingUseCase :
        LoadAnnouncementsUseCase(
            DefaultFeedRepository(TestAnnouncementDataSource,
                TestMomentDataSource
            ),
            FixedTimeProvider(TimeUtils.ConferenceDays[2].end.toInstant())
        ) {
        override fun execute(parameters: Unit) {
            result.postValue(Result.Error(Exception("Error!")))
        }
    }

    private fun createFeedViewModel(
        loadFilteredSessionsUseCase: LoadFilteredUserSessionsUseCase =
            LoadFilteredUserSessionsUseCase(
                DefaultSessionAndUserEventRepository(
                    TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
                )
            ),

        signInViewModelDelegate: SignInViewModelDelegate = FakeSignInViewModelDelegate().apply {
            loadUser("123")
        },
        loadAnnouncementUseCase: LoadAnnouncementsUseCase = LoadAnnouncementsUseCase(
            DefaultFeedRepository(TestAnnouncementDataSource,
                TestMomentDataSource
            ),
            FixedTimeProvider(TimeUtils.ConferenceDays[2].end.toInstant())
        ),
        loadMomentsUseCase: LoadMomentsUseCase = LoadMomentsUseCase(
            DefaultFeedRepository(TestAnnouncementDataSource,
                TestMomentDataSource
            )
        )
    ): FeedViewModel {
        return FeedViewModel(
            loadAnnouncementsUseCase = loadAnnouncementUseCase,
            loadFilteredUserSessionsUseCase = loadFilteredSessionsUseCase,
            signInViewModelDelegate = signInViewModelDelegate,
            getTimeZoneUseCase = createGetTimeZoneUseCase(),
            themedActivityDelegate = FakeThemedActivityDelegate(),
            feedHeaderLiveData = FeedHeaderLiveData(loadMomentsUseCase).apply {
                handler = mockHandler
            },
            analyticsHelper = FakeAnalyticsHelper()
        )
    }

    private fun createGetTimeZoneUseCase() =
        object : GetTimeZoneUseCase(FakePreferenceStorage()) {}
}