/*
 * Copyright (c) 2017 Google Inc.
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
package com.google.samples.apps.iosched.feed;

import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.samples.apps.iosched.feed.data.FeedMessage;
import com.google.samples.apps.iosched.lib.R;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class FeedFragment extends Fragment implements FeedContract.View {
    private static final String TAG = makeLogTag(FeedFragment.class);

    private FeedContract.Presenter mPresenter;
    private RecyclerView mRecyclerView;
    private FeedAdapter mFeedAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.feed_fragment, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.feed_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mFeedAdapter = new FeedAdapter(getContext());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), VERTICAL));
        mRecyclerView.setAdapter(mFeedAdapter);
        View header = view.findViewById(R.id.header_anim);
        if (header instanceof ImageView) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) ContextCompat.getDrawable(
                    getContext(), R.drawable.avd_header_feed);
            ((ImageView) header).setImageDrawable(avd);
            avd.start();
        }
        return view;
    }

    @Override
    public void setPresenter(FeedContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showErrorMessage() {
        Snackbar.make(mRecyclerView, R.string.feed_error, Snackbar.LENGTH_SHORT);
    }

    @Override
    public void addFeedMessage(FeedMessage feedMessage) {
        mFeedAdapter.addFeedMessage(feedMessage);
        mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, 0);
    }

    @Override
    public void updateFeedMessage(FeedMessage feedMessage) {
        mFeedAdapter.updateFeedMessage(feedMessage);
        mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, 0);

    }

    @Override
    public void removeFeedMessage(FeedMessage feedMessage) {
        mFeedAdapter.removeFeedMessage(feedMessage);
        mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, 0);
    }
}
