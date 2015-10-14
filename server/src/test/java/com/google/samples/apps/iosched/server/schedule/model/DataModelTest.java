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

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.samples.apps.iosched.server.schedule.model.InputJsonKeys.VendorAPISource;
import com.google.iosched.test.TestHelper;

import org.junit.Before;
import org.junit.Test;

public class DataModelTest {

  private JsonDataSources sources;
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    sources = new JsonDataSources();
    sources.addSource(
        new JsonDataSource(VendorAPISource.MainTypes.rooms,
        TestHelper.readJsonTestDataFile("sample_rooms.json").getAsJsonArray()));
    sources.addSource(
        new JsonDataSource(VendorAPISource.MainTypes.categories,
        TestHelper.readJsonTestDataFile("sample_categories.json").getAsJsonArray()));
    JsonElement el = TestHelper.readJsonTestDataFile("sample_speakers.json");
    sources.addSource(
        new JsonDataSource(VendorAPISource.MainTypes.speakers,
            el.getAsJsonObject().get("results").getAsJsonArray()));
    el = TestHelper.readJsonTestDataFile("sample_topics.json");
    sources.addSource(
        new JsonDataSource(VendorAPISource.MainTypes.topics,
            el.getAsJsonObject().get("results").getAsJsonArray()));
  }

  /**
   * Test method for {@link com.google.iosched.model.DataExtractor#extractFromDataSources(com.google.iosched.model.JsonDataSources)}.
   */
  @Test
  public void testExtractFromDataSources() {
    assertEquals(8, sources.getSource(VendorAPISource.MainTypes.rooms.name()).size());
    assertEquals(28, sources.getSource(VendorAPISource.MainTypes.categories.name()).size());
    assertEquals(44, sources.getSource(VendorAPISource.MainTypes.speakers.name()).size());
    assertEquals(55, sources.getSource(VendorAPISource.MainTypes.topics.name()).size());
  }

  /**
   * Test method for {@link com.google.iosched.model.DataExtractor#extractRooms(com.google.iosched.model.JsonDataSources)}.
   */
  @Test
  public void testExtractRooms() {
    JsonArray rooms = new DataExtractor(false).extractRooms(sources);
    assertEquals(8, rooms.size());
    JsonObject firstRoom = rooms.get(0).getAsJsonObject();
    String roomName = firstRoom.get(OutputJsonKeys.Rooms.name.name()).getAsString();
    assertEquals(true, roomName.startsWith("Room "));
  }

}
