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

package com.google.samples.apps.iosched.tv.ui

import androidx.leanback.app.BrowseSupportFragment
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.tv.ui.schedule.ScheduleFragment

/**
 * Creates a [ScheduleFragment] for a given conference day from the nav row.
 */
class MainPageRowFragmentFactory : BrowseSupportFragment.FragmentFactory<ScheduleFragment>() {

    override fun createFragment(rowObj: Any): ScheduleFragment {
        ConferenceDays.forEachIndexed { index, _ ->
            return ScheduleFragment.newInstance(index)
        }
        throw IllegalArgumentException("Invalid row selected: $rowObj")
    }
}
