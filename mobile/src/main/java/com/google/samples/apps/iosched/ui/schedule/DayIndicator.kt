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

package com.google.samples.apps.iosched.ui.schedule

import com.google.samples.apps.iosched.model.ConferenceDay

/** An indicator for days on the Schedule. */
class DayIndicator(
    val day: ConferenceDay,
    val checked: Boolean = false,
    val enabled: Boolean = true
) {
    // Only the day is used for equality
    override fun equals(other: Any?): Boolean =
        this === other || (other is DayIndicator && day == other.day)

    // Only the day is used for equality
    override fun hashCode(): Int = day.hashCode()

    fun areUiContentsTheSame(other: DayIndicator): Boolean {
        return checked == other.checked && enabled == other.enabled
    }
}
