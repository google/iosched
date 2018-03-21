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

package com.google.samples.apps.iosched.tv.ui.sessiondetail.di

import com.google.samples.apps.iosched.tv.ui.sessiondetail.SessionDetailFragment
import com.google.samples.apps.iosched.tv.ui.sessiondetail.SessionDetailViewModelFactory
import dagger.Subcomponent

/**
 * Session detail component for the tv app, created and managed in the [TvApplication].
 */
@Subcomponent(modules = arrayOf(TvSessionDetailModule::class))
interface TvSessionDetailComponent {

    fun sessionDetailViewModelFactory(): SessionDetailViewModelFactory

    fun inject(sessionDetailFragment: SessionDetailFragment)
}
