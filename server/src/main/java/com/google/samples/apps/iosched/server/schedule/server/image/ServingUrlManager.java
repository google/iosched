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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.common.base.Optional;
import java.util.List;
import org.joda.time.DateTime;

/**
 * Manages serving URLs for image files in GCS. This class uses Google Cloud Datastore to store
 * serving URL information for GCS images files.
 */
public enum ServingUrlManager {
  INSTANCE; // Enum-enforced singleton instance

  static final String ENTITY_KIND = "ImageServingUrl";
  static final String SERVING_URL_PROPERTY = "servingUrl";
  static final String SOURCE_URL_PROPERTY = "sourceUrl";
  static final String TIMESTAMP_PROPERTY = "timestamp";

  DatastoreService datastore;
  ImagesService imagesService;

  ServingUrlManager() {
    this.datastore = DatastoreServiceFactory.getDatastoreService();
    this.imagesService = ImagesServiceFactory.getImagesService();
  }

  /**
   * Create a serving URL for an image file in GCS using App Engine's Image Service.
   *
   * @param gcsFilename GCS filename of the image file.
   * @param sourceUrlOpt Optionally the external URL where the image was originally fetched.
   * @return the created serving URL as a plain string.
   */
  public String createServingUrl(GcsFilename gcsFilename, Optional<String> sourceUrlOpt) {
    // Return serving URL if already created for the GCS file.
    String servingUrl = getServingUrl(gcsFilename);
    if (servingUrl != null) {
      return servingUrl;
    }

    // Generate serving URL using ImageService.
    servingUrl = imagesService.getServingUrl(ServingUrlOptions.Builder
        .withGoogleStorageFileName(getGcsFullPath(gcsFilename)).secureUrl(true));

    // Record the serving URL in Datastore.
    recordServingUrl(gcsFilename, servingUrl, sourceUrlOpt);
    return servingUrl;
  }

  /**
   * Retrieve a previously created serving URL by specifying the GCS filename.
   */
  public String getServingUrl(GcsFilename gcsFilename) {
    if (gcsFilename == null) {
      return null;
    }
    Key key = KeyFactory.createKey(ENTITY_KIND, getGcsFullPath(gcsFilename));
    try {
      return (String) datastore.get(key).getProperty(SERVING_URL_PROPERTY);
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  /**
   * Retrieve a previously created serving URL by specifying the source image URL.
   */
  public String getServingUrl(String sourceUrl) {
    if (isNullOrEmpty(sourceUrl)) {
      return null;
    }
    Query query =
        new Query(ENTITY_KIND)
            .setFilter(new FilterPredicate(SOURCE_URL_PROPERTY, FilterOperator.EQUAL, sourceUrl));
    List<Entity> result = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
    if ((result == null || result.isEmpty())) {
      return null;
    }
    return (String) result.get(0).getProperty(SERVING_URL_PROPERTY);
  }


  private void recordServingUrl(GcsFilename gcsFilename, String servingUrl,
      Optional<String> sourceUrlOpt) {
    Key key = KeyFactory.createKey(ENTITY_KIND, getGcsFullPath(gcsFilename));

    Entity entity = new Entity(key);
    entity.setProperty(SERVING_URL_PROPERTY, servingUrl);
    entity.setProperty(SOURCE_URL_PROPERTY, sourceUrlOpt.or(""));
    entity.setProperty(TIMESTAMP_PROPERTY, DateTime.now().getMillis());
    datastore.put(entity);
  }

  private String getGcsFullPath(GcsFilename gcsFilename) {
    return String.format("/gs/%s/%s", gcsFilename.getBucketName(), gcsFilename.getObjectName());
  }
}
