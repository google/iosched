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

package com.google.samples.apps.iosched.ar

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.InstallStatus.INSTALLED
import com.google.ar.core.ArCoreApk.InstallStatus.INSTALL_REQUESTED
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.web.webview.ArWebView

class ArActivity : AppCompatActivity() {

    private var userRequestedInstall = true

    override fun onResume() {
        super.onResume()
        try {
            when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                INSTALLED -> {
                    val arWebView = ArWebView(this)
                    setContentView(arWebView)
                    // TODO: Load the actual AR feature
                    arWebView.loadUrl("https://www.google.com")
                }
                INSTALL_REQUESTED -> {
                    userRequestedInstall = false
                    return
                }
                else -> return
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Toast.makeText(this, getString(R.string.need_to_install_ar_core), Toast.LENGTH_LONG)
                .show()
        }
    }
}