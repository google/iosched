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

package com.google.samples.apps.iosched.shared.data.tag

import com.google.samples.apps.iosched.shared.model.Tag

/**
 * TODO: Placeholder
 */
object RemoteTagDataSource : TagDataSource {

    private var tags = listOf(
            Tag("tag0", "TRACK", 0, "Ads", 0xFFB0BEC5.toInt()),
            Tag("tag1", "TRACK", 1, "Android", 0xFFAED581.toInt()),
            Tag("tag2", "TRACK", 2, "Assistant", 0xFF1CE8B5.toInt()),
            Tag("tag3", "TRACK", 3, "Cloud", 0xFF80CBC4.toInt()),
            Tag("tag4", "TRACK", 4, "Design", 0xFFF8BBD0.toInt()),
            Tag("tag5", "TRACK", 5, "Firebase", 0xFFFFD54F.toInt()),
            Tag("tag6", "TRACK", 6, "IoT", 0xFFBCAAA4.toInt()),
            Tag("tag7", "TRACK", 7, "Location & Maps", 0xFFEF9A9A.toInt()),
            Tag("tag8", "TRACK", 8, "Machine Learning", 0xFFBCC8Fb.toInt()),
            Tag("tag9", "TRACK", 9, "Misc", 0xFFC5C9E9.toInt()),
            Tag("tag10", "TRACK", 10, "Mobile Web", 0xFFFFF176.toInt()),
            Tag("tag11", "TRACK", 11, "Search", 0xFF90CAF9.toInt()),
            Tag("tag12", "TRACK", 12, "VR", 0xFFFF8A65.toInt())
    )

    override fun getTags() = tags
}
