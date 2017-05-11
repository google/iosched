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

package io.material.demo.shrine.products;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.LruCache;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

/** Singleton class that handles image requests using Volley. */
public class ImageRequester {
  private static Context ctx;
  private static ImageRequester instance;
  private final RequestQueue requestQueue;
  private final ImageLoader imageLoader;
  private final int maxByteSize;

  private ImageRequester(Context context) {
    ctx = context;
    this.requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
    this.requestQueue.start();
    this.maxByteSize = calculateMaxByteSize();
    this.imageLoader =
        new ImageLoader(
            requestQueue,
            new ImageLoader.ImageCache() {
              private final LruCache<String, Bitmap> lruCache =
                  new LruCache<String, Bitmap>(maxByteSize) {
                    @Override
                    protected int sizeOf(String url, Bitmap bitmap) {
                      return bitmap.getByteCount();
                    }
                  };

              @Override
              public synchronized Bitmap getBitmap(String url) {
                return lruCache.get(url);
              }

              @Override
              public synchronized void putBitmap(String url, Bitmap bitmap) {
                lruCache.put(url, bitmap);
              }
            });
    instance = this;
  }

  private static int calculateMaxByteSize() {
    DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
    final int screenBytes = displayMetrics.widthPixels * displayMetrics.heightPixels * 4;
    return screenBytes * 3;
  }

  /**
   * Returns the existing ImageRequester instantiated, or a new instance if there is not yet an
   * instance.
   *
   * @param context Of the calling activity.
   * @return ImageRequester
   */
  public static synchronized ImageRequester getInstance(Context context) {
    if (instance == null) {
      instance = new ImageRequester(context);
    }
    return instance;
  }

  public void setImageFromUrl(NetworkImageView networkImageView, String url) {
    networkImageView.setImageUrl(url, imageLoader);
  }
}
