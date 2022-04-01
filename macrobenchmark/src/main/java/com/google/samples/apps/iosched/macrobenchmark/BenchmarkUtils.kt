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
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

const val TARGET_PACKAGE = "com.google.samples.apps.iosched"

val scheduleRecyclerSelector: BySelector
    get() = By.res(TARGET_PACKAGE, "recyclerview_schedule")

fun MacrobenchmarkScope.startMainAndWait() {
    // Need to have, because [StartupMode.HOT] wouldn't do anything
    pressHome()
    // Start activity defined by an action
    val intent = Intent("$TARGET_PACKAGE.STARTUP_ACTIVITY")
    Log.d("Benchmark", "Starting activity $intent")

    // This can be usually without the intent, but in our case we have LauncherActivity with onboarding.
    startActivityAndWait(intent)
}

fun MacrobenchmarkScope.startMainAndConfirmDialogs() {
    startMainAndWait()

    // The first time the app is run, customize schedule dialog is shown
    confirmDialog()

    // Later, it may show I/O notifications dialog
    // Sometimes it happens that both dialogs are shown one after another
    confirmDialog()
}

fun MacrobenchmarkScope.scrollSchedule() {
    // Need to wait until schedule is loaded
    device.wait(Until.hasObject(scheduleRecyclerSelector), 10_000)

    val recycler = device.findObject(scheduleRecyclerSelector)
    // Need to set, otherwise scrolling could trigger system gesture navigation
    recycler.setGestureMargin(device.displayWidth / 5)

    // Fling several times
    repeat(3) { recycler.fling(Direction.DOWN) }
}

fun MacrobenchmarkScope.confirmDialog() {
    // Check if button is appeared or return
    val dialogPositiveButton = device.findObject(By.res("android", "button1")) ?: return
    dialogPositiveButton.click()
    device.waitForIdle()
    // Need short delay, because it may be animating the dialog out
    Thread.sleep(1_000)
}
