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

package com.google.samples.apps.iosched.shared.di

import com.google.samples.apps.iosched.shared.data.session.FakeSessionDataSource
import com.google.samples.apps.iosched.shared.data.session.SessionDataSource
import com.google.samples.apps.iosched.shared.data.tag.FakeTagDataSource
import com.google.samples.apps.iosched.shared.data.tag.TagDataSource
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Module where classes created in the shared module are created.
 */
@Module
class SharedModule {

    /**
     * Defines the implementation of [SessionDataSource] that should be used.
     * The [SessionDataSource] is a singleton.
     */
    @Singleton
    @Provides
    fun provideSessionDataSource(): SessionDataSource {
        return FakeSessionDataSource
    }

    /**
     * Defines the implementation of [TagDataSource] that should be used.
     * The [TagDataSource] is a singleton.
     */
    @Singleton
    @Provides
    fun provideTagDataSource(): TagDataSource {
        return FakeTagDataSource
    }
}
