/*
 * Copyright 2020 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("java-platform")
    id("maven-publish")
}

val appcompat = "1.1.0-beta01"
val activity = "1.0.0-beta01"
val cardview = "1.0.0"
val archTesting = "2.0.0"
val arcore = "1.7.0"
val browser = "1.0.0"
val constraintLayout = "1.1.3"
val core = "1.2.0-alpha02"
val crashlytics = "2.9.8"
val dagger = "2.24"
val drawerLayout = "1.1.0-alpha02"
val espresso = "3.1.1"
val firebaseAuth = "16.1.0"
val firebaseConfig = "16.1.2"
val firebaseCore = "16.0.6"
val firebaseFirestore = "17.1.4"
val firebaseFunctions = "16.3.0"
val firebaseMessaging = "17.3.4"
val firebaseUi = "4.0.0"
val flexbox = "1.1.0"
val fragment = "1.1.0-beta01"
val glide = "4.6.1"
val googleMapUtils = "0.5"
val googlePlayServicesMaps = "16.0.0"
val googlePlayServicesVision = "17.0.2"
val gson = "2.8.6"
val hamcrest = "1.3"
val junit = "4.12"
val junitExt = "1.1.1"
val lifecycle = "2.1.0-beta01"
val lottie = "3.0.0"
val material = "1.1.0-alpha07"
val mockito = "2.8.9"
val mockitoKotlin = "1.5.0"
val okhttp = "3.10.0"
val okio = "1.14.0"
val pageIndicator = "1.3.0"
val room = "2.1.0"
val rules = "1.1.1"
val runner = "1.1.1"
val threetenabp = "1.0.5"
val timber = "4.7.1"
val viewpager2 = "1.0.0"

dependencies {
    constraints {
        api("${Libs.ACTIVITY_KTX}:$activity")
        api("${Libs.APPCOMPAT}:$appcompat")
        api("${Libs.CARDVIEW}:$cardview")
        api("${Libs.ARCH_TESTING}:$archTesting")
        api("${Libs.ARCORE}:$arcore")
        api("${Libs.BROWSER}:$browser")
        api("${Libs.CONSTRAINT_LAYOUT}:$constraintLayout")
        api("${Libs.CORE_KTX}:$core")
        api("${Libs.CRASHLYTICS}:$crashlytics")
        api("${Libs.DAGGER_ANDROID}:$dagger")
        api("${Libs.DAGGER_ANDROID_SUPPORT}:$dagger")
        api("${Libs.DAGGER_COMPILER}:$dagger")
        api("${Libs.DAGGER_ANDROID_PROCESSOR}:$dagger")
        api("${Libs.DRAWER_LAYOUT}:$drawerLayout")
        api("${Libs.ESPRESSO_CORE}:$espresso")
        api("${Libs.ESPRESSO_CONTRIB}:$espresso")
        api("${Libs.FIREBASE_AUTH}:$firebaseAuth")
        api("${Libs.FIREBASE_CONFIG}:$firebaseConfig")
        api("${Libs.FIREBASE_CORE}:$firebaseCore")
        api("${Libs.FIREBASE_FIRESTORE}:$firebaseFirestore")
        api("${Libs.FIREBASE_FUNCTIONS}:$firebaseFunctions")
        api("${Libs.FIREBASE_MESSAGING}:$firebaseMessaging")
        api("${Libs.FIREBASE_UI_AUTH}:$firebaseUi")
        api("${Libs.FLEXBOX}:$flexbox")
        api("${Libs.FRAGMENT_KTX}:$fragment")
        api("${Libs.GLIDE}:$glide")
        api("${Libs.GLIDE_COMPILER}:$glide")
        api("${Libs.GOOGLE_MAP_UTILS}:$googleMapUtils")
        api("${Libs.GOOGLE_PLAY_SERVICES_MAPS}:$googlePlayServicesMaps")
        api("${Libs.GOOGLE_PLAY_SERVICES_VISION}:$googlePlayServicesVision")
        api("${Libs.GSON}:$gson")
        api("${Libs.HAMCREST}:$hamcrest")
        api("${Libs.JUNIT}:$junit")
        api("${Libs.EXT_JUNIT}:$junitExt")
        api("${Libs.KOTLIN_STDLIB}:${Versions.KOTLIN}")
        api("${Libs.LIFECYCLE_COMPILER}:$lifecycle")
        api("${Libs.LIFECYCLE_EXTENSIONS}:$lifecycle")
        api("${Libs.LIFECYCLE_LIVE_DATA}:$lifecycle")
        api("${Libs.LIFECYCLE_VIEW_MODEL}:$lifecycle")
        api("${Libs.LOTTIE}:$lottie")
        api("${Libs.MATERIAL}:$material")
        api("${Libs.MOCKITO_CORE}:$mockito")
        api("${Libs.MOCKITO_KOTLIN}:$mockitoKotlin")
        api("${Libs.NAVIGATION_FRAGMENT_KTX}:${Versions.NAVIGATION}")
        api("${Libs.NAVIGATION_UI_KTX}:${Versions.NAVIGATION}")
        api("${Libs.ROOM_KTX}:$room")
        api("${Libs.ROOM_RUNTIME}:$room")
        api("${Libs.ROOM_COMPILER}:$room")
        api("${Libs.OKHTTP}:$okhttp")
        api("${Libs.OKHTTP_LOGGING_INTERCEPTOR}:$okhttp")
        api("${Libs.OKIO}:$okio")
        api("${Libs.INK_PAGE_INDICATOR}:$pageIndicator")
        api("${Libs.RULES}:$rules")
        api("${Libs.RUNNER}:$runner")
        api("${Libs.THREETENABP}:$threetenabp")
        api("${Libs.THREETENBP}:${Versions.THREETENBP}")
        api("${Libs.TIMBER}:$timber")
        api("${Libs.VIEWPAGER2}:$viewpager2")
    }
}

publishing {
    publications {
        create<MavenPublication>("myPlatform") {
            from(components["javaPlatform"])
        }
    }
}
