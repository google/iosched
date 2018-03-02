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

import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.di.ViewModelKey
import com.google.samples.apps.iosched.ui.MainModule
import com.google.samples.apps.iosched.ui.schedule.agenda.ScheduleAgendaFragment
import com.google.samples.apps.iosched.ui.schedule.day.ScheduleDayFragment
import com.google.samples.apps.iosched.ui.schedule.day.filters.ScheduleFilterFragment
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
     * Generates an [AndroidInjector] for the [ScheduleFragment] as a Dagger subcomponent of the
     * [MainModule].
     */
    @ContributesAndroidInjector
    internal abstract fun contributeScheduleFragment(): ScheduleFragment

    /**
     * Generates an [AndroidInjector] for the [ScheduleDayFragment] as a Dagger subcomponent of the
     * [MainModule].
     */
    @ContributesAndroidInjector
    internal abstract fun contributeScheduleDayFragment(): ScheduleDayFragment

    /**
     * Generates an [AndroidInjector] for the [ScheduleAgendaFragment] as a Dagger subcomponent of
     * the [MainModule].
     */
    @ContributesAndroidInjector
    internal abstract fun contributeScheduleAgendaFragment(): ScheduleAgendaFragment

    /**
     * Generates an [AndroidInjector] for the [ScheduleFilterFragment] as a Dagger subcomponent of
     * the [MainModule].
     */
    @ContributesAndroidInjector
    internal abstract fun contributeScheduleFilterFragment(): ScheduleFilterFragment

    /**
     * The ViewModels are created by Dagger in a map. Via the @ViewModelKey, we define that we
     * want to get a [ScheduleViewModel] class.
     */
    @Binds
    @IntoMap
    @ViewModelKey(ScheduleViewModel::class)
    abstract fun bindScheduleFragmentViewModel(viewModel: ScheduleViewModel): ViewModel
}
