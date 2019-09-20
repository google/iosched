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

package com.google.samples.apps.iosched.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.google.android.instantapps.InstantApps
import javax.inject.Inject

/**
 * Shows the Play Store dialog to install the app.
 */
class InstallAppStoreLauncher @Inject constructor() {

    fun showDialog(activity: FragmentActivity) {
        /**
         * Intent to launch after the app has been installed.
         */
        val postInstallIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://adssched.firebaseapp.com/dev-summit")
        ).addCategory(Intent.CATEGORY_BROWSABLE)

        InstantApps.showInstallPrompt(
            activity,
            postInstallIntent,
            REQUEST_CODE,
            null
        )
    }

    companion object {
        private val REQUEST_CODE = 1
    }
}
