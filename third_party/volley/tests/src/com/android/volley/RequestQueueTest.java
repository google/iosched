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

import com.android.volley.Request.Priority;
import com.android.volley.mock.MockNetwork;
import com.android.volley.mock.MockRequest;
import com.android.volley.toolbox.NoCache;
import com.android.volley.utils.CacheTestUtils;
import com.android.volley.utils.ImmediateResponseDelivery;

import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.LargeTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@LargeTest
public class RequestQueueTest extends InstrumentationTestCase {
    private ResponseDelivery mDelivery;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mDelivery = new ImmediateResponseDelivery();
    }

    /**
     * Make a list of requests with random priorities.
     * @param count Number of requests to make
     */
    private List<MockRequest> makeRequests(int count) {
        Request.Priority[] allPriorities = Request.Priority.values();
        Random random = new Random();

        List<MockRequest> requests = new ArrayList<MockRequest>();
        for (int i = 0; i < count; i++) {
            MockRequest request = new MockRequest();
            Request.Priority priority = allPriorities[random.nextInt(allPriorities.length)];
            request.setCacheKey(String.valueOf(i));
            request.setPriority(priority);
            requests.add(request);
        }
        return requests;
    }

    @UiThreadTest
    public void testAdd_requestProcessedInCorrectOrder() throws Exception {
        int requestsToMake = 100;

        OrderCheckingNetwork network = new OrderCheckingNetwork();
        RequestQueue queue = new RequestQueue(new NoCache(), network, 1, mDelivery);

        for (Request<?> request : makeRequests(requestsToMake)) {
            queue.add(request);
        }

        network.setExpectedRequests(requestsToMake);
        queue.start();
        network.waitUntilExpectedDone(2000); // 2 seconds
        queue.stop();
    }

    public void testAdd_dedupeByCacheKey() throws Exception {
        OrderCheckingNetwork network = new OrderCheckingNetwork();
        final AtomicInteger parsed = new AtomicInteger();
        final AtomicInteger delivered = new AtomicInteger();
        // Enqueue 2 requests with the same cache key. The first request takes 1.5s. Assert that the
        // second request is only handled after the first one has been parsed and delivered.
        DelayedRequest req1 = new DelayedRequest(1500, parsed, delivered);
        DelayedRequest req2 = new DelayedRequest(0, parsed, delivered) {
            @Override
            protected Response<Object> parseNetworkResponse(NetworkResponse response) {
                assertEquals(1, parsed.get());  // req1 must have been parsed.
                assertEquals(1, delivered.get());  // req1 must have been parsed.
                return super.parseNetworkResponse(response);
            }
        };
        network.setExpectedRequests(2);
        RequestQueue queue = new RequestQueue(new NoCache(), network, 3, mDelivery);
        queue.add(req1);
        queue.add(req2);
        queue.start();
        network.waitUntilExpectedDone(2000);
        queue.stop();
    }

    public void testCancelAll_onlyCorrectTag() throws Exception {
        MockNetwork network = new MockNetwork();
        RequestQueue queue = new RequestQueue(new NoCache(), network, 3, mDelivery);
        Object tagA = new Object();
        Object tagB = new Object();
        MockRequest req1 = new MockRequest();
        req1.setTag(tagA);
        MockRequest req2 = new MockRequest();
        req2.setTag(tagB);
        MockRequest req3 = new MockRequest();
        req3.setTag(tagA);
        MockRequest req4 = new MockRequest();
        req4.setTag(tagA);

        queue.add(req1); // A
        queue.add(req2); // B
        queue.add(req3); // A
        queue.cancelAll(tagA);
        queue.add(req4); // A

        assertTrue(req1.cancel_called); // A cancelled
        assertFalse(req2.cancel_called); // B not cancelled
        assertTrue(req3.cancel_called); // A cancelled
        assertFalse(req4.cancel_called); // A added after cancel not cancelled
    }

    private class OrderCheckingNetwork implements Network {
        private Priority mLastPriority = Priority.IMMEDIATE;
        private int mLastSequence = -1;
        private Semaphore mSemaphore;

        public void setExpectedRequests(int expectedRequests) {
            // Leave one permit available so the waiter can find it.
            expectedRequests--;
            mSemaphore = new Semaphore(-expectedRequests);
        }

        public void waitUntilExpectedDone(long timeout)
                throws InterruptedException, TimeoutError {
            if (mSemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS) == false) {
                throw new TimeoutError();
            }
        }

        @Override
        public NetworkResponse performRequest(Request<?> request) {
            Priority thisPriority = request.getPriority();
            int thisSequence = request.getSequence();

            int priorityDiff = thisPriority.compareTo(mLastPriority);

            // Should never experience a higher priority after a lower priority
            assertFalse(priorityDiff > 0);

            // If we're not transitioning to a new priority block, check sequence numbers
            if (priorityDiff == 0) {
                assertTrue(thisSequence > mLastSequence);
            }
            mLastSequence = thisSequence;
            mLastPriority = thisPriority;

            mSemaphore.release();
            return new NetworkResponse(new byte[16]);
        }
    }

    private class DelayedRequest extends Request<Object> {
        private final long mDelayMillis;
        private final AtomicInteger mParsedCount;
        private final AtomicInteger mDeliveredCount;

        public DelayedRequest(long delayMillis, AtomicInteger parsed, AtomicInteger delivered) {
            super(Request.Method.GET, "http://buganizer/", null);
            mDelayMillis = delayMillis;
            mParsedCount = parsed;
            mDeliveredCount = delivered;
        }

        @Override
        protected Response<Object> parseNetworkResponse(NetworkResponse response) {
            mParsedCount.incrementAndGet();
            SystemClock.sleep(mDelayMillis);
            return Response.success(new Object(), CacheTestUtils.makeRandomCacheEntry(null));
        }

        @Override
        protected void deliverResponse(Object response) {
            mDeliveredCount.incrementAndGet();
        }
    }

}
