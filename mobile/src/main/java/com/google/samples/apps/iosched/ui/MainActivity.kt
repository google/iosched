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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.firebase.ui.auth.IdpResponse
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ActivityMainBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.setupWithNavController
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.dialogs.AttendeeDialogFragment
import com.google.samples.apps.iosched.ui.dialogs.AttendeeDialogFragment.Companion.DIALOG_USER_ATTENDEE_PREFERENCE
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
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

    private lateinit var binding: ActivityMainBinding

    private val currentFragment: MainNavigationFragment?
        get() {
            return navHostFragment?.childFragmentManager
                ?.primaryNavigationFragment as? MainNavigationFragment
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainActivityViewModel: MainActivityViewModel = viewModelProvider(viewModelFactory)
        mainActivityViewModel.theme.observe(this, Observer(::updateForTheme))

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Request that our window is laid out full screen, to draw edge-to-edge. See the following
        // blog post for more info:
        // https://medium.com/androiddevelopers/gesture-navigation-going-edge-to-edge-812f62e4e83e
        binding.root.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        // Refresh conference data on launch
        if (savedInstanceState == null) {
            setupBottomNavigationBar() // otherwise this happens in onRestoreInstanceState
            Timber.d("Refreshing conference data on launch")
            mainActivityViewModel.refreshConferenceData()
        }

        mainActivityViewModel.navigateToUserAttendeeDialogAction.observe(this, EventObserver {
            openAttendeeDialog()
        })
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
        val navGraphIds = listOf(
            R.navigation.nav_schedule,
            R.navigation.nav_info,
            R.navigation.nav_agenda
        )

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = binding.navigation.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )

        // Choose when to show/hide the Bottom Navigation View
        // TODO: trying out keeping it visible all the time to gather feedback
        controller.value?.addOnDestinationChangedListener { _, destination, _ ->
            binding.navigation.visibility = when (destination.id) {
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

    private fun openAttendeeDialog() {
        AttendeeDialogFragment().show(supportFragmentManager, DIALOG_USER_ATTENDEE_PREFERENCE)
    }
}
