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


public class YoutubeURLConverter extends Converter {
  private boolean acceptsNull;
  private static Pattern[] patterns = {
    Pattern.compile("youtube\\.com\\/watch\\?v=([^&?/]+)"),
    Pattern.compile("youtube\\.com\\/embed\\/([^&?/]+)"),
    Pattern.compile("youtube\\.com\\/v\\/([^&?/]+)"),
    Pattern.compile("youtu\\.be\\/([^&?/]+)"),
    Pattern.compile("^([A-Za-z0-9_-]{11})$")
  };
  private static final MessageFormat outFormat = new MessageFormat("https://youtu.be/{0}");

  public YoutubeURLConverter(boolean acceptsNull) {
    this.acceptsNull = acceptsNull;
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
    if (acceptsNull && str.isEmpty()) {
      return value;
    }
    for (Pattern p: patterns) {
      Matcher m = p.matcher(str);
      if (m.find()) {
        return new JsonPrimitive(outFormat.format(new String[]{m.group(1)}));
      }
    }
    throw new ConverterException(value, this);
  }
}
