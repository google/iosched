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

package com.google.samples.apps.iosched.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.google.samples.apps.iosched.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ModelRequest;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.bitmap.RequestListener;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class ImageLoader {
    private static final String TAG = makeLogTag(ImageLoader.class);
    private static final ModelCache<GlideUrl> urlCache = new ModelCache<GlideUrl>(150);

    private final ModelRequest.ImageModelRequest<String> mGlideModelRequest;
    private final CenterCrop mCenterCrop;
    private final Transformation<Bitmap> mNone;

    private int mPlaceHolderResId = -1;

    /**
     * Construct a standard ImageLoader object.
     */
    public ImageLoader(Context context) {
        VariableWidthImageLoader imageLoader = new VariableWidthImageLoader(context);
        mGlideModelRequest = Glide.with(context).using(imageLoader);
        mCenterCrop = new CenterCrop(Glide.get(context).getBitmapPool());
        mNone = Transformation.NONE;
    }

    /**
     * Construct an ImageLoader with a default placeholder drawable.
     */
    public ImageLoader(Context context, int placeHolderResId) {
        this(context);
        mPlaceHolderResId = placeHolderResId;
    }

    /**
     * Load an image from a url into an ImageView using the default placeholder
     * drawable if available.
     * @param url The web URL of an image.
     * @param imageView The target ImageView to load the image into.
     * @param requestListener A listener to monitor the request result.
     */
    public void loadImage(String url, ImageView imageView, RequestListener<String> requestListener) {
        loadImage(url, imageView, requestListener, null, false);
    }

    /**
     * Load an image from a url into an ImageView using the given placeholder drawable.
     *
     * @param url The web URL of an image.
     * @param imageView The target ImageView to load the image into.
     * @param requestListener A listener to monitor the request result.
     * @param placholderOverride A placeholder to use in place of the default placholder.
     */
    public void loadImage(String url, ImageView imageView, RequestListener<String> requestListener,
            Drawable placholderOverride) {
        loadImage(url, imageView, requestListener, placholderOverride, false /*crop*/);
    }

    /**
     * Load an image from a url into an ImageView using the default placeholder
     * drawable if available.
     * @param url The web URL of an image.
     * @param imageView The target ImageView to load the image into.
     * @param requestListener A listener to monitor the request result.
     * @param placeholderOverride A drawable to use as a placeholder for this specific image.
     *                            If this parameter is present, {@link #mPlaceHolderResId}
     *                            if ignored for this request.
     */
    public void loadImage(String url, ImageView imageView, RequestListener<String> requestListener,
                Drawable placeholderOverride, boolean crop) {
        BitmapRequestBuilder request = beginImageLoad(url, requestListener, crop)
                .animate(R.anim.image_fade_in);
        if (placeholderOverride != null) {
            request.placeholder(placeholderOverride);
        } else if (mPlaceHolderResId != -1) {
            request.placeholder(mPlaceHolderResId);
        }
        request.into(imageView);
    }

    public BitmapRequestBuilder beginImageLoad(String url,
            RequestListener<String> requestListener, boolean crop) {
        return mGlideModelRequest.load(url)
                .asBitmap() // don't allow animated GIFs
                .listener(requestListener)
                .transform(crop ? mCenterCrop : mNone);
    }

    /**
     * Load an image from a url into the given image view using the default placeholder if
     * available.
     * @param url The web URL of an image.
     * @param imageView The target ImageView to load the image into.
     */
    public void loadImage(String url, ImageView imageView) {
        loadImage(url, imageView, false /*crop*/);
    }

    /**
     * Load an image from a url into an ImageView using the default placeholder
     * drawable if available.
     * @param url The web URL of an image.
     * @param imageView The target ImageView to load the image into.
     * @param crop True to apply a center crop to the image.
     */
    public void loadImage(String url, ImageView imageView, boolean crop) {
        loadImage(url, imageView, null, null, crop);
    }

    private static class VariableWidthImageLoader extends BaseGlideUrlLoader<String> {
        private static final Pattern PATTERN = Pattern.compile("__w-((?:-?\\d+)+)__");

        public VariableWidthImageLoader(Context context) {
            super(context, urlCache);
        }

        @Override
        public String getId(String model) {
            return model;
        }

        /**
         * If the URL contains a special variable width indicator (eg "__w-200-400-800__")
         * we get the buckets from the URL (200, 400 and 800 in the example) and replace
         * the URL with the best bucket for the requested width (the bucket immediately
         * larger than the requested width).
         */
        @Override
        protected String getUrl(String model, int width, int height) {
            Matcher m = PATTERN.matcher(model);
            int bestBucket = 0;
            if (m.find()) {
                String[] found = m.group(1).split("-");
                for (String bucketStr : found) {
                    bestBucket = Integer.parseInt(bucketStr);
                    if (bestBucket >= width) {
                        // the best bucket is the first immediately bigger than the requested width
                        break;
                    }
                }
                if (bestBucket > 0) {
                    model = m.replaceFirst("w"+bestBucket);
                    LOGD(TAG, "width="+width+", URL successfully replaced by "+model);
                }
            }
            return model;
        }
    }
}
