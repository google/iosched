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

package com.google.samples.apps.iosched.shared.data.agenda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.data.config.AppConfigDataSource
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.AFTER_DARK_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.AFTER_DARK_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BADGE_DEVICE_PICK_UP_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BADGE_DEVICE_PICK_UP_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BADGE_DEVICE_PICK_UP_DAY3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BADGE_DEVICE_PICK_UP_DAY3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BADGE_PICK_UP_DAY0_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BADGE_PICK_UP_DAY0_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BADGE_PICK_UP_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BADGE_PICK_UP_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.CODELABS_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.CODELABS_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.CODELABS_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.CODELABS_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.CODELABS_DAY3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.CODELABS_DAY3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.CONCERT_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.CONCERT_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.DEVELOPER_KEYNOTE_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.DEVELOPER_KEYNOTE_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.GOOGLE_KEYNOTE_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.GOOGLE_KEYNOTE_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.IO_STORE_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.IO_STORE_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.IO_STORE_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.IO_STORE_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.IO_STORE_DAY3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.IO_STORE_DAY3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.OFFICE_HOURS_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.OFFICE_HOURS_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.OFFICE_HOURS_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.OFFICE_HOURS_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.OFFICE_HOURS_DAY3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.OFFICE_HOURS_DAY3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SANDBOXES_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SANDBOXES_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SANDBOXES_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SANDBOXES_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SANDBOXES_DAY3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SANDBOXES_DAY3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.StringsChangedCallback
import org.threeten.bp.ZonedDateTime

/**
 * Single point of access to agenda data for the presentation layer.
 */
interface AgendaRepository {
    fun getAgenda(): List<Block>

    fun getObservableAgenda(): LiveData<List<Block>>
}

class DefaultAgendaRepository(private val appConfigDataSource: AppConfigDataSource) :
    AgendaRepository {

    private val blocks by lazy {
        generateBlocks(appConfigDataSource)
    }

    /**
     * Generates a list of [Block]s. Default values of each [Block] is supplied from the
     * default values stored as shared/src/main/res/xml/remote_config_defaults.xml.
     * Add a corresponding entry in RemoteConfig is any [Block]s need to be overridden.
     */
    private fun generateBlocks(dataSource: AppConfigDataSource): List<Block> {
        return listOf(
            Block(
                title = "Badge pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BADGE_PICK_UP_DAY0_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BADGE_PICK_UP_DAY0_END_TIME).value)
            ),
            Block(
                title = "Badge pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BADGE_PICK_UP_DAY1_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BADGE_PICK_UP_DAY1_END_TIME).value)
            ),
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BREAKFAST_DAY1_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BREAKFAST_DAY1_END_TIME).value)
            ),
            Block(
                title = "Google Keynote",
                type = "keynote",
                color = 0xfffbbc05.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(GOOGLE_KEYNOTE_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(GOOGLE_KEYNOTE_END_TIME).value)
            ),
            Block(
                title = "I/O Store",
                type = "store",
                color = 0xffffffff.toInt(),
                strokeColor = 0xffff6c00.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(IO_STORE_DAY1_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(IO_STORE_DAY1_END_TIME).value)
            ),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(LUNCH_DAY1_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(LUNCH_DAY1_END_TIME).value)
            ),
            Block(
                title = "Developer Keynote",
                type = "keynote",
                color = 0xfffbbc05.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(DEVELOPER_KEYNOTE_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(DEVELOPER_KEYNOTE_END_TIME).value)
            ),
            Block(
                title = "Sessions",
                type = "session",
                isDark = true,
                color = 0xff5bb975.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SESSIONS_DAY1_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SESSIONS_DAY1_END_TIME).value)
            ),
            Block(
                title = "Codelabs",
                type = "codelab",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(CODELABS_DAY1_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(CODELABS_DAY1_END_TIME).value)
            ),
            Block(
                title = "Office Hours & App Reviews",
                type = "office_hours",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(OFFICE_HOURS_DAY1_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(OFFICE_HOURS_DAY1_END_TIME).value)
            ),
            Block(
                title = "Sandboxes",
                type = "sandbox",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SANDBOXES_DAY1_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SANDBOXES_DAY1_END_TIME).value)
            ),
            Block(
                title = "After Dark",
                type = "after_hours",
                color = 0xff164fa5.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(AFTER_DARK_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(AFTER_DARK_END_TIME).value)
            ),
            Block(
                title = "Badge pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource
                        .getStringLiveData(BADGE_DEVICE_PICK_UP_DAY2_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource
                        .getStringLiveData(BADGE_DEVICE_PICK_UP_DAY2_END_TIME).value)
            ),
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BREAKFAST_DAY2_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BREAKFAST_DAY2_END_TIME).value)
            ),
            Block(
                title = "I/O Store",
                type = "store",
                color = 0xffffffff.toInt(),
                strokeColor = 0xffff6c00.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(IO_STORE_DAY2_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(IO_STORE_DAY2_END_TIME).value)
            ),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(LUNCH_DAY2_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(LUNCH_DAY2_END_TIME).value)
            ),
            Block(
                title = "Sessions",
                type = "session",
                isDark = true,
                color = 0xff5bb975.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SESSIONS_DAY2_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SESSIONS_DAY2_END_TIME).value)
            ),
            Block(
                title = "Codelabs",
                type = "codelab",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(CODELABS_DAY2_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(CODELABS_DAY2_END_TIME).value)
            ),
            Block(
                title = "Office Hours & App Reviews",
                type = "office_hours",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(OFFICE_HOURS_DAY2_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(OFFICE_HOURS_DAY2_END_TIME).value)
            ),
            Block(
                title = "Sandboxes",
                type = "sandbox",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SANDBOXES_DAY2_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SANDBOXES_DAY2_END_TIME).value)
            ),
            Block(
                title = "Concert",
                type = "concert",
                color = 0xff164fa5.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(CONCERT_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(CONCERT_END_TIME).value)
            ),
            Block(
                title = "Badge pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource
                        .getStringLiveData(BADGE_DEVICE_PICK_UP_DAY3_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource
                        .getStringLiveData(BADGE_DEVICE_PICK_UP_DAY3_END_TIME).value)
            ),
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BREAKFAST_DAY3_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(BREAKFAST_DAY3_END_TIME).value)
            ),
            Block(
                title = "I/O Store",
                type = "store",
                color = 0xffffffff.toInt(),
                strokeColor = 0xffff6c00.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(IO_STORE_DAY3_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(IO_STORE_DAY3_END_TIME).value)
            ),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(LUNCH_DAY3_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(LUNCH_DAY3_END_TIME).value)
            ),
            Block(
                title = "Sessions",
                type = "session",
                isDark = true,
                color = 0xff5bb975.toInt(),
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SESSIONS_DAY3_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SESSIONS_DAY3_END_TIME).value)
            ),
            Block(
                title = "Codelabs",
                type = "codelab",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(CODELABS_DAY3_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(CODELABS_DAY3_END_TIME).value)
            ),
            Block(
                title = "Office Hours & App Reviews",
                type = "office_hours",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(OFFICE_HOURS_DAY3_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(OFFICE_HOURS_DAY3_END_TIME).value)
            ),
            Block(
                title = "Sandboxes",
                type = "sandbox",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SANDBOXES_DAY3_START_TIME).value),
                endTime = ZonedDateTime.parse(
                    dataSource.getStringLiveData(SANDBOXES_DAY3_END_TIME).value)
            )
        )
    }

    override fun getAgenda(): List<Block> = blocks

    /**
     * Returns a list of [Block]s as [LiveData].
     * This is needed because each start/end time could be overridden from RemoteConfig.
     * When the time is updated from RemoteConfig, the List needs to be observable otherwise
     * the value change in RemoteConfig isn't effective unless restarting the app.
     */
    override fun getObservableAgenda(): LiveData<List<Block>> {
        val result: MutableLiveData<List<Block>> = MutableLiveData()
        result.postValue(getAgenda())
        appConfigDataSource.syncStringsAsync(object : StringsChangedCallback {
            override fun onChanged(changedKeys: List<String>) {
                if (!changedKeys.isEmpty()) {
                    result.postValue(generateBlocks(appConfigDataSource))
                }
            }
        })
        return result
    }
}
