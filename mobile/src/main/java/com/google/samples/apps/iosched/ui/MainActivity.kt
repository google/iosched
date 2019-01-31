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
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.setupWithNavController
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.util.signin.FirebaseAuthErrorCodeConverter
import com.google.samples.apps.iosched.util.updateForTheme
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private var navHostFragment: NavHostFragment? = null

    private var currentNavController: LiveData<NavController>? = null

    private val currentFragment: MainNavigationFragment?
        get() {
            return navHostFragment
                ?.childFragmentManager
                ?.primaryNavigationFragment as? MainNavigationFragment
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This VM instance is shared between activity and fragments, as it's scoped to MainActivity
        val scheduleViewModel: ScheduleViewModel = viewModelProvider(viewModelFactory)
        scheduleViewModel.theme.observe(this, Observer {
            updateForTheme(it)
        })

        setContentView(R.layout.activity_main)

        // Refresh conference data on launch
        if (savedInstanceState == null) {
            setupBottomNavigationBar() // otherwise this happens in onRestoreInstanceState
            Timber.d("Refreshing conference data on launch")
            scheduleViewModel.onSwipeRefresh()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Handle errors coming from Firebase Auth - e.g. user cancels flow
        if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("Main Activity: An activity returned RESULT_CANCELED")
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

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.navigation)

        val navGraphIds = listOf(
            R.navigation.nav_schedule,
            R.navigation.nav_info,
            R.navigation.nav_agenda
        )

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )

        // Choose when to show/hide the Bottom Navigation View
        // TODO: trying out keeping it visible all the time to gather feedback
        controller.value?.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavigationView.visibility = when (destination.id) {
                R.id.scheduleFragment -> View.VISIBLE
                else -> View.VISIBLE
            }
        }
        currentNavController = controller
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        currentFragment?.onUserInteraction()
    }
}
