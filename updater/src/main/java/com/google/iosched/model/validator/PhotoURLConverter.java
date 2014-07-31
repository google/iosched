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
import com.google.iosched.Config;

public class PhotoURLConverter extends Converter {

  private static final String PHOTO_WIDTH_COMPONENT = "__w-200-400-600-800-1000__/";

  private String entityBaseUrl;
  public PhotoURLConverter(String entityName) {
    entityBaseUrl = Config.PHOTO_BASE_URL + entityName + "/" + PHOTO_WIDTH_COMPONENT;
  }

  @Override
  public JsonPrimitive convert(JsonPrimitive value) {
    if (value == null) {
      return null;
    }
    String entityId = value.getAsString();
    return new JsonPrimitive(entityBaseUrl+entityId+".jpg");
  }
}
