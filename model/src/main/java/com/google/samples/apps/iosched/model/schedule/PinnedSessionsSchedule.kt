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

package com.google.samples.apps.iosched.model.schedule

/**
 * Class representation of JSON format passed to the AR module for showing user's pinned sessions.
 * The field name must not be changed.
 */
data class PinnedSessionsSchedule(val schedule: List<PinnedSession>)

/**
 * Class representation of JSON format for a single session.
 * The field names must not be changed.
 *
 * Example entity is like
 * {
 *    "name": "session1",
 *    "location": "Room 1",
 *    "day": "3/29",
 *    "time": "13:30",
 *    "timestamp": 82547983,
 *    "description": "Session description"
 * }
 */
data class PinnedSession(
    val name: String,
    val location: String,
    val day: String,
    val time: String,
    val timestamp: Long,
    val description: String
)