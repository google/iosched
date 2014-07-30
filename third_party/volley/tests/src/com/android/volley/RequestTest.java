/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.volley;

import android.test.suitebuilder.annotation.SmallTest;

import com.android.volley.Request.Priority;

import junit.framework.TestCase;

@SmallTest
public class RequestTest extends TestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCompareTo() {
        int sequence = 0;
        TestRequest low = new TestRequest(Priority.LOW);
        low.setSequence(sequence++);
        TestRequest low2 = new TestRequest(Priority.LOW);
        low2.setSequence(sequence++);
        TestRequest high = new TestRequest(Priority.HIGH);
        high.setSequence(sequence++);
        TestRequest immediate = new TestRequest(Priority.IMMEDIATE);
        immediate.setSequence(sequence++);

        // "Low" should sort higher because it's really processing order.
        assertTrue(low.compareTo(high) > 0);
        assertTrue(high.compareTo(low) < 0);
        assertTrue(low.compareTo(low2) < 0);
        assertTrue(low.compareTo(immediate) > 0);
        assertTrue(immediate.compareTo(high) < 0);
    }

    private class TestRequest extends Request<Object> {
        private Priority mPriority = Priority.NORMAL;
        public TestRequest(Priority priority) {
            super(Request.Method.GET, "", null);
            mPriority = priority;
        }

        @Override
        public Priority getPriority() {
            return mPriority;
        }

        @Override
        protected void deliverResponse(Object response) {
        }

        @Override
        protected Response<Object> parseNetworkResponse(NetworkResponse response) {
            return null;
        }
    }
}
