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

package com.google.samples.apps.iosched.shared.data

import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.junit.Assert
import org.junit.Test

/**
 * Tests for [BootstrapConferenceDataSource]
 */
class BootstrapConferenceDataSourceTest {

    @Test
    fun loadJson_resultIsNotEmpty() {
        val data = BootstrapConferenceDataSource.loadAndParseBootstrapData()
        Assert.assertThat(data.sessions, hasSize(greaterThan(0)))
        Assert.assertThat(data.rooms, hasSize(greaterThan(0)))
        Assert.assertThat(data.speakers, hasSize(greaterThan(0)))
        Assert.assertThat(data.tags, hasSize(greaterThan(0)))
    }
}
