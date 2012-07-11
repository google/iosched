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

import com.google.android.apps.iosched.io.XmlHandler.HandlerException;
import com.google.android.apps.iosched.util.ParserUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Opens a local {@link Resources#getXml(int)} and passes the resulting
 * {@link XmlPullParser} to the given {@link XmlHandler}.
 */
public class LocalExecutor {
    private Resources mRes;
    private ContentResolver mResolver;

    public LocalExecutor(Resources res, ContentResolver resolver) {
        mRes = res;
        mResolver = resolver;
    }

    public void execute(Context context, String assetName, XmlHandler handler)
            throws HandlerException {
        try {
            final InputStream input = context.getAssets().open(assetName);
            final XmlPullParser parser = ParserUtils.newPullParser(input);
            handler.parseAndApply(parser, mResolver);
        } catch (HandlerException e) {
            throw e;
        } catch (XmlPullParserException e) {
            throw new HandlerException("Problem parsing local asset: " + assetName, e);
        } catch (IOException e) {
            throw new HandlerException("Problem parsing local asset: " + assetName, e);
        }
    }

    public void execute(int resId, XmlHandler handler) throws HandlerException {
        final XmlResourceParser parser = mRes.getXml(resId);
        try {
            handler.parseAndApply(parser, mResolver);
        } finally {
            parser.close();
        }
    }
}
