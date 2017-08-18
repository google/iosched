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

package com.google.samples.apps.iosched.provider;

import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Provides helper methods for specifying query parameters on {@code Uri}s.
 */
public class ScheduleContractHelper {

    public static final String QUERY_PARAMETER_DISTINCT = "distinct";

    private static final String QUERY_PARAMETER_OVERRIDE_ACCOUNT_NAME = "overrideAccountName";

    private static final String QUERY_PARAMETER_CALLER_IS_SYNC_ADAPTER = "callerIsSyncAdapter";

    private static final String QUERY_PARAMETER_ACCOUNT_UPDATE_ALLOWED = "accountUpdateAllowed";


    public static boolean isUriCalledFromSyncAdapter(Uri uri) {
        return uri.getBooleanQueryParameter(QUERY_PARAMETER_CALLER_IS_SYNC_ADAPTER, false);
    }

    public static Uri setUriAsCalledFromSyncAdapter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(QUERY_PARAMETER_CALLER_IS_SYNC_ADAPTER, "true")
                .build();
    }

    public static boolean isQueryDistinct(Uri uri){
        return !TextUtils.isEmpty(uri.getQueryParameter(QUERY_PARAMETER_DISTINCT));
    }

    public static String formatQueryDistinctParameter(String parameter){
        return ScheduleContractHelper.QUERY_PARAMETER_DISTINCT + " " + parameter;
    }

    public static String getOverrideAccountName(Uri uri) {
        return uri.getQueryParameter(QUERY_PARAMETER_OVERRIDE_ACCOUNT_NAME);
    }

    /**
     * Adds an account override parameter to the {@code uri}. This is used by the
     * {@link ScheduleProvider} when fetching account-specific data.
     */
    public static Uri addOverrideAccountName(Uri uri, String accountName) {
        return uri.buildUpon().appendQueryParameter(
                QUERY_PARAMETER_OVERRIDE_ACCOUNT_NAME, accountName).build();
    }

    public static boolean isAccountUpdateAllowed(Uri uri) {
        String value = uri.getQueryParameter(QUERY_PARAMETER_ACCOUNT_UPDATE_ALLOWED);
        return value != null && "true".equals(value);
    }

    /**
     * Adds a parameter to indicate that account_name updates are allowed.
     * This is used by the {@link ScheduleProvider#update(Uri, ContentValues, String, String[])}
     * for specific updates after a sign-in.
     */
    public static Uri addOverrideAccountUpdateAllowed(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
                QUERY_PARAMETER_ACCOUNT_UPDATE_ALLOWED, "true").build();
    }
}
