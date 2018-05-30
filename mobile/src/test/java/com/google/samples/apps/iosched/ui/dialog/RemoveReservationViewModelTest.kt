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

@file:Suppress("FunctionName")

package com.google.samples.apps.iosched.ui.dialog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CancelAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationViewModel
import com.google.samples.apps.iosched.ui.schedule.day.TestUserEventDataSource
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [RemoveReservationViewModel].
 */
class RemoveReservationViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testRemoveReservation() {
        val reservationActionUseCaseMock = mock<ReservationActionUseCase> {}
        val viewModel = createRemoveReservationViewModel(reservationActionUseCaseMock)
        val testUid = "testUid"
        val parameters = ReservationRequestParameters(
            testUid, TestData.session1.id, CancelAction()
        )
        viewModel.userId = testUid
        viewModel.sessionId = TestData.session1.id

        viewModel.onRemoveClicked()

        verify(reservationActionUseCaseMock).execute(parameters)
        val dismissDialogEvent: Event<Boolean>? =
            LiveDataTestUtil.getValue(viewModel.dismissDialogAction)
        assertThat(dismissDialogEvent?.getContentIfNotHandled(), `is`(true))
    }

    @Test
    fun testKeepSeat() {
        val viewModel = createRemoveReservationViewModel()

        viewModel.onCancelClicked()

        val dismissDialogEvent: Event<Boolean>? =
            LiveDataTestUtil.getValue(viewModel.dismissDialogAction)
        assertThat(dismissDialogEvent?.getContentIfNotHandled(), `is`(true))
    }

    private fun createRemoveReservationViewModel(
        reservationActionUseCase: ReservationActionUseCase = createReservationActionUseCase()
    ): RemoveReservationViewModel {
        return RemoveReservationViewModel(reservationActionUseCase)
    }

    private fun createReservationActionUseCase(): ReservationActionUseCase {
        return object : ReservationActionUseCase(
            DefaultSessionAndUserEventRepository(
                TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
            )
        ) {}
    }
}
