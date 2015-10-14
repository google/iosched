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
package com.google.samples.apps.iosched.server.schedule.server.input;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.samples.apps.iosched.server.schedule.Config;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.EntityFetcher;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.VendorAPIEntityFetcher;
import com.google.samples.apps.iosched.server.schedule.model.InputJsonKeys;

import java.io.IOException;
import java.util.HashMap;

/**
 * Encapsulation of the VendorAPI fetcher.
 */
public class VendorDynamicInput extends DataSourceInput<InputJsonKeys.VendorAPISource.MainTypes> {

  private boolean extractUnpublished = Config.SHOW_UNPUBLISHED_DATA;

  public VendorDynamicInput() {
    super(new VendorAPIEntityFetcher());
  }

  public VendorDynamicInput(EntityFetcher fetcher) {
    super(fetcher);
  }

  public void setExtractUnpublished(boolean extractUnpublished) {
    this.extractUnpublished = extractUnpublished;
  }

  @Override
  public Class<InputJsonKeys.VendorAPISource.MainTypes> getType() {
    return InputJsonKeys.VendorAPISource.MainTypes.class;
  }

  @Override
  public JsonArray fetch(InputJsonKeys.VendorAPISource.MainTypes entityType) throws IOException {
    return fetchArray(entityType, 1);
  }

  public JsonArray fetchArray(InputJsonKeys.VendorAPISource.MainTypes entityType,
      int page) throws IOException {

    HashMap<String, String> params = null;

    if (entityType.equals(InputJsonKeys.VendorAPISource.MainTypes.topics) || entityType.equals(InputJsonKeys.VendorAPISource.MainTypes.speakers)) {
      params = new HashMap<String, String>();

      // Topics and speakers require param "includeinfo=true" to bring extra data
      params.put("includeinfo", "true");

      if (entityType.equals(InputJsonKeys.VendorAPISource.MainTypes.topics)) {
        if (extractUnpublished) {
          params.put("minpublishstatus", "0");
        }
      }
    }

    if (page == 0) {
      page = 1;
    } else if (page > 1) {
      if (params == null) {
        params = new HashMap<String, String>();
      }
      params.put("page", Integer.toString(page));
    }

    JsonElement element = getFetcher().fetch(entityType, params);

    if (element.isJsonArray()) {
        return element.getAsJsonArray();
    } else if (element.isJsonObject()) {
      // check if there are extra pages requiring further fetching
      JsonObject obj = element.getAsJsonObject();
      checkPagingConsistency(entityType, page, obj);

      int pageSize = obj.get("pagesize").getAsInt();
      int totalEntities = obj.get("total").getAsInt();
      JsonArray elements = getEntities(obj);
      if (page*pageSize < totalEntities) {
        // fetch the next page
        elements.addAll(fetchArray(entityType, page+1));
      }
      return elements;
    } else {
      throw new JsonParseException("Invalid response from Vendor API. Request should return "
          + "either a JsonArray or a JsonObject, but returned "+element.getClass().getName()
          +". Entity fetcher is "+getFetcher());
    }
  }

  private void checkPagingConsistency(InputJsonKeys.VendorAPISource.MainTypes entityType,
      int requestedPage, JsonObject obj) {
    if (!obj.has("page") || !obj.has("pagesize") || !obj.has("total") ||
        (!obj.has("results") && !obj.has("topics"))) {
      throw new JsonParseException("Invalid response from Vendor API when"
          + "paging "+entityType+" results. At least one of the required properties "
          + "(page, pagesize, total, results|topics) could not be found.");
    }
    int currentPage = obj.get("page").getAsInt();
    if (requestedPage>0 && requestedPage != currentPage) {
      throw new JsonParseException("Invalid response from Vendor API when"
          + "paging "+entityType+" results. Requested page "+requestedPage
          +" but got page "+currentPage);
    }
  }

  private JsonArray getEntities(JsonObject pagedObject) {
    JsonArray elements;
    if (pagedObject.has("results")) {
      elements = pagedObject.get("results").getAsJsonArray();
    } else if (pagedObject.has("topics")) {
      elements = pagedObject.get("topics").getAsJsonArray();
    } else {
      throw new JsonParseException("Invalid JSON format for a paged result. Expected either a \"results\" or a topics array property.");
    }
    return elements;
  }
}
