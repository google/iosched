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

plugins {
    id("java-library")
    kotlin("jvm")
}

dependencies {
    api(platform(project(":depconstraints")))
    implementation(project(":model"))
    // Kotlin
    implementation(Libs.KOTLIN_STDLIB)

    // ThreeTenBP for the shared module only. Date and time API for Java.
    testImplementation(Libs.THREETENBP)
    compileOnly("org.threeten:threetenbp:${Versions.THREETENBP}:no-tzdb")
}
