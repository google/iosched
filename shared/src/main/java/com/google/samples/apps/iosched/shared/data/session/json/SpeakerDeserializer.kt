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
import com.google.samples.apps.iosched.model.Speaker
import java.lang.reflect.Type

/**
 * Deserializer for [Speaker]s.
 */
class SpeakerDeserializer : JsonDeserializer<Speaker> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Speaker {
        val obj = json?.asJsonObject!!
        val social = obj.getAsJsonObject("socialLinks")
        return Speaker(
            id = obj.get("id").asString,
            name = obj.get("name").asString,
            imageUrl = obj.get("thumbnailUrl")?.asString ?: "",
            company = obj.get("company")?.asString ?: "",
            abstract = obj.get("bio")?.asString ?: "",
            websiteUrl = social?.get("website")?.asString,
            twitterUrl = social?.get("twitter")?.asString,
            githubUrl = social?.get("github")?.asString,
            linkedInUrl = social?.get("linkedin")?.asString
        )
    }
}
