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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.samples.apps.iosched.io.model.Partner;
import com.google.samples.apps.iosched.provider.ScheduleContract;

import java.util.ArrayList;
import java.util.HashMap;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A class for transforming Partners JSON to equivalent operations on the content provider.
 *
 * Processes incoming json into a map of {partnerId: Partner, ...}, which can then be transformed
 * into a set of content provider operations that will make the DB mirror this data.
 */
public class PartnersHandler extends JSONHandler {
    private static final String TAG = makeLogTag(PartnersHandler.class);
    private HashMap<String, Partner> mPartners = new HashMap<String, Partner>();

    public PartnersHandler(Context context) {
        super(context);
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContract.addCallerIsSyncAdapterParameter(
                ScheduleContract.Partners.CONTENT_URI);

        // TODO: Think about incremental updates...
        list.add(ContentProviderOperation.newDelete(uri).build());

        for (Partner partner : mPartners.values()) {
            list.add(buildPartnerInsertOperation(partner));
        }
    }

    @Override
    public void process(JsonElement element) {
        // Does partner data expire like experts data does?
        for (Partner partner : new Gson().fromJson(element, Partner[].class)) {
            mPartners.put(partner.id, partner);
        }
    }

    private ContentProviderOperation buildPartnerInsertOperation(Partner partner) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                ScheduleContract.Partners.CONTENT_URI);
        return builder.withValue(ScheduleContract.Partners.PARTNER_ID, partner.id)
                .withValue(ScheduleContract.Partners.PARTNER_NAME, partner.name)
                .withValue(ScheduleContract.Partners.PARTNER_DESC, partner.desc)
                .withValue(ScheduleContract.Partners.PARTNER_WEBSITE_URL, partner.website)
                .withValue(ScheduleContract.Partners.PARTNER_LOGO_URL, partner.logo)
                .build();
    }
}
