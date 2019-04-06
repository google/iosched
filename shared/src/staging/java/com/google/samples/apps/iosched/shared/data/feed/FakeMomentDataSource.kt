/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.shared.data.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.ColorUtils
import org.threeten.bp.ZonedDateTime

/**
 * Returns hardcoded data for development and testing.
 */
object FakeMomentDataSource : MomentDataSource {
    private val moment = Moment(
        id = "1",
        title = "KeyNote: Day 1",
        streamUrl = "https://www.youtube.com",
        startTime = ZonedDateTime.now(),
        endTime = ZonedDateTime.now(),
        textColor = ColorUtils.parseHexColor("#fff"),
        ctaType = Moment.CTA_MAP_LOCATION,
        imageUrl = "",
        attendeeRequired = false,
        timeVisible = false,
        featureId = "",
        featureName = "EATs Tent"
    )

    override fun getObservableMoments(): LiveData<Result<List<Moment>>> {
        val result = MutableLiveData<Result<List<Moment>>>()
        result.postValue(
            Result.Success(
                arrayListOf(
                    moment
                )
            )
        )
        return result
    }

    override fun clearSubscriptions() {}
}