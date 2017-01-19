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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.EntityFetcher;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.RemoteFilesEntityFetcherFactory;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.RemoteFilesEntityFetcherFactory.FetcherBuilder;
import com.google.samples.apps.iosched.server.schedule.server.input.ExtraInput;
import com.google.samples.apps.iosched.server.schedule.server.input.VendorDynamicInput;
import com.google.iosched.test.TestHelper;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;

public class DataExtractorTest {

  private EntityFetcher fakeFetcher;
  private JsonDataSources sources;

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

    RemoteFilesEntityFetcherFactory.setBuilder(new FetcherBuilder() {
      @Override
      public FetcherBuilder setSourceFiles(String... filenames) {
        return this;
      }

      @Override
      public EntityFetcher build() {
        return fakeFetcher;
      }
    });

    sources = new ExtraInput().fetchAllDataSources();
    sources.putAll(new VendorDynamicInput(fakeFetcher).fetchAllDataSources());
  }

  /**
   * Test method for {@link com.google.iosched.model.DataExtractor#extractFromDataSources(com.google.iosched.model.JsonDataSources)}.
   */
  @Test
  public void testExtractFromDataSources() {
    assertNotNull(new DataExtractor(false).extractFromDataSources(sources));
  }

  @Test
  public void testHasMainTag() {
    JsonObject newData = new DataExtractor(false).extractFromDataSources(sources);
    JsonElement mainTag = newData.get(OutputJsonKeys.MainTypes.sessions.name()).getAsJsonArray()
        .get(0).getAsJsonObject().get(OutputJsonKeys.Sessions.mainTag.name());
    assertNotNull(mainTag);
    assertTrue(mainTag.getAsString().startsWith("TOPIC"));
  }

  @Test
  public void testHasHashTag() {
    JsonObject newData = new DataExtractor(false).extractFromDataSources(sources);
    JsonElement hashtag = newData.get(OutputJsonKeys.MainTypes.sessions.name()).getAsJsonArray()
        .get(0).getAsJsonObject().get(OutputJsonKeys.Sessions.hashtag.name());
    assertNotNull(hashtag);
    assertFalse(hashtag.getAsString().startsWith("TOPIC"));
    assertFalse(hashtag.getAsString().startsWith("THEME"));
    assertFalse(hashtag.getAsString().startsWith("TYPE"));
  }

}
