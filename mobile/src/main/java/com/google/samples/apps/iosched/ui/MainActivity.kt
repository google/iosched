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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.NavigationHeaderBinding
import com.google.samples.apps.iosched.shared.di.ExploreArEnabledFlag
import com.google.samples.apps.iosched.shared.di.FeedFeatureEnabledFlag
import com.google.samples.apps.iosched.shared.di.MapFeatureEnabledFlag
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import com.google.samples.apps.iosched.util.HeightTopWindowInsetsListener
import com.google.samples.apps.iosched.util.NoopWindowInsetsListener
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.signin.FirebaseAuthErrorCodeConverter
import com.google.samples.apps.iosched.util.updateForTheme
import com.google.samples.apps.iosched.widget.NavigationBarContentFrameLayout
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity(), NavigationHost, DrawerListener {

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
            R.id.navigation_settings
        )
    }

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    @JvmField
    @FeedFeatureEnabledFlag
    var feedFeatureEnabled: Boolean = false

    @Inject
    @JvmField
    @MapFeatureEnabledFlag
    var mapFeatureEnabled: Boolean = false

    @Inject
    @JvmField
    @ExploreArEnabledFlag
    var exploreArFeatureEnabled: Boolean = false

    private lateinit var viewModel: MainActivityViewModel

    private lateinit var content: FrameLayout
    private lateinit var drawer: DrawerLayout
    private lateinit var navigation: NavigationView
    private lateinit var navHeaderBinding: NavigationHeaderBinding
    private lateinit var navController: NavController
    private var navHostFragment: NavHostFragment? = null

    private lateinit var statusScrim: View

    private var currentNavId = NAV_ID_NONE
    private var pendingNavId = NAV_ID_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = viewModelProvider(viewModelFactory)
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
        drawer.addDrawerListener(this)

        navHeaderBinding = NavigationHeaderBinding.inflate(layoutInflater).apply {
            viewModel = this@MainActivity.viewModel
            lifecycleOwner = this@MainActivity
        }

        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?

        navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentNavId = destination.id
        }

        navigation = findViewById(R.id.navigation)
        navigation.apply {
            addHeaderView(navHeaderBinding.root)

            menu.findItem(R.id.navigation_map).isVisible = mapFeatureEnabled
            menu.findItem(R.id.navigation_feed).isVisible = feedFeatureEnabled
            menu.findItem(R.id.navigation_explore_ar).isVisible = exploreArFeatureEnabled
            setNavigationItemSelectedListener {
                closeDrawer()
                navigateWhenDrawerClosed(it.itemId)
                true
            }
            setupWithNavController(navController)
        }

        // Update the Navigation header view to pad itself down
        navHeaderBinding.root.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePadding(top = padding.top + insets.systemWindowInsetTop)
        }

        if (savedInstanceState == null) {
            // default to showing Schedule
            val initialNavId = intent.getIntExtra(EXTRA_NAVIGATION_ID, R.id.navigation_schedule)
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
            // Hide the Explore AR menu if the device is not ARCore-certified
            Timber.d("ArCoreAvailability = $it")
            if (it.isUnsupported) {
                navigation.menu.findItem(R.id.navigation_explore_ar).isVisible = false
            }
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
        if (drawer.isDrawerOpen(navigation)) {
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
            ?.primaryNavigationFragment as MainNavigationFragment?
    }

    private fun navigateWhenDrawerClosed(navId: Int) {
        if (drawer.isDrawerVisible(navigation)) {
            // Replacing the fragment while the drawer is animating causes jank, so instead save the
            // id and we'll replace it later.
            pendingNavId = navId
        } else {
            navigateTo(navId)
        }
    }

    private fun navigateTo(navId: Int) {
        if (navId == currentNavId) {
            return // user tapped the current item
        }
        // Handle launching new activities, otherwise assume the destination is handled by the nav
        // graph.
        if (navId == R.id.navigation_explore_ar) {
            // TODO: Launch the ArActivity. Need to resolve the AR module is installed at this
            // moment
            Toast.makeText(
                this, "Launching AR Activity",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        navController.navigate(navId)
    }

    private fun openSignInDialog() {
        SignInDialogFragment().show(supportFragmentManager, DIALOG_SIGN_IN)
    }

    private fun openSignOutDialog() {
        SignOutDialogFragment().show(supportFragmentManager, DIALOG_SIGN_OUT)
    }

    // -- DrawerListener overrides

    override fun onDrawerClosed(drawerView: View) {
        if (drawerView == navigation && pendingNavId != NAV_ID_NONE) {
            navigateTo(pendingNavId)
            pendingNavId = NAV_ID_NONE
        }
    }

    override fun onDrawerOpened(drawerView: View) {}

    override fun onDrawerStateChanged(newState: Int) {}

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
}
