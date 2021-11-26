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
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenDetailBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun openDetail() {

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)

        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            // compilationMode = CompilationMode.SpeedProfile()
            iterations = 3,
            setupBlock = {
                val intent = Intent()
                intent.`package` = TARGET_PACKAGE
                intent.action = "com.google.samples.apps.iosched.STARTUP_ACTIVITY"

                startActivityAndWait(intent)
            }
        ) {

            val recycler = device.findObject(By.res(TARGET_PACKAGE, "recyclerview_schedule"))
            // with input events from automation.
            recycler.setGestureMargin(device.displayWidth / 5)

            for (i in 1..10) {
                recycler.scroll(Direction.DOWN, 2f)
                device.waitForIdle()
            }


        }

    }
}