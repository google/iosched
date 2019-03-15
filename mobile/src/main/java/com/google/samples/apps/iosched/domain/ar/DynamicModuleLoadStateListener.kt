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

import android.app.PendingIntent
import android.content.IntentSender
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import timber.log.Timber

/**
 * An abstract class that wraps [SplitInstallStateUpdatedListener] to expose callbacks depending on
 * the status of the [SplitInstallSessionStatus].
 */
abstract class DynamicModuleLoadStateListener : SplitInstallStateUpdatedListener {

    private var isRegistered = false

    fun register(manager: SplitInstallManager) {
        manager.registerListener(this)
        isRegistered = true
    }

    fun unregister(manager: SplitInstallManager) {
        if (isRegistered) {
            manager.unregisterListener(this)
            isRegistered = false
        }
    }

    override fun onStateUpdate(state: SplitInstallSessionState) {
        Timber.d("onStateUpdate. Status: ${state.status()}")

        when (state.status()) {
            SplitInstallSessionStatus.PENDING -> onPending()
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> requiresConfirmation(state)
            SplitInstallSessionStatus.DOWNLOADING -> onDownloading(
                state.bytesDownloaded(), state.totalBytesToDownload()
            )
            SplitInstallSessionStatus.INSTALLING -> onInstalling()
            SplitInstallSessionStatus.INSTALLED -> onInstalled()
            SplitInstallSessionStatus.UNKNOWN or SplitInstallSessionStatus.FAILED -> onFailure()
            SplitInstallSessionStatus.CANCELED -> onCanceled()
        }
    }

    open fun onPending() = Unit

    private fun requiresConfirmation(state: SplitInstallSessionState) {
        val resolutionIntent: PendingIntent = state.resolutionIntent() ?: return
        onRequiresConfirmation(resolutionIntent.intentSender)
    }

    abstract fun onRequiresConfirmation(intentSender: IntentSender)

    abstract fun onDownloading(bytesDownloaded: Long, totalBytesToDownload: Long)

    abstract fun onInstalling()

    open fun onInstalled() = Unit

    open fun onFailure() = Unit

    open fun onCanceled() = Unit
}