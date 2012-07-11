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

package com.google.android.apps.iosched.io;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Abstract class that handles reading and parsing an {@link XmlPullParser} into
 * a set of {@link ContentProviderOperation}. It catches recoverable network
 * exceptions and rethrows them as {@link HandlerException}. Any local
 * {@link ContentProvider} exceptions are considered unrecoverable.
 * <p>
 * This class is only designed to handle simple one-way synchronization.
 */
public abstract class XmlHandler {
    private final String mAuthority;

    public XmlHandler(String authority) {
        mAuthority = authority;
    }

    /**
     * Parse the given {@link XmlPullParser}, turning into a series of
     * {@link ContentProviderOperation} that are immediately applied using the
     * given {@link ContentResolver}.
     */
    public void parseAndApply(XmlPullParser parser, ContentResolver resolver)
            throws HandlerException {
        try {
            final ArrayList<ContentProviderOperation> batch = parse(parser, resolver);
            resolver.applyBatch(mAuthority, batch);

        } catch (HandlerException e) {
            throw e;
        } catch (XmlPullParserException e) {
            throw new HandlerException("Problem parsing XML response", e);
        } catch (IOException e) {
            throw new HandlerException("Problem reading response", e);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            // TODO: write unit tests to exercise full provider
            // TODO: consider catching version checking asserts here, and then
            // wrapping around to retry parsing again.
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    /**
     * Parse the given {@link XmlPullParser}, returning a set of
     * {@link ContentProviderOperation} that will bring the
     * {@link ContentProvider} into sync with the parsed data.
     */
    public abstract ArrayList<ContentProviderOperation> parse(XmlPullParser parser,
            ContentResolver resolver) throws XmlPullParserException, IOException;

    /**
     * General {@link IOException} that indicates a problem occured while
     * parsing or applying an {@link XmlPullParser}.
     */
    public static class HandlerException extends IOException {
        public HandlerException(String message) {
            super(message);
        }

        public HandlerException(String message, Throwable cause) {
            super(message);
            initCause(cause);
        }

        @Override
        public String toString() {
            if (getCause() != null) {
                return getLocalizedMessage() + ": " + getCause();
            } else {
                return getLocalizedMessage();
            }
        }
    }
}
