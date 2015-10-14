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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.EntityFetcher;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.RemoteFilesEntityFetcherFactory;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.RemoteFilesEntityFetcherFactory.FetcherBuilder;
import com.google.samples.apps.iosched.server.schedule.model.InputJsonKeys.ExtraSource.MainTypes;
import com.google.samples.apps.iosched.server.schedule.model.JsonDataSources;
import com.google.samples.apps.iosched.server.schedule.server.input.ExtraInput;
import com.google.iosched.test.TestHelper;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;

public class ExtraInputTest {

  private EntityFetcher fakeFetcher;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    fakeFetcher = new EntityFetcher() {

      @Override
      public JsonElement fetch(Enum<?> entityType, Map<String, String> params) throws IOException {
        String filename = "sample_"+entityType.name()+".json";
        InputStream stream = TestHelper.openTestDataFileStream(filename);
        assertNotNull("open file "+filename, stream);
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
  }

  @Test
  public void testFetch() throws IOException {
    ExtraInput api = new ExtraInput();
    JsonDataSources dataSources = api.fetchAllDataSources();
    assertNotNull(dataSources.getSource(MainTypes.tag_conf.name()));
    assertNotNull(dataSources.getSource(MainTypes.tag_category_mapping.name()));

    assertEquals(13, dataSources.getSource(MainTypes.tag_conf.name()).size());
    assertEquals(3, dataSources.getSource(MainTypes.tag_category_mapping.name()).size());
  }

}
