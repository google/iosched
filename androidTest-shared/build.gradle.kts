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
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdkVersion(Versions.COMPILE_SDK)
    defaultConfig {
        minSdkVersion(Versions.MIN_SDK)
        targetSdkVersion(Versions.TARGET_SDK)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lintOptions {
        // Version changes are beyond our control, so don't warn. The IDE will still mark these.
        disable("GradleDependency")
    }
}

dependencies {
    api(platform(project(":depconstraints")))

    implementation(Libs.KOTLIN_STDLIB)

    // Architecture Components
    implementation(Libs.LIFECYCLE_EXTENSIONS)
    implementation(Libs.LIFECYCLE_LIVE_DATA)
    implementation(Libs.LIFECYCLE_VIEW_MODEL)
}
