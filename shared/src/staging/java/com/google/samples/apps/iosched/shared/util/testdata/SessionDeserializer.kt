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

package com.google.samples.apps.iosched.shared.util.testdata

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.threeten.bp.ZonedDateTime
import java.lang.reflect.Type

/**
 * Deserializer for sessions. Returns a temporary session object, [SessionTemp].
 */
class SessionDeserializer : JsonDeserializer<SessionTemp> {

    override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
    ): SessionTemp {
        val obj = json?.asJsonObject!!

        val tags: List<String> = getListFromJsonArray(obj, "tags")

        val speakers = getListFromJsonArray(obj, "speakers")

        return SessionTemp(
                id = obj.get("id").asString,
                sessionUrl = obj.get("url").asString,
                title = obj.get("title").asString,
                startTime = ZonedDateTime.parse(obj.get("startTimestamp").asString),
                endTime = ZonedDateTime.parse(obj.get("endTimestamp").asString),
                abstract = obj.get("description").asString,
                photoUrl = obj.get("photoUrl").asString,
                liveStreamUrl = "",
                speakers = speakers.toSet(),
                tags = tags.toList(),
                relatedSessions = emptySet(),
                youTubeUrl = "",
                room = obj.get("room").asString
        )
    }

    private fun getListFromJsonArray(obj: JsonObject, key: String): List<String> {
        val array = obj.get(key).asJsonArray
        val stringList = ArrayList<String>()
        array.mapTo(stringList) { it.asString }
        return stringList
    }
}
