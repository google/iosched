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

package com.google.samples.apps.iosched.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.core.view.updatePaddingRelative
import androidx.databinding.BindingAdapter
import androidx.fragment.app.viewModels
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentSettingsBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.ui.MainActivityViewModel
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.info.InfoFragment
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import com.google.samples.apps.iosched.ui.signin.setupProfileMenuItem
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : MainNavigationFragment() {

    companion object {
        private const val DIALOG_NEED_TO_SIGN_IN = "dialog_need_to_sign_in"
        private const val DIALOG_CONFIRM_SIGN_OUT = "dialog_confirm_sign_out"
        private const val DIALOG_SCHEDULE_HINTS = "dialog_schedule_hints"
    }

    private val viewModel: SettingsViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel.navigateToThemeSelector.observe(viewLifecycleOwner, EventObserver {
            ThemeSettingDialogFragment.newInstance()
                .show(requireFragmentManager(), null)
        })

        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.toolbar.setupProfileMenuItem(mainActivityViewModel, this)

        binding.settingsScroll.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePaddingRelative(bottom = padding.bottom + insets.systemWindowInsetBottom)
        }
        mainActivityViewModel.navigateToSignInDialogAction.observe(
            viewLifecycleOwner, EventObserver {
                openSignInDialog()
            })

        mainActivityViewModel.navigateToSignOutDialogAction.observe(
            viewLifecycleOwner, EventObserver {
                openSignOutDialog()
            })

        return binding.root
    }

    private fun openSignInDialog() {
        val dialog = SignInDialogFragment()
        dialog.show(
            requireActivity().supportFragmentManager, SettingsFragment.DIALOG_NEED_TO_SIGN_IN
        )
    }

    private fun openSignOutDialog() {
        val dialog = SignOutDialogFragment()
        dialog.show(
            requireActivity().supportFragmentManager, SettingsFragment.DIALOG_CONFIRM_SIGN_OUT
        )
    }
}

@BindingAdapter(value = ["dialogTitle", "fileLink"], requireAll = true)
fun createDialogForFile(button: View, dialogTitle: String, fileLink: String) {
    val context = button.context
    button.setOnClickListener {
        val webView = WebView(context).apply { loadUrl(fileLink) }
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        AlertDialog.Builder(context)
            .setTitle(dialogTitle)
            .setView(webView)
            .create()
            .show()
    }
}

@BindingAdapter("versionName")
fun setVersionName(view: TextView, versionName: String) {
    view.text = view.resources.getString(R.string.version_name, versionName)
}
