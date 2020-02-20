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

import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.domain.CoroutinesUseCase
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedSession
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.FakeSearchAppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsCollectionContaining.hasItem
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsInstanceOf.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@ExperimentalCoroutinesApi
@RunWith(Parameterized::class)
class SearchUseCaseTest(private val useCase: CoroutinesUseCase<String, List<Searchable>>) {

    companion object {
        val coroutineRule = MainCoroutineRule()

        @JvmStatic
        @Parameterized.Parameters
        fun useCases() = listOf(
            arrayOf(
                SessionSimpleSearchUseCase(
                    DefaultSessionRepository(TestDataRepository),
                    coroutineRule.testDispatcher
                )
            ),
            arrayOf(
                SessionFtsSearchUseCase(
                    DefaultSessionRepository(TestDataRepository),
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
    fun search_MatchesOnTitle() = coroutineRule.runBlockingTest {
        val result = useCase(parameters = FakeSearchAppDatabase.QUERY_TITLE)
        assertThatResultContainsOnlySession0(result)
    }

    @Test
    fun search_MatchesOnAbstract() = coroutineRule.runBlockingTest {
        val result = useCase(FakeSearchAppDatabase.QUERY_ABSTRACT)
        assertThatResultContainsOnlySession0(result)
    }

    @Test
    fun search_returnsEmptyListForInvalidQuery() = coroutineRule.runBlockingTest {
        val result = useCase(FakeSearchAppDatabase.QUERY_EMPTY)
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val sessions = (result as Result.Success).data
        assertThat(sessions, `is`(equalTo(emptyList())))
    }

    @Test
    fun search_emptyQuery() = coroutineRule.runBlockingTest {
        val result = useCase(FakeSearchAppDatabase.QUERY_ONLY_SPACES)
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val sessions = (result as Result.Success).data
        assertThat(sessions, `is`(equalTo(emptyList())))
    }

    private fun assertThatResultContainsOnlySession0(result: Result<List<Searchable>>) =
        coroutineRule.runBlockingTest {
            assertThat(result, `is`(instanceOf(Result.Success::class.java)))

            val sessions = (result as Result.Success).data.map {
                (it as SearchedSession).session
            }
            assertThat(sessions.size, `is`(equalTo(1)))
            assertThat(sessions, hasItem(TestData.session0))
        }
}
