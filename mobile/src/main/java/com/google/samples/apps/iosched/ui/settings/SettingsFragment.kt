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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.ViewModelProvider
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentSettingsBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import javax.inject.Inject

class SettingsFragment : MainNavigationFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = viewModelProvider(viewModelFactory)

        viewModel.navigateToThemeSelector.observe(this, EventObserver {
            ThemeSettingDialogFragment.newInstance()
                    .show(requireFragmentManager(), null)
        })

        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }
}

private const val CHROME_PACKAGE = "com.android.chrome"

@BindingAdapter("websiteLink")
fun websiteLink(
    button: Button,
    url: String?
) {
    url ?: return
    button.setOnClickListener {
        val context = it.context
        if (context.isChromeCustomTabsSupported()) {
            CustomTabsIntent.Builder().apply {
                setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
                setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
            }.build().launchUrl(context, Uri.parse(url))
        } else {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent, null)
        }
    }
}

@BindingAdapter(value = ["dialogTitle", "fileLink"], requireAll = true)
fun createDialogForFile(button: Button, dialogTitle: String, fileLink: String) {
    val context = button.context
    button.setOnClickListener {
        val webView = WebView(context).apply { loadUrl(fileLink) }
        AlertDialog.Builder(context)
            .setTitle(dialogTitle)
            .setView(webView)
            .create()
            .show()
    }
}

private fun Context.isChromeCustomTabsSupported(): Boolean {
    val serviceIntent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
    serviceIntent.setPackage(CHROME_PACKAGE)
    val resolveInfos = packageManager.queryIntentServices(serviceIntent, 0)
    return !(resolveInfos == null || resolveInfos.isEmpty())
}
