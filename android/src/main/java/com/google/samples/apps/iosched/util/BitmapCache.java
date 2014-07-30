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

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This class holds our bitmap caches (memory and disk).
 */
public class BitmapCache implements ImageLoader.ImageCache {
    private static final String TAG = makeLogTag(BitmapCache.class);

    // Default memory cache size as a percent of device memory class
    private static final float DEFAULT_MEM_CACHE_PERCENT = 0.15f;

    private LruCache<String, Bitmap> mMemoryCache;

    /**
     * Don't instantiate this class directly, use
     * {@link #getInstance(android.support.v4.app.FragmentManager, float)}.
     * @param memCacheSize Memory cache size in KB.
     */
    private BitmapCache(int memCacheSize) {
        init(memCacheSize);
    }

    /**
     * Find and return an existing BitmapCache stored in a {@link RetainFragment}, if not found a
     * new one is created using the supplied params and saved to a {@link RetainFragment}.
     *
     * @param fragmentManager The fragment manager to use when dealing with the retained fragment.
     * @param fragmentTag The tag of the retained fragment (should be unique for each memory cache
     *                    that needs to be retained).
     * @param memCacheSize Memory cache size in KB.
     */
    public static BitmapCache getInstance(FragmentManager fragmentManager, String fragmentTag,
            int memCacheSize) {
        BitmapCache bitmapCache = null;
        RetainFragment mRetainFragment = null;

        if (fragmentManager != null) {
            // Search for, or create an instance of the non-UI RetainFragment
            mRetainFragment = getRetainFragment(fragmentManager, fragmentTag);

            // See if we already have a BitmapCache stored in RetainFragment
            bitmapCache = (BitmapCache) mRetainFragment.getObject();
        }

        // No existing BitmapCache, create one and store it in RetainFragment
        if (bitmapCache == null) {
            bitmapCache = new BitmapCache(memCacheSize);
            if (mRetainFragment != null) {
                mRetainFragment.setObject(bitmapCache);
            }
        }
        return bitmapCache;
    }

    public static BitmapCache getInstance(FragmentManager fragmentManager, int memCacheSize) {
        return getInstance(fragmentManager, TAG, memCacheSize);
    }

    public static BitmapCache getInstance(FragmentManager fragmentManager, float memCachePercent) {
        return getInstance(fragmentManager, calculateMemCacheSize(memCachePercent));
    }

    public static BitmapCache getInstance(FragmentManager fragmentManger) {
        return getInstance(fragmentManger, DEFAULT_MEM_CACHE_PERCENT);
    }

    /**
     * Initialize the cache.
     */
    private void init(int memCacheSize) {
        // Set up memory cache
        LOGD(TAG, "Memory cache created (size = " + memCacheSize + "KB)");
        mMemoryCache = new LruCache<String, Bitmap>(memCacheSize) {
            /**
             * Measure item size in kilobytes rather than units which is more practical
             * for a bitmap cache
             */
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                final int bitmapSize = getBitmapSize(bitmap) / 1024;
                return bitmapSize == 0 ? 1 : bitmapSize;
            }
        };
    }

    /**
     * Adds a bitmap to both memory and disk cache.
     * @param data Unique identifier for the bitmap to store
     * @param bitmap The bitmap to store
     */
    public void addBitmapToCache(String data, Bitmap bitmap) {
        if (data == null || bitmap == null) {
            return;
        }

        synchronized (mMemoryCache) {
            // Add to memory cache
            if (mMemoryCache.get(data) == null) {
                LOGD(TAG, "Memory cache put - " + data);
                mMemoryCache.put(data, bitmap);
            }
        }
    }

    /**
     * Get from memory cache.
     *
     * @param data Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    public Bitmap getBitmapFromMemCache(String data) {
        if (data != null) {
            synchronized (mMemoryCache) {
                final Bitmap memBitmap = mMemoryCache.get(data);
                if (memBitmap != null) {
                    LOGD(TAG, "Memory cache hit - " + data);
                    return memBitmap;
                }
            }
            LOGD(TAG, "Memory cache miss - " + data);
        }
        return null;
    }

    /**
     * Clears the memory cache.
     */
    public void clearCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
            LOGD(TAG, "Memory cache cleared");
        }
    }

    /**
     * Sets the memory cache size based on a percentage of the max available VM memory.
     * Eg. setting percent to 0.2 would set the memory cache to one fifth of the available
     * memory. Throws {@link IllegalArgumentException} if percent is < 0.05 or > .8.
     * memCacheSize is stored in kilobytes instead of bytes as this will eventually be passed
     * to construct a LruCache which takes an int in its constructor.
     *
     * This value should be chosen carefully based on a number of factors
     * Refer to the corresponding Android Training class for more discussion:
     * http://developer.android.com/training/displaying-bitmaps/
     *
     * @param percent Percent of memory class to use to size memory cache
     * @return Memory cache size in KB
     */
    public static int calculateMemCacheSize(float percent) {
        if (percent < 0.05f || percent > 0.8f) {
            throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
                    + "between 0.05 and 0.8 (inclusive)");
        }
        return Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);
    }

    /**
     * Get the size in bytes of a bitmap.
     */
    public static int getBitmapSize(Bitmap bitmap) {
        return bitmap.getByteCount();
    }

    /**
     * Locate an existing instance of this Fragment or if not found, create and
     * add it using FragmentManager.
     *
     * @param fm The FragmentManager manager to use.
     * @param fragmentTag The tag of the retained fragment (should be unique for each memory
     *                    cache that needs to be retained).
     * @return The existing instance of the Fragment or the new instance if just
     *         created.
     */
    private static RetainFragment getRetainFragment(FragmentManager fm, String fragmentTag) {
        // Check to see if we have retained the worker fragment.
        RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(fragmentTag);

        // If not retained (or first time running), we need to create and add it.
        if (mRetainFragment == null) {
            mRetainFragment = new RetainFragment();
            fm.beginTransaction().add(mRetainFragment, fragmentTag).commitAllowingStateLoss();
        }

        return mRetainFragment;
    }

    @Override
    public Bitmap getBitmap(String key) {
        return getBitmapFromMemCache(key);
    }

    @Override
    public void putBitmap(String key, Bitmap bitmap) {
        addBitmapToCache(key, bitmap);
    }

    /**
     * A simple non-UI Fragment that stores a single Object and is retained over configuration
     * changes. It will be used to retain the BitmapCache object.
     */
    public static class RetainFragment extends Fragment {
        private Object mObject;

        /**
         * Empty constructor as per the Fragment documentation
         */
        public RetainFragment() {}

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure this Fragment is retained over a configuration change
            setRetainInstance(true);
        }

        /**
         * Store a single object in this Fragment.
         *
         * @param object The object to store
         */
        public void setObject(Object object) {
            mObject = object;
        }

        /**
         * Get the stored object.
         *
         * @return The stored object
         */
        public Object getObject() {
            return mObject;
        }
    }

}
