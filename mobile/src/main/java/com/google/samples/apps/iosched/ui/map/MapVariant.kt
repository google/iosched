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

import androidx.annotation.RawRes
import com.google.samples.apps.iosched.R.raw
import com.google.samples.apps.iosched.shared.BuildConfig
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

/**
 * A variant of the map UI. Depending on the variant, Map UI may show different markers, tile
 * overlays, etc.
 */
enum class MapVariant(
    val start: Instant,
    val end: Instant,
    @RawRes val markersResId: Int
) {
    AFTER_HOURS(
        ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_AFTERHOURS_START).toInstant(),
        ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_END).toInstant(),
        raw.map_markers
    ),
    CONCERT(
        ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY2_CONCERT_START).toInstant(),
        ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY2_END).toInstant(),
        raw.map_markers
    ),
    // Note: must be last to facilitate [forTime]
    DAY(
        ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_START).toInstant(),
        ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY3_END).toInstant(),
        raw.map_markers
    );

    operator fun contains(time: Instant): Boolean {
        return time in start..end
    }

    companion object {
        /** Returns the first variant containing the specified time, or [DAY] if none is found. */
        fun forTime(time: Instant = Instant.now()): MapVariant {
            return values().find { variant -> time in variant } ?: DAY
        }
    }
}
