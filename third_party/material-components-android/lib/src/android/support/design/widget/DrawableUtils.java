/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.design.widget;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.util.Log;
import java.lang.reflect.Method;

/** Caution. Gross hacks ahead. */
class DrawableUtils {

  private static final String LOG_TAG = "DrawableUtils";

  private static Method sSetConstantStateMethod;
  private static boolean sSetConstantStateMethodFetched;

  private DrawableUtils() {}

  static boolean setContainerConstantState(
      DrawableContainer drawable, Drawable.ConstantState constantState) {
    // We can use getDeclaredMethod() on v9+
    return setContainerConstantStateV9(drawable, constantState);
  }

  private static boolean setContainerConstantStateV9(
      DrawableContainer drawable, Drawable.ConstantState constantState) {
    if (!sSetConstantStateMethodFetched) {
      try {
        sSetConstantStateMethod =
            DrawableContainer.class.getDeclaredMethod(
                "setConstantState", DrawableContainer.DrawableContainerState.class);
        sSetConstantStateMethod.setAccessible(true);
      } catch (NoSuchMethodException e) {
        Log.e(LOG_TAG, "Could not fetch setConstantState(). Oh well.");
      }
      sSetConstantStateMethodFetched = true;
    }
    if (sSetConstantStateMethod != null) {
      try {
        sSetConstantStateMethod.invoke(drawable, constantState);
        return true;
      } catch (Exception e) {
        Log.e(LOG_TAG, "Could not invoke setConstantState(). Oh well.");
      }
    }
    return false;
  }
}
