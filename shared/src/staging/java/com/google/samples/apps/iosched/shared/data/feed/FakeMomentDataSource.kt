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

package com.google.samples.apps.iosched.shared.data.feed

import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.shared.util.ColorUtils
import org.threeten.bp.ZonedDateTime

/**
 * Returns hardcoded data for development and testing.
 */
object FakeMomentDataSource : MomentDataSource {
    private val imageBaseUrl =
        "https://firebasestorage.googleapis.com/v0/b/io2019-festivus/o/images%2Fhome/"
    private val moment = Moment(
        id = "1",
        title = "Keynote",
        streamUrl = "https://youtu.be/lyRPyRKHO8M",
        startTime = ZonedDateTime.parse("2019-05-07T10:00:00-07:00"),
        endTime = ZonedDateTime.parse("2019-05-07T11:30:00-07:00"),
        textColor = ColorUtils.parseHexColor("#ffffff"),
        ctaType = Moment.CTA_LIVE_STREAM,
        imageUrl = "https://firebasestorage.googleapis.com/v0/b/io2019-festivus/o/images%2Fhome" +
            "%2FHome-GoogleKeynote%402x.png?alt=media&token=0df80e81-5bea-4171-9016-4f1e3dcfc7f9",
        imageUrlDarkTheme = "https://firebasestorage.googleapis.com/v0/b/io2019-festivus/o/images" +
            "%2Fhome%2FDM-Home-GoogleKeynote%402x.png?alt=media&token=bfd169ae-f501-4cf8-94be-" +
            "462682d40fd7",
        attendeeRequired = false,
        timeVisible = false,
        featureId = "",
        featureName = ""
    )

    override fun getMoments(): List<Moment> = listOf(moment)
}
