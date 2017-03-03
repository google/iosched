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

package com.google.samples.apps.iosched.injection;

import android.app.Activity;

import com.google.samples.apps.iosched.messaging.MessagingRegistration;
import com.google.samples.apps.iosched.messaging.MessagingRegistrationWithGCM;

/**
 * Provides a way to inject stub classes when running integration tests.
 */
public class MessagingRegistrationProvider {

    private static MessagingRegistration stubMessagingRegistration;

    public static void setStubMessagingRegistration(MessagingRegistration messaging) {
        stubMessagingRegistration= messaging;
    }

    public static MessagingRegistration provideMessagingRegistration(Activity activity) {
        if (stubMessagingRegistration != null) {
            return stubMessagingRegistration;
        } else {
            return new MessagingRegistrationWithGCM(activity);
        }
    }
}
