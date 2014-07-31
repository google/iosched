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
package com.google.iosched.model.validator;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public abstract class Converter {
  abstract JsonPrimitive convert(JsonPrimitive value);
  public JsonPrimitive convert(JsonElement value) {
    if (value == null || value instanceof JsonPrimitive) {
      return convert((JsonPrimitive) value);
    }
    throw new ConverterException(value, this, "JsonElement should be JsonPrimitive");
  }
}
