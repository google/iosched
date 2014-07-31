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

import com.google.gson.JsonPrimitive;

public class IntegerToStringConverter extends Converter {
  @Override
  public JsonPrimitive convert(JsonPrimitive value) {
    if (value == null) {
      return null;
    }
    if (value.isNumber()) {
      return new JsonPrimitive(String.valueOf(value.getAsInt()));
    }
    try {
      Integer.parseInt(value.getAsString());
      // if it parses correctly, it is validated:
      return value;
    } catch (NumberFormatException ex) {
      throw new ConverterException(value, this);
    }
  }
}
