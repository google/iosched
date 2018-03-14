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

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.wear.ambient.AmbientModeSupport

/**
 * Base class for fragments embedded in Activities implementing Ambient support via
 * AmbientModeSupport. Allows ambient mode changes and UI update messages to be passed from the
 * Activity to the child WearableFragment.
 *
 * Note:
 * These messages are not sent automatically; the parent Activity class must callback the
 * appropriate WearableFragment methods when a WearableFragment is in view.
 *
 * [AmbientModeSupport]
 */
abstract class WearableFragment : Fragment() {
    private var isAmbient = false

    abstract fun onUpdateAmbient()

    fun onEnterAmbient(ambientDetails: Bundle?) {
        isAmbient = true
    }

    fun onExitAmbient() {
        isAmbient = false
    }

    fun isAmbient(): Boolean {
        return isAmbient
    }
}
