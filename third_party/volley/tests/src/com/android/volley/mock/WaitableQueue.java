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

package com.android.volley.mock;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// TODO: the name of this class sucks
@SuppressWarnings("serial")
public class WaitableQueue extends PriorityBlockingQueue<Request<?>> {
    private final Request<?> mStopRequest = new MagicStopRequest();
    private final Semaphore mStopEvent = new Semaphore(0);

    // TODO: this isn't really "until empty" it's "until next call to take() after empty"
    public void waitUntilEmpty(long timeoutMillis)
            throws TimeoutException, InterruptedException {
        add(mStopRequest);
        if (!mStopEvent.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException();
        }
    }

    @Override
    public Request<?> take() throws InterruptedException {
        Request<?> item = super.take();
        if (item == mStopRequest) {
            mStopEvent.release();
            return take();
        }
        return item;
    }

    private static class MagicStopRequest extends Request<Object> {
        public MagicStopRequest() {
            super(Request.Method.GET, "", null);
        }

        @Override
        public Priority getPriority() {
            return Priority.LOW;
        }

        @Override
        protected Response<Object> parseNetworkResponse(NetworkResponse response) {
            return null;
        }

        @Override
        protected void deliverResponse(Object response) {
        }
    }
}
