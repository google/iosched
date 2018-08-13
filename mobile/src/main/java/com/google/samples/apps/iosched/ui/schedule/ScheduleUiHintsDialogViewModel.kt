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

package com.google.samples.apps.iosched.ui.schedule

import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.domain.prefs.ScheduleUiHintsShowActionUseCase
import javax.inject.Inject

/**
 * ViewModel for the dialog to show the schedule hints.
 */
class ScheduleUiHintsDialogViewModel @Inject constructor(
    private val scheduleUiHintsShowActionUseCase: ScheduleUiHintsShowActionUseCase
) : ViewModel() {

    fun onDismissed() {
        scheduleUiHintsShowActionUseCase(true)
    }
}
