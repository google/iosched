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

import android.os.Build
import com.google.samples.apps.iosched.model.Theme
import com.google.samples.apps.iosched.model.Theme.BATTERY_SAVER
import com.google.samples.apps.iosched.model.themeFromStorageKey
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.di.DefaultDispatcher
import com.google.samples.apps.iosched.shared.domain.FlowUseCase
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

open class ObserveThemeModeUseCase @Inject constructor(
    private val preferenceStorage: PreferenceStorage,
    @DefaultDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, Theme>(dispatcher) {
    override fun execute(parameters: Unit): Flow<Result<Theme>> {
        return preferenceStorage.observableSelectedTheme.map {
            val theme = themeFromStorageKey(it)
                ?: when {
                    Build.VERSION.SDK_INT >= 29 -> Theme.SYSTEM
                    else -> BATTERY_SAVER
                }
            Result.Success(theme)
        }
    }
}
