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
package com.google.samples.apps.iosched.server.schedule.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.EntityFetcher;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.VendorAPIEntityFetcher;
import com.google.samples.apps.iosched.server.schedule.model.InputJsonKeys.VendorAPISource.MainTypes;
import com.google.samples.apps.iosched.server.schedule.server.input.VendorDynamicInput;
import com.google.iosched.test.TestHelper;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;

public class VendorInputTest {

  private EntityFetcher fakeFetcher;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    fakeFetcher = new EntityFetcher() {

      @Override
      public JsonElement fetch(Enum<?> entityType, Map<String, String> params) throws IOException {
        String filename = "sample_"+entityType.name();
        if (params != null && params.get("page") != null
              && Integer.parseInt(params.get("page")) > 1) {
          filename+="_page"+params.get("page");
        }
        filename+=".json";
        InputStream stream = TestHelper.openTestDataFileStream(filename);
        JsonReader reader = new JsonReader(new InputStreamReader(stream, Charset.forName("UTF-8")));
        return new JsonParser().parse(reader);
      }
    };
  }

  @Test
  public void testFetch() throws IOException {
    VendorDynamicInput api = new VendorDynamicInput(fakeFetcher);
    JsonArray categories = api.fetch(MainTypes.categories);
    JsonArray rooms = api.fetch(MainTypes.rooms);
    JsonArray speakers = api.fetch(MainTypes.speakers);
    JsonArray topics = api.fetch(MainTypes.topics);

    assertEquals(8, rooms.size());
    assertEquals(28, categories.size());
    assertEquals(44, speakers.size());
    assertEquals(55, topics.size());
  }


  /**
   *
   * This is the real remote fetch. Doesn't fit well as a unit test, though, but it's here to
   * help quickly identifying issues in the remote API. This test is only run if the vender
   * base url is set.
   *
   * @throws IOException
   */
  @Test
  public void testRemoteFetch() throws IOException {
    if (!VendorAPIEntityFetcher.BASE_URL.equals("UNDEFINED")) {
      VendorDynamicInput api = new VendorDynamicInput();
      JsonArray categories = api.fetch(MainTypes.categories);
      JsonArray rooms = api.fetch(MainTypes.rooms);
      JsonArray speakers = api.fetch(MainTypes.speakers);
      JsonArray topics = api.fetch(MainTypes.topics);
      assertNotNull(categories);
      assertNotNull(rooms);
      assertNotNull(speakers);
      assertNotNull(topics);
    }
  }
}
