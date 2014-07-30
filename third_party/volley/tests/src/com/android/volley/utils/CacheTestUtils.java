// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.volley.utils;

import com.android.volley.Cache;

import java.util.Random;

public class CacheTestUtils {

    /**
     * Makes a random cache entry.
     * @param data Data to use, or null to use random data
     * @param isExpired Whether the TTLs should be set such that this entry is expired
     * @param needsRefresh Whether the TTLs should be set such that this entry needs refresh
     */
    public static Cache.Entry makeRandomCacheEntry(
            byte[] data, boolean isExpired, boolean needsRefresh) {
        Random random = new Random();
        Cache.Entry entry = new Cache.Entry();
        if (data != null) {
            entry.data = data;
        } else {
            entry.data = new byte[random.nextInt(1024)];
        }
        entry.etag = String.valueOf(random.nextLong());
        entry.serverDate = random.nextLong();
        entry.ttl = isExpired ? 0 : Long.MAX_VALUE;
        entry.softTtl = needsRefresh ? 0 : Long.MAX_VALUE;
        return entry;
    }

    /**
     * Like {@link #makeRandomCacheEntry(byte[], boolean, boolean)} but
     * defaults to an unexpired entry.
     */
    public static Cache.Entry makeRandomCacheEntry(byte[] data) {
        return makeRandomCacheEntry(data, false, false);
    }
}
