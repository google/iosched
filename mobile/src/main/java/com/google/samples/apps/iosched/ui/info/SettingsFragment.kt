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

package com.google.samples.apps.iosched.ui.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.wrappers.InstantApps
import com.google.samples.apps.iosched.databinding.FragmentSettingsBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.dialogs.NotificationsPreferencesDialogDispatcher
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class SettingsFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject lateinit var notificationDialogDispatcher: NotificationsPreferencesDialogDispatcher

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val settingsViewModel: SettingsViewModel = viewModelProvider(viewModelFactory)
        val binding = FragmentSettingsBinding.inflate(inflater, container, false).apply {
            viewModel = settingsViewModel
            isInstantApp = InstantApps.isInstantApp(requireContext())
            lifecycleOwner = viewLifecycleOwner
        }
        settingsViewModel.showSignIn.observe(this, EventObserver {
            notificationDialogDispatcher.startDialog(requireActivity())
        })
        settingsViewModel.navigateToThemeSelector.observe(this, EventObserver {
            ThemeSettingDialogFragment.newInstance().show(requireFragmentManager(), null)
        })
        return binding.root
    }
}
