/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.model;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.os.CancellationSignal;

import com.google.samples.apps.iosched.provider.ScheduleContract.Tags;
import com.google.samples.apps.iosched.util.CursorModelLoader;
import com.google.samples.apps.iosched.util.CursorModelLoader.CursorTransform;


public class TagMetadataCursorTransform implements CursorTransform<TagMetadata> {

    @Override
    public Cursor performQuery(@NonNull CursorModelLoader<TagMetadata> loader,
            @NonNull CancellationSignal cancellationSignal) {
        return ContentResolverCompat.query(loader.getContext().getContentResolver(),
                Tags.CONTENT_URI, TagMetadata.TAGS_PROJECTION, null, null, null,
                cancellationSignal);
    }

    @Override
    public TagMetadata cursorToModel(@NonNull CursorModelLoader<TagMetadata> loader,
            @NonNull Cursor cursor) {
        return new TagMetadata(cursor);
    }

    @Override
    public Uri getObserverUri(@NonNull CursorModelLoader<TagMetadata> loader) {
        return Tags.CONTENT_URI;
    }
}
