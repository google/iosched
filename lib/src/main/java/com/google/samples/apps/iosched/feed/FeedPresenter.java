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

import com.google.samples.apps.iosched.feed.data.FeedMessage;

import java.util.ArrayList;
import java.util.List;

public class FeedPresenter implements FeedContract.Presenter {
    private FeedContract.View mView;

    public FeedPresenter(FeedContract.View view) {
        mView = view;
    }

    @Override
    public void loadInitialData() {
        List<FeedMessage> feedMessages = new ArrayList<>();

        //TODO(sigelbaum) Remove mock data generation.
        fillWithMockData(feedMessages);

        mView.updateFromDataset(feedMessages);

        //TODO(sigelbaum) firebase init listeners!
    }

    //TODO(sigelbaum) Remove.
    private void fillWithMockData(List<FeedMessage> feedMessages) {
        String mockDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing" +
                "elit. Integer nec eleifend arcu, eu convallis nisi. Etiam bibendum, " +
                "tellus nec ultric" +
                "ies suscipit, metus justo fringilla urna, non mollis tortor libero non enim. " +
                "Mauris sit" +
                " amet venenatis risus, at ullamcorper " +
                "ligula. Maecenas adipiscing non odio ut hendrerit. Phasellus pharetra id sapien " +
                "laoreet sagittis. Etiam mattis " +
                "bibendum elit. Vestibulum rutrum a libero eget facilisis. " +
                "Duis tempor quam velit, " +
                "at viverra tellus faucibus ut. " +
                "Mauris venenatis odio sed lorem dignissim, vitae tristique " +
                "arcu sagittis.";
        String[] imageUrls = {
                "https://i.redd.it/jt44llj1m7ny.jpg",
                "http://i.imgur.com/DvDyT67.jpg",
                "http://i.imgur.com/UvbJlrL.jpg",
                "https://i.redd.it/oackrjykh3ny.jpg",
                "https://i.imgur.com/w6dVaSl.jpg",
//                "http://i.imgur.com/J6yZD8F.jpg",
                "",
                "https://i.redd.it/uzinuxmje6ny.jpg",
                "https://www.demilked.com/magazine/wp-content/uploads/2015/09/old-cartoon" +
                        "-characters-age-today-andrew-tarusov-thumb640.jpg",
                "https://i.redd.it/8xr85tal46ny.jpg",
                "http://i.imgur.com/L6i8Baq.jpg"};
        int defaultCategoryColor = 0xffafbdc4;
        feedMessages.add(new FeedMessage(0, "Agenda", defaultCategoryColor,
                "0Title!", mockDescription, true, "link",
                imageUrls[0], System.currentTimeMillis(), true, true));
        feedMessages.add(new FeedMessage(1, "Event", defaultCategoryColor,
                "1Title!", mockDescription, true, "link",
                imageUrls[1], System.currentTimeMillis(), true, true));
        feedMessages.add(new FeedMessage(2, "Social", defaultCategoryColor,
                "2Title!", mockDescription, true, "link",
                imageUrls[2], System.currentTimeMillis(), true, true));
        feedMessages.add(new FeedMessage(3, "Emergency", defaultCategoryColor,
                "3Title!", mockDescription, true, "link",
                imageUrls[3], System.currentTimeMillis(), true, true));
        feedMessages.add(new FeedMessage(4, "Emergency", defaultCategoryColor,
                "4Title!", mockDescription, true, "link",
                imageUrls[4], System.currentTimeMillis(), true, true));
        feedMessages.add(new FeedMessage(5, "Emergency", defaultCategoryColor,
                "5Title!", mockDescription, true, "link",
                imageUrls[5], System.currentTimeMillis(), true, true));
        feedMessages.add(new FeedMessage(6, "Emergency", defaultCategoryColor,
                "6Title!", mockDescription, true, "link",
                imageUrls[6], System.currentTimeMillis(), true, true));
        feedMessages.add(new FeedMessage(7, "Emergency", defaultCategoryColor,
                "7Title!", mockDescription, true, "link",
                imageUrls[7], System.currentTimeMillis(), true, true));
        feedMessages.add(new FeedMessage(8, "Emergency", defaultCategoryColor,
                "8Title!", mockDescription, true, "link",
                imageUrls[8], System.currentTimeMillis(), true, true));
        feedMessages.add(new FeedMessage(9, "Emergency", defaultCategoryColor,
                "9Title!", mockDescription, true, "link",
                imageUrls[9], System.currentTimeMillis(), true, true));
    }
}
