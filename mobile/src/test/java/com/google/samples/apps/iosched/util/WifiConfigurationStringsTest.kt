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

package com.google.samples.apps.iosched.util

import junit.framework.Assert.assertEquals
import org.junit.Test

class WifiConfigurationStringsTest {
    @Test
    fun testWrapQuotes_emptyString() {
        val original = ""
        val expected = "\"\""
        val quoted = original.wrapInQuotes()
        assertEquals(expected, quoted)
    }

    @Test
    fun testWrapQuotes_alreadyQuotes() {
        val original = "\"io2018\""
        val expected = "\"io2018\""
        val quoted = original.wrapInQuotes()
        assertEquals(expected, quoted)
    }

    @Test
    fun testWrapQuotes_noQuotes() {
        val original = "io2018"
        val expected = "\"io2018\""
        val quoted = original.wrapInQuotes()
        assertEquals(expected, quoted)
    }

    @Test
    fun testWrapQuotes_oneStartingQuote() {
        val original = "\"io2018"
        val expected = "\"io2018\""
        val quoted = original.wrapInQuotes()
        assertEquals(expected, quoted)
    }

    @Test
    fun testWrapQuotes_oneEndingQuote() {
        val original = "io2018\""
        val expected = "\"io2018\""
        val quoted = original.wrapInQuotes()
        assertEquals(expected, quoted)
    }

    @Test
    fun testUnwrapQuotes_emptyString() {
        val original = ""
        val expected = ""
        val unquoted = original.unwrapQuotes()
        assertEquals(expected, unquoted)
    }

    @Test
    fun testUnwrapQuotes_alreadyQuotes() {
        val original = "\"io2018\""
        val expected = "io2018"
        val unquoted = original.unwrapQuotes()
        assertEquals(expected, unquoted)
    }

    @Test
    fun testUnwrapQuotes_noQuotes() {
        val original = "io2018"
        val expected = "io2018"
        val unquoted = original.unwrapQuotes()
        assertEquals(expected, unquoted)
    }

    @Test
    fun testUnwrapQuotes_oneStartingQuote() {
        val original = "\"io2018"
        val expected = "io2018"
        val quoted = original.unwrapQuotes()
        assertEquals(expected, quoted)
    }

    @Test
    fun testUnwrapQuotes_oneEndingQuote() {
        val original = "io2018\""
        val expected = "io2018"
        val quoted = original.unwrapQuotes()
        assertEquals(expected, quoted)
    }
}
