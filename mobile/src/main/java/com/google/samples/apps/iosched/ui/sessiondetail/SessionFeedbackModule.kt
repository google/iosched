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

package com.google.samples.apps.iosched.ui.sessiondetail

import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.di.FragmentScoped
import com.google.samples.apps.iosched.shared.di.ViewModelKey
import com.google.samples.apps.iosched.ui.sessioncommon.SessionViewPoolModule
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * Module where classes needed to create the [SessionFeedbackFragment] are defined.
 */
@Module
@Suppress("UNUSED")
internal abstract class SessionFeedbackModule {

    /**
     * Generates an AndroidInjector for the [SessionFeedbackFragment].
     */
    @FragmentScoped
    @ContributesAndroidInjector(modules = [SessionViewPoolModule::class])
    internal abstract fun contributeSessionFeedbackFragment(): SessionFeedbackFragment

    /**
     * The ViewModels are created by Dagger in a map. Via the @ViewModelKey, we define that we
     * want to get a [SessionFeedbackViewModel] class.
     */
    @Binds
    @IntoMap
    @ViewModelKey(SessionFeedbackViewModel::class)
    abstract fun bindSessionFeedbackFragmentViewModel(
        viewModel: SessionFeedbackViewModel
    ): ViewModel
}
