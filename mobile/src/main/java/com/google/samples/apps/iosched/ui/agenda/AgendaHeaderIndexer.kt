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

package com.google.samples.apps.iosched.ui.agenda

import com.google.samples.apps.iosched.model.Block
import org.threeten.bp.ZonedDateTime

/**
 * Find the first block of each day (rounded down to nearest day) and return pairs of
 * index to start time. Assumes that [agendaItems] are sorted by ascending start time.
 */
fun indexAgendaHeaders(agendaItems: List<Block>): List<Pair<Int, ZonedDateTime>> {
    return agendaItems
        .mapIndexed { index, block -> index to block.startTime }
        .distinctBy { it.second.dayOfMonth }
}
