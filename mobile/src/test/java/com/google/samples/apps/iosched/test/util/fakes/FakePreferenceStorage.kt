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

package com.google.samples.apps.iosched.test.util.fakes

import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakePreferenceStorage(
    override var onboardingCompleted: Boolean = false,
    override var scheduleUiHintsShown: Boolean = false,
    override var notificationsPreferenceShown: Boolean = false,
    override var preferToReceiveNotifications: Boolean = false,
    override var snackbarIsStopped: Boolean = false,
    override var preferConferenceTimeZone: Boolean = true,
    override var sendUsageStatistics: Boolean = false,
    override var selectedFilters: String? = null,
    override var selectedTheme: String? = null,
    override var observableSelectedTheme: Flow<String?> = flowOf(null)
) : PreferenceStorage
