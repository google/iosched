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
package com.google.iosched.model;

import java.util.HashMap;
import java.util.Iterator;

/**
 * An encapsulation of a JsonDataSource collection.
 *
 */
public class JsonDataSources implements Iterable<String> {
  private HashMap<String, JsonDataSource> sources;

  /**
   *
   */
  public JsonDataSources() {
    this.sources = new HashMap<String, JsonDataSource>();
  }

  public JsonDataSource getSource(String entityType) {
    return sources.get(entityType);
  }

  /**
   * Associates the specified {@link JsonDataSource} with the specified name
   * in this map. If the map previously contained a mapping for the key,
   * the old value is replaced.
   *
   * @param data value to be associated with the specified name
   * @return the previous {@link JsonDataSource} associated with <tt>name</tt>,
   *         or <tt>null</tt> if there was no mapping for <tt>name</tt>.
   */
  public JsonDataSource addSource(JsonDataSource data) {
    return sources.put(data.getSourceType().name(), data);
  }

  public void putAll(JsonDataSources dataSources) {
    this.sources.putAll(dataSources.sources);
  }

  @Override
  public Iterator<String> iterator() {
    return sources.keySet().iterator();
  }
}
