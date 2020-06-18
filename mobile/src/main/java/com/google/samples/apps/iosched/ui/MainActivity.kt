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
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.ar.ArActivity
import com.google.samples.apps.iosched.databinding.NavigationHeaderBinding
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.di.CodelabsEnabledFlag
import com.google.samples.apps.iosched.shared.di.ExploreArEnabledFlag
import com.google.samples.apps.iosched.shared.di.MapFeatureEnabledFlag
import com.google.samples.apps.iosched.shared.domain.ar.ArConstants
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import com.google.samples.apps.iosched.util.HeightTopWindowInsetsListener
import com.google.samples.apps.iosched.util.NoopWindowInsetsListener
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.navigationItemBackground
import com.google.samples.apps.iosched.util.shouldCloseDrawerFromBackPress
import com.google.samples.apps.iosched.util.signin.FirebaseAuthErrorCodeConverter
import com.google.samples.apps.iosched.util.updateForTheme
import com.google.samples.apps.iosched.widget.HashtagIoDecoration
import com.google.samples.apps.iosched.widget.NavigationBarContentFrameLayout
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

    private lateinit var content: FrameLayout
    private lateinit var drawer: DrawerLayout
    private lateinit var navigation: NavigationView
    private lateinit var navHeaderBinding: NavigationHeaderBinding
    private lateinit var navController: NavController
    private var navHostFragment: NavHostFragment? = null

    private lateinit var statusScrim: View

    private var currentNavId = NAV_ID_NONE

    // For sending pinned sessions as JSON to the AR module
    private var pinnedSessionsJson: String? = null
    private var canSignedInUserDemoAr: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Update for Dark Mode straight away
        updateForTheme(viewModel.currentTheme)

        setContentView(R.layout.activity_main)

        val drawerContainer: NavigationBarContentFrameLayout = findViewById(R.id.drawer_container)
        // Let's consume any
        drawerContainer.setOnApplyWindowInsetsListener { v, insets ->
            // Let the view draw it's navigation bar divider
            v.onApplyWindowInsets(insets)

            // Consume any horizontal insets and pad all content in. There's not much we can do
            // with horizontal insets
            v.updatePadding(
                left = insets.systemWindowInsetLeft,
                right = insets.systemWindowInsetRight
            )
            insets.replaceSystemWindowInsets(
                0, insets.systemWindowInsetTop,
                0, insets.systemWindowInsetBottom
            )
        }

        content = findViewById(R.id.content_container)
        content.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        // Make the content ViewGroup ignore insets so that it does not use the default padding
        content.setOnApplyWindowInsetsListener(NoopWindowInsetsListener)

        statusScrim = findViewById(R.id.status_bar_scrim)
        statusScrim.setOnApplyWindowInsetsListener(HeightTopWindowInsetsListener)

        drawer = findViewById(R.id.drawer)

        navHeaderBinding = NavigationHeaderBinding.inflate(layoutInflater).apply {
            lifecycleOwner = this@MainActivity
        }

        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?

        navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentNavId = destination.id
            val isTopLevelDestination = TOP_LEVEL_DESTINATIONS.contains(destination.id)
            val lockMode = if (isTopLevelDestination) {
                DrawerLayout.LOCK_MODE_UNLOCKED
            } else {
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            }
            drawer.setDrawerLockMode(lockMode)
        }

        navigation = findViewById(R.id.navigation)
        navigation.apply {
            // Add the #io19 decoration
            val menuView = findViewById<RecyclerView>(R.id.design_navigation_view)?.apply {
                addItemDecoration(HashtagIoDecoration(context))
            }
            // Update the Navigation header view to pad itself down
            navHeaderBinding.root.doOnApplyWindowInsets { v, insets, padding ->
                v.updatePadding(top = padding.top + insets.systemWindowInsetTop)
                // NavigationView doesn't dispatch insets to the menu view, so pad the bottom here.
                menuView?.updatePadding(bottom = insets.systemWindowInsetBottom)
            }
            addHeaderView(navHeaderBinding.root)

            itemBackground = navigationItemBackground(context)

            menu.findItem(R.id.navigation_map).isVisible = mapFeatureEnabled
            menu.findItem(R.id.navigation_codelabs).isVisible = codelabsFeatureEnabled
            menu.findItem(R.id.navigation_explore_ar).apply {
                // Handle launching new activities, otherwise assume the destination is handled
                // by the nav graph. We want to launch a new Activity for only the AR menu
                isVisible = exploreArFeatureEnabled
                setOnMenuItemClickListener {
                    if (connectivityManager.activeNetworkInfo?.isConnected == true) {
                        if (viewModel.arCoreAvailability.value?.isSupported == true) {
                            analyticsHelper
                                .logUiEvent("Navigate to Explore I/O ARCore supported",
                                    AnalyticsActions.CLICK)
                            openExploreAr()
                        } else {
                            analyticsHelper
                                .logUiEvent("Navigate to Explore I/O ARCore NOT supported",
                                    AnalyticsActions.CLICK)
                            openArCoreNotSupported()
                        }
                    } else {
                        openNoConnection()
                    }
                    closeDrawer()
                    true
                }
            }
            setupWithNavController(navController)
        }

        if (savedInstanceState == null) {
            // default to showing Home
            val initialNavId = intent.getIntExtra(EXTRA_NAVIGATION_ID, R.id.navigation_feed)
            navigation.setCheckedItem(initialNavId) // doesn't trigger listener
            navigateTo(initialNavId)
        }

        viewModel.theme.observe(this, Observer(::updateForTheme))

        viewModel.navigateToSignInDialogAction.observe(this, EventObserver {
            openSignInDialog()
        })

        viewModel.navigateToSignOutDialogAction.observe(this, EventObserver {
            openSignOutDialog()
        })
        viewModel.arCoreAvailability.observe(this, Observer {
            // Start observing ArCoreAvailability otherwise the value isn't updated
            Timber.d("ArCoreAvailability = $it")
        })
        viewModel.pinnedSessionsJson.observe(this, Observer {
            // Need to observe the pinnedSessions otherwise it's considered as inactive
            pinnedSessionsJson = it
        })
        viewModel.canSignedInUserDemoAr.observe(this, Observer {
            Timber.d("Signed in user can demo ar = $it")
            canSignedInUserDemoAr = it
        })
    }

    override fun registerToolbarWithNavigation(toolbar: Toolbar) {
        val appBarConfiguration = AppBarConfiguration(TOP_LEVEL_DESTINATIONS, drawer)
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        currentNavId = navigation.checkedItem?.itemId ?: NAV_ID_NONE
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

    override fun onBackPressed() {
        /**
         * If the drawer is open, the behavior changes based on the API level.
         * When gesture nav is enabled (Q+), we want back to exit when the drawer is open.
         * When button navigation is enabled (on Q or pre-Q) we want to close the drawer on back.
         */
        if (drawer.isDrawerOpen(navigation) && drawer.shouldCloseDrawerFromBackPress()) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }

    private fun closeDrawer() {
        drawer.closeDrawer(GravityCompat.START)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        getCurrentFragment()?.onUserInteraction()
    }

    private fun getCurrentFragment(): MainNavigationFragment? {
        return navHostFragment
            ?.childFragmentManager
            ?.primaryNavigationFragment as? MainNavigationFragment
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
