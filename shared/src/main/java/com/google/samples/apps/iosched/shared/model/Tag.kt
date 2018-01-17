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
 * Describes a tag, which contains meta-information about a conference session. A tag has two
 * components, a category, and a name, and together these give a tag its semantic meaning. For
 * example, a session may contain the following tags: {category: "TRACK", name: "ANDROID"} and
 * {category: "TYPE", name: "OFFICEHOURS"}. The first tag defines the session track as Android, and
 * the second tag defines the session type as an office hour.
 */
internal interface Tag {
    fun getId(): String

    /**
     * Tag category type. For example, "Track", "Level", "Type", "Theme". etc.
     */
    fun getCategory(): String

    /**
     * Tag name within a category. For example, "Android", or "Ads", or "Design".
     */
    fun getName(): String

    /**
     * The color associated with this tag as a hex string. Example, "#FFEE88".
     */
    fun getColor(): String
}
