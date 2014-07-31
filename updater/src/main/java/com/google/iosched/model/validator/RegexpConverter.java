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

import java.util.regex.Pattern;


public class RegexpConverter extends Converter {
  private Pattern pattern;
  private boolean acceptsNull;

  public RegexpConverter(String pattern) {
    this(pattern, true);
  }
  public RegexpConverter(String pattern, boolean acceptsNull) {
    this.acceptsNull = acceptsNull;
    this.pattern = Pattern.compile(pattern);
  }

  @Override
  public JsonPrimitive convert(JsonPrimitive value) {
    if (value == null) {
      if (acceptsNull) {
        return null;
      } else {
        throw new ConverterException(value, this);
      }
    }
    String str = value.getAsString();
    if (str.isEmpty() || pattern.matcher(str).matches()) {
      return value;
    } else {
      throw new ConverterException(value, this);
    }
  }
}
