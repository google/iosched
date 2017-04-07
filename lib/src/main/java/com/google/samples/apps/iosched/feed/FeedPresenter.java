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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.samples.apps.iosched.feed.data.FeedMessage;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class FeedPresenter implements FeedContract.Presenter {
    private static final String TAG = makeLogTag(FeedPresenter.class);

    private FeedContract.View mView;
    private ChildEventListener mEventListener;

    public FeedPresenter(FeedContract.View view) {
        mView = view;
    }

    @Override
    public void initializeDataListener(DatabaseReference databaseReference) {
        if (mEventListener == null) {
            mEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    try {
                        FeedMessage feedMessage = dataSnapshot.getValue(FeedMessage.class);
                        mView.addFeedMessage(feedMessage);
                    } catch (DatabaseException e) {
                        mView.showErrorMessage();
                        LOGE(TAG, "Firebase error - " + e);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    try {
                        FeedMessage feedMessage = dataSnapshot.getValue(FeedMessage.class);
                        mView.updateFeedMessage(feedMessage);
                    } catch (DatabaseException e) {
                        mView.showErrorMessage();
                        LOGE(TAG, "Firebase error - " + e);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    try {
                        FeedMessage feedMessage = dataSnapshot.getValue(FeedMessage.class);
                        mView.removeFeedMessage(feedMessage);
                    } catch (DatabaseException e) {
                        mView.showErrorMessage();
                        LOGE(TAG, "Firebase error - " + e);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    try {
                        FeedMessage feedMessage = dataSnapshot.getValue(FeedMessage.class);
                        mView.updateFeedMessage(feedMessage);
                    } catch (DatabaseException e) {
                        mView.showErrorMessage();
                        LOGE(TAG, "Firebase error - " + e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    mView.showErrorMessage();
                }
            };
        }
        databaseReference.addChildEventListener(mEventListener);
    }

    @Override
    public void removeDataListener(DatabaseReference databaseReference) {
        if (mEventListener != null) {
            databaseReference.removeEventListener(mEventListener);
            mEventListener = null;
        }
    }
}
