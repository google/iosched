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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.feed.DefaultFeedRepository
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.feed.GetConferenceStateUseCase
import com.google.samples.apps.iosched.shared.domain.feed.LoadAnnouncementsUseCase
import com.google.samples.apps.iosched.shared.domain.feed.LoadCurrentMomentUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCaseLegacy
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeThemedActivityDelegate
import com.google.samples.apps.iosched.test.util.time.FixedTimeProvider
import com.google.samples.apps.iosched.ui.SectionHeader
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.ui.theme.ThemedActivityDelegate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancel
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.threeten.bp.Instant

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

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val testDispatcher = coroutineRule.testDispatcher

    private val defaultFeedRepository =
        DefaultFeedRepository(TestAnnouncementDataSource, TestMomentDataSource)

    // Loads feed roughly during the Keynote time
    private val defaultTimeProvider =
        FixedTimeProvider(TestData.TestConferenceDays[0].start.plusHours(4).toInstant())

    @Test
    fun testDataIsLoaded_ObservablesUpdated() {
        // Create ViewModel with the use case and load the feed.
        val viewModel = createFeedViewModel()
        val feedObservable = LiveDataTestUtil.getValue(viewModel.feed)

        // Check that data was loaded correctly.
        // At the specified time, the Moment is relevant and there is one Announcement.
        // Add two more for the Sessions carousel and the "Announcements' heading.
        assertThat(feedObservable?.size, `is`(equalTo(4)))
        assertThat(feedObservable?.get(0) as? Moment, `is`(equalTo(TestData.moment1)))
        assertThat(feedObservable?.get(1), `is`(instanceOf(SectionHeader::class.java)))
        assertThat(feedObservable?.get(2) as? Announcement, `is`(equalTo(TestData.feedItem1)))
        assertThat(feedObservable?.get(3), `is`(instanceOf(FeedSocialChannelsSection::class.java)))

        // Must cancel because there's a flow in [GetConferenceStateUseCase] that never finishes.
        viewModel.viewModelScope.cancel()
        // Cancel is not synchronous so we need to wait for it to avoid leaks.
        coroutineRule.testDispatcher.advanceUntilIdle()
    }

    @Test
    fun testDataIsLoaded_Fails() {
        // Create ViewModel with a use case that returns an error
        val viewModel =
            createFeedViewModel(loadAnnouncementUseCase = FailingUseCase(testDispatcher))

        // Verify that an error was caught
        val errorMessage = LiveDataTestUtil.getValue(viewModel.errorMessage)
        assertTrue(errorMessage?.peekContent()?.isNotEmpty() ?: false)

        // Must cancel because there's a flow in [GetConferenceStateUseCase] that never finishes.
        viewModel.viewModelScope.cancel()
        // Cancel is not synchronous so we need to wait for it to avoid leaks.
        coroutineRule.testDispatcher.advanceUntilIdle()
    }

    /**
     * Use case that always returns an error when executed.
     */
    class FailingUseCase(coroutineDispatcher: CoroutineDispatcher) : LoadAnnouncementsUseCase(
        DefaultFeedRepository(TestAnnouncementDataSource, TestMomentDataSource),
        coroutineDispatcher
    ) {
        override fun execute(parameters: Instant): List<Announcement> {
            throw Exception("Error!")
        }
    }

    private fun createFeedViewModel(
        loadCurrentMomentUseCase: LoadCurrentMomentUseCase =
            LoadCurrentMomentUseCase(defaultFeedRepository, testDispatcher),
        loadAnnouncementUseCase: LoadAnnouncementsUseCase =
            LoadAnnouncementsUseCase(defaultFeedRepository, testDispatcher),
        loadFilteredSessionsUseCase: LoadFilteredUserSessionsUseCase =
            LoadFilteredUserSessionsUseCase(
                DefaultSessionAndUserEventRepository(
                    TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
                ),
                testDispatcher
            ),
        getTimeZoneUseCaseLegacy: GetTimeZoneUseCaseLegacy =
            GetTimeZoneUseCaseLegacy(FakePreferenceStorage(), testDispatcher),
        getConferenceStateUseCase: GetConferenceStateUseCase =
            GetConferenceStateUseCase(testDispatcher, defaultTimeProvider),
        timeProvider: TimeProvider = defaultTimeProvider,
        signInViewModelDelegate: SignInViewModelDelegate = FakeSignInViewModelDelegate().apply {
            loadUser("123")
        },
        themedActivityDelegate: ThemedActivityDelegate = FakeThemedActivityDelegate()
    ): FeedViewModel {
        return FeedViewModel(
            loadCurrentMomentUseCase = loadCurrentMomentUseCase,
            loadAnnouncementsUseCase = loadAnnouncementUseCase,
            loadFilteredUserSessionsUseCase = loadFilteredSessionsUseCase,
            getTimeZoneUseCaseLegacy = getTimeZoneUseCaseLegacy, // TODO(COROUTINES): Migrate
            getConferenceStateUseCase = getConferenceStateUseCase,
            timeProvider = timeProvider,
            analyticsHelper = FakeAnalyticsHelper(),
            signInViewModelDelegate = signInViewModelDelegate,
            themedActivityDelegate = themedActivityDelegate
        )
    }
}
