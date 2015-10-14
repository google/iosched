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

package com.google.samples.apps.iosched.social;


import android.content.Context;
import android.net.Uri;

import com.google.samples.apps.iosched.R;

/**
 * The data source for {@link com.google.samples.apps.iosched.social.SocialFragment}. The data
 * is static and not fetched through a query.
 */
public class SocialModel {

    public static final String PLUS_DIRECT_TARGET = "https://plus.google.com/";
    public static final String PLUS_SEARCH_TARGET = "https://plus.google.com/s/";
    public static final String TWITTER_HASHTAG_TARGET = "https://twitter.com/hashtag/";
    public static final String TWITTER_TARGET = "https://twitter.com/";

    private final Context mContext;

    public SocialModel(Context context) {
        mContext = context;
    }

    /**
     * Hard-coded labels and links to select social media targets.
     */
    public enum SocialLinksEnum {
        GPLUS_IO15("#io15", PLUS_SEARCH_TARGET),
        TWITTER_IO15("io15", TWITTER_HASHTAG_TARGET),
        GPLUS_DEVS("+googledevelopers", PLUS_DIRECT_TARGET),
        TWITTER_DEVS("googledevs", TWITTER_TARGET),
        GPLUS_EXTENDED("#io15extended", PLUS_SEARCH_TARGET),
        TWITTER_EXTENDED("io15extended", TWITTER_HASHTAG_TARGET),
        GPLUS_REQUEST("#io15request", PLUS_SEARCH_TARGET),
        TWITTER_REQUEST("io15request", TWITTER_HASHTAG_TARGET);

        private String mTag;
        private String mTarget;

        SocialLinksEnum(String tag, String uri) {
            this.mTag = tag;
            this.mTarget = uri;
        }

        public String getTag() {
            return mTag;
        }

        public String getTarget() {
            return mTarget;
        }

        public Uri getUri() {
            return Uri.parse(getTarget() + Uri.encode(getTag()));
        }
    }

    /**
     * Returns the content description for a social link.
     */
    protected String getContentDescription(SocialLinksEnum socialValue) {
        switch (socialValue) {
            case GPLUS_IO15:
                return mContext.getResources().getString(
                        R.string.social_io15_gplus_content_description);
            case TWITTER_IO15:
                return mContext.getResources().getString(
                        R.string.social_io15_twitter_content_description);
            case GPLUS_EXTENDED:
                return mContext.getResources().getString(
                        R.string.social_extended_gplus_content_description);
            case TWITTER_EXTENDED:
                return mContext.getResources().getString(
                        R.string.social_extended_twitter_content_description);
            case GPLUS_REQUEST:
                return mContext.getResources().getString(
                        R.string.social_request_gplus_content_description);
            case TWITTER_REQUEST:
                return mContext.getResources().getString(
                        R.string.social_request_twitter_content_description);
            default:
                return "";
        }
    }

}
