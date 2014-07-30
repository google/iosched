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

package com.google.samples.apps.iosched.nearby;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.util.Patterns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.google.samples.apps.iosched.Config.METADATA_URL;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Class for resolving BLE device metadata via proxy.
 */
public class MetadataResolver {
    static String TAG = makeLogTag("MetadataResolver");

    static Map<String, String> mDeviceUrlMap;
    static RequestQueue mRequestQueue;

    static boolean mIsInitialized = false;

    public static void initialize(Context context) {
        mDeviceUrlMap = new HashMap<String, String>();
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }

        mIsInitialized = true;
    }

    public static String getURLForDevice(NearbyDevice device) {
        if (!mIsInitialized) {
            Log.e(TAG, "Not initialized.");
            return null;
        }

        // If the device name is already a URL, use it.
        String deviceName = device.getName();
        String url = deviceName;
        if (Patterns.WEB_URL.matcher(deviceName).matches()) {
            // TODO: Improve this.
            // For now, if there's no scheme present, add a default http:// scheme.
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
        } else {
            // Otherwise, try doing the lookup.
            url = mDeviceUrlMap.get(deviceName);
        }
        return url;
    }

    public static void getBatchMetadata(List<NearbyDevice> batchList) {
        if (!mIsInitialized) {
            Log.e(TAG, "Not initialized.");
            return;
        }

        JSONObject jsonObj = createRequestObject(batchList);

        Map<String, NearbyDevice> deviceMap = new HashMap<String, NearbyDevice>();

        for (int i = 0; i < batchList.size(); i++) {
            NearbyDevice nearbyDevice = batchList.get(i);
            deviceMap.put(nearbyDevice.getUrl(), nearbyDevice);
        }

        JsonObjectRequest jsObjRequest = createMetadataRequest(jsonObj, deviceMap);

        // Queue the request
        mRequestQueue.add(jsObjRequest);
    }

    private static JsonObjectRequest createMetadataRequest(JSONObject jsonObj, final Map<String, NearbyDevice> deviceMap) {
        return new JsonObjectRequest(
                METADATA_URL,
                jsonObj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonResponse) {

                        try {
                            JSONArray foundMetaData = jsonResponse.getJSONArray("metadata");

                            int deviceCount = foundMetaData.length();
                            for (int i = 0; i < deviceCount; i++) {

                                JSONObject deviceData = foundMetaData.getJSONObject(i);

                                String title = "Unknown name";
                                String url = "Unknown url";
                                String description = "Unknown description";
                                String iconUrl = "/favicon.ico";
                                String id = deviceData.getString("id");

                                if (deviceData.has("title")) {
                                    title = deviceData.getString("title");
                                }
                                if (deviceData.has("url")) {
                                    url = deviceData.getString("url");
                                }
                                if (deviceData.has("description")) {
                                    description = deviceData.getString("description");
                                }
                                if (deviceData.has("icon")) {
                                    // We might need to do some magic here.
                                    iconUrl = deviceData.getString("icon");
                                }
                                // Provisions for a favicon specified as a relative URL.
                                if (!iconUrl.startsWith("http")) {
                                    // Lets just assume we are dealing with a relative path.
                                    Uri fullUri = Uri.parse(url);
                                    Uri.Builder builder = fullUri.buildUpon();
                                    // Append the default favicon path to the URL.
                                    builder.path(iconUrl);
                                    iconUrl = builder.toString();
                                }

                                DeviceMetadata deviceMetadata = new DeviceMetadata();
                                deviceMetadata.title = title;
                                deviceMetadata.description = description;
                                deviceMetadata.siteUrl = url;
                                deviceMetadata.iconUrl = iconUrl;
                                downloadIcon(deviceMetadata, deviceMap.get(id));

                                // Look up the device from the input and update the data
                                deviceMap.get(id).onDeviceInfo(deviceMetadata);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.i(TAG, "VolleyError: " + volleyError.toString());
                    }
                }
        );
    }

    private static JSONObject createRequestObject(List<NearbyDevice> devices) {
        JSONObject jsonObj = new JSONObject();

        try {
            JSONArray urlArray = new JSONArray();

            for(int dIdx = 0; dIdx < devices.size(); dIdx++) {
                NearbyDevice device = devices.get(dIdx);

                JSONObject urlObject = new JSONObject();

                urlObject.put("url", device.getUrl());
                urlObject.put("rssi", device.getLastRSSI());
                urlArray.put(urlObject);
            }


            JSONObject location = new JSONObject();

            location.put("lat", 49.129837);
            location.put("lon", 120.38142);

            jsonObj.put("location",  location);
            jsonObj.put("objects", urlArray);

        } catch(JSONException ex) {

        }
        return jsonObj;
    }

    /**
     * Asynchronously download the image for the nearby device.
     * @param metadata
     * @param listener
     */
    private static void downloadIcon(final DeviceMetadata metadata, final OnMetadataListener listener) {
        ImageRequest imageRequest = new ImageRequest(metadata.iconUrl, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                Log.i(TAG, "Got an image: " + response.toString());
                metadata.icon = response;
                listener.onDeviceInfo(metadata);
            }
        }, 0, 0, null, null);
        mRequestQueue.add(imageRequest);
    }

    public interface OnMetadataListener {
        public void onDeviceInfo(DeviceMetadata deviceMetadata);
    }
}
