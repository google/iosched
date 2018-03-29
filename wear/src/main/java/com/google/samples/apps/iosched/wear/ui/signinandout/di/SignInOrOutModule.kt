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

package com.google.samples.apps.iosched.wear.ui.signinandout.di

import com.google.samples.apps.iosched.shared.di.FragmentScoped
import com.google.samples.apps.iosched.wear.ui.signinandout.SignInOrOutFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Module where classes needed to create the [SignInOrOutFragment] are defined.
 */
@Module
internal abstract class SignInOrOutModule {

    @FragmentScoped
    @ContributesAndroidInjector
    internal abstract fun contributeSignInOrOutFragment(): SignInOrOutFragment
}
