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
package com.google.iosched.server;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.ShortBlob;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;


/**
 * Hold log information for each run of the Updater and save this to the datastore.
 *
 */
public class UpdateRunLogger {

  private static final String UPDATERUN_ENTITY_KIND = "UpdateRun";
  private final Logger logger = Logger.getLogger(UpdateRunLogger.class.getName());

  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  private long lastStart;
  private HashMap<String, Long> timers;

  public UpdateRunLogger() {
    timers = new HashMap<String, Long>();
  }

  public void startTimer() {
    this.lastStart = System.currentTimeMillis();
  }

  public void stopTimer(String description) {
    timers.put((timers.size()+1)+"_"+description, System.currentTimeMillis() - lastStart);
  }

  public Entity getLastRun() {
    Query query = new Query(UPDATERUN_ENTITY_KIND).addSort("date", Query.SortDirection.DESCENDING);
    List<Entity> result = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
    if (result == null || result.isEmpty()) {
      return null;
    }
    return result.get(0);
  }

  public List<Entity> getMostRecentRuns(int number) {
    Query query = new Query(UPDATERUN_ENTITY_KIND).addSort("date", Query.SortDirection.DESCENDING);
    return datastore.prepare(query).asList(FetchOptions.Builder.withLimit(number));
  }

  public void logNoopRun() {
    logger.fine("Run APIUpdater. No updates required.");
  }

  public void logUpdateRun(int majorVersion, int minorVersion, String filename, byte[] hash,
      JsonObject data, boolean forced) {
    Entity updateRun = new Entity(UPDATERUN_ENTITY_KIND);
    updateRun.setProperty("date", new Date());
    updateRun.setProperty("hash", new ShortBlob(hash));
    updateRun.setProperty("forced", forced);
    updateRun.setProperty("majorVersion", majorVersion);
    updateRun.setProperty("minorVersion", minorVersion);
    for (Entry<String, Long> performanceItem: timers.entrySet()) {
      updateRun.setProperty("time_"+performanceItem.getKey(), performanceItem.getValue());
    }
    updateRun.setProperty("filename", filename);
    StringBuilder sb = new StringBuilder();
    for (Entry<String, JsonElement> el: data.entrySet()) {
      if (el.getValue().isJsonArray()) {
        sb.append(el.getKey()).append("=").append(el.getValue().getAsJsonArray().size()).append(" ");
      }
    }
    if (sb.length()>0) {
      // remove trailing space
      sb.deleteCharAt(sb.length()-1);
    }
    updateRun.setProperty("summary", sb.toString());
    datastore.put(updateRun);
    timers.clear();
  }
}
