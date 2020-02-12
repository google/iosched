/*
 * Copyright 2020 Google LLC
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

import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.data.config.AppConfigDataSource
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource
import org.threeten.bp.ZonedDateTime

/**
 * Generates a list of [Block]s. Default values of each [Block] is supplied from the
 * default values stored as shared/src/main/res/xml/remote_config_defaults.xml.
 * Add a corresponding entry in RemoteConfig is any [Block]s need to be overridden.
 */
fun generateBlocks(dataSource: AppConfigDataSource): List<Block> {
    return listOf(
        Block(
            title = BADGE_PICKUP_TITLE,
            type = BADGE_PICKUP_TYPE,
            color = 0xffe6e6e6.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BADGE_PICK_UP_DAY0_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BADGE_PICK_UP_DAY0_END_TIME
                ).value)
        ),
        Block(
            title = BADGE_PICKUP_TITLE,
            type = BADGE_PICKUP_TYPE,
            color = 0xffe6e6e6.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BADGE_PICK_UP_DAY1_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BADGE_PICK_UP_DAY1_END_TIME
                ).value)
        ),
        Block(
            title = BREAKFAST_TITLE,
            type = MEAL_TYPE,
            color = 0xfffad2ce.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BREAKFAST_DAY1_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BREAKFAST_DAY1_END_TIME).value)
        ),
        Block(
            title = GOOGLE_KEYNOTE_TITLE,
            type = KEYNOTE_TYPE,
            color = 0xfffbbc05.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.GOOGLE_KEYNOTE_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.GOOGLE_KEYNOTE_END_TIME).value)
        ),
        Block(
            title = IO_STORE_TITLE,
            type = STORE_TYPE,
            color = 0xffffffff.toInt(),
            strokeColor = 0xffff6c00.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.IO_STORE_DAY1_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.IO_STORE_DAY1_END_TIME).value)
        ),
        Block(
            title = LUNCH_TITLE,
            type = MEAL_TYPE,
            color = 0xfffad2ce.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.LUNCH_DAY1_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(RemoteAppConfigDataSource.LUNCH_DAY1_END_TIME).value)
        ),
        Block(
            title = DEVELOPER_KEYNOTE_TITLE,
            type = KEYNOTE_TYPE,
            color = 0xfffbbc05.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.DEVELOPER_KEYNOTE_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.DEVELOPER_KEYNOTE_END_TIME
                ).value)
        ),
        Block(
            title = SESSIONS_TITLE,
            type = SESSIONS_TYPE,
            isDark = true,
            color = 0xff5bb975.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SESSIONS_DAY1_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SESSIONS_DAY1_END_TIME).value)
        ),
        Block(
            title = CODELABS_TITLE,
            type = CODELABS_TYPE,
            color = 0xff4285f4.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.CODELABS_DAY1_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.CODELABS_DAY1_END_TIME).value)
        ),
        Block(
            title = OFFICE_HOURS_APP_REVIEWS_TITLE,
            type = OFFICE_HOURS_TYPE,
            color = 0xff4285f4.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.OFFICE_HOURS_DAY1_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.OFFICE_HOURS_DAY1_END_TIME
                ).value)
        ),
        Block(
            title = SANDBOXES_TITLE,
            type = SANDBOXES_TYPE,
            color = 0xff4285f4.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SANDBOXES_DAY1_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SANDBOXES_DAY1_END_TIME).value)
        ),
        Block(
            title = AFTER_DARK_TITLE,
            type = AFTER_HOURS_TYPE,
            color = 0xff164fa5.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.AFTER_DARK_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(RemoteAppConfigDataSource.AFTER_DARK_END_TIME).value)
        ),
        Block(
            title = BADGE_PICKUP_TITLE,
            type = BADGE_PICKUP_TYPE,
            color = 0xffe6e6e6.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource
                    .getStringLiveData(
                        RemoteAppConfigDataSource.BADGE_DEVICE_PICK_UP_DAY2_START_TIME
                    ).value),
            endTime = ZonedDateTime.parse(
                dataSource
                    .getStringLiveData(
                        RemoteAppConfigDataSource.BADGE_DEVICE_PICK_UP_DAY2_END_TIME
                    ).value)
        ),
        Block(
            title = BREAKFAST_TITLE,
            type = MEAL_TYPE,
            color = 0xfffad2ce.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BREAKFAST_DAY2_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BREAKFAST_DAY2_END_TIME).value)
        ),
        Block(
            title = IO_STORE_TITLE,
            type = STORE_TYPE,
            color = 0xffffffff.toInt(),
            strokeColor = 0xffff6c00.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.IO_STORE_DAY2_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.IO_STORE_DAY2_END_TIME).value)
        ),
        Block(
            title = LUNCH_TITLE,
            type = MEAL_TYPE,
            color = 0xfffad2ce.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.LUNCH_DAY2_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(RemoteAppConfigDataSource.LUNCH_DAY2_END_TIME).value)
        ),
        Block(
            title = SESSIONS_TITLE,
            type = SESSIONS_TYPE,
            isDark = true,
            color = 0xff5bb975.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SESSIONS_DAY2_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SESSIONS_DAY2_END_TIME).value)
        ),
        Block(
            title = CODELABS_TITLE,
            type = CODELABS_TYPE,
            color = 0xff4285f4.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.CODELABS_DAY2_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.CODELABS_DAY2_END_TIME).value)
        ),
        Block(
            title = OFFICE_HOURS_APP_REVIEWS_TITLE,
            type = OFFICE_HOURS_TYPE,
            color = 0xff4285f4.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.OFFICE_HOURS_DAY2_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.OFFICE_HOURS_DAY2_END_TIME
                ).value)
        ),
        Block(
            title = SANDBOXES_TITLE,
            type = SANDBOXES_TYPE,
            color = 0xff4285f4.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SANDBOXES_DAY2_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SANDBOXES_DAY2_END_TIME).value)
        ),
        Block(
            title = CONCERT_TITLE,
            type = CONCERT_TYPE,
            color = 0xff164fa5.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(RemoteAppConfigDataSource.CONCERT_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(RemoteAppConfigDataSource.CONCERT_END_TIME).value)
        ),
        Block(
            title = BADGE_PICKUP_TITLE,
            type = BADGE_PICKUP_TYPE,
            color = 0xffe6e6e6.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource
                    .getStringLiveData(
                        RemoteAppConfigDataSource.BADGE_DEVICE_PICK_UP_DAY3_START_TIME
                    ).value),
            endTime = ZonedDateTime.parse(
                dataSource
                    .getStringLiveData(
                        RemoteAppConfigDataSource.BADGE_DEVICE_PICK_UP_DAY3_END_TIME
                    ).value)
        ),
        Block(
            title = BREAKFAST_TITLE,
            type = MEAL_TYPE,
            color = 0xfffad2ce.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BREAKFAST_DAY3_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.BREAKFAST_DAY3_END_TIME).value)
        ),
        Block(
            title = IO_STORE_TITLE,
            type = STORE_TYPE,
            color = 0xffffffff.toInt(),
            strokeColor = 0xffff6c00.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.IO_STORE_DAY3_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.IO_STORE_DAY3_END_TIME).value)
        ),
        Block(
            title = LUNCH_TITLE,
            type = MEAL_TYPE,
            color = 0xfffad2ce.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.LUNCH_DAY3_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.LUNCH_DAY3_END_TIME).value)
        ),
        Block(
            title = SESSIONS_TITLE,
            type = SESSIONS_TYPE,
            isDark = true,
            color = 0xff5bb975.toInt(),
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SESSIONS_DAY3_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SESSIONS_DAY3_END_TIME).value)
        ),
        Block(
            title = CODELABS_TITLE,
            type = CODELABS_TYPE,
            color = 0xff4285f4.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.CODELABS_DAY3_START_TIME).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.CODELABS_DAY3_END_TIME).value)
        ),
        Block(
            title = OFFICE_HOURS_APP_REVIEWS_TITLE,
            type = OFFICE_HOURS_TYPE,
            color = 0xff4285f4.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.OFFICE_HOURS_DAY3_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.OFFICE_HOURS_DAY3_END_TIME
                ).value)
        ),
        Block(
            title = SANDBOXES_TITLE,
            type = SANDBOXES_TYPE,
            color = 0xff4285f4.toInt(),
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SANDBOXES_DAY3_START_TIME
                ).value),
            endTime = ZonedDateTime.parse(
                dataSource.getStringLiveData(
                    RemoteAppConfigDataSource.SANDBOXES_DAY3_END_TIME).value)
        )
    )
}

private const val AFTER_DARK_TITLE = "After Dark"
private const val BADGE_PICKUP_TITLE = "Badge pick-up"
private const val BREAKFAST_TITLE = "Breakfast"
private const val CODELABS_TITLE = "Codelabs"
private const val CONCERT_TITLE = "Concert"
private const val DEVELOPER_KEYNOTE_TITLE = "Developer Keynote"
private const val GOOGLE_KEYNOTE_TITLE = "Google Keynote"
private const val IO_STORE_TITLE = "I/O Store"
private const val LUNCH_TITLE = "Lunch"
private const val OFFICE_HOURS_APP_REVIEWS_TITLE = "Office Hours & App Reviews"
private const val SANDBOXES_TITLE = "Sandboxes"
private const val SESSIONS_TITLE = "Sessions"

private const val AFTER_HOURS_TYPE = "after_hours"
private const val BADGE_PICKUP_TYPE = "badge"
private const val CONCERT_TYPE = "concert"
private const val MEAL_TYPE = "meal"
private const val CODELABS_TYPE = "codelab"
private const val KEYNOTE_TYPE = "keynote"
private const val STORE_TYPE = "store"
private const val OFFICE_HOURS_TYPE = "office_hours"
private const val SANDBOXES_TYPE = "sandbox"
private const val SESSIONS_TYPE = "session"
