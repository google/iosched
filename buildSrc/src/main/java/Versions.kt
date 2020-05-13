/*
 * Copyright 2020 Google LLC
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

object Versions {
    val versionName = "7.0.15" // X.Y.Z; X = Major, Y = minor, Z = Patch level
    private val versionCodeBase = 70150 // XYYZZM; M = Module (tv, mobile)
    val versionCodeMobile = versionCodeBase + 3

    const val COMPILE_SDK = 29
    const val TARGET_SDK = 29
    const val MIN_SDK = 21

    // Versions that are used for classpath (and library) dependencies
    // b/146592586 prevents updating the AGP to 4.0
    const val ANDROID_GRADLE_PLUGIN = "4.0.0-rc01"
    const val FABRIC = "1.31.2"
    const val GOOGLE_SERVICES = "4.3.3"
    const val KOTLIN = "1.3.70"
    const val NAVIGATION = "2.2.0"

    // TODO: Remove this once the version for
    //  "org.threeten:threetenbp:${Versions.threetenbp}:no-tzdb" using java-platform in the
    //  depconstraints/build.gradle.kts is defined
    const val THREETENBP = "1.3.6"
}
