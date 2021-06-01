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

package com.google.samples.apps.iosched

import android.app.Application
import androidx.startup.AppInitializer
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.util.initializers.TimberInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Initialization of libraries.
 */
@HiltAndroidApp
class MainApplication : Application() {

    // Even if the var isn't used, needs to be initialized at application startup.
    @Inject lateinit var analyticsHelper: AnalyticsHelper

    override fun onCreate() {
        super.onCreate()
        AppInitializer.getInstance(this)
            .initializeComponent(TimberInitializer::class.java)
    }
}
