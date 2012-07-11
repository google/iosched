/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.io.model;

import com.google.gson.JsonElement;

public class GenericResponse {
    public JsonElement error;
//
//    public void checkResponseForAuthErrorsAndThrow() throws IOException {
//        if (error != null && error.isJsonObject()) {
//            JsonObject errorObject = error.getAsJsonObject();
//            int errorCode = errorObject.get("code").getAsInt();
//            String errorMessage = errorObject.get("message").getAsString();
//            if (400 <= errorCode && errorCode < 500) {
//                // The API currently only returns 400 unfortunately.
//                throw ...
//            }
//        }
//    }
}
