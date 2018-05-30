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

import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.di.ChildFragmentScoped
import com.google.samples.apps.iosched.shared.di.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * Module that defines the child fragments related to sign in/out.
 */
@Module
internal abstract class SignInDialogModule {

    /**
     * Generates an [AndroidInjector] for the [SignInDialogFragment].
     */
    @ChildFragmentScoped
    @ContributesAndroidInjector
    internal abstract fun contributeSignInDialogFragment(): SignInDialogFragment

    /**
     * Generates an [AndroidInjector] for the [SignOutDialogFragment].
     */
    @ChildFragmentScoped
    @ContributesAndroidInjector
    internal abstract fun contributeSignOutDialogFragment(): SignOutDialogFragment

    /**
     * Generates an [AndroidInjector] for the [NotificationsPreferenceDialogFragment].
     */
    @ChildFragmentScoped
    @ContributesAndroidInjector
    internal abstract fun contributeNotificationsPreferenceDialogFragment():
        NotificationsPreferenceDialogFragment

    /**
     * The ViewModels are created by Dagger in a map. Via the @ViewModelKey, we define that we
     * want to get a [SignInViewModel] class.
     */
    @Binds
    @IntoMap
    @ViewModelKey(SignInViewModel::class)
    abstract fun bindSignInViewModel(viewModel: SignInViewModel): ViewModel

    /**
     * The ViewModels are created by Dagger in a map. Via the @ViewModelKey, we define that we
     * want to get a [NotificationsPreferenceViewModel] class.
     */
    @Binds
    @IntoMap
    @ViewModelKey(NotificationsPreferenceViewModel::class)
    abstract fun bindNotificationsPreferenceViewModel(viewModel: NotificationsPreferenceViewModel):
        ViewModel
}
