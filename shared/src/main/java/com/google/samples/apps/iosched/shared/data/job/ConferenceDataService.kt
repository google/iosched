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

package com.google.samples.apps.iosched.shared.data.job

import android.app.job.JobParameters
import android.app.job.JobService
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCase
import com.google.samples.apps.iosched.shared.result.succeeded
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * A Job that refreshes the conference data in the repository (if the app is active) and
 * in the cache (if the app is not active).
 */
@AndroidEntryPoint
class ConferenceDataService : JobService() {

    @Inject
    lateinit var refreshEventDataUseCase: RefreshConferenceDataUseCase

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onStartJob(params: JobParameters?): Boolean {

        Timber.i("ConferenceDataService triggering refresh conference data.")

        // Execute off the main thread
        serviceScope.launch {
            val result = refreshEventDataUseCase(Unit)

            when {
                result.succeeded -> {
                    Timber.d("ConferenceDataService finished successfuly.")
                    // Finishing indicating this job doesn't need to be rescheduled.
                    jobFinished(params, false)
                }
                else -> {
                    Timber.e("ConferenceDataService failed. It will retry.")
                    // Indicating job shold be rescheduled
                    jobFinished(params, true)
                }
            }
        }
        // Returning true to indicate we're not done yet (execution still running in the background)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Return true to indicate this job should run again.
        return true
    }

    companion object {
        const val JOB_ID = 0xFE0FE0
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
