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

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GPlusURLConverter extends Converter {

  private static Pattern[] plusRecognizedPatterns = {
    Pattern.compile("(\\+[\\w]{2,})"),  // +VanityUrl
    Pattern.compile("([\\d]{20,})"),    // /8759384579284782342/  (G+ ID)
  };

  private static final Pattern acceptableUrlPattern = Pattern.compile("https?:\\/\\/.+");

  private static final MessageFormat plusFormat = new MessageFormat("https://plus.google.com/{0}");

  @Override
  public JsonPrimitive convert(JsonPrimitive value) {
    if (value == null) {
      return null;
    }
    String str = value.getAsString();
    if (str.isEmpty()) {
      return value;
    }
    for (Pattern p: plusRecognizedPatterns) {
      Matcher m = p.matcher(str);
      if (m.find()) {
        return new JsonPrimitive(plusFormat.format(new String[]{m.group(1)}));
      }
    }

    // If URL starts with http/https:
    if (acceptableUrlPattern.matcher(str).matches()) {
      return value;
    }

    // Otherwise, just add https://:
    str = "https://" + str;
    return new JsonPrimitive(str);
  }
}
