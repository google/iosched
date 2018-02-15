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

package com.google.samples.apps.iosched.ui.schedule


import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.shared.data.tag.TagDataSource
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.result.Result
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [LoadTagsByCategoryUseCase]
 */
class LoadTagsByCategoryUseCaseTest {

    @Test
    fun returnsOrderedTags() {
        val useCase = LoadTagsByCategoryUseCase(TagRepository(TestSessionDataSource))
        val tags = useCase.executeNow(Unit) as Result.Success<List<Tag>>

        // Expected values to assert
        val expected = listOf(
                // category = LEVEL
                TestData.beginnerTag, TestData.intermediateTag, TestData.advancedTag,
                // category = TRACK
                TestData.androidTag, TestData.webTag,
                // category = TYPE
                TestData.sessionsTag, TestData.codelabsTag
        )

        assertEquals(expected, tags.data)
    }

    object TestSessionDataSource : TagDataSource {

        override fun getTags(): List<Tag> {
            return TestData.tagsList
        }
    }
}
