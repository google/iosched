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

package com.google.samples.apps.iosched.ui.signin

import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.di.MainDispatcher
import com.google.samples.apps.iosched.shared.di.ReservationEnabledFlag
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefIsShownUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class SignInViewModelDelegateModule {

    @Singleton
    @Provides
    fun provideSignInViewModelDelegate(
        dataSource: ObserveUserAuthStateUseCase,
        notificationsPrefIsShownUseCase: NotificationsPrefIsShownUseCase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @ReservationEnabledFlag isReservationEnabledByRemoteConfig: Boolean
    ): SignInViewModelDelegate {
        return FirebaseSignInViewModelDelegate(
            observeUserAuthStateUseCase = dataSource,
            notificationsPrefIsShownUseCase = notificationsPrefIsShownUseCase,
            ioDispatcher = ioDispatcher,
            mainDispatcher = mainDispatcher,
            isReservationEnabledByRemoteConfig = isReservationEnabledByRemoteConfig
        )
    }
}
