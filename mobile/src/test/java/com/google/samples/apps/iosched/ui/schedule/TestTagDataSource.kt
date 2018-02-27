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

import com.google.samples.apps.iosched.shared.data.tag.TagDataSource
import com.google.samples.apps.iosched.shared.model.Tag

/**
 * Generates dummy tag data to be used in tests.
 */
object TestTagDataSource : TagDataSource {

    private val androidTag = Tag("1", "TRACK", 0, "Android", 0xFFAED581.toInt())
    private val webTag = Tag("2", "TRACK", 1, "Web", 0xFFFFF176.toInt())
    private val sessionsTag = Tag("101", "TYPE", 0, "Sessions", 0)
    private val codelabsTag = Tag("102", "TYPE", 1, "Codelabs", 0)
    private val beginnerTag = Tag("201", "LEVEL", 0, "Beginner", 0)
    private val intermediateTag = Tag("202", "LEVEL", 1, "Intermediate", 0)

    override fun getTags() =
        listOf(androidTag, webTag, sessionsTag, codelabsTag, beginnerTag, intermediateTag)
}
