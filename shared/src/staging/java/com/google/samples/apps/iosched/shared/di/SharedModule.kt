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

import com.google.samples.apps.iosched.shared.data.BootstrapConferenceDataSource
import com.google.samples.apps.iosched.shared.data.ConferenceDataSource
import com.google.samples.apps.iosched.shared.data.map.FakeMapMetadataDataSource
import com.google.samples.apps.iosched.shared.data.map.MapMetadataDataSource
import com.google.samples.apps.iosched.shared.data.session.FakeSessionDataSource
import com.google.samples.apps.iosched.shared.data.session.agenda.AgendaDataSource
import com.google.samples.apps.iosched.shared.data.session.agenda.FakeAgendaDataSource
import com.google.samples.apps.iosched.shared.data.tag.FakeTagDataSource
import com.google.samples.apps.iosched.shared.data.tag.TagDataSource
import com.google.samples.apps.iosched.shared.data.userevent.FakeUserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Module where classes created in the shared module are created.
 */
@Module
class SharedModule {

// Define the data source implementations that should be used. All data sources are singletons.

    @Singleton
    @Provides
    @Named("remoteConfDatasource")
    fun provideConferenceDataSource(): ConferenceDataSource {
        return FakeSessionDataSource
    }

    @Singleton
    @Provides
    @Named("bootstrapConfDataSource")
    fun provideBootstrapRemoteSessionDataSource(): ConferenceDataSource {
        return BootstrapConferenceDataSource
    }

    @Singleton
    @Provides
    fun provideAgendaDataSource(): AgendaDataSource {
        return FakeAgendaDataSource
    }

    @Singleton
    @Provides
    fun provideTagDataSource(): TagDataSource {
        return FakeTagDataSource
    }

    @Singleton
    @Provides
    fun provideMapMetadataDataSource(): MapMetadataDataSource {
        return FakeMapMetadataDataSource
    }

    @Singleton
    @Provides
    fun provideUserEventDataSource(): UserEventDataSource {
        return FakeUserEventDataSource
    }
}
