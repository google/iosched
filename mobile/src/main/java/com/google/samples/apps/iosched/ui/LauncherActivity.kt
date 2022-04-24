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

package com.google.samples.apps.iosched.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.samples.apps.iosched.ui.LaunchNavigatonAction.NavigateToMainActivityAction
import com.google.samples.apps.iosched.ui.LaunchNavigatonAction.NavigateToOnboardingAction
import com.google.samples.apps.iosched.ui.onboarding.OnboardingActivity
import com.google.samples.apps.iosched.util.collectLifecycleFlow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * A 'Trampoline' activity for sending users to an appropriate screen on launch.
 */
@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: LaunchViewModel by viewModels()

        collectLifecycleFlow(viewModel.launchDestination) { action ->
            when (action) {
                is NavigateToMainActivityAction -> startActivity(
                    Intent(this@LauncherActivity, MainActivity::class.java)
                )
                is NavigateToOnboardingAction -> startActivity(
                    Intent(this@LauncherActivity, OnboardingActivity::class.java)
                )
            }
            finish()
        }
    }
}
