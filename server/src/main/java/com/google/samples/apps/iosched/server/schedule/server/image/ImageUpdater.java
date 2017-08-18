/*
 * Copyright 2017 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.server.schedule.server.image;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;
import com.google.common.base.Optional;
import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.schedule.model.InputJsonKeys.VendorAPISource.MainTypes;
import com.google.samples.apps.iosched.server.schedule.model.InputJsonKeys.VendorAPISource.Speakers;
import com.google.samples.apps.iosched.server.schedule.model.JsonDataSource;
import com.google.samples.apps.iosched.server.schedule.model.JsonDataSources;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetches images from EvenPoint, stores in GCS, and set up serving URL with App Engine's image
 * service.
 * <p>
 * For 2017, we only have images for speaker photos. To add the support of another image types:
 * 1. Update GCS_IMAGE_FOLDERS to specify a GCS folder where images of that type should be stored.
 * 2. Update parseImageUrls to extract image URLs from EventPoint sources.
 */
public class ImageUpdater {

  private static final Map<MainTypes, String> GCS_IMAGE_FOLDERS;

  static {
    GCS_IMAGE_FOLDERS = new HashMap<>();
    GCS_IMAGE_FOLDERS.put(MainTypes.speakers, "images/speakers");
  }

  private final Logger LOGGER = Logger.getLogger(ImageUpdater.class.getName());

  private final GcsService gcsService = GcsServiceFactory.createGcsService(
      RetryParams.getDefaultInstance());

  private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

  /**
   * An ImageUpdate run prepares images in GCS for serving. It fetches images from EventPoint URLs,
   * stores image files in GCS, and sets up serving URLs with App Engine's image service. The
   * serving URLs allow on-demand image resizing with URL parameters.
   *
   * We only fetch and process images for new URLs encountered from EventPoint. We won't detect
   * image content changes from URLs previously processed. EventPoint ensures the image from a URL
   * will not change.
   *
   * @see <a href="https://cloud.google.com/appengine/docs/standard/java/images/">Google App Engine
   * - Overview of Images API for Java</a>
   */
  public void run(JsonDataSources rawCmsSources) throws IOException {

    Map<MainTypes, List<String>> sourceUrlsByType = parseImageUrls(rawCmsSources);
    for (MainTypes type : sourceUrlsByType.keySet()) {
      List<String> sourceUrls = sourceUrlsByType.get(type);

      for (String sourceUrl : sourceUrls) {
        // Skip URLs that have been previously fetched and processed.
        if (ServingUrlManager.INSTANCE.getServingUrl(sourceUrl) != null) {
          LOGGER.info("Skip previously fetched image at '" + sourceUrl + "'.");
          continue;
        }

        // Fetches image from EventPoint URL.
        URL url = new URL(sourceUrl);
        HTTPResponse response = null;
        try {
          response = urlFetchService.fetch(url);
        } catch (IOException e) {
          // response is null when fetching throws exception.
        }
        if (response == null || response.getResponseCode() != SC_OK) {
          LOGGER.log(Level.WARNING, "Failed to fetch image at '" + sourceUrl + "'.");
          continue;
        }
        byte[] imageData = response.getContent();

        // Stores image to GCS using image content's MD5 as file name.
        String imageId = Hashing.md5().hashBytes(imageData).toString();
        String imageExt = parseFileExtFromUrl(sourceUrl);
        GcsFilename gcsFilename = new GcsFilename(Config.CLOUD_STORAGE_BUCKET,
            GCS_IMAGE_FOLDERS.get(type) + "/" + imageId + "." + imageExt);
        try {
          gcsService.createOrReplace(
              gcsFilename, GcsFileOptions.getDefaultInstance(), ByteBuffer.wrap(imageData));
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Failed to store image at " + sourceUrl + " to GCS.");
          continue;
        }

        // Set up serving URL using ImageService.
        ServingUrlManager.INSTANCE.createServingUrl(gcsFilename, Optional.of(sourceUrl));
        LOGGER.info("Fetched image at '" + sourceUrl + "'.");
      }
    }
  }

  /**
   * Extracts the list of image URLs for every type in EventPoint sources.
   */
  static Map<MainTypes, List<String>> parseImageUrls(JsonDataSources rawCmsSources) {
    Map<MainTypes, List<String>> imageUrls = new HashMap<>();

    // Extract speaker photos.
    imageUrls.put(MainTypes.speakers, new ArrayList<String>());
    JsonDataSource speakers = rawCmsSources.getSource(MainTypes.speakers.name());
    for (JsonObject speaker : speakers) {
      String photoUrl = speaker.get(Speakers.Photo.name()).getAsString();
      if (!isNullOrEmpty(photoUrl)) {
        imageUrls.get(MainTypes.speakers).add(photoUrl);
      }
    }
    return imageUrls;
  }

  static String parseFileExtFromUrl(String sourceUrl) {
    if (isNullOrEmpty(sourceUrl)) {
      return null;
    }
    String ext = null;
    try {
      String path = new URI(sourceUrl).getPath();
      int idx = path.lastIndexOf('.');
      if (idx >= 0) {
        ext = path.substring(idx + 1);
      }
    } catch (URISyntaxException e) {
    }
    return isNullOrEmpty(ext) ? "jpg" : ext;  // return "jpg" if failed to parse extension.
  }
}
