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

package com.google.samples.apps.iosched.explore.data;

import java.util.ArrayList;

/**
 * Data describing an Explore Event Card.
 */
public class EventData {
    private ArrayList<EventCard> mCards = new ArrayList<>();
    private String mTitle;

    public EventData() {}

    public void addEventCard(EventCard card) {
        mCards.add(card);
    }

    public ArrayList<EventCard> getCards() {
        return mCards;
    }

    public String getTitle() {
        return mTitle;
    }
}
