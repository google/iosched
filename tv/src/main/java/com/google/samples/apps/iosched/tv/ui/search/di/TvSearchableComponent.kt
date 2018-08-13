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

package com.google.samples.apps.iosched.tv.ui.search.di

import com.google.samples.apps.iosched.tv.TvApplication
import com.google.samples.apps.iosched.tv.search.SessionContentProvider
import com.google.samples.apps.iosched.tv.ui.search.SearchableActivity
import com.google.samples.apps.iosched.tv.ui.search.SearchableViewModelFactory
import dagger.Subcomponent

/**
 * Search component for the tv app, created and persisted in the [TvApplication].
 */
@Subcomponent(modules = [(TvSearchableModule::class)])
interface TvSearchableComponent {

    fun searchableViewModelFactory(): SearchableViewModelFactory

    fun inject(searchableActivity: SearchableActivity)
    fun inject(sessionContentProvider: SessionContentProvider)
}
