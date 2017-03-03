/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.io.model;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

import org.junit.Test;

@SmallTest
public class CardTest {
    @Test
    public void testDateParsing_knownGoodTimeSet() {
        long millis = Card.getEpochMillisFromTimeString("2016-05-07T12:24:24-0700");
        Assert.assertTrue("Known good time comparison failed.", millis == 1462649064000L);
    }
}
