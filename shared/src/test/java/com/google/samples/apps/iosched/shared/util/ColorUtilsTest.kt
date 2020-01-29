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

package com.google.samples.apps.iosched.shared.util

import java.lang.IllegalArgumentException
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorUtilsTest {

    @Test
    fun parseHexColor_withAlpha() {
        assertEquals(0x8033b5e5L.toInt(), ColorUtils.parseHexColor("#8033b5e5"))
    }

    @Test
    fun parseHexColor_withoutAlpha() {
        assertEquals(0xff33b5e5L.toInt(), ColorUtils.parseHexColor("#33b5e5"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseHexColor_emptyString() {
        ColorUtils.parseHexColor("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseHexColor_noHashSymbol() {
        ColorUtils.parseHexColor("000000")
    }

    @Test(expected = NumberFormatException::class)
    fun parseHexColor_invalidHex() {
        ColorUtils.parseHexColor("#D0GF00D1")
    }
}
