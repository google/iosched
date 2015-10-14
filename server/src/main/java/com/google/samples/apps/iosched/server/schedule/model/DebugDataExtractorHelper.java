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

import static com.google.samples.apps.iosched.server.schedule.model.DataModelHelper.set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;


/**
 * When running on DEBUG mode, change some data in the sessions.
 *
 */
public class DebugDataExtractorHelper {

  protected static int[][] sessionTimes = {
    {9,30,9,45}, {9,15, 9,55}, {10,0, 11,0}, {11,5,11,55},
    {13,0,13,55}, {14,5,15,0}, {14,30,15,0}, {15,0, 16,0}, {16,5,17,0}
  };
  protected static int[] days = {25, 26};

  protected static String[] rooms = {"110", "120", "130", "210", "220", "230"};

  protected static HashMap<String, JsonObject> tagById;
  protected static ArrayList<JsonObject> topicTags;
  protected static JsonObject[] themeTags = {
    new JsonParser().parse("{\"category\": \"THEME\",\"tag\": \"THEME_DESIGN\",\"name\": \"Design\", \"order_in_category\": 1, \"abstract\": \"\"}").getAsJsonObject(),
    new JsonParser().parse("{\"category\": \"THEME\",\"tag\": \"THEME_DEVELOP\",\"name\": \"Develop\", \"order_in_category\": 2, \"abstract\": \"\"}").getAsJsonObject(),
    new JsonParser().parse("{\"category\": \"THEME\",\"tag\": \"THEME_DISTRIBUTE\",\"name\": \"Distribute\", \"order_in_category\": 3, \"abstract\": \"\"}").getAsJsonObject(),
  };
  protected static float[] themeDistribution = {0.25f, 0.5f, 0.25f};

  protected static JsonObject[] typeTags = {
    new JsonParser().parse("{\"category\": \"TYPE\",\"tag\": \"TYPE_SESSION\",\"name\": \"Session\", \"order_in_category\": 1, \"abstract\": \"\"}").getAsJsonObject(),
    new JsonParser().parse("{\"category\": \"TYPE\",\"tag\": \"TYPE_CODELAB\",\"name\": \"Codelab\", \"order_in_category\": 2, \"abstract\": \"\"}").getAsJsonObject(),
    new JsonParser().parse("{\"category\": \"TYPE\",\"tag\": \"TYPE_OFFICE_HOURS\",\"name\": \"Office Hours\", \"order_in_category\": 3, \"abstract\": \"\"}").getAsJsonObject(),
    new JsonParser().parse("{\"category\": \"TYPE\",\"tag\": \"TYPE_BOX_TALKS\",\"name\": \"'Box Talks\", \"order_in_category\": 4, \"abstract\": \"\"}").getAsJsonObject(),
  };

  protected static float[] typeDistribution = {0.70f, 0.1f, 0.1f, 0.1f};

  private static Random r = new Random();

  public static final void changeRooms(JsonArray allRooms) {
    for (String room: rooms) {
      JsonObject dest = new JsonObject();
      JsonPrimitive roomId = new JsonPrimitive(room);
      JsonPrimitive roomName = new JsonPrimitive("Room "+room);
      DataModelHelper.set(roomId, dest, OutputJsonKeys.Rooms.id);
      DataModelHelper.set(roomName, dest, OutputJsonKeys.Rooms.name);
      allRooms.add(dest);
    }
  }

  public static final void changeCategories(HashMap<String, JsonObject> categoryToTagMap, JsonArray allTags) {
    topicTags = new ArrayList<JsonObject>();
    Iterator<Map.Entry<String, JsonObject>> it = categoryToTagMap.entrySet().iterator();
    while (it.hasNext()) {
      JsonObject tag = it.next().getValue();
      String category = tag.get(OutputJsonKeys.Tags.category.name()).getAsString();
      if ("TOPIC".equals(category)) {
        topicTags.add(tag);
      } else {
        it.remove();
      }
    }
    for (JsonObject tag: themeTags) {
      allTags.add(tag);
    }
    for (JsonObject tag: typeTags) {
      allTags.add(tag);
    }
  }

  static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

  public static final void changeSession(JsonObject session, Set<String> usedTags) {
    int hash = session.get(OutputJsonKeys.Sessions.id.name()).getAsString().hashCode();
    r.setSeed(hash);

    // timeslot:
    int day = days[uniform(2)];
    int[] timeSlot = sessionTimes[uniform(sessionTimes.length)];
    Calendar start = new GregorianCalendar(2014, Calendar.JUNE, day, timeSlot[0], timeSlot[1], 0);
    Calendar end = new GregorianCalendar(2014, Calendar.JUNE, day, timeSlot[2], timeSlot[3], 0);
    long offset = TimeZone.getTimeZone("PST").getOffset(start.getTimeInMillis());
    start.setTimeInMillis(start.getTimeInMillis()-offset);
    end.setTimeInMillis(end.getTimeInMillis()-offset);
    String startS = formatter.format(start.getTime());
    String endS = formatter.format(end.getTime());

    DataModelHelper.set(new JsonPrimitive(startS), session, OutputJsonKeys.Sessions.startTimestamp);
    DataModelHelper.set(new JsonPrimitive(endS), session, OutputJsonKeys.Sessions.endTimestamp);

    // Room:
    DataModelHelper.set(new JsonPrimitive(rooms[uniform(rooms.length)]), session, OutputJsonKeys.Sessions.room);

    JsonArray tags = new JsonArray();

    // 2 random topic tags
    Collections.shuffle(topicTags, r); // not the most efficient, but good enough and avoid duplicates
    if (topicTags.size()>0) tags.add(topicTags.get(0).get(OutputJsonKeys.Tags.tag.name()));
    if (topicTags.size()>1) tags.add(topicTags.get(1).get(OutputJsonKeys.Tags.tag.name()));

    // 1 randomly distributed theme tag
    tags.add(themeTags[roullette(themeDistribution)].get(OutputJsonKeys.Tags.tag.name()));

    // 1 randomly distributed type tag
    tags.add(typeTags[roullette(typeDistribution)].get(OutputJsonKeys.Tags.tag.name()));

    for (JsonElement tag: tags) {
      usedTags.add(tag.getAsString());
    }
    DataModelHelper.set(tags, session, OutputJsonKeys.Sessions.tags);

    // Livestream
    boolean isLiveStream = uniform(2)==1;
    if (isLiveStream) {
      DataModelHelper.set(new JsonPrimitive("https://www.youtube.com/watch?v=dQw4w9WgXcQ"), session, OutputJsonKeys.Sessions.youtubeUrl);
      DataModelHelper.set(new JsonPrimitive("http://www.google.com/humans.txt"), session, OutputJsonKeys.Sessions.captionsUrl);
      DataModelHelper.set(new JsonPrimitive(Boolean.TRUE), session, OutputJsonKeys.Sessions.isLivestream);
    } else {
      session.remove(OutputJsonKeys.Sessions.youtubeUrl.name());
      session.remove(OutputJsonKeys.Sessions.captionsUrl.name());
      DataModelHelper.set(new JsonPrimitive(Boolean.FALSE), session, OutputJsonKeys.Sessions.isLivestream);
    }
  }

  private static int uniform(int upperBound) {
    return r.nextInt(upperBound);
  }

  private static int roullette(float[] distributions) {
    float value = r.nextFloat();
    float current = 0f;
    for (int i=0; i<distributions.length; i++) {
      current+=distributions[i];
      if (value <= current) {
        return i;
      }
    }
    return distributions.length-1;
  }
}
