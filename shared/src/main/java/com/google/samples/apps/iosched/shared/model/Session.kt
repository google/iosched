/*
 * Copyright 2018 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.shared.model

/**
 * Describes a conference session. Sessions have specific start and end times, and they represent a
 * variety of conference events: talks, sandbox demos, office hours, etc. A session is usually
 * associated with one or more [Tag]s.
 */
internal interface Session {
    /**
     * Unique string identifying this session.
     */
    fun getId(): String

    /**
     * Start time of the session in RFC 3339 time. Example, "2017-05-19T22:30:00Z"
     */
    fun getStartTime(): String

    /**
     * End time of the session in RFC 3339 time. Example, "2017-05-19T22:30:00Z"
     */
    fun getEndTime(): String

    /**
     * Session title.
     */
    fun getTitle(): String

    /**
     * Body of text explaining this session in detail.
     */
    fun getAbstract(): String

    /**
     * Full URL for the session online.
     */
    fun getSessionUrl(): String

    /**
     * Url for the session livestream.
     */
    fun getLiveStreamUrl(): String

    /**
     * Full URL to YouTube.
     */
    fun getYouTubeUrl(): String

    /**
     * The [Tag]s associated with the session. Ordered, with the most important tags appearing
     * first.
     */
    fun getTags(): List<Tag>

    /**
     * The session speakers.
     */
    fun getSpeakers(): Set<Speaker>

    /**
     * The session's photo URL.
     */
    fun getPhotoUrl(): String

    /**
     * Sessions related to this session.
     */
    fun getRelatedSessions(): Set<Session>
}
