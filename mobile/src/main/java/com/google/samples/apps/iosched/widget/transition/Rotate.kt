/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.google.samples.apps.iosched.widget.transition

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.transition.Transition
import android.transition.TransitionValues
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep

/**
 * A [Transition] which animates the rotation of a [View].
 */
class Rotate : Transition {

    @Keep
    constructor() : super()

    @Keep
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun getTransitionProperties(): Array<String> {
        return TRANSITION_PROPERTIES
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) return null

        val startRotation = startValues.values[PROP_ROTATION] as Float
        val endRotation = endValues.values[PROP_ROTATION] as Float
        if (startRotation == endRotation) return null

        val view = endValues.view
        // ensure the pivot is set
        view.pivotX = view.width / 2f
        view.pivotY = view.height / 2f
        return ObjectAnimator.ofFloat(endValues.view, View.ROTATION, startRotation, endRotation)
    }

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        if (view == null || view.width <= 0 || view.height <= 0) return
        transitionValues.values[PROP_ROTATION] = view.rotation
    }

    companion object {

        private const val PROP_ROTATION = "iosched:rotate:rotation"
        private val TRANSITION_PROPERTIES = arrayOf(PROP_ROTATION)
    }
}
