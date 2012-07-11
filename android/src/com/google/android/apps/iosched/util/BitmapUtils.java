/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.iosched.util;

import com.google.android.apps.iosched.service.SyncService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class for fetching and disk-caching images from the web.
 */
public class BitmapUtils {
    private static final String TAG = "BitmapUtils";

    // TODO: for concurrent connections, DefaultHttpClient isn't great, consider other options
    // that still allow for sharing resources across bitmap fetches.

    public static interface OnFetchCompleteListener {
        public void onFetchComplete(Object cookie, Bitmap result);
    }

    /**
     * Only call this method from the main (UI) thread. The {@link OnFetchCompleteListener} callback
     * be invoked on the UI thread, but image fetching will be done in an {@link AsyncTask}.
     */
    public static void fetchImage(final Context context, final String url,
            final OnFetchCompleteListener callback) {
        fetchImage(context, url, null, null, callback);
    }

    /**
     * Only call this method from the main (UI) thread. The {@link OnFetchCompleteListener} callback
     * be invoked on the UI thread, but image fetching will be done in an {@link AsyncTask}.
     *
     * @param cookie An arbitrary object that will be passed to the callback.
     */
    public static void fetchImage(final Context context, final String url,
            final BitmapFactory.Options decodeOptions,
            final Object cookie, final OnFetchCompleteListener callback) {
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                final String url = params[0];
                if (TextUtils.isEmpty(url)) {
                    return null;
                }

                // First compute the cache key and cache file path for this URL
                File cacheFile = null;
                try {
                    MessageDigest mDigest = MessageDigest.getInstance("SHA-1");
                    mDigest.update(url.getBytes());
                    final String cacheKey = bytesToHexString(mDigest.digest());
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        cacheFile = new File(
                                Environment.getExternalStorageDirectory()
                                        + File.separator + "Android"
                                        + File.separator + "data"
                                        + File.separator + context.getPackageName()
                                        + File.separator + "cache"
                                        + File.separator + "bitmap_" + cacheKey + ".tmp");
                    }
                } catch (NoSuchAlgorithmException e) {
                    // Oh well, SHA-1 not available (weird), don't cache bitmaps.
                }

                if (cacheFile != null && cacheFile.exists()) {
                    Bitmap cachedBitmap = BitmapFactory.decodeFile(
                            cacheFile.toString(), decodeOptions);
                    if (cachedBitmap != null) {
                        return cachedBitmap;
                    }
                }

                try {
                    // TODO: check for HTTP caching headers
                    final HttpClient httpClient = SyncService.getHttpClient(
                            context.getApplicationContext());
                    final HttpResponse resp = httpClient.execute(new HttpGet(url));
                    final HttpEntity entity = resp.getEntity();

                    final int statusCode = resp.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK || entity == null) {
                        return null;
                    }

                    final byte[] respBytes = EntityUtils.toByteArray(entity);

                    // Write response bytes to cache.
                    if (cacheFile != null) {
                        try {
                            cacheFile.getParentFile().mkdirs();
                            cacheFile.createNewFile();
                            FileOutputStream fos = new FileOutputStream(cacheFile);
                            fos.write(respBytes);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            Log.w(TAG, "Error writing to bitmap cache: " + cacheFile.toString(), e);
                        } catch (IOException e) {
                            Log.w(TAG, "Error writing to bitmap cache: " + cacheFile.toString(), e);
                        }
                    }

                    // Decode the bytes and return the bitmap.
                    return BitmapFactory.decodeByteArray(respBytes, 0, respBytes.length,
                            decodeOptions);
                } catch (Exception e) {
                    Log.w(TAG, "Problem while loading image: " + e.toString(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                callback.onFetchComplete(cookie, result);
            }
        }.execute(url);
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
