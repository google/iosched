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

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.samples.apps.iosched.server.schedule.model.validator.Converters;
import com.google.iosched.test.TestHelper;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class DataModelHelperTest {

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

  }

  /**
   * Test method for {@link com.google.iosched.model.DataModelHelper#set(com.google.gson.JsonObject, java.lang.Enum, com.google.gson.JsonObject, java.lang.Enum, com.google.iosched.model.validator.Converter)}.
   * @throws IOException
   */
  @Test
  public void testSetSimple() throws IOException {
    JsonObject dest = new JsonObject();
    String originalValue = "testvalue";
    Enum<?> destKey = InputJsonKeys.VendorAPISource.Rooms.Name;
    JsonPrimitive value = new JsonPrimitive(originalValue);
    DataModelHelper.set(value, dest, destKey);
    assertEquals(1, dest.entrySet().size());
    assertEquals(originalValue, dest.get(destKey.name()).getAsString());
  }

  /**
   * Test method for {@link com.google.iosched.model.DataModelHelper#set(com.google.gson.JsonObject, java.lang.Enum, com.google.gson.JsonObject, java.lang.Enum, com.google.iosched.model.validator.Converter)}.
   * @throws IOException
   */
  @Test
  public void testSetSimpleFromComplex() throws IOException {
    JsonObject src = (JsonObject) TestHelper.readJsonTestDataFile("sample_topic.json");
    JsonObject dest = new JsonObject();
    Enum<?> srcKey = InputJsonKeys.VendorAPISource.Topics.Title;
    Enum<?> destKey = InputJsonKeys.VendorAPISource.Topics.Title;
    DataModelHelper.set(src, srcKey, dest, destKey);
    assertEquals("Enabling Blind and Low-Vision Accessibility On Android", dest.get(destKey.name()).getAsString());
  }

  /**
   * Test method for {@link com.google.iosched.model.DataModelHelper#set(com.google.gson.JsonObject, java.lang.Enum, com.google.gson.JsonObject, java.lang.Enum, com.google.iosched.model.validator.Converter)}.
   * @throws IOException
   */
  @Test
  public void testSetDateFromComplex() throws IOException {
    JsonObject src = (JsonObject) TestHelper.readJsonTestDataFile("sample_topic.json");
    JsonObject dest = new JsonObject();
    Enum<?> srcKey = InputJsonKeys.VendorAPISource.Topics.Start;
    Enum<?> destKey = InputJsonKeys.VendorAPISource.Topics.Start;
    DataModelHelper.set(src, srcKey, dest, destKey, Converters.DATETIME);
    assertEquals("2013-05-16T21:00:00Z", dest.get(destKey.name()).getAsString());
  }


  /**
   * Test method for {@link com.google.iosched.model.DataModelHelper#set(com.google.gson.JsonObject, java.lang.Enum, com.google.gson.JsonObject, java.lang.Enum, com.google.iosched.model.validator.Converter)}.
   * @throws IOException
   */
  @Test
  public void testSetInComplexObject() throws IOException {
    JsonObject dest = (JsonObject) TestHelper.readJsonTestDataFile("sample_topic.json");
    String originalValue = "testvalue";
    Enum<?> destKey = InputJsonKeys.VendorAPISource.Rooms.Name;
    JsonPrimitive value = new JsonPrimitive(originalValue);
    DataModelHelper.set(value, dest, destKey);
    assertEquals(originalValue, dest.get(destKey.name()).getAsString());
  }

  /**
   * Test method for {@link com.google.iosched.model.DataModelHelper#maybeFixPropertyName(java.lang.String)}.
   */
  @Test
  public void testMaybeFixPropertyName() {
    assertEquals("property1", DataModelHelper.maybeFixPropertyName("_property1"));
    assertEquals("property1", DataModelHelper.maybeFixPropertyName("property1"));
    assertEquals("_property1", DataModelHelper.maybeFixPropertyName("__property1"));
    assertEquals("$_property1", DataModelHelper.maybeFixPropertyName("$_property1"));
    assertEquals("property1_", DataModelHelper.maybeFixPropertyName("property1_"));
  }

}
