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

package com.google.samples.apps.iosched.shared.data.session.json

import com.google.samples.apps.iosched.model.SessionId
import org.threeten.bp.ZonedDateTime

/**
 * Like `Session` but with list of IDs instead of objects in tags, speakers and related sessions.
 */
data class SessionTemp(
    /**
     * Unique string identifying this session.
     */
    val id: SessionId,

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
     * The session room.
     */
    val room: String,

    /**
     * Url for the session livestream.
     */
    val liveStreamUrl: String,

    /**
     * Indicates if the Session has a live stream.
     */
    val isLivestream: Boolean,

    /**
     * Full URL to YouTube.
     */
    val youTubeUrl: String,

    /**
     * IDs of the `Tag`s associated with the session. Ordered, with the most important tags
     * appearing first.
     */
    val tagNames: List<String>,

    /**
     * IDs of the session speakers.
     */
    val speakers: Set<String>,

    /**
     * The session's photo URL.
     */
    val photoUrl: String,

    /**
     * IDs of the sessions related to this session.
     */
    val relatedSessions: Set<String>
)
