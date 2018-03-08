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

package com.google.samples.apps.iosched.shared.data

import com.google.samples.apps.iosched.shared.model.ConferenceData
import com.google.samples.apps.iosched.shared.model.TestData
import com.google.samples.apps.iosched.shared.model.TestData.session0
import com.google.samples.apps.iosched.shared.model.TestData.session1
import com.google.samples.apps.iosched.shared.model.TestData.session2
import com.google.samples.apps.iosched.shared.model.TestData.session3
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsNot.not
import org.hamcrest.core.IsNull.notNullValue
import org.hamcrest.core.IsNull.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.IOException
import org.hamcrest.core.Is.`is` as Is

/**
 * Unit tests for [ConferenceDataRepository].
 */
class ConferenceDataRepositoryTest {

    lateinit private var repo: ConferenceDataRepository

    @Test
    fun remotePrevailsOverBootstrap() {
        // Given a repo with a working remote data source that returns session0
        // and a bootstrap that returns session 3
        repo = ConferenceDataRepository(
                remoteDataSource = TestConfDataSourceSession0(),
                boostrapDataSource = BootstrapDataSourceSession3())

        // When requesting data
        val data = repo.getConferenceData()

        // Only session 0 should be available because remote has priority
        assertThat(data.sessions.first(), Is(equalTo(session0)))
        // and meta info should be set
        assertThat(repo.latestUpdateSource, Is(equalTo(UpdateSource.NETWORK)))
        assertThat(repo.dataLastUpdated, Is(not(equalTo(0L))))
        assertThat(repo.latestException, Is(nullValue()))
        assertThat(repo.currentConferenceDataVersion, Is(equalTo(NETWORK_DATA_VERSION)))
    }

    @Test
    fun remoteNotAvailable_bootstrapUsed() {
        // Given a repo with unavailable remote data source
        // and a bootstrap that returns session 3
        repo = ConferenceDataRepository(
                remoteDataSource = NotAvailableDataSource(),
                boostrapDataSource = BootstrapDataSourceSession3())

        // When requesting data
        val data = repo.getConferenceData()

        // Only session 3 should be available because remote has priority
        assertThat(data.sessions.first(), Is(equalTo(session3)))
        // and meta info should be set
        assertThat(repo.latestUpdateSource, Is(equalTo(UpdateSource.BOOTSTRAP)))
        assertThat(repo.dataLastUpdated, Is(equalTo(0L)))
        assertThat(repo.latestException, Is(nullValue()))
        assertThat(repo.currentConferenceDataVersion, Is(equalTo(BOOTSTRAP_DATA_VERSION)))
    }

    @Test
    fun remoteNotAvailableCacheAvailable_cacheUsed() {
        // Given a repo with a cache (that returns session1) and unavailable remote data source
        // and a bootstrap that returns session 3
        repo = ConferenceDataRepository(
                remoteDataSource = TestConfDataSourceOnlyCachedSession1(),
                boostrapDataSource = BootstrapDataSourceSession3())

        // When requesting data
        val data = repo.getConferenceData()

        // Only session 1 should be available because remote has priority
        assertThat(data.sessions.first(), Is(equalTo(session1)))
        // and meta info should be set
        assertThat(repo.latestUpdateSource, Is(equalTo(UpdateSource.CACHE)))
        assertThat(repo.dataLastUpdated, Is(equalTo(0L)))
        assertThat(repo.latestException, Is(nullValue()))
        assertThat(repo.currentConferenceDataVersion, Is(equalTo(CACHE_DATA_VERSION)))
    }

    @Test
    fun offlineRequest_cacheAvailable() {
        // Given a repo with a working remote data source that returns session0
        // and a bootstrap that returns session 1
        repo = ConferenceDataRepository(
                remoteDataSource = TestConfDataSourceOnlyCachedSession1(),
                boostrapDataSource = NotAvailableDataSource())

        // When requesting OFFLINE data
        val data = repo.getOfflineConferenceData()

        // Only session 1 should be available because remote has priority
        assertThat(data.sessions.first(), Is(equalTo(session1)))
        // and meta info should be set
        assertThat(repo.latestUpdateSource, Is(equalTo(UpdateSource.NONE))) // Using "offline"!
        assertThat(repo.dataLastUpdated, Is(equalTo(0L)))
        assertThat(repo.latestException, Is(nullValue()))
        assertThat(repo.currentConferenceDataVersion, Is(equalTo(0))) // Using "offline"!
    }

    @Test
    fun offlineRequest_cacheNotAvailable() {
        // Given a repo with unavailable remote data source
        // and a bootstrap that returns session 1
        repo = ConferenceDataRepository(
                remoteDataSource = NotAvailableDataSource(),
                boostrapDataSource = TestConfDataSourceSession1())

        // When requesting OFFLINE data
        val data = repo.getOfflineConferenceData()

        // Only session 1 should be available because remote has priority
        assertThat(data.sessions.first(), Is(equalTo(session1)))
        // and meta info should be set
        assertThat(repo.latestUpdateSource, Is(equalTo(UpdateSource.NONE))) // Using "offline"!
        assertThat(repo.dataLastUpdated, Is(equalTo(0L)))
        assertThat(repo.latestException, Is(nullValue()))
        assertThat(repo.currentConferenceDataVersion, Is(equalTo(0))) // Using "offline"!
    }

    @Test
    fun networkExceptionCacheAvailable_cacheReturned() {
        // Given a repo with unavailable remote data source that throws an exception but
        // has a cache available that returns session 2
        // and a bootstrap that returns session 3
        repo = ConferenceDataRepository(
                remoteDataSource = ThrowingDataSourceCacheSession2(),
                boostrapDataSource = BootstrapDataSourceSession3())

        // When requesting data
        val data = repo.getConferenceData()

        // Only session 2 should be available because remote has a cache
        assertThat(data.sessions.first(), Is(equalTo(session2)))
        // and meta info should be set
        assertThat(repo.latestUpdateSource, Is(equalTo(UpdateSource.CACHE)))
        assertThat(repo.dataLastUpdated, Is(equalTo(0L)))
        assertThat(repo.latestException, Is(notNullValue()))
        assertThat(repo.currentConferenceDataVersion, Is(equalTo(CACHE_DATA_VERSION)))
    }

    @Test
    fun networkExceptionCacheUnavailable_cacheReturned() {
        // Given a repo with unavailable remote data (that throws an exception) and no cache
        // and a bootstrap that returns session 1
        repo = ConferenceDataRepository(
                remoteDataSource = ThrowingDataSourceNoCache(),
                boostrapDataSource = TestConfDataSourceSession1())

        // When requesting data
        val data = repo.getConferenceData()

        // Only session 1 should be available because remote has no good data
        assertThat(data.sessions.first(), Is(equalTo(session1)))
        // and meta info should be set
        assertThat(repo.latestUpdateSource, Is(equalTo(UpdateSource.BOOTSTRAP)))
        assertThat(repo.dataLastUpdated, Is(equalTo(0L)))
        assertThat(repo.latestException, Is(notNullValue()))
        assertThat(repo.currentConferenceDataVersion, Is(equalTo(CACHE_DATA_VERSION)))
    }
}

/**
 * Test data and classes:
 */

private const val NETWORK_DATA_VERSION = 42
private const val CACHE_DATA_VERSION = 23
private const val BOOTSTRAP_DATA_VERSION = 314

private class TestConfDataSourceSession0 : ConferenceDataSource {
    override fun getConferenceData(): ConferenceData? {
        return conferenceData
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        throw NotImplementedError() // Not used
    }

    private val conferenceData = ConferenceData(
            sessions = listOf(TestData.session0),
            tags = listOf(TestData.androidTag, TestData.webTag),
            blocks = emptyList(),
            speakers = listOf(TestData.speaker),
            rooms = emptyList(),
            version = NETWORK_DATA_VERSION
    )
}

private class TestConfDataSourceSession1 : ConferenceDataSource {
    override fun getConferenceData(): ConferenceData? {
        return ConferenceData(
                sessions = listOf(TestData.session1),
                tags = listOf(TestData.androidTag, TestData.webTag),
                blocks = emptyList(),
                speakers = listOf(TestData.speaker),
                rooms = emptyList(),
                version = NETWORK_DATA_VERSION
        )
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return ConferenceData(
                sessions = listOf(TestData.session1),
                tags = listOf(TestData.androidTag, TestData.webTag),
                blocks = emptyList(),
                speakers = listOf(TestData.speaker),
                rooms = emptyList(),
                version = CACHE_DATA_VERSION
        )
    }
}

private class BootstrapDataSourceSession3 : ConferenceDataSource {
    override fun getConferenceData(): ConferenceData? {
        throw NotImplementedError() // Not used
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return ConferenceData(
                sessions = listOf(TestData.session3),
                tags = listOf(TestData.androidTag, TestData.webTag),
                blocks = emptyList(),
                speakers = listOf(TestData.speaker),
                rooms = emptyList(),
                version = BOOTSTRAP_DATA_VERSION
        )
    }
}

private class TestConfDataSourceOnlyCachedSession1 : ConferenceDataSource {
    override fun getConferenceData(): ConferenceData? {
        return null
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return ConferenceData(
                sessions = listOf(TestData.session1),
                tags = listOf(TestData.androidTag, TestData.webTag),
                blocks = emptyList(),
                speakers = listOf(TestData.speaker),
                rooms = emptyList(),
                version = CACHE_DATA_VERSION
        )
    }
}

class NotAvailableDataSource : ConferenceDataSource {
    override fun getConferenceData(): ConferenceData? {
        return null
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return null
    }
}

private class ThrowingDataSourceCacheSession2 : ConferenceDataSource {
    override fun getConferenceData(): ConferenceData? {
        throw IOException("Test")
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return ConferenceData(
                sessions = listOf(TestData.session2),
                tags = listOf(TestData.androidTag, TestData.webTag),
                blocks = emptyList(),
                speakers = listOf(TestData.speaker),
                rooms = emptyList(),
                version = CACHE_DATA_VERSION
        )
    }
}

private class ThrowingDataSourceNoCache : ConferenceDataSource {
    override fun getConferenceData(): ConferenceData? {
        throw IOException("Test")
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return null
    }
}
