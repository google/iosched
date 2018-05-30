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

package com.google.samples.apps.iosched.firestore.entity

import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class ReservationRequestResultTest {

    @Test
    fun getIfPresent_nutNull() {
        val presentName =
            ReservationRequestResult.ReservationRequestStatus.RESERVE_SUCCEEDED.toString()

        assertThat(
            ReservationRequestResult.ReservationRequestStatus.getIfPresent(presentName),
            `is`(notNullValue())
        )
    }

    @Test
    fun getIfPresent_notPresent_returnsNull() {
        val notPresentName = "not present"

        assertThat(
            ReservationRequestResult.ReservationRequestStatus.getIfPresent(notPresentName),
            `is`(nullValue())
        )
    }
}
