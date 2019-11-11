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

package com.google.samples.apps.iosched.model

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is

class TagTest {

    val androidTagId1 = Tag("1", "TRACK", "track_android", 0, "Android", 0xFFAED581.toInt())
    val androidTagId2 = Tag("2", "TRACK", "track_android", 0, "Android", 0xFFAED581.toInt())
    val webTagId2 = Tag("2", "TRACK", "track_web", 1, "Web", 0xFFFFF176.toInt())

    @Test
    fun tag_differentId_notEqual() {
        assertThat(androidTagId1, Is(not(equalTo(webTagId2))))
    }

    @Test
    fun tag_sameId_equal() {
        assertThat(androidTagId2, Is(equalTo(webTagId2)))
    }

    @Test
    fun tag_sameIdDifferentContent_equal() {
        assertThat(androidTagId2, Is(equalTo(webTagId2)))
    }
}