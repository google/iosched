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

import com.google.samples.apps.iosched.model.Speaker
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SpeakerUtilsTest {
    private lateinit var speaker1: Speaker
    private lateinit var speaker2: Speaker
    private lateinit var speaker3: Speaker

    @Before
    fun setup() {
        speaker1 = Speaker(
            id = "1", name = "Troy McClure", imageUrl = "",
            company = "", abstract = ""
        )

        speaker2 = Speaker(
            id = "2", name = "Ziggy Anderson", imageUrl = "",
            company = "", abstract = ""
        )

        speaker3 = Speaker(
            id = "3", name = "Leah Hadley", imageUrl = "",
            company = "", abstract = ""
        )
    }

    @Test
    fun alphabeticallyOrderedSpeakerListSortSpeakers() {
        val speakerSet = linkedSetOf(speaker1, speaker2, speaker3)

        val speakerList = SpeakerUtils.alphabeticallyOrderedSpeakerList(speakerSet)
        val expectedSpeakerList = arrayListOf(speaker3, speaker1, speaker2)

        Assert.assertEquals(expectedSpeakerList, speakerList)
    }
}
