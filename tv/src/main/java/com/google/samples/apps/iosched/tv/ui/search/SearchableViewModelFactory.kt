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

package com.google.samples.apps.iosched.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.domain.sessions.LoadSessionUseCase

/**
 * Creates [SearchableViewModel]s, used with the [android.arch.lifecycle.ViewModelProviders].
 */
class SearchableViewModelFactory(
    private val sessionRepository: SessionRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchableViewModel(LoadSessionUseCase(sessionRepository)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
