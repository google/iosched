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
package com.google.samples.apps.iosched.session.data;

import android.support.annotation.Keep;

import com.google.firebase.database.PropertyName;

@Keep
public class QueueAction {
    public String session;
    public String action;

    @PropertyName("request_id")
    public String requestId;

    public QueueAction() {
    }

    public QueueAction(String session, String action, String requestId) {
        this.session = session;
        this.action = action;
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "QueueAction{" +
                "session='" + session + '\'' +
                ", action='" + action + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}