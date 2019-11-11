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
import com.google.samples.apps.iosched.shared.data.config.AppConfigDataSource
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

class FakeAppConfigDataSource(
    private val autoScrollFlagEnabled: Boolean = true
) : AppConfigDataSource {
    private val times1: Map<String, MutableLiveData<String>> = mapOf(
        RemoteAppConfigDataSource.REGISTRATION_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.REGISTRATION_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.KEYNOTE_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.KEYNOTE_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY1_1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY1_1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY1_2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY1_2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY1_3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY1_3_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.TEA_BREAK_DAY1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.TEA_BREAK_DAY1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.PARTY_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.PARTY_END_TIME to MutableLiveData()
    )

    private val times2: Map<String, MutableLiveData<String>> = mapOf(
        RemoteAppConfigDataSource.REGISTRATION_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.REGISTRATION_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.BREAKFAST_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.LUNCH_DAY2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY2_1_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY2_1_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY2_2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY2_2_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY2_3_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.SESSIONS_DAY2_3_END_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.TEA_BREAK_DAY2_START_TIME to MutableLiveData(),
        RemoteAppConfigDataSource.TEA_BREAK_DAY2_END_TIME to MutableLiveData()
    )

    private val labels: Map<String, MutableLiveData<String>> = mapOf(
        RemoteAppConfigDataSource.LABEL_REGISTRATION to MutableLiveData("Registration"),
        RemoteAppConfigDataSource.LABEL_KEYNOTE to MutableLiveData("Keynote"),
        RemoteAppConfigDataSource.LABEL_SESSIONS to MutableLiveData("Sessions"),
        RemoteAppConfigDataSource.LABEL_BREAKFAST to MutableLiveData("Breakfast"),
        RemoteAppConfigDataSource.LABEL_LUNCH to MutableLiveData("Lunch"),
        RemoteAppConfigDataSource.LABEL_TEA_BREAK to MutableLiveData("Tea Break"),
        RemoteAppConfigDataSource.LABEL_PARTY to MutableLiveData("Party")
    )

    init {
        val startTimeDay1 = TimeUtils.ConferenceDays[0].start
        initTimes(startTimeDay1, times1)
        val startTimeDay2 = TimeUtils.ConferenceDays[1].start
        initTimes(startTimeDay2, times2)
    }

    private fun initTimes(
        startTimeDay2: ZonedDateTime,
        times: Map<String, MutableLiveData<String>>
    ) {
        times.values.forEachIndexed { index, mutableLiveData ->
            mutableLiveData.postValue(
                startTimeDay2.plusMinutes(index.toLong()).format(ISO_OFFSET_DATE_TIME)
            )
        }
    }

    override suspend fun syncStrings() {}

    override fun getString(key: String): LiveData<String> {
        return times1[key] ?: times2[key] ?: labels[key]
            ?: throw NotFoundException("Value for $key not found")
    }

    override suspend fun isAutoScrollFlagEnabled() = autoScrollFlagEnabled
}
