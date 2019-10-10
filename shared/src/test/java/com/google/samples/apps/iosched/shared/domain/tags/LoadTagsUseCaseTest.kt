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
import com.google.samples.apps.iosched.shared.result.Result.Success
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TODO: placeholder/example for use cases.
 */
class LoadTagsUseCaseTest {

    @Test
    fun returnsListOfTags() {
        val loadTagsUseCase = LoadTagsUseCase(TagRepository(TestDataRepository))
        val tags: Success<List<Tag>> =
            loadTagsUseCase.executeNow(Unit) as Success<List<Tag>>

        assertEquals(tags.data, TagRepository(TestDataRepository).getTags())
    }
}
