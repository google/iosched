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

package com.google.samples.apps.iosched.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.samples.apps.iosched.io.*;
import com.google.samples.apps.iosched.io.map.model.Tile;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.FileUtils;
import com.google.samples.apps.iosched.util.Lists;
import com.google.samples.apps.iosched.util.MapUtils;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGBuilder;
import com.larvalabs.svgandroid.SVGParseException;
import com.turbomanage.httpclient.BasicHttpClient;
import com.turbomanage.httpclient.ConsoleRequestLogger;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.RequestLogger;

import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * Helper class that parses conference data and imports them into the app's
 * Content Provider.
 */
public class ConferenceDataHandler {
    private static final String TAG = makeLogTag(SyncHelper.class);

    // Shared preferences key under which we store the timestamp that corresponds to
    // the data we currently have in our content provider.
    private static final String SP_KEY_DATA_TIMESTAMP = "data_timestamp";

    // symbolic timestamp to use when we are missing timestamp data (which means our data is
    // really old or nonexistent)
    private static final String DEFAULT_TIMESTAMP = "Sat, 1 Jan 2000 00:00:00 GMT";

    private static final String DATA_KEY_ROOMS = "rooms";
    private static final String DATA_KEY_BLOCKS = "blocks";
    private static final String DATA_KEY_TAGS = "tags";
    private static final String DATA_KEY_SPEAKERS = "speakers";
    private static final String DATA_KEY_SESSIONS = "sessions";
    private static final String DATA_KEY_SEARCH_SUGGESTIONS = "search_suggestions";
    private static final String DATA_KEY_MAP = "map";
    private static final String DATA_KEY_HASHTAGS = "hashtags";
    private static final String DATA_KEY_EXPERTS = "experts";
    private static final String DATA_KEY_VIDEOS = "video_library";
    private static final String DATA_KEY_PARTNERS = "partners";

    private static final String[] DATA_KEYS_IN_ORDER = {
            DATA_KEY_ROOMS,
            DATA_KEY_BLOCKS,
            DATA_KEY_TAGS,
            DATA_KEY_SPEAKERS,
            DATA_KEY_SESSIONS,
            DATA_KEY_SEARCH_SUGGESTIONS,
            DATA_KEY_MAP,
            DATA_KEY_HASHTAGS,
            DATA_KEY_EXPERTS,
            DATA_KEY_VIDEOS,
            DATA_KEY_PARTNERS
    };

    Context mContext = null;

    // Handlers for each entity type:
    RoomsHandler mRoomsHandler = null;
    BlocksHandler mBlocksHandler = null;
    TagsHandler mTagsHandler = null;
    SpeakersHandler mSpeakersHandler = null;
    SessionsHandler mSessionsHandler = null;
    SearchSuggestHandler mSearchSuggestHandler = null;
    MapPropertyHandler mMapPropertyHandler = null;
    ExpertsHandler mExpertsHandler = null;
    HashtagsHandler mHashtagsHandler = null;
    VideosHandler mVideosHandler = null;
    PartnersHandler mPartnersHandler = null;

    // Convenience map that maps the key name to its corresponding handler (e.g.
    // "blocks" to mBlocksHandler (to avoid very tedious if-elses)
    HashMap<String, JSONHandler> mHandlerForKey = new HashMap<String, JSONHandler>();

    // Tally of total content provider operations we carried out (for statistical purposes)
    private int mContentProviderOperationsDone = 0;

    public ConferenceDataHandler(Context ctx) {
        mContext = ctx;
    }

    /**
     * Parses the conference data in the given objects and imports the data into the
     * content provider. The format of the data is documented at https://code.google.com/p/iosched.
     *
     * @param dataBodies The collection of JSON objects to parse and import.
     * @param dataTimestamp The timestamp of the data. This should be in RFC1123 format.
     * @param downloadsAllowed Whether or not we are supposed to download data from the internet if needed.
     * @throws IOException If there is a problem parsing the data.
     */
    public void applyConferenceData(String[] dataBodies, String dataTimestamp,
            boolean downloadsAllowed) throws IOException {
        LOGD(TAG, "Applying data from " + dataBodies.length + " files, timestamp " + dataTimestamp);

        // create handlers for each data type
        mHandlerForKey.put(DATA_KEY_ROOMS, mRoomsHandler = new RoomsHandler(mContext));
        mHandlerForKey.put(DATA_KEY_BLOCKS, mBlocksHandler = new BlocksHandler(mContext));
        mHandlerForKey.put(DATA_KEY_TAGS, mTagsHandler = new TagsHandler(mContext));
        mHandlerForKey.put(DATA_KEY_SPEAKERS, mSpeakersHandler = new SpeakersHandler(mContext));
        mHandlerForKey.put(DATA_KEY_SESSIONS, mSessionsHandler = new SessionsHandler(mContext));
        mHandlerForKey.put(DATA_KEY_SEARCH_SUGGESTIONS, mSearchSuggestHandler =
                new SearchSuggestHandler(mContext));
        mHandlerForKey.put(DATA_KEY_MAP, mMapPropertyHandler = new MapPropertyHandler(mContext));
        mHandlerForKey.put(DATA_KEY_EXPERTS, mExpertsHandler = new ExpertsHandler(mContext));
        mHandlerForKey.put(DATA_KEY_HASHTAGS, mHashtagsHandler = new HashtagsHandler(mContext));
        mHandlerForKey.put(DATA_KEY_VIDEOS, mVideosHandler = new VideosHandler(mContext));
        mHandlerForKey.put(DATA_KEY_PARTNERS, mPartnersHandler = new PartnersHandler(mContext));

        // process the jsons. This will call each of the handlers when appropriate to deal
        // with the objects we see in the data.
        LOGD(TAG, "Processing " + dataBodies.length + " JSON objects.");
        for (int i = 0; i < dataBodies.length; i++) {
            LOGD(TAG, "Processing json object #" + (i + 1) + " of " + dataBodies.length);
            processDataBody(dataBodies[i]);
        }

        // the sessions handler needs to know the tag and speaker maps to process sessions
        mSessionsHandler.setTagMap(mTagsHandler.getTagMap());
        mSessionsHandler.setSpeakerMap(mSpeakersHandler.getSpeakerMap());

        // produce the necessary content provider operations
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        for (String key : DATA_KEYS_IN_ORDER) {
            LOGD(TAG, "Building content provider operations for: " + key);
            mHandlerForKey.get(key).makeContentProviderOperations(batch);
            LOGD(TAG, "Content provider operations so far: " + batch.size());
        }
        LOGD(TAG, "Total content provider operations: " + batch.size());

        // download or process local map tile overlay files (SVG files)
        LOGD(TAG, "Processing map overlay files");
        processMapOverlayFiles(mMapPropertyHandler.getTileOverlays(), downloadsAllowed);

        // finally, push the changes into the Content Provider
        LOGD(TAG, "Applying " + batch.size() + " content provider operations.");
        try {
            int operations = batch.size();
            if (operations > 0) {
                mContext.getContentResolver().applyBatch(ScheduleContract.CONTENT_AUTHORITY, batch);
            }
            LOGD(TAG, "Successfully applied " + operations + " content provider operations.");
            mContentProviderOperationsDone += operations;
        } catch (RemoteException ex) {
            LOGE(TAG, "RemoteException while applying content provider operations.");
            throw new RuntimeException("Error executing content provider batch operation", ex);
        } catch (OperationApplicationException ex) {
            LOGE(TAG, "OperationApplicationException while applying content provider operations.");
            throw new RuntimeException("Error executing content provider batch operation", ex);
        }

        // notify all top-level paths
        LOGD(TAG, "Notifying changes on all top-level paths on Content Resolver.");
        ContentResolver resolver = mContext.getContentResolver();
        for (String path : ScheduleContract.TOP_LEVEL_PATHS) {
            Uri uri = ScheduleContract.BASE_CONTENT_URI.buildUpon().appendPath(path).build();
            resolver.notifyChange(uri, null);
        }


    // update our data timestamp
        setDataTimestamp(dataTimestamp);
        LOGD(TAG, "Done applying conference data.");
    }

    public int getContentProviderOperationsDone() {
        return mContentProviderOperationsDone;
    }

    /**
     * Processes a conference data body and calls the appropriate data type handlers
     * to process each of the objects represented therein.
     *
     * @param dataBody The body of data to process
     * @throws IOException If there is an error parsing the data.
     */
    private void processDataBody(String dataBody) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(dataBody));
        JsonParser parser = new JsonParser();
        try {
            reader.setLenient(true); // To err is human

            // the whole file is a single JSON object
            reader.beginObject();

            while (reader.hasNext()) {
                // the key is "rooms", "speakers", "tracks", etc.
                String key = reader.nextName();
                if (mHandlerForKey.containsKey(key)) {
                    // pass the value to the corresponding handler
                    mHandlerForKey.get(key).process(parser.parse(reader));
                } else {
                    LOGW(TAG, "Skipping unknown key in conference data json: " + key);
                    reader.skipValue();
                }
            }
            reader.endObject();
        } finally {
            reader.close();
        }
    }

    /**
     * Synchronise the map overlay files either from the local assets (if available) or from a remote url.
     *
     * @param collection Set of tiles containing a local filename and remote url.
     * @throws IOException
     */
    private void processMapOverlayFiles(Collection<Tile> collection, boolean downloadAllowed) throws IOException, SVGParseException {
        // clear the tile cache on disk if any tiles have been updated
        boolean shouldClearCache = false;
        // keep track of used files, unused files are removed
        ArrayList<String> usedTiles = Lists.newArrayList();
        for (Tile tile : collection) {
            final String filename = tile.filename;
            final String url = tile.url;

            usedTiles.add(filename);

            if (!MapUtils.hasTile(mContext, filename)) {
                shouldClearCache = true;
                // copy or download the tile if it is not stored yet
                if (MapUtils.hasTileAsset(mContext, filename)) {
                    // file already exists as an asset, copy it
                    MapUtils.copyTileAsset(mContext, filename);
                } else if (downloadAllowed && !TextUtils.isEmpty(url)) {
                    try {
                        // download the file only if downloads are allowed and url is not empty
                        File tileFile = MapUtils.getTileFile(mContext, filename);
                        BasicHttpClient httpClient = new BasicHttpClient();
                        httpClient.setRequestLogger(mQuietLogger);
                        HttpResponse httpResponse = httpClient.get(url, null);
                        FileUtils.writeFile(httpResponse.getBody(), tileFile);

                        // ensure the file is valid SVG
                        InputStream is = new FileInputStream(tileFile);
                        SVG svg = new SVGBuilder().readFromInputStream(is).build();
                        is.close();
                    } catch (IOException ex) {
                        LOGE(TAG, "FAILED downloading map overlay tile "+url+
                                ": " + ex.getMessage(), ex);
                    } catch (SVGParseException ex) {
                        LOGE(TAG, "FAILED parsing map overlay tile "+url+
                                ": " + ex.getMessage(), ex);
                    }
                } else {
                    LOGD(TAG, "Skipping download of map overlay tile" +
                            " (since downloadsAllowed=false)");
                }
            }
        }

        if (shouldClearCache) {
            MapUtils.clearDiskCache(mContext);
        }

        MapUtils.removeUnusedTiles(mContext, usedTiles);
    }

    // Returns the timestamp of the data we have in the content provider.
    public String getDataTimestamp() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                SP_KEY_DATA_TIMESTAMP, DEFAULT_TIMESTAMP);
    }

    // Sets the timestamp of the data we have in the content provider.
    public void setDataTimestamp(String timestamp) {
        LOGD(TAG, "Setting data timestamp to: " + timestamp);
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString(
                SP_KEY_DATA_TIMESTAMP, timestamp).commit();
    }

    // Reset the timestamp of the data we have in the content provider
    public static void resetDataTimestamp(final Context context) {
        LOGD(TAG, "Resetting data timestamp to default (to invalidate our synced data)");
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(
                SP_KEY_DATA_TIMESTAMP).commit();
    }

    /**
     * A type of ConsoleRequestLogger that does not log requests and responses.
     */
    private RequestLogger mQuietLogger = new ConsoleRequestLogger(){
        @Override
        public void logRequest(HttpURLConnection uc, Object content) throws IOException { }

        @Override
        public void logResponse(HttpResponse res) { }
    };

}
