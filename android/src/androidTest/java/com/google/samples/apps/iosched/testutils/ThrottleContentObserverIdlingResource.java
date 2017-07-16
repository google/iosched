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

package com.google.samples.apps.iosched.testutils;

import android.content.Context;
import android.support.test.espresso.IdlingResource;

import com.google.samples.apps.iosched.util.ThrottledContentObserver;
import com.google.samples.apps.iosched.util.TimeUtils;

/**
 * An {@link IdlingResource} that waits for the {@link ThrottledContentObserver#THROTTLE_DELAY}.
 */
public class ThrottleContentObserverIdlingResource implements IdlingResource {

    private long mStartTime;

    private long mElapsedTime;

    private Context mContext;

    private ResourceCallback resourceCallback;

    public ThrottleContentObserverIdlingResource(Context context) {
        mStartTime = TimeUtils.getCurrentTime(context);
        mContext = context;
    }

    @Override
    public String getName() {
        return ThrottleContentObserverIdlingResource.class.getName();
    }

    @Override
    public boolean isIdleNow() {
        mElapsedTime = TimeUtils.getCurrentTime(mContext) - mStartTime;
        boolean idle = mElapsedTime > ThrottledContentObserver.getThrottleDelay();
        if (idle) {
            resourceCallback.onTransitionToIdle();
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }

}
