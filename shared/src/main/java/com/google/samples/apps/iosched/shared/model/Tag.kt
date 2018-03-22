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
 * Describes a tag, which contains meta-information about a conference session. A tag has two
 * components, a category, and a name, and together these give a tag its semantic meaning. For
 * example, a session may contain the following tags: {category: "TRACK", name: "ANDROID"} and
 * {category: "TYPE", name: "OFFICEHOURS"}. The first tag defines the session track as Android, and
 * the second tag defines the session type as an office hour.
 */
data class Tag (
    /**
     * Unique string identifying this tag.
     */
    val id: String,

    /**
     * Tag category type. For example, "Track", "Level", "Type", "Theme". etc.
     */
    val category: String,

    /**
     * This tag's order within its [category].
     */
    val orderInCategory: Int,

    /**
     * Tag name within a category. For example, "Android", or "Ads", or "Design".
     */
    val name: String,

    /**
     * The color associated with this tag as a color integer.
     */
    val color: Int
) {
    /** Only IDs are used for equality. */
    override fun equals(other: Any?): Boolean = this === other || (other is Tag && other.id == id)

    /** Only IDs are used for equality. */
    override fun hashCode(): Int = id.hashCode()

    fun isUiContentEqual(other: Tag) = color == other.color && name == other.name

    companion object {
        /** Category value for topic tags */
        const val CATEGORY_TRACK = "TRACK"

        const val CATEGORY_TYPE = "TYPE"
    }
}
