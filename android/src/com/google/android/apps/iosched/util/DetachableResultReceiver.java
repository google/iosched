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

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * Proxy {@link ResultReceiver} that offers a listener interface that can be
 * detached. Useful for when sending callbacks to a {@link Service} where a
 * listening {@link Activity} can be swapped out during configuration changes.
 */
public class DetachableResultReceiver extends ResultReceiver {
    private static final String TAG = "DetachableResultReceiver";

    private Receiver mReceiver;

    public DetachableResultReceiver(Handler handler) {
        super(handler);
    }

    public void clearReceiver() {
        mReceiver = null;
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        } else {
            Log.w(TAG, "Dropping result on floor for code " + resultCode + ": "
                    + resultData.toString());
        }
    }
}
