/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.actionbarsherlock.internal.nineoldandroids.animation;

import android.view.animation.Interpolator;

/**
 * This class holds a time/value pair for an animation. The Keyframe class is used
 * by {@link ValueAnimator} to define the values that the animation target will have over the course
 * of the animation. As the time proceeds from one keyframe to the other, the value of the
 * target object will animate between the value at the previous keyframe and the value at the
 * next keyframe. Each keyframe also holds an optional {@link TimeInterpolator}
 * object, which defines the time interpolation over the intervalue preceding the keyframe.
 *
 * <p>The Keyframe class itself is abstract. The type-specific factory methods will return
 * a subclass of Keyframe specific to the type of value being stored. This is done to improve
 * performance when dealing with the most common cases (e.g., <code>float</code> and
 * <code>int</code> values). Other types will fall into a more general Keyframe class that
 * treats its values as Objects. Unless your animation requires dealing with a custom type
 * or a data structure that needs to be animated directly (and evaluated using an implementation
 * of {@link TypeEvaluator}), you should stick to using float and int as animations using those
 * types have lower runtime overhead than other types.</p>
 */
@SuppressWarnings("rawtypes")
public abstract class Keyframe implements Cloneable {
    /**
     * The time at which mValue will hold true.
     */
    float mFraction;

    /**
     * The type of the value in this Keyframe. This type is determined at construction time,
     * based on the type of the <code>value</code> object passed into the constructor.
     */
    Class mValueType;

    /**
     * The optional time interpolator for the interval preceding this keyframe. A null interpolator
     * (the default) results in linear interpolation over the interval.
     */
    private /*Time*/Interpolator mInterpolator = null;

    /**
     * Flag to indicate whether this keyframe has a valid value. This flag is used when an
     * animation first starts, to populate placeholder keyframes with real values derived
     * from the target object.
     */
    boolean mHasValue = false;

    /**
     * Constructs a Keyframe object with the given time and value. The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     * @param value The value that the object will animate to as the animation time approaches
     * the time in this keyframe, and the the value animated from as the time passes the time in
     * this keyframe.
     */
    public static Keyframe ofInt(float fraction, int value) {
        return new IntKeyframe(fraction, value);
    }

    /**
     * Constructs a Keyframe object with the given time. The value at this time will be derived
     * from the target object when the animation first starts (note that this implies that keyframes
     * with no initial value must be used as part of an {@link ObjectAnimator}).
     * The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     */
    public static Keyframe ofInt(float fraction) {
        return new IntKeyframe(fraction);
    }

    /**
     * Constructs a Keyframe object with the given time and value. The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     * @param value The value that the object will animate to as the animation time approaches
     * the time in this keyframe, and the the value animated from as the time passes the time in
     * this keyframe.
     */
    public static Keyframe ofFloat(float fraction, float value) {
        return new FloatKeyframe(fraction, value);
    }

    /**
     * Constructs a Keyframe object with the given time. The value at this time will be derived
     * from the target object when the animation first starts (note that this implies that keyframes
     * with no initial value must be used as part of an {@link ObjectAnimator}).
     * The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     */
    public static Keyframe ofFloat(float fraction) {
        return new FloatKeyframe(fraction);
    }

    /**
     * Constructs a Keyframe object with the given time and value. The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     * @param value The value that the object will animate to as the animation time approaches
     * the time in this keyframe, and the the value animated from as the time passes the time in
     * this keyframe.
     */
    public static Keyframe ofObject(float fraction, Object value) {
        return new ObjectKeyframe(fraction, value);
    }

    /**
     * Constructs a Keyframe object with the given time. The value at this time will be derived
     * from the target object when the animation first starts (note that this implies that keyframes
     * with no initial value must be used as part of an {@link ObjectAnimator}).
     * The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     */
    public static Keyframe ofObject(float fraction) {
        return new ObjectKeyframe(fraction, null);
    }

    /**
     * Indicates whether this keyframe has a valid value. This method is called internally when
     * an {@link ObjectAnimator} first starts; keyframes without values are assigned values at
     * that time by deriving the value for the property from the target object.
     *
     * @return boolean Whether this object has a value assigned.
     */
    public boolean hasValue() {
        return mHasValue;
    }

    /**
     * Gets the value for this Keyframe.
     *
     * @return The value for this Keyframe.
     */
    public abstract Object getValue();

    /**
     * Sets the value for this Keyframe.
     *
     * @param value value for this Keyframe.
     */
    public abstract void setValue(Object value);

    /**
     * Gets the time for this keyframe, as a fraction of the overall animation duration.
     *
     * @return The time associated with this keyframe, as a fraction of the overall animation
     * duration. This should be a value between 0 and 1.
     */
    public float getFraction() {
        return mFraction;
    }

    /**
     * Sets the time for this keyframe, as a fraction of the overall animation duration.
     *
     * @param fraction time associated with this keyframe, as a fraction of the overall animation
     * duration. This should be a value between 0 and 1.
     */
    public void setFraction(float fraction) {
        mFraction = fraction;
    }

    /**
     * Gets the optional interpolator for this Keyframe. A value of <code>null</code> indicates
     * that there is no interpolation, which is the same as linear interpolation.
     *
     * @return The optional interpolator for this Keyframe.
     */
    public /*Time*/Interpolator getInterpolator() {
        return mInterpolator;
    }

    /**
     * Sets the optional interpolator for this Keyframe. A value of <code>null</code> indicates
     * that there is no interpolation, which is the same as linear interpolation.
     *
     * @return The optional interpolator for this Keyframe.
     */
    public void setInterpolator(/*Time*/Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    /**
     * Gets the type of keyframe. This information is used by ValueAnimator to determine the type of
     * {@link TypeEvaluator} to use when calculating values between keyframes. The type is based
     * on the type of Keyframe created.
     *
     * @return The type of the value stored in the Keyframe.
     */
    public Class getType() {
        return mValueType;
    }

    @Override
    public abstract Keyframe clone();

    /**
     * This internal subclass is used for all types which are not int or float.
     */
    static class ObjectKeyframe extends Keyframe {

        /**
         * The value of the animation at the time mFraction.
         */
        Object mValue;

        ObjectKeyframe(float fraction, Object value) {
            mFraction = fraction;
            mValue = value;
            mHasValue = (value != null);
            mValueType = mHasValue ? value.getClass() : Object.class;
        }

        public Object getValue() {
            return mValue;
        }

        public void setValue(Object value) {
            mValue = value;
            mHasValue = (value != null);
        }

        @Override
        public ObjectKeyframe clone() {
            ObjectKeyframe kfClone = new ObjectKeyframe(getFraction(), mValue);
            kfClone.setInterpolator(getInterpolator());
            return kfClone;
        }
    }

    /**
     * Internal subclass used when the keyframe value is of type int.
     */
    static class IntKeyframe extends Keyframe {

        /**
         * The value of the animation at the time mFraction.
         */
        int mValue;

        IntKeyframe(float fraction, int value) {
            mFraction = fraction;
            mValue = value;
            mValueType = int.class;
            mHasValue = true;
        }

        IntKeyframe(float fraction) {
            mFraction = fraction;
            mValueType = int.class;
        }

        public int getIntValue() {
            return mValue;
        }

        public Object getValue() {
            return mValue;
        }

        public void setValue(Object value) {
            if (value != null && value.getClass() == Integer.class) {
                mValue = ((Integer)value).intValue();
                mHasValue = true;
            }
        }

        @Override
        public IntKeyframe clone() {
            IntKeyframe kfClone = new IntKeyframe(getFraction(), mValue);
            kfClone.setInterpolator(getInterpolator());
            return kfClone;
        }
    }

    /**
     * Internal subclass used when the keyframe value is of type float.
     */
    static class FloatKeyframe extends Keyframe {
        /**
         * The value of the animation at the time mFraction.
         */
        float mValue;

        FloatKeyframe(float fraction, float value) {
            mFraction = fraction;
            mValue = value;
            mValueType = float.class;
            mHasValue = true;
        }

        FloatKeyframe(float fraction) {
            mFraction = fraction;
            mValueType = float.class;
        }

        public float getFloatValue() {
            return mValue;
        }

        public Object getValue() {
            return mValue;
        }

        public void setValue(Object value) {
            if (value != null && value.getClass() == Float.class) {
                mValue = ((Float)value).floatValue();
                mHasValue = true;
            }
        }

        @Override
        public FloatKeyframe clone() {
            FloatKeyframe kfClone = new FloatKeyframe(getFraction(), mValue);
            kfClone.setInterpolator(getInterpolator());
            return kfClone;
        }
    }
}
