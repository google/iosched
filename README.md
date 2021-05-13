Google I/O Android App
======================
[![Build Status](https://travis-ci.org/google/iosched.svg?branch=master)](https://travis-ci.org/google/iosched)

**Due to global events, Google I/O 2020 was canceled and thus an Android app
update was not released to Google Play. However, prior to the cancellation, the
team implemented several architecture improvements, reflected in the code
published in June, 2020. The general look and feel of the app is unchanged, and
the app still uses the data from Google I/O 2019.**

Google I/O is a developer conference with several days of deep
technical content featuring technical sessions and hundreds of demonstrations
from developers showcasing their technologies.

This project is the Android app for the conference.

For a simpler fork of the app, check out the [Android Dev Summit App in the adssched branch](https://github.com/google/iosched/tree/adssched). In this variant some features are removed, such as reservations and the map screen, and Instant App support is added.

# Features

The app displays a list of conference events - sessions, office hours, app
reviews, codelabs, etc. - and allows the user to filter these events by event
types and by topics (Android, Firebase, etc.). Users can see details about
events, and they can star events that interest them. Conference attendees can
reserve events to guarantee a seat.

Other features include a Map of the venue, informational pages to
guide attendees during the conference in Info, and time-relevant information
during the conference in Home.

<div>
  <img align="center" src="schedule.png" alt="Schedule screenshot" height="640" width="320">
</div>

# Development Environment

The app is written entirely in Kotlin and uses the Gradle build system.

To build the app, use the `gradlew build` command or use "Import Project" in
Android Studio. A canary or stable version of Android Studio 4.0 or newer is
required and may be downloaded
[here](https://developer.android.com/studio/).

# Architecture

The architecture is built around
[Android Architecture Components](https://developer.android.com/topic/libraries/architecture/).

We followed the recommendations laid out in the
[Guide to App Architecture](https://developer.android.com/jetpack/docs/guide)
when deciding on the architecture for the app. We kept logic away from
Activities and Fragments and moved it to
[ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)s.
We observed data using
[LiveData](https://developer.android.com/topic/libraries/architecture/livedata)
and used the [Data Binding Library](https://developer.android.com/topic/libraries/data-binding/)
to bind UI components in layouts to the app's data sources.

We used a Repository layer for handling data operations. IOSched's data comes
from a few different sources -  user data is stored in
[Cloud Firestore](https://firebase.google.com/docs/firestore/)
(either remotely or in
a local cache for offline use), user preferences and settings are stored in
DataStore, conference data is stored remotely and is fetched and stored
in memory for the app to use, etc. - and the repository modules
are responsible for handling all data operations and abstracting the data sources
from the rest of the app (we liked using Firestore, but if we wanted to swap it
out for a different data source in the future, our architecture allows us to do
so in a clean way).

We implemented a lightweight domain layer, which sits between the data layer
and the presentation layer, and handles discrete pieces of business logic off
the UI thread. See the `.\*UseCase.kt` files under `shared/domain` for
[examples](https://github.com/google/iosched/search?q=UseCase&unscoped_q=UseCase).

We used [Navigation component](https://developer.android.com/guide/navigation)
to simplify into a single Activity app.

We used [Room](https://developer.android.com/jetpack/androidx/releases/room)
for Full Text Search using [Fts4](https://developer.android.com/reference/androidx/room/Fts4)
to search for a session, speaker, or codelab.

We used [Espresso](https://developer.android.com/training/testing/espresso/)
for basic instrumentation tests and JUnit and
[Mockito](https://github.com/mockito/mockito) for unit testing.

For 2020, we added several Jetpack libraries:

- We added the
[Jetpack Benchmark library](https://developer.android.com/studio/profile/benchmark).
The Benchmark library makes it easy to benchmark your code from within Android Studio.
The library handles warmup, measures your code performance, and outputs benchmarking
results to the Android Studio console. We added a few benchmark tests around
critical paths during app startup - in particular, the parsing of the bootstrap
data. This enables us to automate measuring and monitoring initial startup time.
Here is an example from a benchmark run:

```
Started running tests

Connected to process 30763 on device 'google-pixel_3'.
benchmark:
benchmark:    76,076,101 ns BootstrapConferenceDataSourceBenchmark.benchmark_json_parsing
Tests ran to completion.
```

- We migrated from [dagger-android](https://google.github.io/dagger/android.html) to
[Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
for dependency injection. This allowed us to remove 75% of the dependency
injection code, and also resulted in a 13% build time improvement.

- We migrated to
[ViewPager2](https://developer.android.com/training/animation/screen-slide-2)
for our view paging needs. ViewPager2 offers enhanced functionality over the
original ViewPager library, such as right-to-left and vertical orientation support.
For more details on migrating from ViewPager to ViewPager2, please see this
[migration guide](https://developer.android.com/training/animation/vp2-migration).

## Firebase

The app makes considerable use of the following Firebase components:

-  [Cloud Firestore](https://firebase.google.com/docs/firestore/) is our source
for all user data (events starred or reserved by a user). Firestore gave us
automatic sync  and also seamlessly managed offline functionality
for us.
- [Firebase Cloud Functions](https://firebase.google.com/docs/functions/)
allowed us to run backend code. The reservations feature heavily depended on Cloud
Functions working in conjuction with Firestore.
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging/concept-options)
let us inform the app about changes to conference data on our server.
- [Remote Config](https://firebase.google.com/docs/remote-config/) helped us
manage in-app constants.

For 2020, we migrated to the Firebase Kotlin extension (KTX) libraries to
write more idiomatic Kotlin code when calling Firebase APIs. To learn more,
read this
[Firebase blog article](https://firebase.googleblog.com/2020/03/firebase-kotlin-ga.html)
on the Firebase KTX libraries.

## Kotlin

We made an early decision to rewrite the app from scratch to bring it in line
with our thinking about modern Android architecture. Using Kotlin for the
rewrite was an easy choice: we liked Kotlin's expressive, concise, and
powerful syntax; we found that Kotlin's support for safety features for
nullability and immutability made our code more resilient; and we leveraged the
enhanced functionality provided by
[Android Ktx extensions](https://developer.android.com/kotlin/ktx).

For 2020, we migrated to
[coroutines](https://developer.android.com/kotlin/coroutines)
for asynchronous tasks. Coroutines is the recommended way to do asynchronous
programming in Kotlin. We also migrated our build scripts to
[Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html).

# Copyright

    Copyright 2014 Google Inc. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

