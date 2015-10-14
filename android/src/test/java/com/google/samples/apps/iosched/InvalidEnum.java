/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched;

import com.google.samples.apps.iosched.framework.QueryEnum;
import com.google.samples.apps.iosched.framework.UserActionEnum;

/**
* Enum used for testing.
*/
public enum InvalidEnum implements QueryEnum, UserActionEnum {
    INVALID(-1, null),;

    private int id;

    private String[] projection;

    InvalidEnum(int id, String[] projection) {
        this.id = id;
        this.projection = projection;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String[] getProjection() {
        return projection;
    }
}
