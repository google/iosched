/*
 * Copyright 2017 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.server.schedule.server.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit test for ImageUpdaterTest
 */
public class ImageUpdaterTest {

  @Test
  public void testParseFileExtFromUrl() throws Exception {
    assertNull(ImageUpdater.parseFileExtFromUrl(null));
    assertNull(ImageUpdater.parseFileExtFromUrl(""));

    assertEquals("jpg",
        ImageUpdater.parseFileExtFromUrl("https://googleapis.com/photos/ae8400a9.jpg"));
    assertEquals("png",
        ImageUpdater.parseFileExtFromUrl("https://googleapis.com/photos/ae8400a9.png"));
    assertEquals("jpg", ImageUpdater
        .parseFileExtFromUrl("https://googleapis.com/photos/ae8400a9.jpg?ts=636256372153900000"));
    assertEquals("gif", ImageUpdater
        .parseFileExtFromUrl("https://googleapis.com/photos/ae8400a9.gif?ts=636256372153900000"));
    assertEquals("jpg", ImageUpdater
        .parseFileExtFromUrl("https://googleapis.com/photos/ae8400a9?ts=636256372153900000"));
  }
}
