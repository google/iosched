/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.ui.map

import com.google.samples.apps.iosched.shared.BuildConfig
import com.google.samples.apps.iosched.ui.map.MapVariant.AFTER_DARK
import com.google.samples.apps.iosched.ui.map.MapVariant.CONCERT
import com.google.samples.apps.iosched.ui.map.MapVariant.DAY
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.threeten.bp.ZonedDateTime

class MapVariantTest {

    @Test
    fun forTime_beforeConference_returnsDay() {
        val time = ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_START).minusMinutes(1)
            .toInstant()
        assertThat(MapVariant.forTime(time), `is`(equalTo(DAY)))
    }

    @Test
    fun forTime_afterConference_returnsDay() {
        val time = ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY3_END).plusMinutes(1).toInstant()
        assertThat(MapVariant.forTime(time), `is`(equalTo(DAY)))
    }

    @Test
    fun forTime_duringAnyDay_returnsDay() {
        var time = ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_START).plusMinutes(1).toInstant()
        assertThat(MapVariant.forTime(time), `is`(equalTo(DAY)))

        time = ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY2_START).plusMinutes(1).toInstant()
        assertThat(MapVariant.forTime(time), `is`(equalTo(DAY)))

        time = ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY3_START).plusMinutes(1).toInstant()
        assertThat(MapVariant.forTime(time), `is`(equalTo(DAY)))
    }

    @Test
    fun forTime_duringAfterHours_returnsAfterHours() {
        val time = ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_AFTERHOURS_START).plusMinutes(1)
            .toInstant()
        assertThat(MapVariant.forTime(time), `is`(equalTo(AFTER_DARK)))
    }

    @Test
    fun forTime_afterAfterHours_returnsDay() {
        val time = ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_END).plusMinutes(1).toInstant()
        assertThat(MapVariant.forTime(time), `is`(equalTo(DAY)))
    }

    @Test
    fun forTime_duringConcert_returnsConcert() {
        val time = ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY2_CONCERT_START).plusMinutes(1)
            .toInstant()
        assertThat(MapVariant.forTime(time), `is`(equalTo(CONCERT)))
    }

    @Test
    fun forTime_afterConcert_returnsDay() {
        val time = ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY2_END).plusMinutes(1).toInstant()
        assertThat(MapVariant.forTime(time), `is`(equalTo(DAY)))
    }
}
