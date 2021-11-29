/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.macrobenchmark

import android.content.Intent
import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val TAG = ScheduleBenchmarks::class.java.simpleName

@LargeTest
@RunWith(AndroidJUnit4::class)
class ScheduleBenchmarks {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun openDetail() {
        // need to keep track which index was selected so we can always select different one
        var selectedIndex = 0

        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            startupMode = StartupMode.WARM, // start the activity every time
            iterations = 10,
            setupBlock = {
                // start main activity
                val intent = Intent("com.google.samples.apps.iosched.STARTUP_ACTIVITY")
                Log.d(TAG, "Starting activity $intent")
                startActivityAndWait(intent)

                //  the children count is only visible part of the screen,
                //  so if we have too many iterations, we must scroll the recycler to other items
                val recycler = device.findObject(By.res(TARGET_PACKAGE, "recyclerview_schedule"))
                if (selectedIndex >= recycler.childCount) {
                    // need to set, otherwise scrolling could trigger system gesture navigation
                    recycler.setGestureMargin(device.displayWidth / 5)
                    // since scroll is unreliable, we scroll surely far enough to have new items
                    repeat(recycler.childCount) { recycler.scroll(Direction.DOWN, 1f) }
                    // and start again at first visible item on the screen
                    selectedIndex = 0
                }
            }
        ) {
            val recycler = device.findObject(By.res(TARGET_PACKAGE, "recyclerview_schedule"))
                ?: return@measureRepeated // TODO: BUG? the measure block is run before setup for CompilationMode.None and StartupMode.Warm

            // click on item which navigates to event detail
            val item = recycler.children[selectedIndex]
                ?: error("Session on index $selectedIndex not found")

            val itemTitleView = item.findObject(By.res(TARGET_PACKAGE, "title"))
            Log.d(TAG, "Opening detail(${itemTitleView?.text}) at index $selectedIndex")
            item.click()

            // wait until detail title is shown
            device.wait(Until.hasObject(By.res(TARGET_PACKAGE, "session_detail_title")), 10_000)

            // next time select different item
            selectedIndex++
        }

    }
}