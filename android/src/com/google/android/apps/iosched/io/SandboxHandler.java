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

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.io.model.SandboxCompany;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.SyncColumns;
import com.google.android.apps.iosched.util.Lists;
import com.google.gson.Gson;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.android.apps.iosched.provider.ScheduleContract.Vendors;
import static com.google.android.apps.iosched.util.LogUtils.LOGI;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Handler that parses developer sandbox JSON data into a list of content provider operations.
 */
public class SandboxHandler extends JSONHandler {

    private static final String TAG = makeLogTag(SandboxHandler.class);
    private static final String BASE_LOGO_URL
            = "http://commondatastorage.googleapis.com/io2012/sandbox%20logos/";

    public SandboxHandler(Context context, boolean local) {
        super(context);
    }

    public ArrayList<ContentProviderOperation> parse(String json)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        SandboxCompany[] companies = new Gson().fromJson(json, SandboxCompany[].class);

        if (companies.length > 0) {
            LOGI(TAG, "Updating developer sandbox data");

            // Clear out existing sandbox companies
            batch.add(ContentProviderOperation
                    .newDelete(ScheduleContract.addCallerIsSyncAdapterParameter(
                            Vendors.CONTENT_URI))
                    .build());

            StringBuilder companyDescription = new StringBuilder();
            String exhibitorsPrefix = mContext.getString(R.string.vendor_exhibitors_prefix);

            for (SandboxCompany company : companies) {
                // Insert sandbox company info
                String website = company.website;
                if (!TextUtils.isEmpty(website) && !website.startsWith("http")) {
                    website = "http://" + website;
                }

                companyDescription.setLength(0);
                if (company.exhibitors != null && company.exhibitors.length > 0) {
                    companyDescription.append(exhibitorsPrefix);
                    companyDescription.append(" ");

                    for (int i = 0; i < company.exhibitors.length; i++) {
                        companyDescription.append(company.exhibitors[i]);
                        if (i >= company.exhibitors.length - 1) {
                            break;
                        }
                        companyDescription.append(", ");
                    }

                    companyDescription.append("\n\n");
                }

                if (!TextUtils.isEmpty(company.company_description)) {
                    companyDescription.append(company.company_description);
                    companyDescription.append("\n\n");
                }

                if (!TextUtils.isEmpty(company.product_description)) {
                    companyDescription.append(company.product_description);
                }

                // Clean up logo URL
                String logoUrl = null;
                if (!TextUtils.isEmpty(company.logo_img)) {
                    logoUrl = company.logo_img.replaceAll(" ", "%20");
                    if (!logoUrl.startsWith("http")) {
                        logoUrl = BASE_LOGO_URL + logoUrl;
                    }
                }

                batch.add(ContentProviderOperation
                        .newInsert(ScheduleContract
                                .addCallerIsSyncAdapterParameter(Vendors.CONTENT_URI))
                        .withValue(SyncColumns.UPDATED, System.currentTimeMillis())
                        .withValue(Vendors.VENDOR_ID,
                                Vendors.generateVendorId(company.company_name))
                        .withValue(Vendors.VENDOR_NAME, company.company_name)
                        .withValue(Vendors.VENDOR_DESC, companyDescription.toString())
                        .withValue(Vendors.VENDOR_PRODUCT_DESC, null) // merged into company desc
                        .withValue(Vendors.VENDOR_LOGO_URL, logoUrl)
                        .withValue(Vendors.VENDOR_URL, website)
                        .withValue(Vendors.TRACK_ID,
                                ScheduleContract.Tracks.generateTrackId(company.product_pod))
                        .build());
            }
        }

        return batch;
    }
}
