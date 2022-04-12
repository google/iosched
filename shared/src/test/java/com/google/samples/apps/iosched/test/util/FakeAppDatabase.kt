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

package com.google.samples.apps.iosched.test.util

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.google.samples.apps.iosched.shared.data.db.AppDatabase
import com.google.samples.apps.iosched.shared.data.db.CodelabFtsDao
import com.google.samples.apps.iosched.shared.data.db.CodelabFtsEntity
import com.google.samples.apps.iosched.shared.data.db.SessionFtsDao
import com.google.samples.apps.iosched.shared.data.db.SessionFtsEntity
import com.google.samples.apps.iosched.shared.data.db.SpeakerFtsDao
import com.google.samples.apps.iosched.shared.data.db.SpeakerFtsEntity
import com.google.samples.apps.iosched.test.data.TestData
import com.nhaarman.mockito_kotlin.mock
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

/**
 * A fake [AppDatabase] used in `SearchUseCaseTest`.
 */
class FakeSearchAppDatabase : AppDatabase() {
    override fun sessionFtsDao(): SessionFtsDao {
        return object : SessionFtsDao {
            override fun insertAll(sessions: List<SessionFtsEntity>) { TODO("not implemented") }

            override fun searchAllSessions(query: String): List<String> {
                return when (query) {
                    QUERY_TITLE.toLowerCase(), QUERY_TITLE_WITH_TOKEN -> listOf(
                        TestData.session0.id
                    )
                    QUERY_ABSTRACT.toLowerCase(), QUERY_ABSTRACT_WITH_TOKEN -> listOf(
                        TestData.session0.id
                    )
                    QUERY_WITH_NO_MATCH.toLowerCase() -> emptyList()
                    else -> emptyList()
                }
            }
        }
    }

    override fun speakerFtsDao(): SpeakerFtsDao {
        return object : SpeakerFtsDao {
            override fun insertAll(speakers: List<SpeakerFtsEntity>) { TODO("not implemented") }
            override fun searchAllSpeakers(query: String): List<String> {
                return emptyList()
            }
        }
    }

    override fun codelabFtsDao(): CodelabFtsDao {
        return object : CodelabFtsDao {
            override fun insertAll(codelabs: List<CodelabFtsEntity>) { TODO("not implemented") }
            override fun searchAllCodelabs(query: String): List<String> {
                return emptyList()
            }
        }
    }

    override fun createOpenHelper(config: DatabaseConfiguration?): SupportSQLiteOpenHelper {
        TODO("not implemented")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        // No-op
        return mock {}
    }

    override fun clearAllTables() {
        TODO("not implemented")
    }

    companion object {
        const val QUERY_TITLE = "session 0"
        const val QUERY_ABSTRACT = "Awesome"
        const val QUERY_WITH_NO_MATCH = "Invalid search query"
        const val QUERY_ONLY_SPACES = "  "
        const val QUERY_TITLE_WITH_TOKEN = "session* AND 0*"
        const val QUERY_ABSTRACT_WITH_TOKEN = "awesome*"
    }
}
