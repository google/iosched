/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.iosched.util;

import com.google.android.apps.iosched.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

/**
 * A helper for showing EULAs and storing a {@link SharedPreferences} bit indicating whether the
 * user has accepted.
 */
public class EulaHelper {
    public static boolean hasAcceptedEula(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean("accepted_eula", false);
    }

    private static void setAcceptedEula(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                sp.edit().putBoolean("accepted_eula", true).commit();
                return null;
            }
        }.execute();
    }

    /**
     * Show End User License Agreement.
     *
     * @param accepted True IFF user has accepted license already, which means it can be dismissed.
     *                 If the user hasn't accepted, then the EULA must be accepted or the program
     *                 exits.
     * @param activity Activity started from.
     */
    public static void showEula(final boolean accepted, final Activity activity) {
        AlertDialog.Builder eula = new AlertDialog.Builder(activity)
                .setTitle(R.string.eula_title)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(R.string.eula_text)
                .setCancelable(accepted);

        if (accepted) {
            // If they've accepted the EULA allow, show an OK to dismiss.
            eula.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        } else {
            // If they haven't accepted the EULA allow, show accept/decline buttons and exit on
            // decline.
            eula
                    .setPositiveButton(R.string.accept,
                            new android.content.DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    setAcceptedEula(activity);
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(R.string.decline,
                            new android.content.DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    activity.finish();
                                }
                            });
        }
        eula.show();
    }
}
