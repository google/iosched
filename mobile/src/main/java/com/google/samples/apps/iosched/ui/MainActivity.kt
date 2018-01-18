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
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.ui.feed.FeedFragment
import com.google.samples.apps.iosched.ui.map.MapFragment
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_schedule -> {
                replaceFragment(ScheduleFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_feed -> {
                replaceFragment(FeedFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_map -> {
                replaceFragment(MapFragment())
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
                .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Add product list fragment if this is first creation
        if (savedInstanceState == null) {
            val fragment = ScheduleFragment()

            supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, fragment, ScheduleFragment.TAG).commit()
        }

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)


    }
}
