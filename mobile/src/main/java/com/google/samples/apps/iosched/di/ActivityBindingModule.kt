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

package com.google.samples.apps.iosched.di

import com.google.samples.apps.iosched.shared.di.ActivityScoped
import com.google.samples.apps.iosched.ui.LaunchModule
import com.google.samples.apps.iosched.ui.LauncherActivity
import com.google.samples.apps.iosched.ui.MainActivity
import com.google.samples.apps.iosched.ui.MainActivityModule
import com.google.samples.apps.iosched.ui.agenda.AgendaModule
import com.google.samples.apps.iosched.ui.codelabs.CodelabsModule
import com.google.samples.apps.iosched.ui.feed.FeedModule
import com.google.samples.apps.iosched.ui.info.InfoModule
import com.google.samples.apps.iosched.ui.map.MapModule
import com.google.samples.apps.iosched.ui.onboarding.OnboardingActivity
import com.google.samples.apps.iosched.ui.onboarding.OnboardingModule
import com.google.samples.apps.iosched.ui.prefs.PreferenceModule
import com.google.samples.apps.iosched.ui.reservation.ReservationModule
import com.google.samples.apps.iosched.ui.schedule.ScheduleModule
import com.google.samples.apps.iosched.ui.search.SearchModule
import com.google.samples.apps.iosched.ui.sessioncommon.EventActionsViewModelDelegateModule
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailActivity
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailModule
import com.google.samples.apps.iosched.ui.sessiondetail.SessionFeedbackModule
import com.google.samples.apps.iosched.ui.settings.SettingsModule
import com.google.samples.apps.iosched.ui.signin.SignInDialogModule
import com.google.samples.apps.iosched.ui.speaker.SpeakerModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * We want Dagger.Android to create a Subcomponent which has a parent Component of whichever module
 * ActivityBindingModule is on, in our case that will be [AppComponent]. You never
 * need to tell [AppComponent] that it is going to have all these subcomponents
 * nor do you need to tell these subcomponents that [AppComponent] exists.
 * We are also telling Dagger.Android that this generated SubComponent needs to include the
 * specified modules and be aware of a scope annotation [@ActivityScoped].
 * When Dagger.Android annotation processor runs it will create 2 subcomponents for us.
 */
@Module
@Suppress("UNUSED")
abstract class ActivityBindingModule {

    @ActivityScoped
    @ContributesAndroidInjector(modules = [LaunchModule::class])
    internal abstract fun launcherActivity(): LauncherActivity

    @ActivityScoped
    @ContributesAndroidInjector(
        modules = [
            OnboardingModule::class,
            SignInDialogModule::class
        ]
    )
    internal abstract fun onboardingActivity(): OnboardingActivity

    @ActivityScoped
    @ContributesAndroidInjector(
        modules = [
            // activity
            MainActivityModule::class,
            // fragments
            AgendaModule::class,
            CodelabsModule::class,
            FeedModule::class,
            InfoModule::class,
            MapModule::class,
            ScheduleModule::class,
            SearchModule::class,
            SessionDetailModule::class,
            SettingsModule::class,
            SpeakerModule::class,
            // other
            PreferenceModule::class,
            ReservationModule::class,
            PreferenceModule::class,
            SessionFeedbackModule::class,
            SignInDialogModule::class,
            EventActionsViewModelDelegateModule::class
        ]
    )
    internal abstract fun mainActivity(): MainActivity

    @ActivityScoped
    @ContributesAndroidInjector(
        modules = [
            SessionDetailModule::class,
            SessionFeedbackModule::class,
            SignInDialogModule::class,
            ReservationModule::class,
            PreferenceModule::class
        ]
    )
    internal abstract fun sessionDetailActivity(): SessionDetailActivity
}
