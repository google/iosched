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
import com.google.samples.apps.iosched.shared.model.Tag
import java.lang.reflect.Type

/**
 * Deserializer for [Tag]s.
 */
class TagDeserializer : JsonDeserializer<Tag> {

    override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
    ): Tag {
        val obj = json?.asJsonObject!!

        return Tag(
                id = obj.get("tag").asString,
                category = obj.get("category").asString,
                color = obj.get("color")?.asString ?: "#000000",
                name = obj.get("name").asString
        )
    }
}
