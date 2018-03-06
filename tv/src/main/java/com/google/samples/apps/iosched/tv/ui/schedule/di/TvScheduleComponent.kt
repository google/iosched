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

package com.google.samples.apps.iosched.tv.ui.schedule.di

import com.google.samples.apps.iosched.tv.ui.schedule.ScheduleFragment
import com.google.samples.apps.iosched.tv.ui.schedule.ScheduleViewModelFactory
import dagger.Subcomponent

/**
 * Schedule component for the tv app, created and managed in the [TvApplication].
 */
@Subcomponent(modules = [(TvScheduleModule::class)])
interface TvScheduleComponent {

    fun scheduleViewModelFactory(): ScheduleViewModelFactory

    fun inject(scheduleFragment: ScheduleFragment)
}