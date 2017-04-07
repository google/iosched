/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.google.samples.apps.iosched.feed;

import com.google.firebase.database.DatabaseReference;
import com.google.samples.apps.iosched.feed.data.FeedMessage;

import java.util.List;

public interface FeedContract {

    /**
     * View for Feed. Responsible for updating UI.
     */
    interface View {
        void setPresenter(FeedContract.Presenter presenter);

        void showErrorMessage();

        void addFeedMessage(FeedMessage feedMessage);

        void updateFeedMessage(FeedMessage feedMessage);

        void removeFeedMessage(FeedMessage feedMessage);
    }

    /**
     * Presenter for Feed. Middle man between repository (Firebase DB) and Feed View.
     */
    interface Presenter {
        void initializeDataListener(DatabaseReference databaseReference);

        void removeDataListener(DatabaseReference databaseReference);
    }
}
