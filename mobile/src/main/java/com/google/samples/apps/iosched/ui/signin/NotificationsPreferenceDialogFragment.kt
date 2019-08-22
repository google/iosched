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

package com.google.samples.apps.iosched.ui.signin

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.wrappers.InstantApps
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.dialogs.InstallAppStoreLauncher
import dagger.android.support.DaggerAppCompatDialogFragment
import javax.inject.Inject

/**
 * Dialog that asks for the user's notifications preference.
 */
class NotificationsPreferenceDialogFragment : DaggerAppCompatDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var installAppStoreLauncher: InstallAppStoreLauncher

    private lateinit var viewModel: NotificationsPreferenceViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = viewModelProvider(viewModelFactory)
        viewModel.installAppEvent.observe(this, EventObserver {
            installAppStoreLauncher.showDialog(requireActivity())
        })
        viewModel.dismissDialogEvent.observe(this, EventObserver {
            dismiss()
        })

        val isInstantApp = InstantApps.isInstantApp(requireContext())
        return if (isInstantApp) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.notifications_preference_dialog_title)
                .setMessage(R.string.notifications_preference_dialog_content_instant)
                .setNegativeButton(R.string.no) { _, _ -> viewModel.onNoClicked() }
                .setNegativeButton(R.string.installApp) { _, _ -> viewModel.onInstallClicked() }
                .create()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.notifications_preference_dialog_title)
                .setMessage(R.string.notifications_preference_dialog_content)
                .setNegativeButton(R.string.no) { _, _ -> viewModel.onNoClicked() }
                .setPositiveButton(R.string.yes) { _, _ -> viewModel.onYesClicked() }
                .create()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val isInstantApp = InstantApps.isInstantApp(requireContext())
        if (!isInstantApp) {
            viewModel.onDismissed()
        }
    }

    companion object {
        const val DIALOG_NOTIFICATIONS_PREFERENCE = "dialog_notifications_preference"
    }
}
