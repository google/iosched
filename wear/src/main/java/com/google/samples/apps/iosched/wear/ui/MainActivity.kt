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

package com.google.samples.apps.iosched.wear.ui

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.wear.ambient.AmbientModeSupport
import android.support.wear.widget.drawer.WearableNavigationDrawerView
import android.view.ViewTreeObserver
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.wear.R
import com.google.samples.apps.iosched.wear.R.layout
import com.google.samples.apps.iosched.wear.ui.schedule.ScheduleFragment
import com.google.samples.apps.iosched.wear.ui.settings.SettingsFragment
import com.google.samples.apps.iosched.wear.ui.signinandout.SignInOrOutFragment
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

private const val NAV_DRAWER_STATE_SIZE = 3

private const val NAV_DRAWER_STATE_FRAGMENT_SCHEDULE = 0
private const val NAV_DRAWER_STATE_FRAGMENT_SIGN_IN_OR_OUT = 1
private const val NAV_DRAWER_STATE_FRAGMENT_SETTINGS = 2

/**
 * Contains Navigation elements and a single content Fragment. Three Fragments are available
 * (schedule, sign in/out, and settings) and are swapped out based on the user's choice via the
 * WearableNavigationDrawerView.
 */
class MainActivity : DaggerAppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var navigationDrawerAdapter: NavigationDrawerAdapter

    private var contentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)

        AmbientModeSupport.attach(this)

        // Sets up Top Navigation Drawer
        navigationDrawerAdapter = NavigationDrawerAdapter()

        navigationDrawer.setAdapter(navigationDrawerAdapter)

        // Peeks navigation drawer on the top.
        navigationDrawer.controller.peekDrawer()
        navigationDrawer.addOnItemSelectedListener(navigationDrawerAdapter)

        // Ensures the drawer doesn't peek on child view scrolling.
        navigationDrawer.setIsAutoPeekEnabled(false)

        // Temporarily peek the nav drawer to help ensure the user is aware of it
        val observer = navigationDrawer.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                navigationDrawer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                navigationDrawer.controller.peekDrawer()
            }
        })

        // Ensure the nav drawer shows the agenda data screen as selected.
        navigationDrawer.setCurrentItem(NAV_DRAWER_STATE_FRAGMENT_SCHEDULE, false)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // TODO (b/74259577): Handle all settings options
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return AmbientActivityCallback()
    }

    private inner class AmbientActivityCallback : AmbientModeSupport.AmbientCallback() {
        /** Prepares the UI for ambient mode.  */
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            super.onEnterAmbient(ambientDetails)

            navigationDrawer.controller.closeDrawer()

            // If the current fragment does not support ambient mode, exit the app.
            (contentFragment as? WearableFragment)?.onEnterAmbient(ambientDetails) ?: finish()
        }

        /**
         * Updates the display in ambient mode on the standard interval. Fragments use this method
         * to update position of the data in the screen to avoid burn-in, if the display
         * requires it.
         */
        override fun onUpdateAmbient() {
            super.onUpdateAmbient()

            (contentFragment as WearableFragment).onExitAmbient()
        }

        /** Restores the UI to active (non-ambient) mode.  */
        override fun onExitAmbient() {
            super.onExitAmbient()

            (contentFragment as WearableFragment).onUpdateAmbient()
        }
    }

    private inner class NavigationDrawerAdapter :
            WearableNavigationDrawerView.WearableNavigationDrawerAdapter(),
            WearableNavigationDrawerView.OnItemSelectedListener {

        private var currentNavigationItemPosition = 0
        private val activity = this@MainActivity

        override fun getCount(): Int {
            return NAV_DRAWER_STATE_SIZE
        }

        override fun getItemText(pos: Int): String? =
            when (pos) {
                NAV_DRAWER_STATE_FRAGMENT_SCHEDULE -> getString(R.string.schedule)
                NAV_DRAWER_STATE_FRAGMENT_SIGN_IN_OR_OUT -> getString(R.string.sign_in_or_out)
                NAV_DRAWER_STATE_FRAGMENT_SETTINGS -> getString(R.string.settings)
                else -> null
            }

        override fun getItemDrawable(pos: Int): Drawable? =
            when (pos) {
                NAV_DRAWER_STATE_FRAGMENT_SCHEDULE ->
                    ContextCompat.getDrawable(
                            activity, R.drawable.ic_nav_schedule_placeholder)

                NAV_DRAWER_STATE_FRAGMENT_SIGN_IN_OR_OUT ->
                    ContextCompat.getDrawable(
                            activity, R.drawable.ic_nav_sign_in_or_out_placeholder)

                NAV_DRAWER_STATE_FRAGMENT_SETTINGS ->
                    ContextCompat.getDrawable(
                            activity, R.drawable.ic_nav_settings_placeholder)

                else -> null
            }

        // Updates content when user changes between items in the navigation drawer.
        override fun onItemSelected(selectedItemPos: Int) {
            // Don't re-create the fragment if the user re-selects the same one.
            if (currentNavigationItemPosition == selectedItemPos) {
                return
            }

            contentFragment = when (selectedItemPos) {
                NAV_DRAWER_STATE_FRAGMENT_SCHEDULE -> ScheduleFragment()
                NAV_DRAWER_STATE_FRAGMENT_SIGN_IN_OR_OUT -> SignInOrOutFragment()
                NAV_DRAWER_STATE_FRAGMENT_SETTINGS -> SettingsFragment()
                else -> throw
                    IllegalStateException("Invalid nav position selected $selectedItemPos")
            }

            supportFragmentManager.inTransaction {
                replace(R.id.main_content_view, contentFragment)
            }

            currentNavigationItemPosition = selectedItemPos
        }
    }
}
