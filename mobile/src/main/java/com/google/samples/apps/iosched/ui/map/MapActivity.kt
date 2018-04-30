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

package com.google.samples.apps.iosched.ui.map

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.inTransaction
import dagger.android.support.DaggerAppCompatActivity

/** Shell activity hosting a [MapFragment] */
class MapActivity : DaggerAppCompatActivity() {

    private lateinit var fragment: MapFragment

    companion object {
        const val EXTRA_FEATURE_ID = "extra.FEATURE_ID"
        const val FRAGMENT_ID = R.id.fragment_container

        fun starterIntent(context: Context, featureId: String): Intent {
            return Intent(context, MapActivity::class.java).apply {
                putExtra(EXTRA_FEATURE_ID, featureId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        if (savedInstanceState == null) {
            val featureId = intent.getStringExtra(EXTRA_FEATURE_ID)
            fragment = if (!TextUtils.isEmpty(featureId)) {
                MapFragment.newInstance(featureId)
            } else {
                MapFragment()
            }
            supportFragmentManager.inTransaction {
                add(FRAGMENT_ID, fragment)
            }
        } else {
            fragment = supportFragmentManager.findFragmentById(FRAGMENT_ID) as MapFragment
        }
    }

    override fun onBackPressed() {
        if (!fragment.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
