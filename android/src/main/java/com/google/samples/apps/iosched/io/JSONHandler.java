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

package com.google.samples.apps.iosched.io;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;

import android.content.ContentProviderOperation;
import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

public abstract class JSONHandler {

    protected static Context mContext;

    public JSONHandler(Context context) {
        mContext = context;
    }

    public abstract void makeContentProviderOperations(ArrayList<ContentProviderOperation> list);

    public abstract void process(JsonElement element);

    /**
     * Loads a raw resource and returns its content as a String.
     *
     * @throws IOException If any error was encountered, such as an incorrect resource ID, or
     *                     inaccessible file.
     */
    public static String parseResource(Context context, int resource) throws IOException {
        InputStream stream = null;
        String data;
        try {
            stream = context.getResources().openRawResource(resource);
            data = parseStream(stream);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore exceptions during stream close, other exceptions thrown earlier will
                    // be handled by the calling methods
                }
            }
        }

        return data;
    }

    /**
     * Loads a file and returns its content as a String.
     *
     * @throws IOException If any error was encountered, such as an incorrect resource ID, or
     *                     inaccessible file.
     */
    public static String parseFile(String filePath) throws IOException {
        InputStream stream = null;
        String data;
        try {
            stream = new FileInputStream(filePath);
            data = parseStream(stream);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore exceptions during stream close, other exceptions thrown earlier will
                    // be handled by the calling methods
                }
            }
        }

        return data;
    }

    private static String parseStream(final InputStream stream) throws IOException {
        Reader reader = null;
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];

        // IO errors are passed up to the calling method and must be caught there.
        try {
            reader = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore exceptions during stream close, other exceptions thrown earlier will
                    // be handled by the calling methods
                }
            }
        }

        return writer.toString();
    }
}
