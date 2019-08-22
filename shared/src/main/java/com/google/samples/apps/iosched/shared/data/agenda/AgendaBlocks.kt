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

import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.data.config.AppConfigDataSource
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.BREAKFAST_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.KEYNOTE_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.KEYNOTE_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LABEL_BREAKFAST
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LABEL_KEYNOTE
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LABEL_LUNCH
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LABEL_PARTY
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LABEL_REGISTRATION
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LABEL_SESSIONS
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LABEL_TEA_BREAK
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.LUNCH_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.PARTY_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.PARTY_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.REGISTRATION_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.REGISTRATION_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.REGISTRATION_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.REGISTRATION_DAY2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY1_1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY1_1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY1_2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY1_2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY1_3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY1_3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY2_1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY2_1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY2_2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY2_2_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY2_3_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.SESSIONS_DAY2_3_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.TEA_BREAK_DAY1_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.TEA_BREAK_DAY1_START_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.TEA_BREAK_DAY2_END_TIME
import com.google.samples.apps.iosched.shared.data.config.RemoteAppConfigDataSource.Companion.TEA_BREAK_DAY2_START_TIME
import org.threeten.bp.ZonedDateTime

/**
 * Generates a list of [Block]s. Default values of each [Block] is supplied from the
 * default values stored as shared/src/main/res/xml/remote_config_defaults.xml.
 * Add a corresponding entry in RemoteConfig is any [Block]s need to be overridden.
 */
fun generateBlocks(dataSource: AppConfigDataSource): List<Block> {
    return listOf(
        Block(
            title = dataSource.getString(LABEL_BREAKFAST).value ?: LABEL_BREAKFAST_DEFAULT,
            type = TYPE_MEAL,
            color = COLOR_MEAL,
            startTime = ZonedDateTime.parse(
                dataSource.getString(BREAKFAST_DAY1_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(BREAKFAST_DAY1_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_REGISTRATION).value ?: LABEL_REGISTRATION_DEFAULT,
            type = TYPE_REGISTRATION,
            color = COLOR_REGISTRATION,
            startTime = ZonedDateTime.parse(
                dataSource.getString(REGISTRATION_DAY1_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(REGISTRATION_DAY1_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_KEYNOTE).value ?: LABEL_KEYNOTE_DEFAULT,
            type = TYPE_KEYNOTE,
            color = COLOR_KEYNOTE,
            startTime = ZonedDateTime.parse(
                dataSource.getString(KEYNOTE_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(KEYNOTE_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_SESSIONS).value ?: LABEL_SESSIONS_DEFAULT,
            type = TYPE_SESSIONS,
            color = COLOR_SESSIONS,
            startTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY1_1_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY1_1_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_LUNCH).value ?: LABEL_LUNCH_DEFAULT,
            type = TYPE_MEAL,
            color = COLOR_MEAL,
            startTime = ZonedDateTime.parse(
                dataSource.getString(LUNCH_DAY1_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(LUNCH_DAY1_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_SESSIONS).value ?: LABEL_SESSIONS_DEFAULT,
            type = TYPE_SESSIONS,
            color = COLOR_SESSIONS,
            startTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY1_2_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY1_2_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_TEA_BREAK).value ?: LABEL_TEA_BREAK_DEFAULT,
            type = TYPE_MEAL,
            color = COLOR_MEAL,
            startTime = ZonedDateTime.parse(
                dataSource.getString(TEA_BREAK_DAY1_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(TEA_BREAK_DAY1_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_SESSIONS).value ?: LABEL_SESSIONS_DEFAULT,
            type = TYPE_SESSIONS,
            color = COLOR_SESSIONS,
            startTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY1_3_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY1_3_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_PARTY).value ?: LABEL_PARTY_DEFAULT,
            type = TYPE_AFTER_HOURS,
            color = COLOR_AFTER_HOURS,
            isDark = true,
            startTime = ZonedDateTime.parse(
                dataSource.getString(PARTY_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(PARTY_END_TIME).value
            )
        ),

        Block(
            title = dataSource.getString(LABEL_BREAKFAST).value ?: LABEL_BREAKFAST_DEFAULT,
            type = TYPE_MEAL,
            color = COLOR_MEAL,
            startTime = ZonedDateTime.parse(
                dataSource.getString(BREAKFAST_DAY2_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(BREAKFAST_DAY2_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_REGISTRATION).value ?: LABEL_REGISTRATION_DEFAULT,
            type = TYPE_REGISTRATION,
            color = COLOR_REGISTRATION,
            startTime = ZonedDateTime.parse(
                dataSource.getString(REGISTRATION_DAY2_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(REGISTRATION_DAY2_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_SESSIONS).value
                    ?: LABEL_SESSIONS_DEFAULT,
            type = TYPE_SESSIONS,
            color = COLOR_SESSIONS,
            startTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY2_1_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY2_1_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_LUNCH).value ?: LABEL_LUNCH_DEFAULT,
            type = TYPE_MEAL,
            color = COLOR_MEAL,
            startTime = ZonedDateTime.parse(
                dataSource.getString(LUNCH_DAY2_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(LUNCH_DAY2_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_SESSIONS).value ?: LABEL_SESSIONS_DEFAULT,
            type = TYPE_SESSIONS,
            color = COLOR_SESSIONS,
            startTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY2_2_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY2_2_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_TEA_BREAK).value ?: LABEL_TEA_BREAK_DEFAULT,
            type = TYPE_MEAL,
            color = COLOR_MEAL,
            startTime = ZonedDateTime.parse(
                dataSource.getString(TEA_BREAK_DAY2_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(TEA_BREAK_DAY2_END_TIME).value
            )
        ),
        Block(
            title = dataSource.getString(LABEL_SESSIONS).value ?: LABEL_SESSIONS_DEFAULT,
            type = TYPE_SESSIONS,
            color = COLOR_SESSIONS,
            startTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY2_3_START_TIME).value
            ),
            endTime = ZonedDateTime.parse(
                dataSource.getString(SESSIONS_DAY2_3_END_TIME).value
            )
        )
    )
}

private const val LABEL_REGISTRATION_DEFAULT = "Registration"
private const val LABEL_KEYNOTE_DEFAULT = "Keynote"
private const val LABEL_SESSIONS_DEFAULT = "Sessions"
private const val LABEL_BREAKFAST_DEFAULT = "Breakfast"
private const val LABEL_LUNCH_DEFAULT = "Lunch"
private const val LABEL_TEA_BREAK_DEFAULT = "Tea Break"
private const val LABEL_PARTY_DEFAULT = "Party"

private const val TYPE_REGISTRATION = "badge"
private const val TYPE_KEYNOTE = "keynote"
private const val TYPE_SESSIONS = "session"
private const val TYPE_MEAL = "meal"
private const val TYPE_AFTER_HOURS = "after_hours"

private const val COLOR_REGISTRATION = 0xffe6e6e6.toInt()
private const val COLOR_KEYNOTE = 0xfffdc93e.toInt()
private const val COLOR_SESSIONS = 0xff73bbf5.toInt()
private const val COLOR_MEAL = 0xff9bdd7c.toInt()
private const val COLOR_AFTER_HOURS = 0xff202124.toInt()
