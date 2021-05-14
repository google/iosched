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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.viewModels
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.samples.apps.iosched.databinding.FragmentSettingsBinding
import com.google.samples.apps.iosched.ui.MainActivityViewModel
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.signin.setupProfileMenuItem
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : MainNavigationFragment() {

    private val mainActivityViewModel: MainActivityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.toolbar.setupProfileMenuItem(mainActivityViewModel, this)
        binding.composeView.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePaddingRelative(bottom = padding.bottom + insets.systemWindowInsetBottom)
        }

        binding.composeView.setContent {
            MdcTheme {
                Surface(Modifier.fillMaxSize()) {
                    val scrollState = rememberScrollState()
                    Column(Modifier.verticalScroll(scrollState)) { // Make the screen scrollable
                        SettingsScreen()
                    }
                }
            }
        }

        return binding.root
    }
}
