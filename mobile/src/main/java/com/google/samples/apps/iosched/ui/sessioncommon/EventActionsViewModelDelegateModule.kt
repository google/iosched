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

package com.google.samples.apps.iosched.ui.sessioncommon

import com.google.samples.apps.iosched.shared.di.ApplicationScope
import com.google.samples.apps.iosched.shared.di.MainDispatcher
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Provides a default implementation of [EventActionsViewModelDelegate].
 */
@InstallIn(ActivityComponent::class)
@Module
internal class EventActionsViewModelDelegateModule {

    @Provides
    fun provideEventActionsViewModelDelegate(
        signInViewModelDelegate: SignInViewModelDelegate,
        starEventUseCase: StarEventAndNotifyUseCase,
        snackbarMessageManager: SnackbarMessageManager,
        @ApplicationScope applicationScope: CoroutineScope,
        @MainDispatcher mainDispatcher: CoroutineDispatcher
    ): EventActionsViewModelDelegate {
        return DefaultEventActionsViewModelDelegate(
            signInViewModelDelegate,
            starEventUseCase,
            snackbarMessageManager,
            applicationScope,
            mainDispatcher
        )
    }
}
