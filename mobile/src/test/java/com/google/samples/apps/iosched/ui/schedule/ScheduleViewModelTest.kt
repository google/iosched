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
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.login.FirebaseUserDataSource
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.data.session.UserEventRepository
import com.google.samples.apps.iosched.shared.data.session.agenda.AgendaRepository
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.tags.LoadTagsByCategoryUseCase
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeLoginViewModelPlugin
import com.google.samples.apps.iosched.ui.login.DefaultLoginViewModelPlugin
import com.google.samples.apps.iosched.ui.login.LoginViewModelPlugin
import com.google.samples.apps.iosched.ui.schedule.day.TestUserEventDataSource
import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun testDataIsLoaded_ObservablesUpdated() {
        // Create test use cases with test data
        val loadSessionsUseCase = LoadUserSessionsByDayUseCase(
            SessionRepository(TestDataRepository),
            UserEventRepository(TestUserEventDataSource)
        )
        val loadAgendaUseCase = LoadAgendaUseCase(AgendaRepository(TestDataRepository))
        val loadTagsUseCase = LoadTagsByCategoryUseCase(TagRepository(TestDataRepository))
        val loginViewModelComponent = createLoginViewModelComponent()

        // Create ViewModel with the use cases
        val viewModel = ScheduleViewModel(
            loadSessionsUseCase, loadAgendaUseCase, loadTagsUseCase, loginViewModelComponent
        )

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
        val mockFirebaseUser = Result.Success(mock<FirebaseUser>())

        // Create ViewModel
        val observableFirebaseUserUseCase = createFirebaseUserDataSource(mockFirebaseUser)
        val loginViewModelComponent = DefaultLoginViewModelPlugin(observableFirebaseUserUseCase)
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        // Check that the expected content description is set
        assertEquals(R.string.a11y_logout, LiveDataTestUtil.getValue(viewModel.profileContentDesc))
    }

    @Test
    fun noLoggedInUser_setsProfileContentDescription() {
        // Given no firebase user
        val noFirebaseUser = Result.Success(null)

        // Create ViewModel
        val observableFirebaseUserUseCase = createFirebaseUserDataSource(noFirebaseUser)
        val loginViewModelComponent = DefaultLoginViewModelPlugin(observableFirebaseUserUseCase)
        val viewModel = createScheduleViewModel(loginViewModelDelegate = loginViewModelComponent)

        // Check that the expected content description is set
        assertEquals(R.string.a11y_login, LiveDataTestUtil.getValue(viewModel.profileContentDesc))
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
        assertEquals(R.string.a11y_login, LiveDataTestUtil.getValue(viewModel.profileContentDesc))
    }

    @Test
    fun testDataIsLoaded_Fails() {
        // Create ViewModel
        val viewModel = createScheduleViewModel()
        val errorMsg = LiveDataTestUtil.getValue(viewModel.errorMessage)
        assertTrue(errorMsg?.peekContent()?.isNotEmpty() ?: false)
    }

    private fun createScheduleViewModel(
        loadSessionsUseCase: LoadUserSessionsByDayUseCase = createSessionsExceptionUseCase(),
        loadAgendaUseCase: LoadAgendaUseCase = createAgendaExceptionUseCase(),
        loadTagsUseCase: LoadTagsByCategoryUseCase = createTagsExceptionUseCase(),
        loginViewModelDelegate: LoginViewModelPlugin = createLoginViewModelComponent()
    ): ScheduleViewModel {
        return ScheduleViewModel(
            loadSessionsUseCase, loadAgendaUseCase, loadTagsUseCase, loginViewModelDelegate
        )
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createSessionsExceptionUseCase(): LoadUserSessionsByDayUseCase {
        val sessionRepository = SessionRepository(TestDataRepository)
        val userEventRepository = UserEventRepository(TestUserEventDataSource)
        return object : LoadUserSessionsByDayUseCase(sessionRepository, userEventRepository) {
            override fun execute(parameters: Pair<UserSessionMatcher, String>):
                    Map<ConferenceDay, List<UserSession>> {
                throw Exception("Testing exception")
            }
        }
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

    private fun createFirebaseUserDataSource(user: Result<FirebaseUser?>) : FirebaseUserDataSource {

        return object : FirebaseUserDataSource {
            override fun getToken(): LiveData<Result<String>> {
                TODO("not implemented")
            }

            override fun getCurrentUser(): LiveData<Result<FirebaseUser?>?> {
                val res = MutableLiveData<Result<FirebaseUser?>>().apply { value = user }
                return res
            }
        }
    }
}
