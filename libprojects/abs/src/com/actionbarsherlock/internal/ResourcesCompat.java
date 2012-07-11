package com.actionbarsherlock.internal;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import com.actionbarsherlock.R;

public final class ResourcesCompat {
    //No instances
    private ResourcesCompat() {}


    /**
     * Support implementation of {@code getResources().getBoolean()} that we
     * can use to simulate filtering based on width and smallest width
     * qualifiers on pre-3.2.
     *
     * @param context Context to load booleans from on 3.2+ and to fetch the
     * display metrics.
     * @param id Id of boolean to load.
     * @return Associated boolean value as reflected by the current display
     * metrics.
     */
    public static boolean getResources_getBoolean(Context context, int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            return context.getResources().getBoolean(id);
        }

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float widthDp = metrics.widthPixels / metrics.density;
        float heightDp = metrics.heightPixels / metrics.density;
        float smallestWidthDp = (widthDp < heightDp) ? widthDp : heightDp;

        if (id == R.bool.abs__action_bar_embed_tabs) {
            if (widthDp >= 480) {
                return true; //values-w480dp
            }
            return false; //values
        }
        if (id == R.bool.abs__split_action_bar_is_narrow) {
            if (widthDp >= 480) {
                return false; //values-w480dp
            }
            return true; //values
        }
        if (id == R.bool.abs__action_bar_expanded_action_views_exclusive) {
            if (smallestWidthDp >= 600) {
                return false; //values-sw600dp
            }
            return true; //values
        }
        if (id == R.bool.abs__config_allowActionMenuItemTextWithIcon) {
            if (widthDp >= 480) {
                return true; //values-w480dp
            }
            return false; //values
        }

        throw new IllegalArgumentException("Unknown boolean resource ID " + id);
    }

    /**
     * Support implementation of {@code getResources().getInteger()} that we
     * can use to simulate filtering based on width qualifiers on pre-3.2.
     *
     * @param context Context to load integers from on 3.2+ and to fetch the
     * display metrics.
     * @param id Id of integer to load.
     * @return Associated integer value as reflected by the current display
     * metrics.
     */
    public static int getResources_getInteger(Context context, int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            return context.getResources().getInteger(id);
        }

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float widthDp = metrics.widthPixels / metrics.density;

        if (id == R.integer.abs__max_action_buttons) {
            if (widthDp >= 600) {
                return 5; //values-w600dp
            }
            if (widthDp >= 500) {
                return 4; //values-w500dp
            }
            if (widthDp >= 360) {
                return 3; //values-w360dp
            }
            return 2; //values
        }

        throw new IllegalArgumentException("Unknown integer resource ID " + id);
    }
}
