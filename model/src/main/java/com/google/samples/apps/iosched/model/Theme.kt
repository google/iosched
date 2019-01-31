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

package com.google.samples.apps.iosched.model

/**
 * Represents the available UI themes for the application
 */
enum class Theme(val storageKey: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system")
}

/**
 * Returns the matching [Theme] for the given [storageKey] value. If there is no matching [Theme],
 * the default theme, [Theme.SYSTEM] is returned.
 */
fun themeFromStorageKey(storageKey: String?): Theme {
    return Theme.values().firstOrNull { it.storageKey == storageKey } ?: Theme.SYSTEM
}