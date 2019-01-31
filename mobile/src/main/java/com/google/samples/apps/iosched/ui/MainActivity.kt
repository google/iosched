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
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.info.InfoFragment
import com.google.samples.apps.iosched.ui.map.MapFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragment
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.util.signin.FirebaseAuthErrorCodeConverter
import com.google.samples.apps.iosched.util.updateForTheme
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity(), DrawerListener {

    companion object {
        /** Key for an int extra defining the initial navigation target. */
        const val EXTRA_NAVIGATION_ID = "extra.NAVIGATION_ID"

        private const val FRAGMENT_ID = R.id.fragment_container
        private const val NAV_ID_NONE = -1
    }

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var currentFragment: MainNavigationFragment

    private lateinit var scheduleViewModel: ScheduleViewModel

    private lateinit var drawer: DrawerLayout
    private lateinit var navigation: NavigationView

    private var currentNavId = NAV_ID_NONE
    private var pendingNavId = NAV_ID_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This VM instance is shared between activity and fragments, as it's scoped to MainActivity
        scheduleViewModel = viewModelProvider(viewModelFactory)
        // Update for Dark Mode straight away
        updateForTheme(scheduleViewModel.currentTheme)

        setContentView(R.layout.activity_main)
        drawer = findViewById(R.id.drawer)
        navigation = findViewById(R.id.navigation)

        drawer.addDrawerListener(this)
        navigation.setNavigationItemSelectedListener {
            closeDrawer()
            navigateWhenDrawerClosed(it.itemId)
            true
        }

        if (savedInstanceState == null) {
            // default to showing Schedule
            val initialNavId = intent.getIntExtra(EXTRA_NAVIGATION_ID, R.id.navigation_schedule)
            navigation.setCheckedItem(initialNavId) // doesn't trigger listener
            navigateTo(initialNavId)
        } else {
            // Find the current fragment
            currentFragment =
                supportFragmentManager.findFragmentById(FRAGMENT_ID) as? MainNavigationFragment
                ?: throw IllegalStateException("Activity recreated, but no fragment found!")
        }

        scheduleViewModel.theme.observe(this, Observer(::updateForTheme))
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
        } else if (!currentFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    private fun closeDrawer() {
        drawer.closeDrawer(GravityCompat.START)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        currentFragment.onUserInteraction()
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
        when (navId) {
            currentNavId -> return // user tapped the current item
            R.id.navigation_schedule -> replaceFragment(ScheduleFragment())
            R.id.navigation_map -> {
                // Scroll to current event next time the schedule is opened.
                scheduleViewModel.userHasInteracted = false
                replaceFragment(MapFragment())
            }
            R.id.navigation_info -> {
                // Scroll to current event next time the schedule is opened.
                scheduleViewModel.userHasInteracted = false
                replaceFragment(InfoFragment())
            }
            else -> return // not a valid nav ID
        }
        currentNavId = navId
    }

    private fun <F> replaceFragment(fragment: F) where F : Fragment, F : MainNavigationFragment {
        supportFragmentManager.inTransaction {
            currentFragment = fragment
            replace(FRAGMENT_ID, fragment)
        }
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
