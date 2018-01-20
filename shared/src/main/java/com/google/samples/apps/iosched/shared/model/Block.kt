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
 * Defines a block of time associated with the conference. For example, a span of time denotes the
 * time when codelabs are offered, or when lunch is provided, etc.
 */
data class Block (
    /**
     * The title of the block. Example, "Sandbox".
     */
    val title: String,

    /**
     * The subtitle of the block. Example, "Registration" (with title "Badge Pick-Up").
     */
    val subtitle: String,

    /**
     * The kind of block. Example, "concert", or "meal".
     */
    val kind: String,

    /**
     * Start time
     */
    val startTime: ZonedDateTime,

    /**
     * End time
     */
    val endTime: ZonedDateTime
)
