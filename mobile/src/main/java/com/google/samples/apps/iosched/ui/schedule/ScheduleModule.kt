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
import com.google.samples.apps.iosched.shared.di.FragmentScoped
import com.google.samples.apps.iosched.shared.di.ViewModelKey
import com.google.samples.apps.iosched.ui.MainActivityViewModel
import com.google.samples.apps.iosched.ui.dialogs.AttendeeDialogFragment
import com.google.samples.apps.iosched.ui.dialogs.AttendeePreferenceViewModel
import com.google.samples.apps.iosched.ui.schedule.filters.ScheduleFilterFragment
import com.google.samples.apps.iosched.ui.sessioncommon.SessionViewPoolModule
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * Module where classes needed to create the [ScheduleFragment] are defined.
 */
@Module
internal abstract class ScheduleModule {

    /**
     * Generates an [AndroidInjector] for the [ScheduleFragment].
     */
    @FragmentScoped
    @ContributesAndroidInjector(
        modules = [
            ScheduleChildFragmentsModule::class,
            SessionViewPoolModule::class
        ]
    )
    internal abstract fun contributeScheduleFragment(): ScheduleFragment

    /**
     * Generates an [AndroidInjector] for the [ScheduleFilterFragment].
     */
    @FragmentScoped
    @ContributesAndroidInjector
    internal abstract fun contributeScheduleFilterFragment(): ScheduleFilterFragment

    /**
     * Generates an [AndroidInjector] for the [AttendeeDialogFragment].
     */
    @FragmentScoped
    @ContributesAndroidInjector
    internal abstract fun contributeAttendeeDialogFragment(): AttendeeDialogFragment

    /**
     * The ViewModels are created by Dagger in a map. Via the @ViewModelKey, we define that we
     * want to get a [ScheduleViewModel] class.
     */
    @Binds
    @IntoMap
    @ViewModelKey(ScheduleViewModel::class)
    abstract fun bindScheduleViewModel(viewModel: ScheduleViewModel): ViewModel

    /**
     * The ViewModels are created by Dagger in a map. Via the @ViewModelKey, we define that we
     * want to get a [MainActivityViewModel] class.
     */
    @Binds
    @IntoMap
    @ViewModelKey(MainActivityViewModel::class)
    abstract fun bindMainActivityViewModel(viewModel: MainActivityViewModel): ViewModel

    /**
     * The ViewModels are created by Dagger in a map. Via the @ViewModelKey, we define that we
     * want to get a [ScheduleUiHintsDialogViewModel] class.
     */
    @Binds
    @IntoMap
    @ViewModelKey(ScheduleUiHintsDialogViewModel::class)
    abstract fun bindScheduleUiHintsDialogViewModel(
        viewModel: ScheduleUiHintsDialogViewModel
    ): ViewModel

    /**
     * The ViewModels are created by Dagger in a map. Via the @ViewModelKey, we define that we
     * want to get a [AttendeePreferenceViewModel] class.
     */
    @Binds
    @IntoMap
    @ViewModelKey(AttendeePreferenceViewModel::class)
    abstract fun bindAttendeePreferenceViewModel(
        viewModel: AttendeePreferenceViewModel
    ): ViewModel
}
