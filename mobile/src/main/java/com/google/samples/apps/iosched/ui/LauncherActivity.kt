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
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.checkAllMatched
import com.google.samples.apps.iosched.ui.LaunchDestination.MAIN_ACTIVITY
import com.google.samples.apps.iosched.ui.LaunchDestination.ONBOARDING
import com.google.samples.apps.iosched.ui.onboarding.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * A 'Trampoline' activity for sending users to an appropriate screen on launch.
 */
@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: LaunchViewModel by viewModels()

        viewModel.launchDestination.observe(
            this,
            EventObserver { destination ->
                when (destination) {
                    MAIN_ACTIVITY -> startActivity(Intent(this, MainActivity::class.java))
                    ONBOARDING -> startActivity(Intent(this, OnboardingActivity::class.java))
                }.checkAllMatched
                finish()
            }
        )
    }
}
