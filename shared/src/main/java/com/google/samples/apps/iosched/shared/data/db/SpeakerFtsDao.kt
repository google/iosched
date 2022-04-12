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

package com.google.samples.apps.iosched.shared.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The Data Access Object for the [SpeakerFtsEntity] class.
 */
@Dao
interface SpeakerFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(speakers: List<SpeakerFtsEntity>)

    @Query("SELECT speakerId FROM speakersFts WHERE speakersFts MATCH :query")
    fun searchAllSpeakers(query: String): List<String>
}
