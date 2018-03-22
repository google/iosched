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

import android.support.annotation.WorkerThread
import org.threeten.bp.ZonedDateTime
import kotlin.LazyThreadSafetyMode.NONE

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
     * The session room.
     */
    val room: Room,

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
     * Subset of [Tag]s that are for visual consumption.
     */
    val displayTags: List<Tag>,

    /**
     * The session speakers.
     */
    val speakers: Set<Speaker>,

    /**
     * The session's photo URL.
     */
    val photoUrl: String,

    /**
     * IDs of the sessions related to this session.
     */
    val relatedSessions: Set<String>
) {

    /**
     * Returns whether the session is currently being live streamed or not.
     */
    fun isLive(): Boolean {
        val now = ZonedDateTime.now()
        // TODO: Determine when a session is live based on the time AND the liveStream being
        // available.
        return startTime <= now && endTime >= now
    }

    /**
     * The year the session was held.
     */
    val year = startTime.year

    /**
     * The duration of the session.
     */
    //TODO: Get duration from the YouTube video. Not every talk fills the full session time.
    val duration = endTime.toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli()

    /**
     * The type of the event e.g. Session, Codelab etc.
     */
    val type: SessionType by lazy(NONE) {
        SessionType.fromTags(tags)
    }
}

/**
 * Represents the type of the event e.g. Session, Codelab etc.
 */
enum class SessionType {
    SESSION,
    APP_REVIEW,
    OFFICE_HOURS,
    CODELAB,
    SANDBOX,
    AFTER_HOURS;

    companion object {

        /**
         * Examine the given [tags] to determine the [SessionType]. Defaults to [SESSION] if no
         * category tag is found.
         */
        @WorkerThread
        fun fromTags(tags: List<Tag>): SessionType {
            val typeTag = tags.firstOrNull { it.category == Tag.CATEGORY_TYPE }

            return when (typeTag?.id) {
                "TYPE_APP_REVIEW" -> APP_REVIEW
                "TYPE_OFFICE_HOURS" -> OFFICE_HOURS
                "TYPE_CODELAB" -> CODELAB
                "TYPE_SANDBOX" -> SANDBOX
                "TYPE_AFTER_HOURS" -> AFTER_HOURS
                else -> SESSION
            }
        }
    }
}
