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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A generic holder for JSON data.
 *
 */
public class JsonDataSource implements Comparable<JsonDataSource>, Iterable<JsonObject> {
  private Enum<?> sourceType;
  private HashMap<String, JsonObject> data;

  public JsonDataSource(Enum<?> sourceType) {
    this.sourceType = sourceType;
    this.data = new HashMap<String, JsonObject>();
  }

  public JsonDataSource(Enum<?> sourceType, JsonArray arr) {
    this(sourceType);
    if (arr != null) {
      addAll(arr);
    }
  }

  public Enum<?> getSourceType() {
    return sourceType;
  }

  public JsonObject getElementById(String id) {
    return data.get(id);
  }

  @Override
  public Iterator<JsonObject> iterator() {
    return data.values().iterator();
  }

  public int size() {
    return data.size();
  }

  public void addElement(String id, JsonObject obj) {
    data.put(id, obj);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof JsonDataSource &&
        this.sourceType.equals(((JsonDataSource) obj).sourceType);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return sourceType.hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(JsonDataSource o) {
    return sourceType.name().compareTo(o.sourceType.name());
  }

  private String getKeyProperty(JsonObject obj) {
    JsonElement idEl = obj.get("Id");
    if (idEl == null) {
      // reflection is not efficient in general, but the overhead should be insignificant
      // compared to the usual times taken to load a datasource (either from HTTP, disk or
      // cloud storage)
      Method m;
      try {
        m = sourceType.getClass().getMethod("getKey");
        if (m != null) {
          return (String) m.invoke(sourceType);
        }
      } catch (Exception e) {
        throw new IllegalArgumentException("Error! Cannot add JsonArray of type "
            + sourceType.name()+" because there is no property \"id\" in the JsonArray element "
            + "nor a static getKey() method in the enum.", e);
      }
    }
    return "Id";
  }

  public void addAll(JsonArray arr) {
    String idProperty = null;
    for (int i=0; i<arr.size(); i++) {
      JsonObject obj = arr.get(i).getAsJsonObject();
      if (idProperty == null) {
        idProperty = getKeyProperty(obj);
      }
      String id = obj.get(idProperty).getAsString();
      addElement(id, obj);
    }
  }
}
