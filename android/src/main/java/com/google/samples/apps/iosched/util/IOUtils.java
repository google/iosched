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

import android.content.Context;

import com.google.android.gms.plus.Account;
import com.google.common.base.Charsets;
import com.google.samples.apps.iosched.BuildConfig;
import com.turbomanage.httpclient.BasicHttpClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Utility methods and constants used for writing and reading to from streams and files.
 */
public class IOUtils {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final boolean AUTHORIZATION_TO_BACKEND_REQUIRED = BuildConfig.DEBUG;

    /**
     * Writes the given string to a {@link File}.
     *
     * @param data The data to be written to the File.
     * @param file The File to write to.
     * @throws IOException
     */
    public static void writeToFile(String data, File file) throws IOException {
        writeToFile(data.getBytes(Charsets.UTF_8), file);
    }

    /**
     * Write the given bytes to a {@link File}.
     *
     * @param data The bytes to be written to the File.
     * @param file The {@link File} to be used for writing the data.
     * @throws IOException
     */
    public static void writeToFile(byte[] data, File file) throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(data);
            os.flush();
            // Perform an fsync on the FileOutputStream.
            os.getFD().sync();
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * Write the given content to an {@link OutputStream}
     * <p/>
     * Note: This method closes the given OutputStream.
     *
     * @param content The String content to write to the OutputStream.
     * @param os The OutputStream to which the content should be written.
     * @throws IOException
     */
    public static void writeToStream(String content, OutputStream os) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(os, Charsets.UTF_8));
            writer.write(content);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Reads a {@link File} as a String
     *
     * @param file The file to be read in.
     * @return Returns the contents of the File as a String.
     * @throws IOException
     */
    public static String readFileAsString(File file) throws IOException {
        return readAsString(new FileInputStream(file));
    }

    /**
     * Reads an {@link InputStream} into a String using the UTF-8 encoding.
     * Note that this method closes the InputStream passed to it.
     *
     *
     * @param is The InputStream to be read.
     * @return The contents of the InputStream as a String.
     * @throws IOException
     */
    public static String readAsString(InputStream is) throws IOException {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();
    }

    /**
     * If {@code AUTHORIZATION_TO_BACKEND_REQUIRED} is true add an authentication header to the
     * given request. The currently signed in user is used to retrieve the auth token.
     * Allows pre-release builds to use a Google Cloud storage bucket with non-public data.
     *
     * @param context Context used to retrieve auth token from SharedPreferences.
     * @param basicHttpClient HTTP client to which the authorization header will be added.
     */
    public static void authorizeHttpClient(Context context, BasicHttpClient basicHttpClient) {
        if (basicHttpClient == null || AccountUtils.getAuthToken(context) == null) {
            return;
        }
        if (AUTHORIZATION_TO_BACKEND_REQUIRED) {
            basicHttpClient.addHeader(AUTHORIZATION_HEADER,
                    BEARER_PREFIX + AccountUtils.getAuthToken(context));
        }
    }
}
