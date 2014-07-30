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

package com.google.samples.apps.iosched.io;

import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import android.app.SearchManager;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class SearchSuggestHandler extends JSONHandler {
    private static final String TAG = makeLogTag(SpeakersHandler.class);
    HashSet<String> mSuggestions = new HashSet<String>();

    public SearchSuggestHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (String word : new Gson().fromJson(element, String[].class)) {
            mSuggestions.add(word);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.SearchSuggest.CONTENT_URI);

        list.add(ContentProviderOperation.newDelete(uri).build());
        for (String word : mSuggestions) {
            list.add(ContentProviderOperation.newInsert(uri)
                .withValue(SearchManager.SUGGEST_COLUMN_TEXT_1, word)
                .build());
        }
    }
}
