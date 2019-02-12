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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.theme.ThemeViewModel
import com.google.samples.apps.iosched.util.updateForTheme
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

/** Shell activity hosting a [MapFragment] */
class MapActivity : DaggerAppCompatActivity() {

    private lateinit var fragment: MapFragment

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

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

        val viewModel: ThemeViewModel = viewModelProvider(viewModelFactory)
        updateForTheme(viewModel.currentTheme)

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

        viewModel.theme.observe(this, Observer(::updateForTheme))
    }

    override fun onBackPressed() {
        if (!fragment.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
