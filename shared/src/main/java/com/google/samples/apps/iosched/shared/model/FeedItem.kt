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

import android.support.annotation.ColorInt
import org.threeten.bp.ZonedDateTime

/**
 * Describes an item in the feed, displaying social-media like updates.
 * Each item includes a message, id, a title and a timestamp.
 * Optionally, it can also include an image and a category with a color.
 * An item can also be marked as priority.
 */
data class FeedItem(
        /**
         * Unique string identifying this item.
         */
        val id: String,

        /**
         * Title of this feed item.
         */
        val title: String,

        /**
         * Message of this feed item.
         */
        val message: String,

        /**
         * Marks this feed item as a priority.
         */
        val priority: Boolean,

        /**
         * Timestamp this feed item was posted at.
         */
        val timestamp: ZonedDateTime,

        /**
         * File name of the image.
         */
        val imageFilename: String,

        /**
         * Item category. Free form string.
         */
        val category: String,

        /**
         * The color associated with this item.
         */
        @ColorInt val color: Int
)