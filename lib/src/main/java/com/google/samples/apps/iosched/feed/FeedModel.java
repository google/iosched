package com.google.samples.apps.iosched.feed;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.archframework.UserActionEnum;

public class FeedModel implements Model<FeedModel.FeedQueryEnum,
        FeedModel.FeedUserActionEnum> {

    @Override
    public FeedQueryEnum[] getQueries() {
        return new FeedQueryEnum[0];
    }

    @Override
    public FeedUserActionEnum[] getUserActions() {
        return FeedUserActionEnum.values();
    }

    @Override
    public void deliverUserAction(FeedUserActionEnum action, @Nullable Bundle args,
                                  UserActionCallback<FeedUserActionEnum> callback) {
    }

    @Override
    public void requestData(FeedQueryEnum query, DataQueryCallback<FeedQueryEnum> callback) {

    }

    @Override
    public void cleanUp() {

    }



    public enum FeedUserActionEnum implements UserActionEnum {
        SWIPE(0);

        int mId;

        FeedUserActionEnum(int id) {
            mId = id;
        }

        @Override
        public int getId() {
            return mId;
        }
    }

    public enum FeedQueryEnum implements QueryEnum {
        FEED(0, null);

        int mId;
        String[] mProjection;

        FeedQueryEnum(int id, String[] projection) {
            mId = id;
            mProjection = projection;
        }

        @Override
        public int getId() {
            return mId;
        }

        @Override
        public String[] getProjection() {
            return mProjection;
        }
    }
}
