/*
 * Copyright 2019 Google LLC
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

import com.google.samples.apps.iosched.shared.data.config.AppConfigDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class FeatureFlagsModule {

    @Provides
    @Singleton
    @ExploreArEnabledFlag
    fun provideEnableExploreArFlag(appConfig: AppConfigDataSource): Boolean {
        return appConfig.isExploreArFeatureEnabled()
    }

    @Provides
    @MapFeatureEnabledFlag
    fun provideMapFeatureEnabledFlag(appConfig: AppConfigDataSource): Boolean {
        return appConfig.isMapFeatureEnabled()
    }

    @Provides
    @CodelabsEnabledFlag
    fun provideCodelabsEnabledFlag(appConfig: AppConfigDataSource): Boolean {
        return appConfig.isCodelabsFeatureEnabled()
    }

    @Provides
    @SearchScheduleEnabledFlag
    fun provideSearchScheduleEnabledFlag(appConfig: AppConfigDataSource): Boolean {
        return appConfig.isSearchScheduleFeatureEnabled()
    }

    @Provides
    @SearchUsingRoomEnabledFlag
    fun provideSearchUsingRoomEnabledFlag(appConfig: AppConfigDataSource): Boolean {
        return appConfig.isSearchUsingRoomFeatureEnabled()
    }

    @Provides
    @AssistantAppEnabledFlag
    fun provideAssistantAppEnabledFlag(appConfig: AppConfigDataSource): Boolean {
        return appConfig.isAssistantAppFeatureEnabled()
    }

    @Provides
    @ReservationEnabledFlag
    fun provideReservationEnabledFlag(appConfig: AppConfigDataSource): Boolean {
        return appConfig.isReservationFeatureEnabled()
    }
}
