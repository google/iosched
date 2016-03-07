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

package com.google.samples.apps.iosched.messaging;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.google.android.gcm.GCMRegistrar;
import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.gcm.ServerUtilities;
import com.google.samples.apps.iosched.util.AccountUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Implements {@link MessagingRegistration} using GCM.
 */
public class MessagingRegistrationWithGCM implements MessagingRegistration {

    private static final String TAG = makeLogTag(MessagingRegistrationWithGCM.class);

    // Asynctask that performs GCM registration in the background
    private AsyncTask<Void, Void, Void> mGCMRegisterTask;

    private Activity mActivity;

    public MessagingRegistrationWithGCM(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void registerDevice() {
        GCMRegistrar.checkDevice(mActivity);
        GCMRegistrar.checkManifest(mActivity);

        final String regId = GCMRegistrar.getRegistrationId(mActivity);

        if (TextUtils.isEmpty(regId)) {
            // Automatically registers application on startup.
            GCMRegistrar.register(mActivity, BuildConfig.GCM_SENDER_ID);

        } else {
            // Get the correct GCM key for the user. GCM key is a somewhat non-standard
            // approach we use in this app. For more about this, check GCM.TXT.
            final String gcmKey = AccountUtils.hasActiveAccount(mActivity) ?
                    AccountUtils
                            .getGcmKey(mActivity, AccountUtils.getActiveAccountName(mActivity)) :
                    null;
            // Device is already registered on GCM, needs to check if it is
            // registered on our server as well.
            if (ServerUtilities.isRegisteredOnServer(mActivity, gcmKey)) {
                // Skips registration.
                LOGI(TAG, "Already registered on the GCM server with right GCM key.");
            } else {
                // Try to register again, but not in the UI thread.
                // It's also necessary to cancel the thread onDestroy(),
                // hence the use of AsyncTask instead of a raw thread.
                mGCMRegisterTask = getGCMRegisterTask(gcmKey, regId);
                mGCMRegisterTask.execute(null, null, null);
            }
        }
    }

    protected AsyncTask<Void, Void, Void> getGCMRegisterTask(final String gcmKey,
            final String regId) {
        return new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                LOGI(TAG, "Registering on the GCM server with GCM key: "
                        + AccountUtils.sanitizeGcmKey(gcmKey));
                boolean registered = ServerUtilities.register(mActivity,
                        regId, gcmKey);
                // At this point all attempts to register with the app
                // server failed, so we need to unregister the device
                // from GCM - the app will try to register again when
                // it is restarted. Note that GCM will send an
                // unregistered callback upon completion, but
                // GCMIntentService.onUnregistered() will ignore it.
                if (!registered) {
                    LOGI(TAG, "GCM registration failed.");
                    GCMRegistrar.unregister(mActivity);
                } else {
                    LOGI(TAG, "GCM registration successful.");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mGCMRegisterTask = null;
            }
        };
    }

    @Override
    public void destroy() {
        if (mGCMRegisterTask != null) {
            LOGD(TAG, "Cancelling GCM registration task.");
            mGCMRegisterTask.cancel(true);
        }

        try {
            GCMRegistrar.onDestroy(mActivity);
        } catch (Exception e) {
            LOGW(TAG, "GCM unregistration error", e);
        }
    }

}
