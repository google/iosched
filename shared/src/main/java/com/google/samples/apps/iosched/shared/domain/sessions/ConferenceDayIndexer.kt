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

package com.google.samples.apps.iosched.shared.domain.sessions

import com.google.samples.apps.iosched.model.ConferenceDay

class ConferenceDayIndexer(
    /**
     * Mapping of ConferenceDay to start position.
     * Values (indexes) must be >= 0 and in ascending order.
     */
    mapping: Map<ConferenceDay, Int>
) {
    init {
        var previous = -1
        mapping.forEach { (_, value) ->
            if (value <= previous) {
                throw IllegalArgumentException("Index values must be >= 0 and in ascending order.")
            }
            previous = value
        }
    }

    /** The ConferenceDays that are indexed. */
    val days = mapping.map { it.key }
    private val startPositions = mapping.map { it.value }

    fun dayForPosition(position: Int): ConferenceDay? {
        startPositions.asReversed().forEachIndexed { index, intVal ->
            if (intVal <= position) {
                // Indexes are inverted because of asReversed()
                return days[days.size - index - 1]
            }
        }
        return null
    }

    fun positionForDay(day: ConferenceDay): Int {
        val index = days.indexOf(day)
        if (index == -1) {
            throw IllegalArgumentException("Unknown day")
        }
        return startPositions[index]
    }
}
