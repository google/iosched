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
package com.google.samples.apps.iosched.archframework;

/**
 * Provides static methods to help with processing {@link QueryEnum}.
 */
public class QueryEnumHelper {

    /**
     * Converts an integer id to the corresponding {@link QueryEnum}.
     * <p/>
     * Typically, used to convert the loaderId, as provided by the {@link
     * android.app.LoaderManager}.
     *
     * @param id    The id of the query.
     * @param enums The list of possible {@link QueryEnum}.
     * @return the {@link QueryEnum} with the given id or null if none found.
     */
    public static QueryEnum getQueryForId(int id, QueryEnum[] enums) {
        QueryEnum match = null;
        if (enums != null) {
            for (int i = 0; i < enums.length; i++) {
                if (enums[i] != null && id == enums[i].getId()) {
                    match = enums[i];
                }
            }
        }
        return match;
    }
}
