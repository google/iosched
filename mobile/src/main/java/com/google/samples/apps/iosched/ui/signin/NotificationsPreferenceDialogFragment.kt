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

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.wrappers.InstantApps
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.DialogNotificationsPreferenceBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.dialogs.InstallAppStoreLauncher
import com.google.samples.apps.iosched.widget.CustomDimDialogFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

/**
 * Dialog that asks for the user's notifications preference.
 */
class NotificationsPreferenceDialogFragment : CustomDimDialogFragment(),
    HasSupportFragmentInjector {

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var installAppStoreLauncher: InstallAppStoreLauncher

    private lateinit var viewModel: NotificationsPreferenceViewModel

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val isInstantApp = InstantApps.isInstantApp(requireContext())

        viewModel = viewModelProvider(viewModelFactory)
        val binding = DialogNotificationsPreferenceBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        // The dialog for the instant app is slightly different
        if (isInstantApp) {
            binding.dialogContent.text =
                    resources.getString(R.string.notifications_preference_dialog_content_instant)
            binding.notificationsPrefButtonYes.visibility = View.GONE
            binding.notificationsInstalledButton.visibility = View.VISIBLE
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.installAppEvent.observe(this, EventObserver {
            installAppStoreLauncher.showDialog(requireActivity())
        })
        viewModel.dismissDialogEvent.observe(this, EventObserver {
            dismiss()
        })
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
