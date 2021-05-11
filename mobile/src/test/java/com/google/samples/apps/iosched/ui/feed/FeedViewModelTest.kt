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
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.feed.DefaultFeedRepository
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.feed.GetConferenceStateUseCase
import com.google.samples.apps.iosched.shared.domain.feed.LoadAnnouncementsUseCase
import com.google.samples.apps.iosched.shared.domain.feed.LoadCurrentMomentUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.StopSnackbarActionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadStarredAndReservedSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.test.data.CoroutineScope
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeThemedActivityDelegate
import com.google.samples.apps.iosched.test.util.time.FixedTimeProvider
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.ui.theme.ThemedActivityDelegate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertEquals
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
    fun testDataIsLoaded_ObservablesUpdated() = coroutineRule.runBlockingTest {
        // Create ViewModel with the use case and load the feed.
        val viewModel = createFeedViewModel()
        val feedObservable = viewModel.feed.first()

        // Check that data was loaded correctly.
        // At the specified time, the Moment is relevant and there is one Announcement.
        // Add two more for the Sessions carousel and the "Announcements' heading.
        assertThat(feedObservable.size, `is`(equalTo(5)))
        assertThat(feedObservable[0] as? Moment, `is`(equalTo(TestData.moment1)))
        assertThat(
            feedObservable[1] as? AnnouncementsHeader,
            `is`(equalTo(AnnouncementsHeader(false)))
        )
        assertThat(feedObservable[2] as? Announcement, `is`(equalTo(TestData.feedItem1)))
        assertThat(feedObservable[3], `is`(instanceOf(FeedSustainabilitySection::class.java)))
        assertThat(feedObservable[4], `is`(instanceOf(FeedSocialChannelsSection::class.java)))

        // Must cancel because there's a flow in [GetConferenceStateUseCase] that never finishes.
        viewModel.viewModelScope.cancel()
        // Cancel is not synchronous so we need to wait for it to avoid leaks.
        coroutineRule.testDispatcher.advanceUntilIdle()
    }

    @Test
    fun testDataIsLoaded_Fails() = coroutineRule.runBlockingTest {
        // Create ViewModel with a use case that returns an error
        val snackbarMessageManager = createSnackbarMessageManager()
        val viewModel = createFeedViewModel(
            loadAnnouncementUseCase = FailingUseCase(testDispatcher),
            snackbarMessageManager = snackbarMessageManager
        )

        // Observe feed to generate an error
        val feed = viewModel.feed.first()

        // Verify that an error was caught
        val msg = snackbarMessageManager.currentSnackbar.value
        assertEquals(R.string.feed_loading_error, msg?.messageId)

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
        override suspend fun execute(parameters: Instant): List<Announcement> {
            throw Exception("Error!")
        }
    }

    private fun createFeedViewModel(
        loadCurrentMomentUseCase: LoadCurrentMomentUseCase =
            LoadCurrentMomentUseCase(defaultFeedRepository, testDispatcher),
        loadAnnouncementUseCase: LoadAnnouncementsUseCase =
            LoadAnnouncementsUseCase(defaultFeedRepository, testDispatcher),
        loadStarredAndReservedSessionsUseCase: LoadStarredAndReservedSessionsUseCase =
            LoadStarredAndReservedSessionsUseCase(
                DefaultSessionAndUserEventRepository(
                    TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
                ),
                testDispatcher
            ),
        getTimeZoneUseCase: GetTimeZoneUseCase =
            GetTimeZoneUseCase(FakePreferenceStorage(), testDispatcher),
        getConferenceStateUseCase: GetConferenceStateUseCase =
            GetConferenceStateUseCase(testDispatcher, defaultTimeProvider),
        timeProvider: TimeProvider = defaultTimeProvider,
        signInViewModelDelegate: SignInViewModelDelegate = FakeSignInViewModelDelegate().apply {
            loadUser("123")
        },
        themedActivityDelegate: ThemedActivityDelegate = FakeThemedActivityDelegate(),
        snackbarMessageManager: SnackbarMessageManager = createSnackbarMessageManager()
    ): FeedViewModel {
        return FeedViewModel(
            loadCurrentMomentUseCase = loadCurrentMomentUseCase,
            loadAnnouncementsUseCase = loadAnnouncementUseCase,
            loadStarredAndReservedSessionsUseCase = loadStarredAndReservedSessionsUseCase,
            getTimeZoneUseCase = getTimeZoneUseCase,
            getConferenceStateUseCase = getConferenceStateUseCase,
            timeProvider = timeProvider,
            analyticsHelper = FakeAnalyticsHelper(),
            signInViewModelDelegate = signInViewModelDelegate,
            themedActivityDelegate = themedActivityDelegate,
            snackbarMessageManager = snackbarMessageManager
        )
    }

    private fun createSnackbarMessageManager(
        preferenceStorage: PreferenceStorage = FakePreferenceStorage()
    ): SnackbarMessageManager {
        return SnackbarMessageManager(
            preferenceStorage,
            coroutineRule.CoroutineScope(),
            StopSnackbarActionUseCase(preferenceStorage, testDispatcher)
        )
    }
}
