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
package com.google.samples.apps.iosched.fcm;

import android.content.Context;

/**
 * Represents the client response when an FCM ping is received. Each type of FCM ping should have
 * an FcmCommand implementation associated with it.
 */
public abstract class FcmCommand {

    /**
     * Defines behavior when FCM is received.
     */
    public abstract void execute(Context context, String type, String extraData);
}