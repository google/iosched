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
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.firebase.ui.auth.IdpResponse
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.ar.ArActivity
import com.google.samples.apps.iosched.databinding.ActivityMainBinding
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.di.CodelabsEnabledFlag
import com.google.samples.apps.iosched.shared.di.ExploreArEnabledFlag
import com.google.samples.apps.iosched.shared.di.MapFeatureEnabledFlag
import com.google.samples.apps.iosched.shared.domain.ar.ArConstants
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.ui.messages.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import com.google.samples.apps.iosched.util.HeightTopWindowInsetsListener
import com.google.samples.apps.iosched.util.signin.FirebaseAuthErrorCodeConverter
import com.google.samples.apps.iosched.util.updateForTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationHost {

    companion object {
        /** Key for an int extra defining the initial navigation target. */
        const val EXTRA_NAVIGATION_ID = "extra.NAVIGATION_ID"

        private const val NAV_ID_NONE = -1

        private const val DIALOG_SIGN_IN = "dialog_sign_in"
        private const val DIALOG_SIGN_OUT = "dialog_sign_out"

        private val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.navigation_feed,
            R.id.navigation_schedule,
            R.id.navigation_map,
            R.id.navigation_info,
            R.id.navigation_agenda,
            R.id.navigation_codelabs,
            R.id.navigation_settings
        )
    }

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    @JvmField
    @MapFeatureEnabledFlag
    var mapFeatureEnabled: Boolean = false

    @Inject
    @JvmField
    @CodelabsEnabledFlag
    var codelabsFeatureEnabled: Boolean = false

    @Inject
    @JvmField
    @ExploreArEnabledFlag
    var exploreArFeatureEnabled: Boolean = false

    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private var currentNavId = NAV_ID_NONE

    // For sending pinned sessions as JSON to the AR module
    private var pinnedSessionsJson: String? = null
    private var canSignedInUserDemoAr: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Update for Dark Mode straight away
        updateForTheme(viewModel.currentTheme)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.statusBarScrim.setOnApplyWindowInsetsListener(HeightTopWindowInsetsListener)

        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentNavId = destination.id
            // TODO: hide nav if not a top-level destination?
        }

        // Either of two different navigation views might exist depending on the configuration.
        binding.bottomNavigation?.apply {
            configureNavMenu(menu)
            setupWithNavController(navController)
            setOnItemReselectedListener { } // prevent navigating to the same item
        }
        binding.navigationRail?.apply {
            configureNavMenu(menu)
            setupWithNavController(navController)
            setOnItemReselectedListener { } // prevent navigating to the same item
        }

        if (savedInstanceState == null) {
            currentNavId = navController.graph.startDestination
            val requestedNavId = intent.getIntExtra(EXTRA_NAVIGATION_ID, currentNavId)
            navigateTo(requestedNavId)
        }

        viewModel.theme.observe(this, Observer(::updateForTheme))

        viewModel.navigateToSignInDialogAction.observe(
            this,
            EventObserver {
                openSignInDialog()
            }
        )

        viewModel.navigateToSignOutDialogAction.observe(
            this,
            EventObserver {
                openSignOutDialog()
            }
        )
        viewModel.arCoreAvailability.observe(
            this,
            Observer {
                // Start observing ArCoreAvailability otherwise the value isn't updated
                Timber.d("ArCoreAvailability = $it")
            }
        )
        viewModel.pinnedSessionsJson.observe(
            this,
            Observer {
                // Need to observe the pinnedSessions otherwise it's considered as inactive
                pinnedSessionsJson = it
            }
        )
        viewModel.canSignedInUserDemoAr.observe(
            this,
            Observer {
                Timber.d("Signed in user can demo ar = $it")
                canSignedInUserDemoAr = it
            }
        )
    }

    private fun configureNavMenu(menu: Menu) {
        menu.findItem(R.id.navigation_map)?.isVisible = mapFeatureEnabled
        menu.findItem(R.id.navigation_codelabs)?.isVisible = codelabsFeatureEnabled
        menu.findItem(R.id.navigation_explore_ar)?.apply {
            // Handle launching new activities, otherwise assume the destination is handled
            // by the nav graph. We want to launch a new Activity for only the AR menu item.
            isVisible = exploreArFeatureEnabled
            setOnMenuItemClickListener {
                if (connectivityManager.activeNetworkInfo?.isConnected == true) {
                    if (viewModel.arCoreAvailability.value?.isSupported == true) {
                        analyticsHelper.logUiEvent(
                            "Navigate to Explore I/O ARCore supported",
                            AnalyticsActions.CLICK
                        )
                        openExploreAr()
                    } else {
                        analyticsHelper.logUiEvent(
                            "Navigate to Explore I/O ARCore NOT supported",
                            AnalyticsActions.CLICK
                        )
                        openArCoreNotSupported()
                    }
                } else {
                    openNoConnection()
                }
                true
            }
        }
    }

    override fun registerToolbarWithNavigation(toolbar: Toolbar) {
        val appBarConfiguration = AppBarConfiguration(TOP_LEVEL_DESTINATIONS)
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentNavId = navController.currentDestination?.id ?: NAV_ID_NONE
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

    override fun onUserInteraction() {
        super.onUserInteraction()
        getCurrentFragment()?.onUserInteraction()
    }

    private fun getCurrentFragment(): MainNavigationFragment? {
        return navHostFragment
            .childFragmentManager
            .primaryNavigationFragment as? MainNavigationFragment
    }

    private fun navigateTo(navId: Int) {
        if (navId == currentNavId) {
            return // user tapped the current item
        }
        navController.navigate(navId)
    }

    private fun openSignInDialog() {
        SignInDialogFragment().show(supportFragmentManager, DIALOG_SIGN_IN)
    }

    private fun openSignOutDialog() {
        SignOutDialogFragment().show(supportFragmentManager, DIALOG_SIGN_OUT)
    }

    private fun openExploreAr() {
        val intent = Intent(
            this,
            ArActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(ArConstants.CAN_SIGNED_IN_USER_DEMO_AR, canSignedInUserDemoAr)
            putExtra(ArConstants.PINNED_SESSIONS_JSON_KEY, pinnedSessionsJson)
        }
        startActivity(intent)
    }

    private fun openNoConnection() {
        navigateTo(R.id.navigation_no_network_ar)
    }

    private fun openArCoreNotSupported() {
        navigateTo(R.id.navigation_phone_does_not_support_arcore)
    }
}
