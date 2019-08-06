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
import com.google.samples.apps.iosched.shared.domain.UseCase
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedSession
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.FakeSearchAppDatabase
import com.google.samples.apps.iosched.test.util.FakeSearchAppDatabase.Companion.QUERY_ABSTRACT
import com.google.samples.apps.iosched.test.util.FakeSearchAppDatabase.Companion.QUERY_EMPTY
import com.google.samples.apps.iosched.test.util.FakeSearchAppDatabase.Companion.QUERY_QUESTION
import com.google.samples.apps.iosched.test.util.FakeSearchAppDatabase.Companion.QUERY_SESSION_0
import com.google.samples.apps.iosched.test.util.FakeSearchAppDatabase.Companion.QUERY_TAGNAME
import com.google.samples.apps.iosched.test.data.runBlockingTest
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsCollectionContaining.hasItem
import org.hamcrest.core.IsCollectionContaining.hasItems
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsInstanceOf.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Unit tests for [SearchUseCase] and [SearchDbUseCase]. Parameterized to test both classes wieh
 * the same cases.
 */
@RunWith(Parameterized::class)
class SearchUseCaseTest(private val useCase: UseCase<String, List<Searchable>>) {

    companion object {

        val coroutineRule = MainCoroutineRule()

        @JvmStatic
        @Parameterized.Parameters
        fun useCases() = listOf(
            arrayOf(
                SearchUseCase(
                    DefaultSessionRepository(TestDataRepository), coroutineRule.testDispatcher
                )
            ),
            arrayOf(
                SearchDbUseCase(
                    DefaultSessionRepository(TestDataRepository),
                    TestDataRepository,
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
        val result = useCase(parameters = QUERY_SESSION_0)
        assertThatResultContainsOnlySession0(result)
    }

    @Test
    fun search_MatchesOnAbstract() = coroutineRule.runBlockingTest {
        val result = useCase(parameters = QUERY_ABSTRACT)
        assertThatResultContainsOnlySession0(result)
    }

    @Test
    fun search_MatchesOnTagName() = coroutineRule.runBlockingTest {
        val result = useCase(parameters = QUERY_TAGNAME)
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val sessions = (result as Result.Success).data.map {
            (it as SearchedSession).session
        }
        assertThat(sessions.size, `is`(equalTo(3)))
        assertThat(sessions, hasItems(TestData.session0, TestData.session1, TestData.session2))
    }

    @Test
    fun search_MatchesOnUserQuestion() = coroutineRule.runBlockingTest {
        val result = useCase(parameters = QUERY_QUESTION)
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val sessions = (result as Result.Success).data.map {
            (it as SearchedSession).session
        }
        assertThat(sessions.size, `is`(equalTo(3)))
        assertThat(sessions, hasItems(TestData.session0, TestData.session1, TestData.session2))
    }

    @Test
    fun search_returnsEmptyListForInvalidQuery() = coroutineRule.runBlockingTest {
        val result = useCase(parameters = QUERY_EMPTY)
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
