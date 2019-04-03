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

package com.google.samples.apps.iosched.domain.ar

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.play.core.splitinstall.SplitInstallHelper
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.samples.apps.iosched.BuildConfig
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.lazyFast
import java.util.concurrent.TimeUnit

/**
 * Activity that installs or launches (if the ar dynamic feature module is already downloaded)
 * the AR module.
 */
class InstallOrLaunchArFeatureActivity : AppCompatActivity() {

    private val handler = Handler()

    private val splitInstallManager: SplitInstallManager by lazyFast {
        SplitInstallManagerFactory.create(this)
    }

    private lateinit var progressbar: ProgressBar

    // Json for showing pinned sessions in the ArActivity
    private lateinit var pinnedSessionsJson: String
    private var canSignedInUserDemoAr: Boolean = false

    private val installStateListener: DynamicModuleLoadStateListener by lazyFast {

        object : DynamicModuleLoadStateListener() {
            override fun onRequiresConfirmation(intentSender: IntentSender) {
                startIntentSender(intentSender, null, 0, 0, 0)
            }

            override fun onPending() {
                progressbar.isVisible = true
                progressbar.isIndeterminate = true
            }

            override fun onDownloading(bytesDownloaded: Long, totalBytesToDownload: Long) {
                if (bytesDownloaded >= (totalBytesToDownload * MIN_PROGRESS_DETERMINATE_PERC)) {
                    progressbar.isIndeterminate = false
                    // Use KB to minimize any overflow issues
                    progressbar.progress = (bytesDownloaded / 1024).toInt()
                    progressbar.max = (totalBytesToDownload / 1024).toInt()
                }
            }

            override fun onInstalling() = Unit

            override fun onInstalled() {
                SplitInstallHelper.updateAppInfo(this@InstallOrLaunchArFeatureActivity)
                tryLaunchOrInstallModule(showFakeDownload = false)
            }

            override fun onFailure() {
                onFeatureModuleLaunchFailure()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_ar_teaser)
        progressbar = findViewById(R.id.progressbar_dynamic_module)
        pinnedSessionsJson =
            intent?.extras?.getString(ArConstants.PINNED_SESSIONS_JSON_KEY, "") ?: ""
        canSignedInUserDemoAr =
            intent?.extras?.getBoolean(ArConstants.CAN_SIGNED_IN_USER_DEMO_AR, false) ?: false
        tryLaunchOrInstallModule(SHOW_FAKE_MODULE_DOWNLOAD)
    }

    private fun tryLaunchOrInstallModule(showFakeDownload: Boolean) {
        if (DYNAMIC_FEATURE_MODULE_NAME in splitInstallManager.installedModules) {
            if (showFakeDownload) {
                // We're set to show our fake download progress. This handy for testing the
                // UI without having to upload to Play
                showFakeFeatureModuleDownload()
            } else {
                launchTargetActivity()
            }
        } else {
            val mgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (mgr.activeNetworkInfo?.isConnected == true) {
                // We have a active network...
                if (mgr.isActiveNetworkMetered) {
                    // ...but it is metered. Confirm with the user first
                    showMeteredNetworkConfirmDialog(::startModuleInstall)
                } else {
                    // ...otherwise just download the module
                    startModuleInstall()
                }
            } else {
                // We have no network connection. Show a failure and finish
                onFeatureModuleLaunchFailure()
            }
        }
    }

    private fun launchTargetActivity() {
        val intent = Intent().apply {
            setClassName(applicationContext.packageName, ACTIVITY_TO_LAUNCH)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ArConstants.PINNED_SESSIONS_JSON_KEY, pinnedSessionsJson)
            putExtra(ArConstants.CAN_SIGNED_IN_USER_DEMO_AR, canSignedInUserDemoAr)
        }
        if (classLoader.loadClass(ACTIVITY_TO_LAUNCH) != null) {
            createPackageContext(packageName, 0).startActivity(intent)
            finish()
        } else if (!isFinishing) {
            onFeatureModuleLaunchFailure()
        }
    }

    private fun showFakeFeatureModuleDownload() {
        val fakeDownloadSize = 350 * 1024L
        installStateListener.onPending()

        val runnable = object : Runnable {
            var progress = 0L

            override fun run() {
                if (isFinishing) {
                    return
                }
                if (progress <= fakeDownloadSize) {
                    installStateListener.onDownloading(progress, fakeDownloadSize)
                    handler.postDelayed(this, 200)
                } else {
                    installStateListener.onInstalling()
                    installStateListener.onInstalled()
                }
                // Increment progress for next time
                progress += (fakeDownloadSize / 20)
            }
        }
        // Trigger the runnable by letting us pend for a second
        handler.postDelayed(runnable, TimeUnit.SECONDS.toMillis(1))
    }

    private fun startModuleInstall() {
        progressbar.isVisible = true
        progressbar.isIndeterminate = true

        // The split isn't installed so lets start the split install request
        installStateListener.register(splitInstallManager)
        val request = SplitInstallRequest.newBuilder()
            .addModule(DYNAMIC_FEATURE_MODULE_NAME)
            .build()
        splitInstallManager.startInstall(request)
    }

    private fun showMeteredNetworkConfirmDialog(onAccept: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.download_ar_dialog_title))
            .setMessage(R.string.download_ar_dialog_message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                onAccept()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                finish()
            }.show()
    }

    private fun onFeatureModuleLaunchFailure() {
        Toast.makeText(
            applicationContext,
            getString(R.string.error_fetch_ar_module),
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    companion object {
        private val SHOW_FAKE_MODULE_DOWNLOAD = BuildConfig.DEBUG
        private const val DYNAMIC_FEATURE_MODULE_NAME = "ar"
        // The name of the Activity for AR. The name must be in sync with the Activity in ar module
        private const val ACTIVITY_TO_LAUNCH = "com.google.samples.apps.iosched.ar.ArActivity"
        // Wait until we've download 5% before show a determinate progress
        private const val MIN_PROGRESS_DETERMINATE_PERC = 0.05f
    }
}