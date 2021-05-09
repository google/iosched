/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.tests.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage.Companion.PREFS_NAME
import com.google.samples.apps.iosched.di.PreferencesStorageModule
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.di.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PreferencesStorageModule::class]
)
@Module
object TestPreferencesStorageModule {

    @Singleton
    @Provides
    fun providePreferenceStorage(dataStore: DataStore<Preferences>): PreferenceStorage =
        DataStorePreferenceStorage(dataStore)

    @Singleton
    @Provides
    fun provideDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope applicationScope: CoroutineScope
    ): DataStore<Preferences> {
        // Using PreferenceDataStoreFactory so we can set our own application scope
        // that we can control and cancel in UI tests
        val datastore = PreferenceDataStoreFactory.create(
            migrations = listOf(SharedPreferencesMigration(context, PREFS_NAME)),
            scope = applicationScope
        ) {
            context.preferencesDataStoreFile(PREFS_NAME)
        }
        return datastore
    }
}
