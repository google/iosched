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

/**
 * Describes a speaker at the conference.
 */
data class Speaker(
    /**
     * Unique string identifying this speaker.
     */
    val id: String,

    /**
     * Name of this speaker.
     */
    val name: String,

    /**
     * Profile photo of this speaker.
     */
    val imageUrl: String,

    /**
     * Company this speaker works for.
     */
    val company: String,

    /**
     * Text describing this speaker in detail.
     */
    val abstract: String,

    /**
     * Full URL of the speaker's G+ profile.
     */
    val gPlusUrl: String,

    /**
     * Full URL of the speaker's Twitter profile.
     */
    val twitterUrl: String
) {
    val hasCompany
        get() = !company.isEmpty()

    val hasAbstract
        get() = !abstract.isEmpty()
}

