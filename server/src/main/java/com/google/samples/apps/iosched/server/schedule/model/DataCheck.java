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

import static com.google.samples.apps.iosched.server.schedule.model.DataModelHelper.get;
import static com.google.samples.apps.iosched.server.schedule.model.DataModelHelper.getAsArray;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.schedule.server.ManifestData;
import com.google.samples.apps.iosched.server.schedule.server.cloudstorage.CloudFileManager;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Safeguard checks about the reliability and consistency of the generated and saved data.
 *
 */
public class DataCheck {

  static Logger LOG = Logger.getLogger(DataCheck.class.getName());

  private CloudFileManager fileManager;

  private SimpleDateFormat sessionDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  private SimpleDateFormat blockDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  public DataCheck(CloudFileManager fileManager) {
    this.fileManager = fileManager;
  }

  /**
   * @param sources
   */
  public CheckResult check(JsonDataSources sources, JsonObject newSessionData, ManifestData manifest) throws IOException {
    newSessionData = clone(newSessionData);
    JsonObject newData = new JsonObject();
    merge(newSessionData, newData);
    JsonObject oldData = new JsonObject();
    for (JsonElement dataFile: manifest.dataFiles) {
      String filename = dataFile.getAsString();
      // except for session data, merge all other files:
      Matcher matcher = Config.SESSIONS_PATTERN.matcher(filename);
      if (!matcher.matches()) {
        JsonObject data = fileManager.readFileAsJsonObject(filename);
        merge(data, oldData);
        merge(data, newData);
      }
    }

    CheckResult result = new CheckResult();

    // check if array of entities is more than 80% the size of the old data:
    checkUsingPredicator(result, oldData, newData, new ArraySizeValidator());

    // Check that no existing tag was removed or had its name changed in a significant way
    checkUsingPredicator(result, oldData, newData,
        OutputJsonKeys.MainTypes.tags, OutputJsonKeys.Tags.tag,
        new EntityValidator() {
          @Override
          public void evaluate(CheckResult result, String entity, JsonObject oldData,
              JsonObject newData) {
            if (newData == null) {
              String tagName = get(oldData, OutputJsonKeys.Tags.tag).getAsString();
              String originalId = get(oldData, OutputJsonKeys.Tags.original_id).getAsString();
              result.failures.add(
                  new CheckFailure(entity, tagName,
                      "Tag could not be found or changed name. Original category ID = " + originalId)
                  );
            }
          }
        });

    // Check that no room was removed
    checkUsingPredicator(result, oldData, newData,
        OutputJsonKeys.MainTypes.rooms, OutputJsonKeys.Rooms.id,
        new EntityValidator() {
          @Override
          public void evaluate(CheckResult result, String entity, JsonObject oldData,
              JsonObject newData) {
            if (newData == null) {
              String id = get(oldData, OutputJsonKeys.Rooms.id).getAsString();
              result.failures.add(
                  new CheckFailure(entity, id,
                      "Room could not be found. Original room: " + oldData)
                  );
            }
          }
        });

    // Check if blocks start and end timestamps are valid
    JsonArray newBlocks = getAsArray(newData, OutputJsonKeys.MainTypes.blocks);
    LOG.info(newData.toString());
    if (newBlocks == null ) {
      StringBuilder sb= new StringBuilder();
      for (Map.Entry<String, JsonElement> entry: newData.entrySet()) {
        sb.append(entry.getKey()).append(", ");
      }
      throw new IllegalArgumentException("Could not find the blocks entities. Entities in newData are: "+sb);
    }
    for (JsonElement el: newBlocks) {
      JsonObject block = el.getAsJsonObject();
      try {
        Date start = blockDateFormat.parse(get(block, OutputJsonKeys.Blocks.start).getAsString());
        Date end = blockDateFormat.parse(get(block, OutputJsonKeys.Blocks.end).getAsString());
        if ( start.getTime() >= end.getTime() ||  // check for invalid start/end combinations
            start.getTime() < Config.CONFERENCE_DAYS[0][0] || // check for block starting before the conference
            end.getTime() > Config.CONFERENCE_DAYS[1][1]) {  // check for block ending after the conference
          result.failures.add(
              new CheckFailure(OutputJsonKeys.MainTypes.blocks.name(), null,
                  "Invalid block start or end date. Block=" + block));
        }
      } catch (ParseException ex) {
        result.failures.add(
            new CheckFailure(OutputJsonKeys.MainTypes.blocks.name(), null,
                "Could not parse block start or end date. Exception="+ex.getMessage()
                +". Block=" + block));
      }
    }

    // Check if sessions start and end timestamps are valid
    JsonArray newSessions = getAsArray(newData, OutputJsonKeys.MainTypes.sessions);
    for (JsonElement el: newSessions) {
      JsonObject session = el.getAsJsonObject();
      try {
        Date start = sessionDateFormat.parse(get(session, OutputJsonKeys.Sessions.startTimestamp).getAsString());
        Date end = sessionDateFormat.parse(get(session, OutputJsonKeys.Sessions.endTimestamp).getAsString());
        if ( start.getTime() >= end.getTime() ) {  // check for invalid start/end combinations
          result.failures.add(
              new CheckFailure(OutputJsonKeys.MainTypes.sessions.name(), get(session, OutputJsonKeys.Sessions.id).getAsString(),
                  "Session ends before or at the same time as it starts. Session=" + session));
        } else if ( end.getTime() - start.getTime() > 6 * 60 * 60 * 1000L ) { // check for session longer than 6 hours
          result.failures.add(
              new CheckFailure(OutputJsonKeys.MainTypes.sessions.name(), get(session, OutputJsonKeys.Sessions.id).getAsString(),
                  "Session is longer than 6 hours. Session=" + session));
        } else if ( start.getTime() < Config.CONFERENCE_DAYS[0][0] || // check for session starting before the conference
            end.getTime() > Config.CONFERENCE_DAYS[1][1]) {  // check for session ending after the conference
          result.failures.add(
              new CheckFailure(OutputJsonKeys.MainTypes.sessions.name(), get(session, OutputJsonKeys.Sessions.id).getAsString(),
                  "Session starts before or ends after the days of the conference. Session=" + session));
        } else {
          // Check if all sessions are covered by at least one free block (except the keynote):
          boolean valid = false;
          if (!get(session, OutputJsonKeys.Sessions.id).getAsString().equals("__keynote__")) {
            for (JsonElement bl: newBlocks) {
              JsonObject block = bl.getAsJsonObject();
              Date blockStart= blockDateFormat.parse(get(block, OutputJsonKeys.Blocks.start).getAsString());
              Date blockEnd = blockDateFormat.parse(get(block, OutputJsonKeys.Blocks.end).getAsString());
              String blockType = get(block, OutputJsonKeys.Blocks.type).getAsString();
              if ("free".equals(blockType) &&
                  start.compareTo(blockStart) >= 0 &&
                  start.compareTo(blockEnd) < 0) {
                valid = true;
                break;
              }
            }
            if (!valid) {
              result.failures.add(
                  new CheckFailure(OutputJsonKeys.MainTypes.sessions.name(), get(session, OutputJsonKeys.Sessions.id).getAsString(),
                      "There is no FREE block where this session start date lies on. Session=" + session));
            }
          }
        }
      } catch (ParseException ex) {
        result.failures.add(
            new CheckFailure(OutputJsonKeys.MainTypes.sessions.name(), get(session, OutputJsonKeys.Sessions.id).getAsString(),
                "Could not parse session start or end date. Exception="+ex.getMessage()
                +". Session=" + session));
      }
    }


    // Check if video sessions (video library) have valid video URLs
    JsonArray newVideoLibrary = getAsArray(newData, OutputJsonKeys.MainTypes.video_library);
    for (JsonElement el: newVideoLibrary) {
      JsonObject session = el.getAsJsonObject();
      JsonPrimitive videoUrl = (JsonPrimitive) get(session, OutputJsonKeys.VideoLibrary.vid);
      if (videoUrl == null || !videoUrl.isString() || videoUrl.getAsString() == null ||
          videoUrl.getAsString().isEmpty()) {
        result.failures.add(
          new CheckFailure(InputJsonKeys.VendorAPISource.MainTypes.topics.name(),
              ""+get(session, OutputJsonKeys.VideoLibrary.id),
              "Video Session has empty vid info. Session: " + session));
      }
    }

    return result;
  }

  public void checkUsingPredicator(CheckResult result, JsonObject oldData, JsonObject newData,
      ArrayValidator predicate) {
    for (Map.Entry<String, JsonElement> entry: oldData.entrySet()) {
      String oldKey = entry.getKey();
      JsonArray oldValues = entry.getValue().getAsJsonArray();
      predicate.evaluate(result, oldKey, oldValues, newData.getAsJsonArray(oldKey));
    }
  }


  public void checkUsingPredicator(CheckResult result, JsonObject oldData, JsonObject newData, Enum<?> entityType, Enum<?> entityKey, EntityValidator predicate) {
    HashMap<String, JsonObject> oldMap = new HashMap<String, JsonObject>();
    HashMap<String, JsonObject> newMap = new HashMap<String, JsonObject>();
    JsonArray oldArray = getAsArray(oldData, entityType);
    JsonArray newArray = getAsArray(newData, entityType);
    if (oldArray!=null) for (JsonElement el: oldArray) {
      JsonObject obj = (JsonObject) el;
      oldMap.put(get(obj, entityKey).getAsString(), obj);
    }
    if (newArray!=null) for (JsonElement el: newArray) {
      JsonObject obj = (JsonObject) el;
      newMap.put(get(obj, entityKey).getAsString(), obj);
    }
    for (String id: oldMap.keySet()) {
      predicate.evaluate(result, entityType.name(), oldMap.get(id), newMap.get(id));
    }
  }


  private JsonObject clone(JsonObject source) {
    JsonObject dest = new JsonObject();
    for (Map.Entry<String, JsonElement> entry: source.entrySet()) {
      JsonArray values = entry.getValue().getAsJsonArray();
      JsonArray cloned = new JsonArray();
      cloned.addAll(values);
      dest.add(entry.getKey(), cloned);
    }
    return dest;
  }

  private void merge(JsonObject source, JsonObject dest) {
    for (Map.Entry<String, JsonElement> entry: source.entrySet()) {
      JsonArray values = entry.getValue().getAsJsonArray();
      if (dest.has(entry.getKey())) {
        dest.get(entry.getKey()).getAsJsonArray().addAll(values);
      } else {
        dest.add(entry.getKey(), values);
      }
    }
  }

  public static final class ArraySizeValidator implements ArrayValidator {
    @Override
    public void evaluate(CheckResult result, String entity, JsonArray oldData, JsonArray newData) {
      // check if collection exists in the new data:
      if (newData == null) {
        result.failures.add(new CheckFailure(entity, null, "Could not find entity in new data"));
        return;
      }
      // check if collection is 80% smaller than the old data:
      if (newData.size() < oldData.size()*0.8) {
        result.failures.add(new CheckFailure(entity, null, "80% or less entities of this type compared to previous"));
      }
    }
  }

  public static interface ArrayValidator {
    void evaluate(CheckResult result, String entity, JsonArray oldData, JsonArray newData);
  }

  public static interface EntityValidator {
    void evaluate(CheckResult result, String entity, JsonObject oldData, JsonObject newData);
  }


  public static class CheckFailure {
    String entity;
    String entityId;
    String failureReason;
    public CheckFailure(String entity, String entityId, String failureReason) {
      this.entity = entity;
      this.entityId = entityId;
      this.failureReason = failureReason;
    }

    @Override
    public String toString() {
      return
          entity==null?"":(entity+" ") +
          entityId==null?"":(entityId+" ") +
          failureReason==null?"":failureReason;
    }
  }

  public static class CheckResult {
    public ArrayList<CheckFailure> failures = new ArrayList<CheckFailure>();
  }

}
