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
    kotlin("android.extensions")
    kotlin("kapt")
}

android {
    compileSdkVersion(Versions.COMPILE_SDK)
    defaultConfig {
        minSdkVersion(Versions.MIN_SDK)
        targetSdkVersion(Versions.TARGET_SDK)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CONFERENCE_TIMEZONE", properties["conference_timezone"] as String)
        buildConfigField("String", "CONFERENCE_DAY1_START", properties["conference_day1_start"] as String)
        buildConfigField("String", "CONFERENCE_DAY1_END", properties["conference_day1_end"] as String)
        buildConfigField("String", "CONFERENCE_DAY2_START", properties["conference_day2_start"] as String)
        buildConfigField("String", "CONFERENCE_DAY2_END", properties["conference_day2_end"] as String)
        buildConfigField("String", "CONFERENCE_DAY3_START", properties["conference_day3_start"] as String)
        buildConfigField("String", "CONFERENCE_DAY3_END", properties["conference_day3_end"] as String)

        buildConfigField("String", "CONFERENCE_DAY1_AFTERHOURS_START", properties["conference_day1_afterhours_start"] as String)
        buildConfigField("String", "CONFERENCE_DAY2_CONCERT_START", properties["conference_day2_concert_start"] as String)

        buildConfigField("String",
                "BOOTSTRAP_CONF_DATA_FILENAME", properties["bootstrap_conference_data_filename"] as String)

        buildConfigField("String",
                "CONFERENCE_WIFI_OFFERING_START", properties["conference_wifi_offering_start"] as String)

        consumerProguardFiles("consumer-proguard-rules.pro")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.incremental"] = "true"
            }
        }
    }

    buildTypes {
        getByName("release") {
            buildConfigField("String", "REGISTRATION_ENDPOINT_URL", "\"https://events-d07ac.appspot.com/_ah/api/registration/v1/register\"")
            buildConfigField("String", "CONFERENCE_DATA_URL", "\"https://firebasestorage.googleapis.com/v0/b/io2019-festivus-prod/o/sessions.json?alt=media&token=89140adf-e228-45a5-9ae3-8ed01547166a\"")
        }
        getByName("debug") {
            buildConfigField("String", "REGISTRATION_ENDPOINT_URL", "\"https://events-dev-62d2e.appspot.com/_ah/api/registration/v1/register\"")
            buildConfigField("String", "CONFERENCE_DATA_URL", "\"https://firebasestorage.googleapis.com/v0/b/io2019-festivus/o/sessions.json?alt=media&token=019af2ec-9fd1-408e-9b86-891e4f66e674\"")
        }
        maybeCreate("staging")
        getByName("staging") {
            initWith(getByName("debug"))

            // Specifies a sorted list of fallback build types that the
            // plugin should try to use when a dependency does not include a
            // "staging" build type.
            // Used with :test-shared, which doesn't have a staging variant.
            matchingFallbacks = listOf("debug")
        }
    }

    lintOptions {
        disable("InvalidPackage", "MissingTranslation")
        // Version changes are beyond our control, so don't warn. The IDE will still mark these.
        disable("GradleDependency")
        // Timber needs to update to new Lint API
        disable("ObsoleteLintCustomCheck")
    }

    // debug and release variants share the same source dir
    sourceSets {
        getByName("debug") {
            java.srcDir("src/debugRelease/java")
        }
        getByName("release") {
            java.srcDir("src/debugRelease/java")
        }
    }

    // Some libs (such as androidx.core:core-ktx 1.2.0 and newer) require Java 8
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // To avoid the compile error: "Cannot inline bytecode built with JVM target 1.8
    // into bytecode that is being built with JVM target 1.6"
    kotlinOptions {
        val options = this as org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
        options.jvmTarget = "1.8"
    }
}

dependencies {
    api(platform(project(":depconstraints")))
    kapt(platform(project(":depconstraints")))
    api(project(":model"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    testImplementation(project(":test-shared"))
    testImplementation(project(":androidTest-shared"))

    // Architecture Components
    implementation(Libs.LIFECYCLE_LIVE_DATA_KTX)
    implementation(Libs.LIFECYCLE_VIEW_MODEL_KTX)
    implementation(Libs.ROOM_KTX)
    implementation(Libs.ROOM_RUNTIME)
    kapt(Libs.ROOM_COMPILER)
    testImplementation(Libs.ARCH_TESTING)

    // Maps
    api(Libs.GOOGLE_MAP_UTILS) {
        exclude(group = "com.google.android.gms")
    }
    api(Libs.GOOGLE_PLAY_SERVICES_MAPS)

    // Utils
    api(Libs.TIMBER)
    implementation(Libs.GSON)
    implementation(Libs.CORE_KTX)

    // OkHttp
    implementation(Libs.OKHTTP)
    implementation(Libs.OKHTTP_LOGGING_INTERCEPTOR)

    // Kotlin
    implementation(Libs.KOTLIN_STDLIB)

    // Coroutines
    api(Libs.COROUTINES)
    testImplementation(Libs.COROUTINES_TEST)

    // Dagger
    implementation(Libs.DAGGER_ANDROID)
    implementation(Libs.DAGGER_ANDROID_SUPPORT)
    kapt(Libs.DAGGER_COMPILER)
    kapt(Libs.DAGGER_ANDROID_PROCESSOR)

    // Firebase
    api(Libs.FIREBASE_AUTH)
    api(Libs.FIREBASE_CONFIG)
    api(Libs.FIREBASE_ANALYTICS)
    api(Libs.FIREBASE_FIRESTORE)
    api(Libs.FIREBASE_FUNCTIONS)
    api(Libs.FIREBASE_MESSAGING)

    // Has to be replaced to avoid compile / runtime conflicts between okhttp and firestore
    api(Libs.OKIO)

    // ThreeTenBP for the shared module only. Date and time API for Java.
    testImplementation(Libs.THREETENBP)
    compileOnly("org.threeten:threetenbp:${Versions.THREETENBP}:no-tzdb")

    // Unit tests
    testImplementation(Libs.JUNIT)
    testImplementation(Libs.HAMCREST)
    testImplementation(Libs.MOCKITO_CORE)
    testImplementation(Libs.MOCKITO_KOTLIN)

    // unit tests livedata
    testImplementation(Libs.ARCH_TESTING)
}

apply(plugin = "com.google.gms.google-services")
