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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.StateSet;
import java.util.ArrayList;

final class StateListAnimator {

  private final ArrayList<Tuple> mTuples = new ArrayList<>();

  private Tuple mLastMatch = null;
  ValueAnimator mRunningAnimator = null;

  private final ValueAnimator.AnimatorListener mAnimationListener =
      new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
          if (mRunningAnimator == animator) {
            mRunningAnimator = null;
          }
        }
      };

  /**
   * Associates the given Animation with the provided drawable state specs so that it will be run
   * when the View's drawable state matches the specs.
   *
   * @param specs The drawable state specs to match against
   * @param animator The animator to run when the specs match
   */
  public void addState(int[] specs, ValueAnimator animator) {
    Tuple tuple = new Tuple(specs, animator);
    animator.addListener(mAnimationListener);
    mTuples.add(tuple);
  }

  /** Called by View */
  void setState(int[] state) {
    Tuple match = null;
    final int count = mTuples.size();
    for (int i = 0; i < count; i++) {
      final Tuple tuple = mTuples.get(i);
      if (StateSet.stateSetMatches(tuple.mSpecs, state)) {
        match = tuple;
        break;
      }
    }
    if (match == mLastMatch) {
      return;
    }
    if (mLastMatch != null) {
      cancel();
    }

    mLastMatch = match;

    if (match != null) {
      start(match);
    }
  }

  private void start(Tuple match) {
    mRunningAnimator = match.mAnimator;
    mRunningAnimator.start();
  }

  private void cancel() {
    if (mRunningAnimator != null) {
      mRunningAnimator.cancel();
      mRunningAnimator = null;
    }
  }

  /**
   * If there is an animation running for a recent state change, ends it.
   *
   * <p>This causes the animation to assign the end value(s) to the View.
   */
  public void jumpToCurrentState() {
    if (mRunningAnimator != null) {
      mRunningAnimator.end();
      mRunningAnimator = null;
    }
  }

  static class Tuple {
    final int[] mSpecs;
    final ValueAnimator mAnimator;

    Tuple(int[] specs, ValueAnimator animator) {
      mSpecs = specs;
      mAnimator = animator;
    }
  }
}
