/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.R;

import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Set of runtime permission utility methods.
 */
public class PermissionsUtils {
    private static final String TAG = makeLogTag(PermissionsUtils.class);

    /**
     * Determine if any permission is in the denied state.
     */
    public static boolean anyPermissionDenied(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_DENIED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Displays a persistent {@link Snackbar} acknowledging the permission dismissal. If one of the
     * permissions is detected in the "Don't ask again" state then the OK button forwards to the
     * AppInfo screen, otherwise, the OK button invokes a permissions request.
     * <p/>
     * This method determines the Do-Not-Ask-Again-State by: 1.) Only being called from {@code
     * onRequestPermissionResult} when at least one permission was not granted. 2.) Checking that a
     * permission is currently in the permission-denied and no-rationale-needed states. 3.) The
     * combination of 1 and 2 indicates that at least one permission is in the Do-Not-Ask state and
     * the only resolution to that is for the user to visit the App Info -> Permissions screen.
     */
    @NonNull
    public static Snackbar displayConditionalPermissionDenialSnackbar(
            @NonNull final Activity activity, final int messageResId, @NonNull String[] permissions,
            int requestCode) {
        return displayConditionalPermissionDenialSnackbar(activity, messageResId, permissions,
                requestCode, true);
    }

    /**
     * Displays a {@link Snackbar} acknowledging the permission dismissal. If one of the permissions
     * is detected in the "Don't ask again" state then the OK button forwards to the AppInfo screen,
     * otherwise, the OK button invokes a permissions request.
     * <p/>
     * This method determines the Do-Not-Ask-Again-State by: 1.) Only being called from {@code
     * onRequestPermissionResult} when at least one permission was not granted. 2.) Checking that a
     * permission is currently in the permission-denied and no-rationale-needed states. 3.) The
     * combination of 1 and 2 indicates that at least one permission is in the Do-Not-Ask state and
     * the only resolution to that is for the user to visit the App Info -> Permissions screen.
     */
    @NonNull
    public static Snackbar displayConditionalPermissionDenialSnackbar(
            @NonNull final Activity activity, final int messageResId, @NonNull String[] permissions,
            int requestCode, boolean isPersistent) {
        boolean permissionInDoNotAskAgainState = false;
        for (final String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) ==
                    PackageManager.PERMISSION_DENIED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                permissionInDoNotAskAgainState = true;
                break;
            }
        }

        if (permissionInDoNotAskAgainState) {
            // User has to manually enable permissions on the AppInfo screen.
            return displayPermissionDeniedAppInfoResolutionSnackbar(activity, messageResId,
                    isPersistent);
        } else {
            // User clicks OK to re-start the permissions requests.
            return displayPermissionRationaleSnackbar(activity, messageResId, permissions,
                    requestCode, isPersistent);
        }
    }

    private static Snackbar displayPermissionDeniedAppInfoResolutionSnackbar(
            @NonNull final Activity activity,
            final int messageResId, final boolean isPersistent) {
        View view = UIUtils.getRootView(activity);
        final int length = isPersistent ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG;

        Snackbar snackbar = Snackbar.make(view, messageResId, length)
                                    .setAction(R.string.ok, new View.OnClickListener() {
                                        @Override
                                        public void onClick(final View v) {
                                            LOGI(TAG, "Invoking App Info screen");
                                            final Intent intent =
                                                    new Intent(
                                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package",
                                                    BuildConfig.APPLICATION_ID, null);
                                            intent.setData(uri);
                                            activity.startActivity(intent);
                                        }
                                    });
        snackbar.show();
        return snackbar;
    }

    /**
     * A persistent Snackbar is displayed that gives a {@code permission} rationale and forwards to
     * the App Info screen to allow the user to resolve the error.
     */
    @NonNull
    public static Snackbar displayPermissionDeniedAppInfoResolutionSnackbar(
            @NonNull final Activity activity, final int messageResId) {

        return displayPermissionDeniedAppInfoResolutionSnackbar(activity, messageResId, true);

    }

    /**
     * A Snackbar is displayed that gives a {@code permission} rationale and an action that only
     * dismisses the Snackbar.
     */
    @NonNull
    public static Snackbar displayPermissionDeniedSnackbar(@NonNull final Activity activity,
            final int messageResId) {
        View view = UIUtils.getRootView(activity);
        final Snackbar snackbar = Snackbar.make(view, messageResId, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                snackbar.dismiss();
            }
        }).show();
        return snackbar;
    }

    /**
     * A persistent Snackbar is displayed that prompts for {@code permissions} again when OK is
     * clicked.
     */
    @NonNull
    public static Snackbar displayPermissionRationaleSnackbar(@NonNull final Activity activity,
            final int messageResId, @NonNull final String[] permissions, final int requestCode) {
        return displayPermissionRationaleSnackbar(activity, messageResId, permissions, requestCode,
                true);
    }

    /**
     * A Snackbar is displayed that prompts for {@code permissions} again when OK is clicked.
     */
    @NonNull
    public static Snackbar displayPermissionRationaleSnackbar(@NonNull final Activity activity,
            final int messageResId, @NonNull final String[] permissions, final int requestCode,
            boolean isPersistent) {
        View view = UIUtils.getRootView(activity);
        final int length = isPersistent ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG;
        Snackbar snackbar = Snackbar.make(view, messageResId, length)
                                    .setAction(R.string.ok, new View.OnClickListener() {
                                        @Override
                                        public void onClick(final View v) {
                                            ActivityCompat.requestPermissions(activity, permissions,
                                                    requestCode);
                                        }
                                    });
        snackbar.show();
        return snackbar;
    }

    /**
     * Determines if all {@code permissions} have already been granted.
     */
    public static boolean permissionsAlreadyGranted(@NonNull Context context,
            @NonNull String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if {@code permission} rationale should be displayed for any {@code permission}.
     */
    public static boolean shouldShowAnyPermissionRationale(@NonNull Activity activity,
            String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }
}
