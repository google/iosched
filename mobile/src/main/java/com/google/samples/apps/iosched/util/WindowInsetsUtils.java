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
import android.view.WindowInsets;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import androidx.core.os.BuildCompat;
import timber.log.Timber;

/**
 * Utils for accessing new {@link WindowInsets} APIs added in Android Q
 */
@SuppressLint("PrivateApi")
public class WindowInsetsUtils {
    private static Method methodGetTappableElementInsets;
    private static boolean methodGetTappableElementInsetsFetched;

    private static boolean insetsFetched;
    private static Class classInsets;
    private static Field fieldInsetsLeft;
    private static Field fieldInsetsTop;
    private static Field fieldInsetsRight;
    private static Field fieldInsetsBottom;

    private WindowInsetsUtils() {}

    /**
     * Returns the system window insets for the given {@link WindowInsets} as a Rect
     */
    private static Rect getSystemWindowInsets(@NonNull final WindowInsets insets) {
        return new Rect(
                insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
    }

    /**
     * Returns the tappable element insets. On Q we use reflection to access the API, before that
     */
    public static Rect getTappableElementInsets(@NonNull final WindowInsets insets) {
        if (!BuildCompat.isAtLeastQ()) {
            // For devices running P and earlier, just return the system window insets
            return getSystemWindowInsets(insets);
        }

        // If we reach here, we're running on a Android Q device so we can expect the APIs
        // to work as intended

        fetchInsetsClazz();
        fetchGetTappableElementsInsetsMethod();

        if (classInsets == null || methodGetTappableElementInsets == null) {
            // Something has gone wrong here, just return the system window insets
            return getSystemWindowInsets(insets);
        }

        try {
            final Object tappableElementInsets = methodGetTappableElementInsets.invoke(insets);
            return insetsToRect(tappableElementInsets);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Timber.e(e, "Error while invoking getTappableElementInsets via reflection");
        }

        // If we get here, something has gone wrong. We will just return the system window insets
        return getSystemWindowInsets(insets);
    }

    private static void fetchInsetsClazz() {
        if (!insetsFetched) {
            try {
                classInsets = Class.forName("android.graphics.Insets");
                fieldInsetsLeft = classInsets.getField("left");
                fieldInsetsTop = classInsets.getField("top");
                fieldInsetsRight = classInsets.getField("right");
                fieldInsetsBottom = classInsets.getField("bottom");
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                // Class does not exist before API 29
                if (BuildCompat.isAtLeastQ()) {
                    Timber.e(e, "Error while retrieving Insets class via reflection");
                }
            }
            insetsFetched = true;
        }
    }

    private static void fetchGetTappableElementsInsetsMethod() {
        if (!methodGetTappableElementInsetsFetched) {
            try {
                methodGetTappableElementInsets =
                        WindowInsets.class.getDeclaredMethod("getTappableElementInsets");
            } catch (NoSuchMethodException e) {
                // Class does not exist before API 29
                if (BuildCompat.isAtLeastQ()) {
                    Timber.e(e, "Error while retrieving getTappableElementInsets"
                            + " method via reflection");
                }
            }
            methodGetTappableElementInsetsFetched = true;
        }
    }

    @NonNull
    private static Rect insetsToRect(@NonNull final Object insets) throws IllegalAccessException {
        return new Rect(fieldInsetsLeft.getInt(insets), fieldInsetsTop.getInt(insets),
                fieldInsetsRight.getInt(insets), fieldInsetsBottom.getInt(insets));
    }
}
