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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.IdpResponse
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.consume
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.info.InfoFragment
import com.google.samples.apps.iosched.ui.map.MapFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragment
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.util.signin.FirebaseAuthErrorCodeConverter
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.navigation
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {
    companion object {
        private const val FRAGMENT_ID = R.id.fragment_container
    }

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var currentFragment: MainNavigationFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // This VM instance is shared between activity and fragments, as it's scoped to MainActivity
        val scheduleViewModel: ScheduleViewModel = viewModelProvider(viewModelFactory)

        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_schedule -> consume { replaceFragment(ScheduleFragment()) }
                R.id.navigation_map -> consume {
                    // Scroll to current event next time the schedule is opened.
                    scheduleViewModel.userHasInteracted = false
                    replaceFragment(MapFragment())
                }
                R.id.navigation_info -> consume {
                    // Scroll to current event next time the schedule is opened.
                    scheduleViewModel.userHasInteracted = false
                    replaceFragment(InfoFragment())
                }
                else -> false
            }
        }
        // Add a listener to prevent reselects from being treated as selects.
        navigation.setOnNavigationItemReselectedListener {}

        if (savedInstanceState == null) {
            // Show Schedule on first creation
            navigation.selectedItemId = R.id.navigation_schedule
        } else {
            // Find the current fragment
            currentFragment =
                supportFragmentManager.findFragmentById(FRAGMENT_ID) as? MainNavigationFragment
                ?: throw IllegalStateException("Activity recreated, but no fragment found!")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("An activity returned RESULT_CANCELED")
            val response = IdpResponse.fromResultIntent(data)
            response?.error?.let {
                snackbarMessageManager.addMessage(
                    SnackbarMessage(
                        messageId = FirebaseAuthErrorCodeConverter.convert(it.errorCode),
                        requestChangeId = UUID.randomUUID().toString()
                    )
                )
            }
        }
    }

    private fun <F> replaceFragment(fragment: F) where F : Fragment, F : MainNavigationFragment {
        supportFragmentManager.inTransaction {
            currentFragment = fragment
            replace(FRAGMENT_ID, fragment)
        }
    }

    override fun onBackPressed() {
        if (!currentFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        currentFragment.onUserInteraction()
    }
}
