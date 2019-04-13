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

package com.google.samples.apps.iosched.shared.data.session.json

/** Like `Codelab` but with a list of IDs instead of tags. */
data class CodelabTemp(
    /** Unique ID identifying this Codelab */
    val id: String,
    /** Codelab title */
    val title: String,
    /** A short description of the codelab content */
    val description: String,
    /** Approximate time in minutes a user would spend doing this codelab */
    val durationMinutes: Int,
    /** URL for an icon to display */
    val iconUrl: String?,
    /** URL to access this codelab on the web */
    val codelabUrl: String,
    /** Names of Tags applicable to this codelab */
    val tagNames: List<String>,
    /** Sort priorty. Higher sort priority should come before lower ones. */
    val sortPriority: Int
)
