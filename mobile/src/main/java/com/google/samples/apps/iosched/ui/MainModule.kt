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

package com.google.samples.apps.iosched.ui

import com.google.samples.apps.iosched.ui.info.InfoModule
import com.google.samples.apps.iosched.ui.map.MapModule
import com.google.samples.apps.iosched.ui.schedule.ScheduleModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Module that defines the builder for the [MainActivity].
 *
 * Each activity in the app will have its own module. This is called "MainModule" because it
 * belongs to the "MainActivity".
 */
@Module
internal abstract class MainModule {
    /**
     * Define in the modules list all other modules that are part of the MainActivity.
     */
    // TODO: add in module for FeedModule
    @ContributesAndroidInjector(
            modules = [ScheduleModule::class, MapModule::class, InfoModule::class])
    internal abstract fun mainActivity(): MainActivity
}