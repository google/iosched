package com.actionbarsherlock.internal.widget;

import android.view.View;

final class IcsView {
    //No instances
    private IcsView() {}

    /**
     * Return only the state bits of {@link #getMeasuredWidthAndState()}
     * and {@link #getMeasuredHeightAndState()}, combined into one integer.
     * The width component is in the regular bits {@link #MEASURED_STATE_MASK}
     * and the height component is at the shifted bits
     * {@link #MEASURED_HEIGHT_STATE_SHIFT}>>{@link #MEASURED_STATE_MASK}.
     */
    public static int getMeasuredStateInt(View child) {
        return (child.getMeasuredWidth()&View.MEASURED_STATE_MASK)
                | ((child.getMeasuredHeight()>>View.MEASURED_HEIGHT_STATE_SHIFT)
                        & (View.MEASURED_STATE_MASK>>View.MEASURED_HEIGHT_STATE_SHIFT));
    }
}
