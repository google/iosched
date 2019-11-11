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

package com.google.samples.apps.iosched.shared.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCase
import timber.log.Timber

/**
 * A Job that refreshes the conference data in the repository (if the app is active) and
 * in the cache (if the app is not active).
 */
class ConferenceDataWorker(
    ctx: Context,
    params: WorkerParameters,
    private val refreshEventDataUseCase: RefreshConferenceDataUseCase
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {

        Timber.i("ConferenceDataService triggering refresh conference data.")

        return try {
            refreshEventDataUseCase(Unit)
            Timber.d("ConferenceDataService finished successfully.")
            // Finishing indicating this job doesn't need to be rescheduled.
            Result.success()
        } catch (e: Exception) {
            Timber.e("ConferenceDataService failed. It will retry.")
            // Indicating worker should be retried
            Result.retry()
        }
    }
}
