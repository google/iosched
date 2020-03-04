/*
 * Copyright 2020 Google LLC
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

package com.google.samples.apps.iosched.ui.appupdate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestUpdateFlow
import javax.inject.Inject

interface AppUpdateViewModelDelegate {

    val appUpdateResult: LiveData<AppUpdateResult>
}

class AppUpdateViewModelImpl @Inject constructor(
    appUpdateManager: AppUpdateManager
) : AppUpdateViewModelDelegate {

    override val appUpdateResult: LiveData<AppUpdateResult> =
        appUpdateManager.requestUpdateFlow().asLiveData()
}

object FakeAppUpdateViewModelImpl : AppUpdateViewModelDelegate {
    override val appUpdateResult: LiveData<AppUpdateResult> =
        MutableLiveData(AppUpdateResult.NotAvailable)
}
