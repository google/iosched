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

package com.google.samples.apps.iosched.shared.domain.settings

import androidx.core.os.BuildCompat
import com.google.samples.apps.iosched.model.Theme
import com.google.samples.apps.iosched.shared.di.MainImmediateDispatcher
import com.google.samples.apps.iosched.shared.domain.UseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class GetAvailableThemesUseCase @Inject constructor(
    @MainImmediateDispatcher dispatcher: CoroutineDispatcher
) : UseCase<Unit, List<Theme>>(dispatcher) {

    override suspend fun execute(parameters: Unit): List<Theme> = when {
        BuildCompat.isAtLeastQ() -> {
            listOf(Theme.LIGHT, Theme.DARK, Theme.SYSTEM)
        }
        else -> {
            listOf(Theme.LIGHT, Theme.DARK, Theme.BATTERY_SAVER)
        }
    }
}
