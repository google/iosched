/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.server.schedule.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.samples.apps.iosched.server.schedule.model.validator.Converter;
import com.google.samples.apps.iosched.server.schedule.model.validator.ConverterException;

/**
 * Helper class with methods to help JSON data model handling.
 *
 */
public class DataModelHelper {

  /**
   * Hardcoded category of a tag to be considered for hashtag.
   */
  static final String CATEGORY_FOR_HASHTAG = "TOPIC";

  public static void set(JsonObject source, Enum<?> sourceProperty, JsonObject dest, Enum<?> destProperty) {
    set(source, sourceProperty, dest, destProperty, null);
  }

  public static void set(JsonObject source, Enum<?> sourceProperty, JsonObject dest, Enum<?> destProperty, Converter sourceConverter) {
    set(get(source, sourceProperty, sourceConverter), dest, destProperty);
  }

  public static void set(JsonElement value, JsonObject dest, Enum<?> destProperty) {
    if (value != null && dest != null) {
      dest.add(maybeFixPropertyName(destProperty.name()), value);
    }
  }

  public static JsonArray getAsArray(JsonObject source, Enum<?> sourceProperty) {
    if (source == null) {
      return null;
    }
    JsonElement el = source.get(sourceProperty.name());
    if (el==null || !el.isJsonArray()) {
      return null;
    }
    return el.getAsJsonArray();
  }

  @SuppressWarnings("rawtypes")
  public static JsonElement get(JsonObject source, Enum sourceProperty) {
    return get(source, sourceProperty, null);
  }

  @SuppressWarnings("rawtypes")
  public static JsonElement get(JsonObject source, Enum sourceProperty, Converter sourceConverter) {
    JsonElement value = source.get(maybeFixPropertyName(sourceProperty.name()));
    if (sourceConverter != null) {
      value = sourceConverter.convert(value);
    }
    return value;
  }


  public static JsonPrimitive getMapValue(JsonElement map, String key, Converter converter,
      String defaultValueStr) {
    JsonPrimitive defaultValue = null;
    if (defaultValueStr != null) {
      defaultValue=new JsonPrimitive(defaultValueStr);
      if (converter != null) defaultValue = converter.convert(defaultValue);
    }
    if (map == null || !map.isJsonArray() ) {
      return defaultValue;
    }
    for (JsonElement el: map.getAsJsonArray()) {
      if (!el.isJsonObject()) {
        continue;
      }
      JsonObject obj = el.getAsJsonObject();
      if (!obj.has("Name") || !obj.has("Value")) {
        continue;
      }
      if (key.equals(obj.getAsJsonPrimitive("Name").getAsString())) {
        JsonElement value = obj.get("Value");
        if (!value.isJsonPrimitive()) {
          throw new ConverterException(value, converter, "Expected a JsonPrimitive");
        }
        if (converter != null) value = converter.convert(value);
        return value.getAsJsonPrimitive();
      }
    }
    return defaultValue;
  }


  public static String maybeFixPropertyName(String name) {
    if (name.charAt(0)=='_') {
      return name.substring(1);
    } else {
      return name;
    }
  }

  static boolean isHashtag(JsonObject tag) {
    return CATEGORY_FOR_HASHTAG.equals(get(tag, OutputJsonKeys.Tags.category).getAsString());
  }
}
