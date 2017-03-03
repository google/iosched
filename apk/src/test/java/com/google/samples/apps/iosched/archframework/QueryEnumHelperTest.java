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

import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class QueryEnumHelperTest {

    private QueryEnum mQueryToMatch;

    private int mIdOfQueryToMatch = 1;

    private QueryEnum[] mQueries;

    @Test
    public void getQueryForId_nullQueries_returnsNull() {
        // Given a null array of queries and a query to match
        mQueries = null;
        mQueryToMatch = createQueryEnumWithId(mIdOfQueryToMatch);

        // When getting the matching query
        QueryEnum match = QueryEnumHelper.getQueryForId(mIdOfQueryToMatch, mQueries);

        // Then the match is null
        assertNull(match);
    }

    @Test
    public void getQueryForId_emptyQueries_returnsNull() {
        // Given an empty array of queries and a query to match
        mQueries = new QueryEnum[0];
        mQueryToMatch = createQueryEnumWithId(mIdOfQueryToMatch);

        // When getting the matching query
        QueryEnum match = QueryEnumHelper.getQueryForId(mIdOfQueryToMatch, mQueries);

        // Then the match is null
        assertNull(match);
    }

    @Test
    public void getQueryForId_matchingQuery_returnsQuery() {
        // Given an array of queries containing the query to match and a null query
        mQueries = new QueryEnum[2];
        mQueryToMatch = createQueryEnumWithId(mIdOfQueryToMatch);
        mQueries[0] = null;
        mQueries[1] = mQueryToMatch;

        // When getting the matching query
        QueryEnum match = QueryEnumHelper.getQueryForId(mIdOfQueryToMatch, mQueries);

        // Then the match is found
        assertEquals(match, mQueryToMatch);
    }

    @Test
    public void getQueryForId_noMatchingQuery_returnsNull() {
        // Given an array of queries not containing the query to match
        mQueries = new QueryEnum[1];
        mQueryToMatch = createQueryEnumWithId(mIdOfQueryToMatch);
        mQueries[0] = createQueryEnumWithId(mIdOfQueryToMatch + 1);

        // When getting the matching query
        QueryEnum match = QueryEnumHelper.getQueryForId(mIdOfQueryToMatch, mQueries);

        // Then the match is not found
        assertNull(match);
    }

    private QueryEnum createQueryEnumWithId(final int id) {
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
}
