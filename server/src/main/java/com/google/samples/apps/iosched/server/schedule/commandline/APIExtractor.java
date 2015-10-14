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
package com.google.samples.apps.iosched.server.schedule.commandline;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.EntityFetcher;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.HTTPRemoteFilesEntityFetcher;
import com.google.samples.apps.iosched.server.schedule.input.fetcher.RemoteFilesEntityFetcherFactory;
import com.google.samples.apps.iosched.server.schedule.model.DataExtractor;
import com.google.samples.apps.iosched.server.schedule.model.JsonDataSources;
import com.google.samples.apps.iosched.server.schedule.server.input.ExtraInput;
import com.google.samples.apps.iosched.server.schedule.server.input.VendorDynamicInput;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.Channels;

/**
 * A class usable on command line that extracts the session data from the CMS.
 */
public class APIExtractor {

  /**
   *
   */
  public APIExtractor() {
    RemoteFilesEntityFetcherFactory.setBuilder(new RemoteFilesEntityFetcherFactory.FetcherBuilder() {
        String[] filenames;

        @Override
        public RemoteFilesEntityFetcherFactory.FetcherBuilder setSourceFiles(String... filenames) {
            this.filenames = filenames;
            return this;
        }

        @Override
        public EntityFetcher build() {
            return new HTTPRemoteFilesEntityFetcher(filenames);
        }
    });
  }


  public void run(OutputStream optionalOutput, boolean extractUnpublished) throws IOException {
    // fill sources with extra input:
    JsonDataSources sources = new ExtraInput().fetchAllDataSources();
    // fill sources with vendor API input:
    VendorDynamicInput vendorInput = new VendorDynamicInput();
    vendorInput.setExtractUnpublished(extractUnpublished);
    sources.putAll(vendorInput.fetchAllDataSources());
    // extract session data from inputs:
    JsonObject newData = new DataExtractor(false).extractFromDataSources(sources);

    // send data to the outputstream
    Writer writer = Channels.newWriter(Channels.newChannel(optionalOutput), "UTF-8");
    JsonWriter optionalOutputWriter = new JsonWriter(writer);
    optionalOutputWriter.setIndent("  ");
    new Gson().toJson(newData, optionalOutputWriter);
    optionalOutputWriter.flush();
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    boolean extractUnpublished = args.length>0 && args[0].equals("-u");
    new APIExtractor().run(System.out, extractUnpublished);
  }

}
