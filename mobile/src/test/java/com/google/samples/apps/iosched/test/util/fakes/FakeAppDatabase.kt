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

package com.google.samples.apps.iosched.test.util.fakes

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.google.samples.apps.iosched.shared.data.db.AppDatabase
import com.google.samples.apps.iosched.shared.data.db.CodelabFtsDao
import com.google.samples.apps.iosched.shared.data.db.SessionFtsDao
import com.google.samples.apps.iosched.shared.data.db.SpeakerFtsDao
import org.mockito.Mockito

class FakeAppDatabase : AppDatabase() {
    override fun sessionFtsDao(): SessionFtsDao {
        return Mockito.mock(SessionFtsDao::class.java)
    }

    override fun speakerFtsDao(): SpeakerFtsDao {
        return Mockito.mock(SpeakerFtsDao::class.java)
    }

    override fun codelabFtsDao(): CodelabFtsDao {
        return Mockito.mock(CodelabFtsDao::class.java)
    }

    override fun createOpenHelper(config: DatabaseConfiguration?): SupportSQLiteOpenHelper {
        return Mockito.mock(SupportSQLiteOpenHelper::class.java)
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        return Mockito.mock(InvalidationTracker::class.java)
    }

    override fun clearAllTables() {}
}