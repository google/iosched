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

package com.google.samples.apps.iosched.shared.domain.tags

import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.TestData.advancedTag
import com.google.samples.apps.iosched.test.data.TestData.androidTag
import com.google.samples.apps.iosched.test.data.TestData.beginnerTag
import com.google.samples.apps.iosched.test.data.TestData.cloudTag
import com.google.samples.apps.iosched.test.data.TestData.codelabsTag
import com.google.samples.apps.iosched.test.data.TestData.intermediateTag
import com.google.samples.apps.iosched.test.data.TestData.sessionsTag
import com.google.samples.apps.iosched.test.data.TestData.webTag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [LoadTagsByCategoryUseCase]
 */
class LoadTagsByCategoryUseCaseTest {

    @ExperimentalCoroutinesApi
    private val dispatcher = TestCoroutineDispatcher()

    @Test
    fun returnsOrderedTags() {
        val useCase = LoadTagsByCategoryUseCase(TagRepository(TestDataRepository), dispatcher)
        val tags = useCase.executeNow(Unit) as Result.Success<List<Tag>>

        // Expected values to assert
        val expected = listOf(
            // category = LEVEL
            beginnerTag, intermediateTag, advancedTag,
            // category = TRACK
            androidTag, cloudTag, webTag,
            // category = TYPE
            sessionsTag, codelabsTag
        )

        assertEquals(expected, tags.data)
    }
}
