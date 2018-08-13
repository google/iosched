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

package com.google.samples.apps.iosched.shared.data.session.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
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

        val tagNames: List<String> = getListFromJsonArray(obj, "tagNames")

        val speakers = getListFromJsonArray(obj, "speakers")

        val relatedSessions = getListFromJsonArray(obj, "relatedSessions")

        @Suppress("UNNECESSARY_SAFE_CALL") // obj.get can return null
        return SessionTemp(
            id = obj.get("id").asString,
            sessionUrl = getUrlFromId(obj.get("id").asString),
            title = obj.get("title").asString,
            startTime = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(obj.get("startTimestamp").asLong), ZoneOffset.UTC
            ),
            endTime = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(obj.get("endTimestamp").asLong), ZoneOffset.UTC
            ),
            abstract = obj.get("description").asString,
            photoUrl = obj.get("photoUrl")?.asString,
            liveStreamUrl = "TODO: Set livestream URL", // TODO Set or remove this (b/77292964)
            isLivestream = obj.get("livestream").asBoolean,
            speakers = speakers.toSet(),
            tagNames = tagNames.toList(),
            relatedSessions = relatedSessions.toSet(),
            youTubeUrl = obj.get("youtubeUrl")?.asString ?: "",
            room = obj.get("room").asString
        )
    }

    private fun getListFromJsonArray(obj: JsonObject, key: String): List<String> {
        val array = obj.get(key).asJsonArray
        val stringList = ArrayList<String>()
        array.mapTo(stringList) { it.asString }
        return stringList
    }

    private fun getUrlFromId(id: String): String {
        val prefix = "https://events.google.com/io/schedule/?section=day&sid="
        return if (id.isNotEmpty()) prefix + id else ""
    }
}
