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
import android.support.v4.widget.DrawerLayout
import android.view.Gravity
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.consume
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.ui.info.InfoFragment
import com.google.samples.apps.iosched.ui.map.MapFragment
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragment
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : DaggerAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            // Show Schedule on first creation
            val fragment = ScheduleFragment()
            supportFragmentManager.inTransaction {
                add(R.id.fragment_container, fragment)
            }
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }

        navigation.setSelectedItemId(R.id.navigation_schedule)

        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_schedule -> consume { replaceFragment(ScheduleFragment()) }
                R.id.navigation_map -> consume { replaceFragment(MapFragment()) }
                R.id.navigation_info -> consume { replaceFragment(InfoFragment()) }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.inTransaction {
            replace(R.id.fragment_container, fragment)
        }
        drawer_layout.setDrawerLockMode(when (fragment) {
            is ScheduleFragment -> DrawerLayout.LOCK_MODE_UNLOCKED
            else -> DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        })
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(Gravity.END)) {
            drawer_layout.closeDrawer(Gravity.END)
            return
        }
        super.onBackPressed()
    }
}
