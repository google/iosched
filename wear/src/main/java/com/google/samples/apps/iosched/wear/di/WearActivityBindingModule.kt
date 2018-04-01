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

package com.google.samples.apps.iosched.wear.di

import com.google.samples.apps.iosched.shared.di.ActivityScoped
import com.google.samples.apps.iosched.wear.ui.MainActivity
import com.google.samples.apps.iosched.wear.ui.schedule.di.ScheduleModule
import com.google.samples.apps.iosched.wear.ui.sessiondetail.SessionDetailActivity
import com.google.samples.apps.iosched.wear.ui.sessiondetail.di.SessionDetailModule
import com.google.samples.apps.iosched.wear.ui.settings.di.SettingsModule
import com.google.samples.apps.iosched.wear.ui.signinandout.di.SignInOrOutModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * We want Dagger.Android to create a Subcomponent which has a parent Component of whichever module
 * WearActivityBindingModule is on, in our case that will be [WearAppComponent]. You never
 * need to tell [WearAppComponent] that it is going to have all these subcomponents
 * nor do you need to tell these subcomponents that [WearAppComponent] exists.
 *
 * We are also telling Dagger.Android that this generated SubComponent needs to include the
 * specified modules and be aware of a scope annotation [@ActivityScoped].
 * When Dagger.Android annotation processor runs it will create one subcomponent for us.
 */
@Module
abstract class WearActivityBindingModule {

    @ActivityScoped
    @ContributesAndroidInjector(
            modules = [
                ScheduleModule::class,
                SettingsModule::class,
                SignInOrOutModule::class
            ]
    )
    internal abstract fun mainActivity(): MainActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = [SessionDetailModule::class])
    internal abstract fun sessionDetailActivity(): SessionDetailActivity
}
