/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.util;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.core.os.BuildCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

/**
 * Utils for accessing new {@link View} APIs added in Android Q for system gestures
 */
@SuppressLint("PrivateApi")
public class ViewGestureUtils {
    private ViewGestureUtils() {}

    @SuppressWarnings("unchecked")
    @NonNull
    public static List<Rect> getSystemGestureExclusionRects(@NonNull final View view) {
        if (BuildCompat.isAtLeastQ()) {
            try {
                Method method = view.getClass().getMethod("getSystemGestureExclusionRects");
                return (List<Rect>) method.invoke(view);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // Method does not exist before API 29
                Timber.e(e, "Error while retrieving getSystemGestureExclusionRects method via reflection");
            }
        }
        // If we're not running on Q, or we hit an error, just return an empty list
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static void setSystemGestureExclusionRects(
            @NonNull final View view, @NonNull final List<Rect> rects) {
        if (BuildCompat.isAtLeastQ()) {
            try {
                Method method = view.getClass().getMethod(
                        "setSystemGestureExclusionRects", List.class);
                method.invoke(view, rects);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // Method does not exist before API 29
                Timber.e(e, "Error while retrieving setSystemGestureExclusionRects method via reflection");
            }
        }
    }

    @SuppressLint("NewApi") // Suppressed because lint doesn't recognize isAtLeastQ()
    public static boolean shouldCloseDrawerFromBackPress(@NonNull final View view) {
        if (BuildCompat.isAtLeastQ()) {
            // If we're running on Q, and this call to closeDrawers if from a key event
            // (for back handling), we should only honor it IF the device is not currently
            // in gesture mode. We approximate that by checking the system gesture insets
            final Rect systemGestureInsets = WindowInsetsUtils.getSystemGestureInsets(
                    view.getRootWindowInsets());
            return systemGestureInsets.left == 0 && systemGestureInsets.right == 0;
        }
        // On P and earlier, always close the drawer
        return true;
    }
}
