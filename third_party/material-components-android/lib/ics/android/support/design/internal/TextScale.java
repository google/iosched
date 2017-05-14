/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.design.internal;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.transition.Transition;
import android.support.transition.TransitionValues;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Map;

/** @hide */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(14)
public class TextScale extends Transition {
  private static final String PROPNAME_SCALE = "android:textscale:scale";

  @Override
  public void captureStartValues(TransitionValues transitionValues) {
    captureValues(transitionValues);
  }

  @Override
  public void captureEndValues(TransitionValues transitionValues) {
    captureValues(transitionValues);
  }

  private void captureValues(TransitionValues transitionValues) {
    if (transitionValues.view instanceof TextView) {
      TextView textview = (TextView) transitionValues.view;
      transitionValues.values.put(PROPNAME_SCALE, textview.getScaleX());
    }
  }

  @Override
  public Animator createAnimator(
      ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
    if (startValues == null
        || endValues == null
        || !(startValues.view instanceof TextView)
        || !(endValues.view instanceof TextView)) {
      return null;
    }
    final TextView view = (TextView) endValues.view;
    Map<String, Object> startVals = startValues.values;
    Map<String, Object> endVals = endValues.values;
    final float startSize =
        startVals.get(PROPNAME_SCALE) != null ? (float) startVals.get(PROPNAME_SCALE) : 1f;
    final float endSize =
        endVals.get(PROPNAME_SCALE) != null ? (float) endVals.get(PROPNAME_SCALE) : 1f;
    if (startSize == endSize) {
      return null;
    }

    ValueAnimator animator = ValueAnimator.ofFloat(startSize, endSize);

    animator.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float animatedValue = (float) valueAnimator.getAnimatedValue();
            view.setScaleX(animatedValue);
            view.setScaleY(animatedValue);
          }
        });
    return animator;
  }
}
