/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.ui.codelabs

import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.codelabs.CodelabsRepository
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.domain.codelabs.GetCodelabsInfoCardShownUseCase
import com.google.samples.apps.iosched.shared.domain.codelabs.LoadCodelabsUseCase
import com.google.samples.apps.iosched.shared.domain.codelabs.SetCodelabsInfoCardShownUseCase
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.util.fakes.FakeAppDatabase
import com.google.samples.apps.iosched.test.util.fakes.FakeConferenceDataSource
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [CodelabsViewModel]
 */
class CodelabsViewModelTest {

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun testData_codelabInfoShown() = runTest {
        val prefs = FakePreferenceStorage(codelabsInfoShown = true)
        val viewModel = createCodelabsViewModel(
            getCodelabsInfoCardShownUseCase = createTestGetCodelabsInfoCardShownUseCase(prefs)
        )
        val codelabs = viewModel.screenContent.first()
        // codelabs does not contain the info card
        assertFalse(codelabs.contains(CodelabsInformationCard))
        // We have other codelabs apart from the header item
        assertTrue(codelabs.size > 1)
    }

    @Test
    fun testData_codelabInfoNotShown() = runTest {
        val prefs = FakePreferenceStorage(codelabsInfoShown = false)
        val viewModel = createCodelabsViewModel(
            getCodelabsInfoCardShownUseCase = createTestGetCodelabsInfoCardShownUseCase(prefs)
        )
        val codelabs = viewModel.screenContent.first()
        // codelabs contain the info card
        assertTrue(codelabs.contains(CodelabsInformationCard))
        // We have other codelabs apart from the header item and info card
        assertTrue(codelabs.size > 2)
    }

    @Test
    fun testData_dismissCodelabInfoCard() = runTest {
        val prefs = FakePreferenceStorage(codelabsInfoShown = false)
        val viewModel = createCodelabsViewModel(
            getCodelabsInfoCardShownUseCase = createTestGetCodelabsInfoCardShownUseCase(prefs),
            setCodelabsInfoCardShownUseCase = createTestSetCodelabsInfoCardShownUseCase(prefs)
        )
        val initialCodelabs = viewModel.screenContent.first()
        // codelabs contain the info card
        assertTrue(initialCodelabs.contains(CodelabsInformationCard))

        viewModel.dismissCodelabsInfoCard()

        val newCodelabs = viewModel.screenContent.first()
        assertFalse(newCodelabs.contains(CodelabsInformationCard))
    }

    private fun createCodelabsViewModel(
        loadCodelabsUseCase: LoadCodelabsUseCase = createTestLoadCodelabsUseCase(),
        getCodelabsInfoCardShownUseCase: GetCodelabsInfoCardShownUseCase =
            createTestGetCodelabsInfoCardShownUseCase(),
        setCodelabsInfoCardShownUseCase: SetCodelabsInfoCardShownUseCase =
            createTestSetCodelabsInfoCardShownUseCase()
    ): CodelabsViewModel {
        return CodelabsViewModel(
            loadCodelabsUseCase,
            getCodelabsInfoCardShownUseCase,
            setCodelabsInfoCardShownUseCase
        )
    }

    private fun createTestLoadCodelabsUseCase(): LoadCodelabsUseCase {
        val conferenceDataRepository = createTestConferenceDataRepository()
        val codelabsRepository: CodelabsRepository =
            createTestCodelabsRepository(conferenceDataRepository)
        return LoadCodelabsUseCase(
            codelabsRepository,
            coroutineRule.testDispatcher
        )
    }

    private fun createTestConferenceDataRepository(): ConferenceDataRepository {
        return ConferenceDataRepository(
            remoteDataSource = FakeConferenceDataSource,
            boostrapDataSource = FakeConferenceDataSource,
            appDatabase = FakeAppDatabase()
        )
    }

    private fun createTestCodelabsRepository(
        conferenceDataRepository: ConferenceDataRepository
    ): CodelabsRepository {
        return CodelabsRepository(conferenceDataRepository)
    }

    private fun createTestGetCodelabsInfoCardShownUseCase(
        preferenceStorage: PreferenceStorage = FakePreferenceStorage()
    ): GetCodelabsInfoCardShownUseCase {
        return GetCodelabsInfoCardShownUseCase(
            preferenceStorage,
            coroutineRule.testDispatcher
        )
    }

    private fun createTestSetCodelabsInfoCardShownUseCase(
        preferenceStorage: PreferenceStorage = FakePreferenceStorage()
    ): SetCodelabsInfoCardShownUseCase {
        return SetCodelabsInfoCardShownUseCase(
            preferenceStorage,
            coroutineRule.testDispatcher
        )
    }
}
