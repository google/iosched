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

package com.google.samples.apps.iosched.io;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.samples.apps.iosched.io.model.Card;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContractHelper;

import java.util.ArrayList;
import java.util.HashMap;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class CardHandler extends JSONHandler {
    private static final String TAG = makeLogTag(CardHandler.class);

    // Map keyed on Card IDs.
    private HashMap<String, Card> mCards = new HashMap<>();

    public CardHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Card card : new Gson().fromJson(element, Card[].class)) {
            mCards.put(card.mId, card);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        LOGI(TAG, "Creating content provider operations for cards: " + mCards.size());
        Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Cards.CONTENT_URI);

        // The list of cards is not large, so for simplicity we delete all of them and repopulate
        list.add(ContentProviderOperation.newDelete(uri).build());
        for (Card card : mCards.values()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            builder.withValue(ScheduleContract.Cards.ACTION_COLOR, card.mActionColor);
            builder.withValue(ScheduleContract.Cards.ACTION_TEXT, card.mActionText);
            builder.withValue(ScheduleContract.Cards.ACTION_URL, card.mActionUrl);
            builder.withValue(ScheduleContract.Cards.ACTION_TYPE, card.mActionType);
            builder.withValue(ScheduleContract.Cards.ACTION_EXTRA, card.mActionExtra);
            builder.withValue(ScheduleContract.Cards.BACKGROUND_COLOR, card.mBackgroundColor);
            builder.withValue(ScheduleContract.Cards.CARD_ID, card.mId);
            try {
                long startTime = Card.getEpochMillisFromTimeString(card.mValidFrom);
                LOGI(TAG, "Processing card with epoch start time: " + startTime);
                builder.withValue(ScheduleContract.Cards.DISPLAY_START_DATE, startTime);
            } catch (IllegalArgumentException exception) {
                LOGE(TAG, "Card time disabled, invalid display start date defined for card: " +
                        card.mTitle + " " + card.mValidFrom);
                builder.withValue(ScheduleContract.Cards.DISPLAY_START_DATE, Long.MAX_VALUE);
            }
            try {
                long endTime = Card.getEpochMillisFromTimeString(card.mValidUntil);
                LOGI(TAG, "Processing card with epoch end time: " + endTime);
                builder.withValue(ScheduleContract.Cards.DISPLAY_END_DATE, endTime);
            } catch (IllegalArgumentException exception) {
                LOGE(TAG, "Card time disabled, invalid display end date defined for card: " +
                        card.mTitle + " " + card.mValidUntil);
                builder.withValue(ScheduleContract.Cards.DISPLAY_END_DATE, 0L);
            }
            builder.withValue(ScheduleContract.Cards.MESSAGE, card.mShortMessage);
            builder.withValue(ScheduleContract.Cards.TEXT_COLOR, card.mTextColor);
            builder.withValue(ScheduleContract.Cards.TITLE, card.mTitle);
            list.add(builder.build());
        }
    }
}
