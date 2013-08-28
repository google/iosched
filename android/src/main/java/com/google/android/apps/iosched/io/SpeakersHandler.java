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

import android.content.ContentProviderOperation;
import android.content.Context;
import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.SyncColumns;
import com.google.android.apps.iosched.util.Lists;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.googledevelopers.Googledevelopers;
import com.google.api.services.googledevelopers.model.PresenterResponse;
import com.google.api.services.googledevelopers.model.PresentersResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.apps.iosched.provider.ScheduleContract.Speakers;
import static com.google.android.apps.iosched.util.LogUtils.*;

public class SpeakersHandler {

    private static final String TAG = makeLogTag(SpeakersHandler.class);

    public SpeakersHandler(Context context) {}

    public ArrayList<ContentProviderOperation> fetchAndParse(
            Googledevelopers conferenceAPI)
            throws IOException {
        PresentersResponse presenters;

        try {
            presenters = conferenceAPI.events().presenters().list(Config.EVENT_ID).execute();

            if (presenters == null || presenters.getPresenters() == null) {
                throw new HandlerException("Speakers list was null.");
            }
        } catch (HandlerException e) {
            LOGE(TAG, "Error fetching speakers", e);
            return Lists.newArrayList();
        }

        return buildContentProviderOperations(presenters);
    }

    public ArrayList<ContentProviderOperation> parseString(String json) {
        JsonFactory jsonFactory = new AndroidJsonFactory();
        try {
            PresentersResponse presenters = jsonFactory.fromString(json, PresentersResponse.class);
            return buildContentProviderOperations(presenters);
        } catch (IOException e) {
            LOGE(TAG, "Error reading speakers from packaged data", e);
            return Lists.newArrayList();
        }
    }

    private ArrayList<ContentProviderOperation> buildContentProviderOperations(
            PresentersResponse presenters) {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        if (presenters != null) {
            List<PresenterResponse> presenterList = presenters.getPresenters();
            int numSpeakers = presenterList.size();

            if (numSpeakers > 0) {
                LOGI(TAG, "Updating presenters data");

                // Clear out existing speakers
                batch.add(ContentProviderOperation.newDelete(
                        ScheduleContract.addCallerIsSyncAdapterParameter(
                                Speakers.CONTENT_URI))
                        .build());

                // Insert latest speaker data
                for (PresenterResponse presenter : presenterList) {
                    // Hack: Fix speaker URL so that it's not being resized
                    // Depends on thumbnail URL being exactly in the format we want
                    String thumbnail = presenter.getThumbnailUrl();
                    if (thumbnail != null) {
                        thumbnail = thumbnail.replace("?sz=50", "?sz=100");
                    }

                    batch.add(ContentProviderOperation.newInsert(ScheduleContract
                            .addCallerIsSyncAdapterParameter(Speakers.CONTENT_URI))
                            .withValue(SyncColumns.UPDATED, System.currentTimeMillis())
                            .withValue(Speakers.SPEAKER_ID, presenter.getId())
                            .withValue(Speakers.SPEAKER_NAME, presenter.getName())
                            .withValue(Speakers.SPEAKER_ABSTRACT, presenter.getBio())
                            .withValue(Speakers.SPEAKER_IMAGE_URL, thumbnail)
                            .withValue(Speakers.SPEAKER_URL, presenter.getPlusoneUrl())
                            .build());
                }
            }

        }
        return batch;
    }
}
