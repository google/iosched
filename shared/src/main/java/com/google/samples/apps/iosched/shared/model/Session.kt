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

package com.google.samples.apps.iosched.shared.model

import org.threeten.bp.ZonedDateTime

/**
 * Describes a conference session. Sessions have specific start and end times, and they represent a
 * variety of conference events: talks, sandbox demos, office hours, etc. A session is usually
 * associated with one or more [Tag]s.
 */
data class Session(
    /**
     * Unique string identifying this session.
     */
    val id: String,

    /**
     * Start time of the session
     */
    val startTime: ZonedDateTime,

    /**
     * End time of the session
     */
    val endTime: ZonedDateTime,

    /**
     * Session title.
     */
    val title: String,

    /**
     * Body of text explaining this session in detail.
     */
    val abstract: String,

    /**
     * Full URL for the session online.
     */
    val sessionUrl: String,

    /**
     * Url for the session livestream.
     */
    val liveStreamUrl: String,

    /**
     * Full URL to YouTube.
     */
    val youTubeUrl: String,

    /**
     * The [Tag]s associated with the session. Ordered, with the most important tags appearing
     * first.
     */
    val tags: List<Tag>,

    /**
     * The session speakers.
     */
    val speakers: Set<Speaker>,

    /**
     * The session's photo URL.
     */
    val photoUrl: String,

    /**
     * Sessions related to this session.
     */
    val relatedSessions: Set<Session>)
