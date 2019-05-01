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
    private static Method methodGetMandatorySystemGestureInsets;
    private static boolean methodGetMandatorySystemGestureInsetsFetched;

    private static Method methodGetSystemGestureInsets;
    private static boolean methodGetSystemGestureInsetsFetched;

    private static boolean insetsFetched;
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
                insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
    }

    /**
     * Returns the system gesture insets. On Q we use reflection to access the API, before that
     * we just use the system window insets
     */
    public static Rect getSystemGestureInsets(@NonNull final WindowInsets insets) {
        // Method does not exist before API 29
        if (BuildCompat.isAtLeastQ()) {
            fetchInsetsFields();
            fetchGetSystemGestureInsetsMethod();
            return invokeInsetsMethod(methodGetSystemGestureInsets, insets);
        }
        // For devices running P and earlier, just return the system window insets
        return getSystemWindowInsets(insets);
    }

    /**
     * Returns the mandatory system gesture insets. On Q we use reflection to access the API,
     * before that we just use the system window insets
     */
    @SuppressWarnings("unused")
    public static Rect getMandatorySystemGestureInsets(@NonNull final WindowInsets insets) {
        // Method does not exist before API 29
        if (BuildCompat.isAtLeastQ()) {
            fetchInsetsFields();
            fetchGetMandatorySystemGestureInsetsMethod();
            return invokeInsetsMethod(methodGetMandatorySystemGestureInsets, insets);
        }
        // For devices running P and earlier, just return the system window insets
        return getSystemWindowInsets(insets);
    }

    private static Rect invokeInsetsMethod(Method method, @NonNull final WindowInsets insets) {
        if (method != null) {
            try {
                return insetsToRect(method.invoke(insets));
            } catch (IllegalAccessException | InvocationTargetException e) {
                Timber.e(e, "Error while invoking %s via reflection", method.getName());
            }
        }
        // If we get here, something has gone wrong. We will just return the system window insets
        return getSystemWindowInsets(insets);
    }

    private static void fetchInsetsFields() {
        if (!insetsFetched) {
            try {
                Class classInsets = Class.forName("android.graphics.Insets");
                fieldInsetsLeft = classInsets.getField("left");
                fieldInsetsTop = classInsets.getField("top");
                fieldInsetsRight = classInsets.getField("right");
                fieldInsetsBottom = classInsets.getField("bottom");
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                Timber.e(e, "Error while retrieving Insets class via reflection");
            }
            insetsFetched = true;
        }
    }

    private static void fetchGetSystemGestureInsetsMethod() {
        if (!methodGetSystemGestureInsetsFetched) {
            try {
                //noinspection JavaReflectionMemberAccess
                methodGetSystemGestureInsets =
                        WindowInsets.class.getMethod("getSystemGestureInsets");
            } catch (NoSuchMethodException e) {
                Timber.e(e, "Error while retrieving getSystemGestureInsets method via reflection");
            }
            methodGetSystemGestureInsetsFetched = true;
        }
    }

    private static void fetchGetMandatorySystemGestureInsetsMethod() {
        if (!methodGetMandatorySystemGestureInsetsFetched) {
            try {
                //noinspection JavaReflectionMemberAccess
                methodGetMandatorySystemGestureInsets =
                        WindowInsets.class.getMethod("getMandatorySystemGestureInsets");
            } catch (NoSuchMethodException e) {
                Timber.e(e, "Error while retrieving getMandatorySystemGestureInsets method via reflection");
            }
            methodGetMandatorySystemGestureInsetsFetched = true;
        }
    }

    @NonNull
    private static Rect insetsToRect(@NonNull final Object insets) throws IllegalAccessException {
        return new Rect(fieldInsetsLeft.getInt(insets), fieldInsetsTop.getInt(insets),
                fieldInsetsRight.getInt(insets), fieldInsetsBottom.getInt(insets));
    }
}
