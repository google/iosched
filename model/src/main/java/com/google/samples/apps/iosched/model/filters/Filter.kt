/*
 * Copyright 2020 Google LLC
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

package com.google.samples.apps.iosched.model.filters

import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.Tag

sealed class Filter {

    data class TagFilter(val tag: Tag) : Filter()

    data class DateFilter(val day: ConferenceDay) : Filter()

    object MyScheduleFilter : Filter()
}
