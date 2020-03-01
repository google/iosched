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

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.samples.apps.iosched.shared.BuildConfig
import com.google.samples.apps.iosched.shared.R
import com.google.samples.apps.iosched.shared.data.BootstrapConferenceDataSource
import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.ConferenceDataSource
import com.google.samples.apps.iosched.shared.data.NetworkConferenceDataSource
import com.google.samples.apps.iosched.shared.data.ar.ArDebugFlagEndpoint
import com.google.samples.apps.iosched.shared.data.ar.DefaultArDebugFlagEndpoint
import com.google.samples.apps.iosched.shared.data.config.AppConfigDataSource
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource
import com.google.samples.apps.iosched.shared.data.db.AppDatabase
import com.google.samples.apps.iosched.shared.data.feed.AnnouncementDataSource
import com.google.samples.apps.iosched.shared.data.feed.DefaultFeedRepository
import com.google.samples.apps.iosched.shared.data.feed.FeedRepository
import com.google.samples.apps.iosched.shared.data.feed.FirestoreAnnouncementDataSource
import com.google.samples.apps.iosched.shared.data.feed.FirestoreMomentDataSource
import com.google.samples.apps.iosched.shared.data.feed.MomentDataSource
import com.google.samples.apps.iosched.shared.data.feedback.DefaultFeedbackEndpoint
import com.google.samples.apps.iosched.shared.data.feedback.FeedbackEndpoint
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.FirestoreUserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.domain.search.FtsMatchStrategy
import com.google.samples.apps.iosched.shared.domain.search.SessionTextMatchStrategy
import com.google.samples.apps.iosched.shared.domain.search.SimpleMatchStrategy
import com.google.samples.apps.iosched.shared.fcm.FcmTopicSubscriber
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.time.DefaultTimeProvider
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.NetworkUtils
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Named
import javax.inject.Singleton

/**
 * Module where classes created in the shared module are created.
 */
@ExperimentalCoroutinesApi
@Module
class SharedModule {

// Define the data source implementations that should be used. All data sources are singletons.

    @Singleton
    @Provides
    @Named("remoteConfDatasource")
    fun provideConferenceDataSource(
        context: Context,
        networkUtils: NetworkUtils
    ): ConferenceDataSource {
        return NetworkConferenceDataSource(context, networkUtils)
    }

    @Singleton
    @Provides
    @Named("bootstrapConfDataSource")
    fun provideBootstrapRemoteSessionDataSource(): ConferenceDataSource {
        return BootstrapConferenceDataSource
    }

    @Singleton
    @Provides
    fun provideConferenceDataRepository(
        @Named("remoteConfDatasource") remoteDataSource: ConferenceDataSource,
        @Named("bootstrapConfDataSource") boostrapDataSource: ConferenceDataSource,
        appDatabase: AppDatabase
    ): ConferenceDataRepository {
        return ConferenceDataRepository(remoteDataSource, boostrapDataSource, appDatabase)
    }

    @Singleton
    @Provides
    fun provideAnnouncementDataSource(firestore: FirebaseFirestore): AnnouncementDataSource {
        return FirestoreAnnouncementDataSource(firestore)
    }

    @Singleton
    @Provides
    fun provideMomentsDataSource(firestore: FirebaseFirestore): MomentDataSource {
        return FirestoreMomentDataSource(firestore)
    }

    @Singleton
    @Provides
    fun provideFeedRepository(
        dataSource: AnnouncementDataSource,
        momentsDataSource: MomentDataSource
    ): FeedRepository {
        return DefaultFeedRepository(dataSource, momentsDataSource)
    }

    @Singleton
    @Provides
    fun provideSessionRepository(
        conferenceDataRepository: ConferenceDataRepository
    ): SessionRepository {
        return DefaultSessionRepository(conferenceDataRepository)
    }

    @Singleton
    @Provides
    fun provideUserEventDataSource(
        firestore: FirebaseFirestore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): UserEventDataSource {
        return FirestoreUserEventDataSource(firestore, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideFeedbackEndpoint(functions: FirebaseFunctions): FeedbackEndpoint {
        return DefaultFeedbackEndpoint(functions)
    }

    @Singleton
    @Provides
    fun provideSessionAndUserEventRepository(
        userEventDataSource: UserEventDataSource,
        sessionRepository: SessionRepository
    ): SessionAndUserEventRepository {
        return DefaultSessionAndUserEventRepository(
            userEventDataSource,
            sessionRepository
        )
    }

    @Singleton
    @Provides
    fun provideFirebaseFireStore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            // This is to enable the offline data
            // https://firebase.google.com/docs/firestore/manage-data/enable-offline
            .setPersistenceEnabled(true)
            .build()
        return firestore
    }

    @Singleton
    @Provides
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return FirebaseFunctions.getInstance()
    }

    @Singleton
    @Provides
    fun provideArDebugFlagEndpoint(functions: FirebaseFunctions): ArDebugFlagEndpoint {
        return DefaultArDebugFlagEndpoint(functions)
    }

    @Singleton
    @Provides
    fun provideTopicSubscriber(): TopicSubscriber {
        return FcmTopicSubscriber()
    }

    @Singleton
    @Provides
    fun provideFirebaseRemoteConfigSettings(): FirebaseRemoteConfigSettings {
        val builder = FirebaseRemoteConfigSettings.Builder()
        if (BuildConfig.DEBUG) {
            builder.minimumFetchIntervalInSeconds = 0
        }
        return builder.build()
    }

    @Singleton
    @Provides
    fun provideFirebaseRemoteConfig(
        configSettings: FirebaseRemoteConfigSettings
    ): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }

    @Singleton
    @Provides
    fun provideAppConfigDataSource(
        remoteConfig: FirebaseRemoteConfig,
        configSettings: FirebaseRemoteConfigSettings,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): AppConfigDataSource {
        return RemoteAppConfigDataSource(remoteConfig, configSettings, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideTimeProvider(): TimeProvider {
        return DefaultTimeProvider
    }

    @Singleton
    @Provides
    fun provideSessionTextMatchStrategy(
        @SearchUsingRoomEnabledFlag useRoom: Boolean,
        appDatabase: AppDatabase
    ): SessionTextMatchStrategy {
        return if (useRoom) FtsMatchStrategy(appDatabase) else SimpleMatchStrategy
    }
}
