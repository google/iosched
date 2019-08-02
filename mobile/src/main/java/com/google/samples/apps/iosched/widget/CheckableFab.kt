/*
 * Copyright 2018 Google LLC
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

package com.google.samples.apps.iosched.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.samples.apps.iosched.R

/**
 * An extension to the [FloatingActionButton] that supports checkable states.
 */
class CheckableFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr), Checkable {

    /** Content description when the view is checked. */
    var contentDescriptionChecked: CharSequence? = null

    /** Content description when the view is unchecked. */
    var contentDescriptionUnchecked: CharSequence? = null

    private var _checked: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                contentDescription =
                    if (value) contentDescriptionChecked else contentDescriptionUnchecked
                refreshDrawableState()
            }
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CheckableFab, defStyleAttr, 0)
        contentDescriptionChecked = a.getString(R.styleable.CheckableFab_contentDescriptionChecked)
        contentDescriptionUnchecked =
            a.getString(R.styleable.CheckableFab_contentDescriptionUnchecked)
        _checked = a.getBoolean(R.styleable.CheckableFab_android_checked, false)
        a.recycle()
    }

    override fun isChecked(): Boolean = _checked

    override fun toggle() {
        _checked = !_checked
    }

    override fun setChecked(checked: Boolean) {
        _checked = checked
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        val state = if (_checked) stateChecked else stateUnchecked
        mergeDrawableStates(drawableState, state)
        return drawableState
    }

    companion object {
        private val stateChecked = intArrayOf(android.R.attr.state_checked)
        private val stateUnchecked = intArrayOf(-android.R.attr.state_checked)
    }
}
