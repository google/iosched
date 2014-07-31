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

import java.util.Random;

public class StringObfuscateConverter extends Converter {

  static Random r = new Random();

  public static String obfuscate(String src) {
    r.setSeed(src.hashCode()); // static seed to keep output stable
    char[] str = src.toCharArray();
    for (int i=0; i<str.length; i++) {
      char ch = str[i];
      if (Character.isLowerCase(ch)) {
        str[i] = (char) ('a' + r.nextInt(25));
      } else if (Character.isUpperCase(ch)) {
        str[i] = (char) ('A' + r.nextInt(25));
      } else if (Character.isDigit(ch)) {
        str[i] = (char) ('0' + r.nextInt(10));
      }
    }
    return new String(str);
  }

  @Override
  public JsonPrimitive convert(JsonPrimitive value) {
    if (value == null) {
      return null;
    }

    return new JsonPrimitive(obfuscate(value.getAsString()));
  }
}
