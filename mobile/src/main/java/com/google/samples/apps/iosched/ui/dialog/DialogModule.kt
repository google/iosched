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

package com.google.samples.apps.iosched.ui.dialog

import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.di.ChildFragmentScoped
import com.google.samples.apps.iosched.shared.di.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * Module that defines the child fragments of the DialogFragments.
 */
@Module
internal abstract class DialogModule {

    /**
     * Generates an [AndroidInjector] for the [SignInDialogFragment].
     */
    @ChildFragmentScoped
    @ContributesAndroidInjector
    internal abstract fun contributeDialogSignInFragment(): SignInDialogFragment

    /**
     * Generates an [AndroidInjector] for the [RemoveReservationDialogFragment].
     */
    @ChildFragmentScoped
    @ContributesAndroidInjector
    internal abstract fun contributeRemoveReservationDialogFragment():
            RemoveReservationDialogFragment

    /**
     * The ViewModels are created by Dagger in a map. Via the @ViewModelKey, we define that we
     * want to get a [RemoveReservationViewModel] class.
     */
    @Binds
    @IntoMap
    @ViewModelKey(RemoveReservationViewModel::class)
    abstract fun bindRemoveReservationViewModel(viewModel: RemoveReservationViewModel): ViewModel
}
