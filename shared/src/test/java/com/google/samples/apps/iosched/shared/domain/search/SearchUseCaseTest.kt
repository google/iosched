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

@file:Suppress(names = ["FunctionName"])

package com.google.samples.apps.iosched.shared.domain.search

import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.FakeSearchAppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsInstanceOf.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@ExperimentalCoroutinesApi
@RunWith(Parameterized::class)
class SearchUseCaseTest(private val useCase: SessionSearchUseCase) {

    companion object {
        val coroutineRule = MainCoroutineRule()

        @JvmStatic
        @Parameterized.Parameters
        fun useCases() = listOf(
            arrayOf(
                SessionSimpleSearchUseCase(
                    DefaultSessionAndUserEventRepository(
                        TestUserEventDataSource(),
                        DefaultSessionRepository(TestDataRepository)
                    ),
                    coroutineRule.testDispatcher
                )
            ),
            arrayOf(
                SessionFtsSearchUseCase(
                    DefaultSessionAndUserEventRepository(
                        TestUserEventDataSource(),
                        DefaultSessionRepository(TestDataRepository)
                    ),
                    FakeSearchAppDatabase(),
                    coroutineRule.testDispatcher
                )
            )
        )
    }

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = Companion.coroutineRule

    @Test
    fun `match on title`() = coroutineRule.runBlockingTest {
        val result = useCase(
            SessionSearchUseCaseParams("user1", FakeSearchAppDatabase.QUERY_TITLE)
        ).first { it !is Loading }

        assertSearchResults(result, listOf(TestData.userSession0))
    }

    @Test
    fun `match on description`() = coroutineRule.runBlockingTest {
        val result = useCase(
            SessionSearchUseCaseParams("user1", FakeSearchAppDatabase.QUERY_ABSTRACT)
        ).first { it !is Loading }

        assertSearchResults(result, listOf(TestData.userSession0))
    }

    @Test
    fun `invalid query returns no results`() = coroutineRule.runBlockingTest {
        val result = useCase(
            SessionSearchUseCaseParams("user1", FakeSearchAppDatabase.QUERY_EMPTY)
        ).first { it !is Loading }

        assertSearchResults(result, emptyList())
    }

    @Test
    fun `blank query returns no results`() = coroutineRule.runBlockingTest {
        val result = useCase(
            SessionSearchUseCaseParams("user1", FakeSearchAppDatabase.QUERY_ONLY_SPACES)
        ).first { it !is Loading }

        assertSearchResults(result, emptyList())
    }

    private fun assertSearchResults(
        result: Result<List<UserSession>>,
        expectedSessions: List<UserSession>
    ) {
        assertThat(result, `is`(instanceOf(Success::class.java)))
        val sessions = (result as Success).data
        assertThat(sessions, `is`(equalTo(expectedSessions)))
    }
}
