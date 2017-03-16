package com.google.samples.apps.iosched.feed;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    @Override
    public FeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(FeedViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        public FeedViewHolder(View itemView) {
            super(itemView);
        }
    }


}
