/*
 * Copyright 2021 Google LLC
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

import kotlin.reflect.full.memberFunctions

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
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.firebase.crashlytics")
    id("dagger.hilt.android.plugin")
}

android {
    compileSdkVersion(Versions.COMPILE_SDK)
    defaultConfig {
        applicationId = "com.google.samples.apps.iosched"
        minSdkVersion(Versions.MIN_SDK)
        targetSdkVersion(Versions.TARGET_SDK)
        versionCode = Versions.versionCodeMobile
        versionName = Versions.versionName
        testInstrumentationRunner = "com.google.samples.apps.iosched.tests.CustomTestRunner"

        buildConfigField(
            "com.google.android.gms.maps.model.LatLng",
            "MAP_VIEWPORT_BOUND_NE",
            "new com.google.android.gms.maps.model.LatLng(${project.properties["map_viewport_bound_ne"]})"
        )
        buildConfigField(
            "com.google.android.gms.maps.model.LatLng",
            "MAP_VIEWPORT_BOUND_SW",
            "new com.google.android.gms.maps.model.LatLng(${project.properties["map_viewport_bound_sw"]})"
        )

        buildConfigField("float", "MAP_CAMERA_FOCUS_ZOOM", project.properties["map_camera_focus_zoom"] as String)

        resValue("dimen", "map_camera_bearing", project.properties["map_default_camera_bearing"] as String)
        resValue("dimen", "map_camera_target_lat", project.properties["map_default_camera_target_lat"] as String)
        resValue("dimen", "map_camera_target_lng", project.properties["map_default_camera_target_lng"] as String)
        resValue("dimen", "map_camera_tilt", project.properties["map_default_camera_tilt"] as String)
        resValue("dimen", "map_camera_zoom", project.properties["map_default_camera_zoom"] as String)
        resValue("dimen", "map_viewport_min_zoom", project.properties["map_viewport_min_zoom"] as String)
        resValue("dimen", "map_viewport_max_zoom", project.properties["map_viewport_max_zoom"] as String)

        manifestPlaceholders["crashlyticsEnabled"] = true

        vectorDrawables.useSupportLibrary = true

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.incremental"] = "true"
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            // TODO: b/120517460 shrinkResource can't be used with dynamic-feature at this moment.
            //       Need to ensure the app size has not increased
            manifestPlaceholders["crashlyticsEnabled"] = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue(
                "string",
                "google_maps_key",
                "AIzaSyD5jqwKMm1SeoYsW25vxCXfTlhDBeZ4H5c"
            )

            buildConfigField("String", "MAP_TILE_URL_BASE", "\"https://storage.googleapis.com/io2019-festivus-prod/images/maptiles\"")
        }
        getByName("debug") {
            versionNameSuffix = "-debug"
            manifestPlaceholders["crashlyticsEnabled"] = false
            resValue(
                "string",
                "google_maps_key",
                "AIzaSyAhJx57ikQH9rYc8IT8W3d2As5cGHMBvuo"
            )

            buildConfigField("String", "MAP_TILE_URL_BASE", "\"https://storage.googleapis.com/io2019-festivus/images/maptiles\"")
        }
        maybeCreate("staging")
        getByName("staging") {
            // TODO: replace with initWith(getByName("debug")) in 7.0.0-beta04
            // https://issuetracker.google.com/issues/186798050
            this::class.memberFunctions.first { it.name == "initWith" }.call(this, getByName("debug"))
            versionNameSuffix = "-staging"

            // Specifies a sorted list of fallback build types that the
            // plugin should try to use when a dependency does not include a
            // "staging" build type.
            // Used with :test-shared, which doesn't have a staging variant.
            matchingFallbacks += listOf("debug")
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.COMPOSE
    }

    signingConfigs {
        // We need to sign debug builds with a debug key to make firebase auth happy
        getByName("debug") {
            storeFile = file("../debug.keystore")
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
        }
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

    lint {
        // Eliminates UnusedResources false positives for resources used in DataBinding layouts
        isCheckGeneratedSources = true
        // Running lint over the debug variant is enough
        isCheckReleaseBuilds = false
        // See lint.xml for rules configuration

        // TODO: Remove when upgrading lifecycle from `2.4.0-alpha01`.
        // Fix: https://android-review.googlesource.com/c/platform/frameworks/support/+/1697465
        // Bug: https://issuetracker.google.com/184830263
        disable("NullSafeMutableLiveData")
    }

    testBuildType = "staging"

    // Required for AR because it includes a library built with Java 8
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

    packagingOptions {
        resources.excludes += "META-INF/AL2.0"
        resources.excludes += "META-INF/LGPL2.1"
    }
}

kapt {
    arguments {
        arg("dagger.hilt.shareTestComponents", "true")
    }
}

dependencies {
    api(platform(project(":depconstraints")))
    kapt(platform(project(":depconstraints")))
    androidTestApi(platform(project(":depconstraints")))

    implementation(project(":shared"))
    implementation(project(":ar"))
    testImplementation(project(":test-shared"))
    testImplementation(project(":androidTest-shared"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(Libs.CORE_KTX)

    // UI
    implementation(Libs.ACTIVITY_KTX)
    implementation(Libs.APPCOMPAT)
    implementation(Libs.FRAGMENT_KTX)
    implementation(Libs.CARDVIEW)
    implementation(Libs.BROWSER)
    implementation(Libs.CONSTRAINT_LAYOUT)
    implementation(Libs.DRAWER_LAYOUT)
    implementation(Libs.MATERIAL)
    implementation(Libs.FLEXBOX)
    implementation(Libs.LOTTIE)
    implementation(Libs.INK_PAGE_INDICATOR)
    implementation(Libs.SLIDING_PANE_LAYOUT)

    // Architecture Components
    implementation(Libs.LIFECYCLE_LIVE_DATA_KTX)
    implementation(Libs.LIFECYCLE_RUNTIME_KTX)
    kapt(Libs.LIFECYCLE_COMPILER)
    testImplementation(Libs.ARCH_TESTING)
    implementation(Libs.NAVIGATION_FRAGMENT_KTX)
    implementation(Libs.NAVIGATION_UI_KTX)
    implementation(Libs.ROOM_KTX)
    implementation(Libs.ROOM_RUNTIME)
    kapt(Libs.ROOM_COMPILER)
    testImplementation(Libs.ROOM_KTX)
    testImplementation(Libs.ROOM_RUNTIME)

    // Compose
    implementation(Libs.ACTIVITY_COMPOSE)
    implementation(Libs.COMPOSE_ANIMATION)
    implementation(Libs.COMPOSE_MATERIAL)
    implementation(Libs.COMPOSE_RUNTIME)
    implementation(Libs.COMPOSE_THEME_ADAPTER)
    implementation(Libs.COMPOSE_TOOLING)
    implementation(Libs.VIEWMODEL_COMPOSE)
    implementation(Libs.MDC_COMPOSE_THEME_ADAPTER)
    androidTestImplementation(Libs.COMPOSE_TEST)

    // Dagger Hilt
    implementation(Libs.HILT_ANDROID)
    androidTestImplementation(Libs.HILT_TESTING)
    kapt(Libs.HILT_COMPILER)
    kaptAndroidTest(Libs.HILT_COMPILER)

    // DataStore
    implementation(Libs.DATA_STORE_PREFERENCES)
    androidTestImplementation(Libs.DATA_STORE_PREFERENCES)

    // Glide
    implementation(Libs.GLIDE)
    kapt(Libs.GLIDE_COMPILER)

    // Fabric and Firebase
    implementation(Libs.FIREBASE_UI_AUTH)
    implementation(Libs.CRASHLYTICS)

    // Date and time API for Java.
    implementation(Libs.THREETENABP)
    testImplementation(Libs.THREETENBP)

    // Kotlin
    implementation(Libs.KOTLIN_STDLIB)

    // Instrumentation tests
    androidTestImplementation(Libs.ESPRESSO_CORE)
    androidTestImplementation(Libs.ESPRESSO_CONTRIB)
    androidTestImplementation(Libs.EXT_JUNIT)
    androidTestImplementation(Libs.RUNNER)
    androidTestImplementation(Libs.RULES)
    androidTestImplementation(Libs.FRAGMENT_TEST)
    debugImplementation(Libs.FRAGMENT_TEST)
    add("stagingImplementation", Libs.FRAGMENT_TEST)

    // Local unit tests
    testImplementation(Libs.JUNIT)
    testImplementation(Libs.MOCKITO_CORE)
    testImplementation(Libs.MOCKITO_KOTLIN)
    testImplementation(Libs.HAMCREST)

    // Solve conflicts with gson. DataBinding is using an old version.
    implementation(Libs.GSON)

    implementation(Libs.ARCORE)
}

apply(plugin = "com.google.gms.google-services")
