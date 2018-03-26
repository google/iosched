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

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.consume
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.ui.info.InfoFragment
import com.google.samples.apps.iosched.ui.map.MapFragment
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragment
import com.google.samples.apps.iosched.widget.HideBottomViewOnScrollBehavior
import com.google.samples.apps.iosched.widget.HideBottomViewOnScrollBehavior.BottomViewCallback
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : DaggerAppCompatActivity() {
    companion object {
        private const val FRAGMENT_ID = R.id.fragment_container
        private const val STATE_BOTTOM_NAV_TRANSLATION = "state.BOTTOM_NAV_TRANSLATION"
    }

    private lateinit var behavior: HideBottomViewOnScrollBehavior<*>
    private lateinit var currentFragment: MainNavigationFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_schedule -> consume { replaceFragment(ScheduleFragment()) }
                R.id.navigation_map -> consume { replaceFragment(MapFragment()) }
                R.id.navigation_info -> consume { replaceFragment(InfoFragment()) }
                else -> false
            }
        }

        behavior = HideBottomViewOnScrollBehavior.from(navigation)
        // Report translation whenever the bottom nav moves
        behavior.addBottomViewCallback(object : BottomViewCallback {
            override fun onSlide(view: View, slideOffset: Float) {
                currentFragment.onBottomNavSlide(view.translationY)
            }

            override fun onStateChanged(view: View, newState: Int) {}
        })

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

    private fun <F> replaceFragment(fragment: F) where F: Fragment, F: MainNavigationFragment {
        supportFragmentManager.inTransaction {
            currentFragment = fragment
            replace(FRAGMENT_ID, fragment)
        }
    }

    fun setBottomNavLockMode(lockMode: Int) {
        behavior.lockMode = lockMode
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat(STATE_BOTTOM_NAV_TRANSLATION, navigation.translationY)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val ty = savedInstanceState.getFloat(STATE_BOTTOM_NAV_TRANSLATION)
        currentFragment.onBottomNavSlide(ty)
    }
}
