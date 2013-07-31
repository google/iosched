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

package com.google.android.apps.iosched.io;

import com.google.android.apps.iosched.io.model.SearchSuggestions;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.Lists;
import com.google.gson.Gson;

import android.app.SearchManager;
import android.content.ContentProviderOperation;
import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;

public class SearchSuggestHandler extends JSONHandler {

    public SearchSuggestHandler(Context context) {
        super(context);
    }

    public ArrayList<ContentProviderOperation> parse(String json)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        SearchSuggestions suggestions = new Gson().fromJson(json, SearchSuggestions.class);
        if (suggestions.words != null) {
            // Clear out suggestions
            batch.add(ContentProviderOperation
                    .newDelete(ScheduleContract.addCallerIsSyncAdapterParameter(
                            ScheduleContract.SearchSuggest.CONTENT_URI))
                    .build());

            // Rebuild suggestions
            for (String word : suggestions.words) {
                batch.add(ContentProviderOperation
                        .newInsert(ScheduleContract.addCallerIsSyncAdapterParameter(
                                ScheduleContract.SearchSuggest.CONTENT_URI))
                        .withValue(SearchManager.SUGGEST_COLUMN_TEXT_1, word)
                        .build());
            }
        }

        return batch;
    }
}
