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

package com.google.samples.apps.iosched.archframework;

/**
 * Helper methods to create instances of {@link QueryEnum} amd {@link UserActionEnum}.
 */
public class ArchFrameworkHelperForTest {

    public static QueryEnum createQueryEnumWithId(final int id) {
        return new QueryEnum() {
            @Override
            public int getId() {
                return id;
            }

            @Override
            public String[] getProjection() {
                return new String[0];
            }
        };
    }

    public static UserActionEnum createUserActionEnumWithId(final int id) {
        return new UserActionEnum() {
            @Override
            public int getId() {
                return id;
            }
        };
    }
}
