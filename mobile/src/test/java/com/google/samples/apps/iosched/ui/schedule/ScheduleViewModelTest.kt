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

@file:Suppress("FunctionName")

package com.google.samples.apps.iosched.ui.schedule

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.login.AuthenticatedUser
import com.google.samples.apps.iosched.shared.data.login.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.session.agenda.AgendaRepository
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.UserEventsMessage
import com.google.samples.apps.iosched.shared.domain.tags.LoadTagsByCategoryUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventUseCase
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.schedule.TagFilterMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeLoginViewModelPlugin
import com.google.samples.apps.iosched.test.util.fakes.FakeStarEventUseCase
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.login.DefaultLoginViewModelPlugin
import com.google.samples.apps.iosched.ui.login.FakeAuthenticatedUserInfo
import com.google.samples.apps.iosched.ui.login.LoginViewModelPlugin
import com.google.samples.apps.iosched.ui.schedule.day.TestUserEventDataSource
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [ScheduleViewModel].
 */
class ScheduleViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testDataIsLoaded_ObservablesUpdated() { // TODO: Very slow test (1s)
        // Create test use cases with test data
        val loadSessionsUseCase = LoadUserSessionsByDayUseCase(
                DefaultSessionAndUserEventRepository(
                        TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository))
        )
        val loadTagsUseCase = LoadTagsByCategoryUseCase(TagRepository(TestDataRepository))
        val loginViewModelPlugin = createDefaultLoginViewModelPlugin()
        // Create ViewModel with the use cases
        val viewModel = createScheduleViewModel(loadSessionsUseCase = loadSessionsUseCase,
                loadTagsUseCase = loadTagsUseCase, loginViewModelDelegate = loginViewModelPlugin)

        // Check that data were loaded correctly
        // Sessions
        for (day in ConferenceDay.values()) {
            assertEquals(
                    TestData.userSessionMap[day],
                    LiveDataTestUtil.getValue(viewModel.getSessionsForDay(day))
            )
        }
        assertFalse(LiveDataTestUtil.getValue(viewModel.isLoading)!!)
        // Tags
        assertEquals(TestData.tagFiltersList, LiveDataTestUtil.getValue(viewModel.tagFilters))
    }

    @Test
    fun profileClicked_whileLoggedIn_logsOut() {
        val loginViewModelComponent = createLoginViewModelComponent()

        // Create ViewModel with the use cases
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)
        loginViewModelComponent.injectIsLoggedIn = true

        // click profile
        viewModel.onProfileClicked()

        assertEquals(1, loginViewModelComponent.logoutRequestsEmitted)
    }

    @Test
    fun profileClicked_whileLoggedOut_logsIn() {
        val loginViewModelComponent = createLoginViewModelComponent()

        // Create ViewModel with the use cases
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        loginViewModelComponent.injectIsLoggedIn = false

        // click profile
        viewModel.onProfileClicked()

        assertEquals(1, loginViewModelComponent.loginRequestsEmitted)
    }

    @Test
    fun loggedInUser_setsProfileContentDescription() {
        // Given a mock firebase user
        val loginViewModelComponent = createDefaultLoginViewModelPlugin()
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        // Check that the expected content description is set
        assertEquals(R.string.a11y_logout,
                LiveDataTestUtil.getValue(viewModel.profileContentDesc))
    }

    @Test
    fun noLoggedInUser_setsProfileContentDescription() {
        // Given no firebase user
        val noFirebaseUser = null

        // Create ViewModel
        val observableFirebaseUserUseCase = createFirebaseUserDataSource(noFirebaseUser)
        val loginViewModelComponent = DefaultLoginViewModelPlugin(observableFirebaseUserUseCase)
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        // Check that the expected content description is set
        assertEquals(R.string.a11y_login,
                LiveDataTestUtil.getValue(viewModel.profileContentDesc))
    }

    @Test
    fun errorLoggingIn_setsProfileContentDescription() {
        // Given no firebase user
        val errorLoadingFirebaseUser = Result.Error(Exception())

        // Create ViewModel
        val observableFirebaseUserUseCase = createFirebaseUserDataSource(errorLoadingFirebaseUser)
        val loginViewModelComponent = DefaultLoginViewModelPlugin(observableFirebaseUserUseCase)
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        // Check that the expected content description is set
        assertEquals(R.string.a11y_login,
                LiveDataTestUtil.getValue(viewModel.profileContentDesc))
    }

    @Test
    fun testDataIsLoaded_Fails() {
        // Create ViewModel
        val viewModel = createScheduleViewModel()
        val errorMsg = LiveDataTestUtil.getValue(viewModel.errorMessage)
        assertTrue(errorMsg?.peekContent()?.isNotEmpty() ?: false)
    }

    private fun createScheduleViewModel(
            loadSessionsUseCase: LoadUserSessionsByDayUseCase = createTestLoadUserSessionsByDayUseCase(),
            loadAgendaUseCase: LoadAgendaUseCase = createAgendaExceptionUseCase(),
            loadTagsUseCase: LoadTagsByCategoryUseCase = createTagsExceptionUseCase(),
            loginViewModelDelegate: LoginViewModelPlugin = createLoginViewModelComponent(),
            starEventUseCase: StarEventUseCase = createStarEventUseCase(),
            reservationActionUseCase: ReservationActionUseCase = createReservationActionUseCase()
    ): ScheduleViewModel {
        return ScheduleViewModel(
                loadSessionsUseCase, loadAgendaUseCase, loadTagsUseCase, loginViewModelDelegate,
                starEventUseCase, reservationActionUseCase)
    }

    /** Starring **/
    @Test
    fun testStarEvent() {
        // Create test use cases with test data
        val loginViewModelComponent = createDefaultLoginViewModelPlugin()
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)
        viewModel.onStarClicked(TestData.session0, TestData.userEvents[0])

        val starEvent: Event<SnackbarMessage>? =
                LiveDataTestUtil.getValue(viewModel.snackBarMessage)

        val snackbarEventContent = starEvent?.getContentIfNotHandled()

        assertThat(snackbarEventContent?.messageId,
                `is`(equalTo(R.string.event_starred)))

        assertThat(snackbarEventContent?.actionId,
                `is`(equalTo(R.string.got_it)))

        //TODO: check changes in data source
    }

    @Test
    fun testUnstarEvent() {
        val loginViewModelComponent = createDefaultLoginViewModelPlugin()
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        viewModel.onStarClicked(TestData.session1, TestData.userEvents[1])

        val starEvent: Event<SnackbarMessage>?
                = LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        val snackbarEventContent = starEvent?.getContentIfNotHandled()

        assertThat(snackbarEventContent?.messageId,
                `is`(equalTo(R.string.event_unstarred)))

        assertThat(snackbarEventContent?.longDuration,
                `is`(equalTo(false)))
    }

    private fun createDefaultLoginViewModelPlugin(): DefaultLoginViewModelPlugin {
        val fakeUser = FakeAuthenticatedUserInfo
        val observableFirebaseUserUseCase = createFirebaseUserDataSource(Success(fakeUser))
        return DefaultLoginViewModelPlugin(observableFirebaseUserUseCase)
    }

    @Test
    fun testStarNullUserEvent() {
        // Create test use cases with test data
        val loginViewModelComponent = createDefaultLoginViewModelPlugin()
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        viewModel.onStarClicked(TestData.session0, null)

        val starEvent: Event<SnackbarMessage>? =
                LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        assertThat(starEvent?.getContentIfNotHandled()?.messageId,
                `is`(equalTo(R.string.event_starred)))
    }

    @Test
    fun testStar_notLoggedInUser() {
        // Create test use cases with test data
        val loginDelegate = FakeLoginViewModelPlugin()
        loginDelegate.injectIsLoggedIn = false

        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginDelegate)

        viewModel.onStarClicked(TestData.session0, TestData.userEvents[1])

        val starEvent: Event<SnackbarMessage>? =
                LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        assertThat(starEvent?.getContentIfNotHandled()?.messageId,
                `is`(not(equalTo(R.string.reservation_request_succeeded))))
        // TODO change with actual resource used
    }

    /** Reservations **/

    @Test
    fun testReserveEvent() {
        // Create test use cases with test data
        val loginViewModelComponent = createDefaultLoginViewModelPlugin()
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        viewModel.onReservationClicked(TestData.session0, TestData.userEvents[4])

        val event: Event<SnackbarMessage>? = LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        assertThat(event?.getContentIfNotHandled()?.messageId,
                `is`(equalTo(R.string.reservation_request_succeeded)))
    }

    @Test
    fun testReserveEvent_notLoggedIn() {
        // Create test use cases with test data
        val loginDelegate = FakeLoginViewModelPlugin()
        loginDelegate.injectIsLoggedIn = false

        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginDelegate)

        viewModel.onReservationClicked(TestData.session0, TestData.userEvents[4])

        val event: Event<SnackbarMessage>? = LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        assertThat(event?.getContentIfNotHandled()?.messageId,
                `is`(not(equalTo(R.string.reservation_request_succeeded))))
        // TODO change with actual resource used
    }

    @Test
    fun testCancelEvent() {
        val loginViewModelComponent = createDefaultLoginViewModelPlugin()
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        viewModel.onReservationClicked(TestData.session1, TestData.userEvents[0])

        val event: Event<SnackbarMessage>? = LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        assertThat(event?.getContentIfNotHandled()?.messageId,
                `is`(equalTo(R.string.reservation_cancel_succeeded)))

    }

    /** New reservation / waitlist **/

    @Test
    fun reservationReceived() {
        // Create test use cases with test data
        val userEventsResult: MutableLiveData<UserEventsResult>
                = MutableLiveData<UserEventsResult>()
        val source = TestUserEventDataSource(userEventsResult)
        val loadSessionsUseCase = createTestLoadUserSessionsByDayUseCase(source)
        val viewModel = createScheduleViewModel(loadSessionsUseCase = loadSessionsUseCase)
        loadSessionsUseCase.execute(TagFilterMatcher() to "")

        // A session goes from not-reserved to reserved
        val oldValue = LiveDataTestUtil.getValue(userEventsResult)
        val newValue = oldValue!!.copy(userEventsMessage = UserEventsMessage.CHANGES_IN_RESERVATIONS)

        userEventsResult.postValue(newValue)

        val starEvent: Event<SnackbarMessage>? =
                LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        assertThat(starEvent?.getContentIfNotHandled()?.messageId,
                `is`(equalTo(R.string.reservation_new)))
    }

    @Test
    fun waitlistReceived() {
        val userEventsResult = MutableLiveData<UserEventsResult>()
        val source = TestUserEventDataSource(userEventsResult)
        val loadSessionsUseCase = createTestLoadUserSessionsByDayUseCase(source)
        val viewModel = createScheduleViewModel(loadSessionsUseCase = loadSessionsUseCase)
        loadSessionsUseCase.execute(TagFilterMatcher() to "")

        // A session goes from not-reserved to reserved
        val oldValue = LiveDataTestUtil.getValue(userEventsResult)
        val newValue = oldValue!!.copy(allDataSynced = true,
                userEventsMessage = UserEventsMessage.CHANGES_IN_WAITLIST)

        userEventsResult.postValue(newValue)

        val starEvent: Event<SnackbarMessage>? =
                LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        assertThat(starEvent?.getContentIfNotHandled()?.messageId,
                `is`(equalTo(R.string.waitlist_new)))
    }

    /**
     * Creates a test [LoadUserSessionsByDayUseCase].
     */
    private fun createTestLoadUserSessionsByDayUseCase(
            userEventDataSource: UserEventDataSource = TestUserEventDataSource()
    ): LoadUserSessionsByDayUseCase {

        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val userEventRepository = DefaultSessionAndUserEventRepository(
                userEventDataSource, sessionRepository)

        return LoadUserSessionsByDayUseCase(userEventRepository)
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createTagsExceptionUseCase(): LoadTagsByCategoryUseCase {
        return object : LoadTagsByCategoryUseCase(TagRepository(TestDataRepository)) {
            override fun execute(parameters: Unit): List<Tag> {
                throw Exception("Testing exception")
            }
        }
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createAgendaExceptionUseCase(): LoadAgendaUseCase {
        return object : LoadAgendaUseCase(AgendaRepository(TestDataRepository)) {
            override fun execute(parameters: Unit): List<Block> {
                throw Exception("Testing exception")
            }
        }
    }

    private fun createLoginViewModelComponent() = FakeLoginViewModelPlugin()

    private fun createFirebaseUserDataSource(
            user: Result<AuthenticatedUserInfo>?
    ) : AuthenticatedUser {

        val res = MutableLiveData<Result<AuthenticatedUserInfo>?>().apply { value = user }

        return object : AuthenticatedUser {
            override fun getToken(): LiveData<Result<String>> {
                TODO("not implemented")
            }

            override fun getCurrentUser(): LiveData<Result<AuthenticatedUserInfo>?> {
                return res
            }
        }
    }

    private fun createStarEventUseCase() = FakeStarEventUseCase()

    private fun createReservationActionUseCase() = object: ReservationActionUseCase(
            DefaultSessionAndUserEventRepository(
                    TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository))) {}
}
