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

package com.google.samples.apps.iosched.messaging;

/**
 * Implements this for messaging service. A messaging service is a service enabling a backend server
 * to send messages to the app, and the device needs to be registered with it. Additionally, the
 * device needs to be registered with the backend server that is actually responsible for deciding
 * when/which message to send.
 */
public interface MessagingRegistration {

    /**
     * Implements registering the device with the messaging service that will take care of
     * delivering the messages send by the backend server, if required, and registering the device
     * with the messaging service that will take care of delivering the messages send by the backend
     * server, if required.
     */
    void registerDevice();

    /**
     * Implements un-registering {@code accountName} with the device.
     */
    void unregisterDevice(String accountName);

    /**
     * Implements to canceling any operation in progress.
     */
    void destroy();
}
