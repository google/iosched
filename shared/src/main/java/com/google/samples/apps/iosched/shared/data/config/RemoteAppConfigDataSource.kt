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

package com.google.samples.apps.iosched.shared.data.config

import android.content.res.Resources.NotFoundException
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.samples.apps.iosched.model.ConferenceWifiInfo
import com.google.samples.apps.iosched.shared.BuildConfig
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume

class RemoteAppConfigDataSource @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    configSettings: FirebaseRemoteConfigSettings,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AppConfigDataSource {

    val times = mutableMapOf<String, String>()

    // Set cache expiration to 0s when debugging to allow easy testing, otherwise
    // use the default value.
    private val cacheExpirySeconds: Long = if (BuildConfig.DEBUG) 0 else DEFAULT_CACHE_EXPIRY_S

    init {
        // Do an initial load of default values
        updateStrings()
        firebaseRemoteConfig.fetch(cacheExpirySeconds).addOnCompleteListener { task ->
            // Async
            if (task.isSuccessful) {
                firebaseRemoteConfig.activate().addOnCompleteListener {
                    updateStrings()
                }
            }
        }
    }

    private fun updateStrings() {
        AgendaTimestampsKey.values().forEach {
            times[it.key] = firebaseRemoteConfig.getString(it.key)
        }
    }

    override suspend fun syncStrings() {
        withContext(ioDispatcher) {
            val task = firebaseRemoteConfig.fetch(cacheExpirySeconds)
            suspendCancellableCoroutine<Unit> { continuation ->
                task.addOnCompleteListener {
                    firebaseRemoteConfig.activate().addOnCompleteListener {
                        updateStrings()
                        continuation.resume(Unit)
                    }.addOnFailureListener { exception ->
                        Timber.w(exception, "Sync strings failed")
                    }
                }
                task.addOnFailureListener { exception ->
                    Timber.w(exception, "Sync strings failed")
                }
            }
        }
    }

    override fun getTimestamp(key: String): String =
        times[key] ?: throw NotFoundException("Value for $key not found")

    override fun getWifiInfo(): ConferenceWifiInfo {
        return ConferenceWifiInfo(
            ssid = firebaseRemoteConfig.getString(WIFI_SSID_KEY),
            password = firebaseRemoteConfig.getString(WIFI_PASSWORD_KEY)
        )
    }

    override fun isMapFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(MAP_FEATURE_ENABLED)

    override fun isExploreArFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(EXPLORE_AR_FEATURE_ENABLED)

    override fun isCodelabsFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(CODELABS_FEATURE_ENABLED)

    override fun isSearchScheduleFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(SEARCH_SCHEDULE_FEATURE_ENABLED)

    override fun isSearchUsingRoomFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(SEARCH_USING_ROOM_FEATURE_ENABLED)

    override fun isAssistantAppFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(ASSISTANT_APP_FEATURE_ENABLED)

    override fun isReservationFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(RESERVATION_FEATURE_ENABLED)

    override fun isFeedEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(FEED_FEATURE_ENABLED)

    companion object {
        const val WIFI_SSID_KEY = "wifi_ssid"
        const val WIFI_PASSWORD_KEY = "wifi_password"

        const val MAP_FEATURE_ENABLED = "map_enabled"
        const val EXPLORE_AR_FEATURE_ENABLED = "explore_ar_enabled"
        const val CODELABS_FEATURE_ENABLED = "codelabs_enabled"
        const val SEARCH_SCHEDULE_FEATURE_ENABLED = "search_schedule_enabled"
        const val SEARCH_USING_ROOM_FEATURE_ENABLED = "search_using_room_enabled"
        const val ASSISTANT_APP_FEATURE_ENABLED = "io_assistant_app_enabled"
        const val RESERVATION_FEATURE_ENABLED = "reservation_enabled"
        const val FEED_FEATURE_ENABLED = "feed_enabled"

        val DEFAULT_CACHE_EXPIRY_S = TimeUnit.MINUTES.toSeconds(12)
    }
}

enum class AgendaTimestampsKey(val key: String) {
    BADGE_PICK_UP_DAY0_START_TIME("badge_pick_up_day0_start_time"),
    BADGE_PICK_UP_DAY0_END_TIME("badge_pick_up_day0_end_time"),
    BADGE_PICK_UP_DAY1_START_TIME("badge_pick_up_day1_start_time"),
    BADGE_PICK_UP_DAY1_END_TIME("badge_pick_up_day1_end_time"),
    BREAKFAST_DAY1_START_TIME("breakfast_day1_start_time"),
    BREAKFAST_DAY1_END_TIME("breakfast_day1_end_time"),
    GOOGLE_KEYNOTE_START_TIME("google_keynote_start_time"),
    GOOGLE_KEYNOTE_END_TIME("google_keynote_end_time"),
    IO_STORE_DAY1_START_TIME("io_store_day1_start_time"),
    IO_STORE_DAY1_END_TIME("io_store_day1_end_time"),
    LUNCH_DAY1_START_TIME("lunch_day1_start_time"),
    LUNCH_DAY1_END_TIME("lunch_day1_end_time"),
    DEVELOPER_KEYNOTE_START_TIME("developer_keynote_start_time"),
    DEVELOPER_KEYNOTE_END_TIME("developer_keynote_end_time"),
    SESSIONS_DAY1_START_TIME("sessions_day1_start_time"),
    SESSIONS_DAY1_END_TIME("sessions_day1_end_time"),
    CODELABS_DAY1_START_TIME("codelabs_day1_start_time"),
    CODELABS_DAY1_END_TIME("codelabs_day1_end_time"),
    OFFICE_HOURS_DAY1_START_TIME("office_hours_day1_start_time"),
    OFFICE_HOURS_DAY1_END_TIME("office_hours_day1_end_time"),
    SANDBOXES_DAY1_START_TIME("sandboxes_day1_start_time"),
    SANDBOXES_DAY1_END_TIME("sandboxes_day1_end_time"),
    AFTER_DARK_START_TIME("after_dark_start_time"),
    AFTER_DARK_END_TIME("after_dark_end_time"),
    BADGE_DEVICE_PICK_UP_DAY2_START_TIME("badge_device_pick_up_day2_start_time"),
    BADGE_DEVICE_PICK_UP_DAY2_END_TIME("badge_device_pick_up_day2_end_time"),
    BREAKFAST_DAY2_START_TIME("breakfast_day2_start_time"),
    BREAKFAST_DAY2_END_TIME("breakfast_day2_end_time"),
    IO_STORE_DAY2_START_TIME("io_store_day2_start_time"),
    IO_STORE_DAY2_END_TIME("io_store_day2_end_time"),
    LUNCH_DAY2_START_TIME("lunch_day2_start_time"),
    LUNCH_DAY2_END_TIME("lunch_day2_end_time"),
    SESSIONS_DAY2_START_TIME("sessions_day2_start_time"),
    SESSIONS_DAY2_END_TIME("sessions_day2_end_time"),
    CODELABS_DAY2_START_TIME("codelabs_day2_start_time"),
    CODELABS_DAY2_END_TIME("codelabs_day2_end_time"),
    OFFICE_HOURS_DAY2_START_TIME("office_hours_day2_start_time"),
    OFFICE_HOURS_DAY2_END_TIME("office_hours_day2_end_time"),
    SANDBOXES_DAY2_START_TIME("sandboxes_day2_start_time"),
    SANDBOXES_DAY2_END_TIME("sandboxes_day2_end_time"),
    CONCERT_START_TIME("concert_start_time"),
    CONCERT_END_TIME("concert_end_time"),
    BADGE_DEVICE_PICK_UP_DAY3_START_TIME("badge_device_pick_up_day3_start_time"),
    BADGE_DEVICE_PICK_UP_DAY3_END_TIME("badge_device_pick_up_day3_end_time"),
    BREAKFAST_DAY3_START_TIME("breakfast_day3_start_time"),
    BREAKFAST_DAY3_END_TIME("breakfast_day3_end_time"),
    IO_STORE_DAY3_START_TIME("io_store_day3_start_time"),
    IO_STORE_DAY3_END_TIME("io_store_day3_end_time"),
    LUNCH_DAY3_START_TIME("lunch_day3_start_time"),
    LUNCH_DAY3_END_TIME("lunch_day3_end_time"),
    SESSIONS_DAY3_START_TIME("sessions_day3_start_time"),
    SESSIONS_DAY3_END_TIME("sessions_day3_end_time"),
    CODELABS_DAY3_START_TIME("codelabs_day3_start_time"),
    CODELABS_DAY3_END_TIME("codelabs_day3_end_time"),
    OFFICE_HOURS_DAY3_START_TIME("office_hours_day3_start_time"),
    OFFICE_HOURS_DAY3_END_TIME("office_hours_day3_end_time"),
    SANDBOXES_DAY3_START_TIME("sandboxes_day3_start_time"),
    SANDBOXES_DAY3_END_TIME("sandboxes_day3_end_time"),
}
