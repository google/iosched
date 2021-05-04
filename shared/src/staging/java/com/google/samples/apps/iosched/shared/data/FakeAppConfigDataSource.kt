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

package com.google.samples.apps.iosched.shared.data

import android.content.res.Resources.NotFoundException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.model.ConferenceWifiInfo
import com.google.samples.apps.iosched.shared.data.config.AppConfigDataSource
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

class FakeAppConfigDataSource : AppConfigDataSource {

    private val times1: Map<String, MutableLiveData<String>> = mapOf(
        RemoteAppConfigDataSource.BADGE_PICK_UP_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BADGE_PICK_UP_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BADGE_PICK_UP_DAY0_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BADGE_PICK_UP_DAY0_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.GOOGLE_KEYNOTE_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.GOOGLE_KEYNOTE_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.IO_STORE_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.IO_STORE_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BADGE_PICK_UP_DAY0_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.DEVELOPER_KEYNOTE_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.DEVELOPER_KEYNOTE_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.CODELABS_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.CODELABS_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.OFFICE_HOURS_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.OFFICE_HOURS_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SANDBOXES_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SANDBOXES_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.AFTER_DARK_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.AFTER_DARK_END_TIME to MutableLiveData()
    )
    private val times2: Map<String, MutableLiveData<String>> = mapOf(
        RemoteAppConfigDataSource.BADGE_DEVICE_PICK_UP_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BADGE_DEVICE_PICK_UP_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.IO_STORE_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.IO_STORE_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.CODELABS_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.CODELABS_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.OFFICE_HOURS_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.OFFICE_HOURS_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SANDBOXES_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SANDBOXES_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.CONCERT_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.CONCERT_END_TIME to MutableLiveData()
    )

    private val times3: Map<String, MutableLiveData<String>> = mapOf(
        RemoteAppConfigDataSource.BADGE_DEVICE_PICK_UP_DAY3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BADGE_DEVICE_PICK_UP_DAY3_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY3_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.IO_STORE_DAY3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.IO_STORE_DAY3_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY3_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY3_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.CODELABS_DAY3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.CODELABS_DAY3_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.OFFICE_HOURS_DAY3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.OFFICE_HOURS_DAY3_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SANDBOXES_DAY3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SANDBOXES_DAY3_END_TIME to MutableLiveData()
    )

    init {
        val startTimeDay1 = TimeUtils.ConferenceDays[0].start
        initTimes(startTimeDay1, times1)
        val startTimeDay2 = TimeUtils.ConferenceDays[1].start
        initTimes(startTimeDay2, times2)
        val startTimeDay3 = TimeUtils.ConferenceDays[2].start
        initTimes(startTimeDay3, times3)
    }

    private fun initTimes(
        startTimeDay: ZonedDateTime,
        times: Map<String, MutableLiveData<String>>
    ) {
        times.values.forEachIndexed { index, mutableLiveData ->
            mutableLiveData.setValue(
                startTimeDay.plusMinutes(index.toLong()).format(ISO_OFFSET_DATE_TIME)
            )
        }
    }

    override fun getStringLiveData(key: String): LiveData<String> {
        return times1[key] ?: times2[key] ?: times3[key]
            ?: throw NotFoundException("Value for $key not found")
    }

    override suspend fun syncStrings() {}

    override fun getWifiInfo(): ConferenceWifiInfo = ConferenceWifiInfo("", "")
    override fun isMapFeatureEnabled(): Boolean = true
    override fun isExploreArFeatureEnabled(): Boolean = true
    override fun isCodelabsFeatureEnabled(): Boolean = true
    override fun isSearchScheduleFeatureEnabled(): Boolean = true
    override fun isSearchUsingRoomFeatureEnabled(): Boolean = true
    override fun isAssistantAppFeatureEnabled(): Boolean = false
    override fun isReservationFeatureEnabled(): Boolean = false
    override fun isFeedEnabled(): Boolean = false
}
