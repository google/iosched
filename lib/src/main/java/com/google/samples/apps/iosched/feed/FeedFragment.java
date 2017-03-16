package com.google.samples.apps.iosched.feed;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.archframework.UpdatableView;
import com.google.samples.apps.iosched.lib.R;

public class FeedFragment extends Fragment implements
        UpdatableView<FeedModel, FeedModel.FeedQueryEnum, FeedModel.FeedUserActionEnum> {

    RecyclerView mRecyclerView;

    public static FeedFragment getInstance(FragmentManager fragmentManager) {
        FeedFragment feedFragment = (FeedFragment) fragmentManager
                .findFragmentById(R.id.main_feed_content);
        if (feedFragment == null) {
            feedFragment = new FeedFragment();
            fragmentManager.beginTransaction().add(R.id.main_feed_content, feedFragment).commit();
        }
        return feedFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.feed_fragment, container, false);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.feed_recycler_view);
        mRecyclerView.setAdapter(new FeedAdapter());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext(),
                linearLayoutManager.getOrientation()));
        return root;
    }

    @Override
    public void displayData(FeedModel model, FeedModel.FeedQueryEnum query) {

    }

    @Override
    public void displayErrorMessage(FeedModel.FeedQueryEnum query) {

    }

    @Override
    public void displayUserActionResult(FeedModel model, FeedModel.FeedUserActionEnum userAction,
                                        boolean success) {

    }

    @Override
    public Uri getDataUri(FeedModel.FeedQueryEnum query) {
        return null;
    }

    @Override
    public void addListener(UserActionListener<FeedModel.FeedUserActionEnum> listener) {

    }
}
