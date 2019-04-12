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

package com.google.samples.apps.iosched.shared.data.session.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Deserializer for Codelabs. Returns temporary Codelab objects, which are later normalized once
 * tags have also been parsed.
 */
class CodelabDeserializer : JsonDeserializer<CodelabTemp> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CodelabTemp {
        val obj = json?.asJsonObject!!
        return CodelabTemp(
            id = obj.get("id").asString,
            title = obj.get("title").asString,
            description = obj.get("description").asString,
            durationMinutes = obj.get("duration").asInt,
            iconUrl = obj.get("icon")?.asString,
            codelabUrl = obj.get("link").asString,
            sortPriority = obj.get("priority")?.asInt ?: 0,
            tagNames = getListFromJsonArray(obj, "tagNames")
        )
    }
}
