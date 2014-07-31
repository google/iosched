/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui.debug;

import android.app.*;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.ui.debug.actions.*;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class DebugActionRunnerFragment extends Fragment {

    private static final String TAG = makeLogTag(DebugActionRunnerFragment.class);

    private TextView mLogArea;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_debug_action_runner, null);
        mLogArea = (TextView) rootView.findViewById(R.id.logArea);
        ViewGroup tests = (ViewGroup) rootView.findViewById(R.id.debug_action_list);
        tests.addView(createTestAction(new ForceSyncNowAction()));
        tests.addView(createTestAction(new ListStarredSessionsDebugAction()));
        tests.addView(createTestAction(new ShowAllDriveFilesDebugAction()));
        tests.addView(createTestAction(new ForceAppDataSyncNowAction()));
        tests.addView(createTestAction(new TestScheduleHelperAction()));
        tests.addView(createTestAction(new ScheduleStarredSessionAlarmsAction()));
        tests.addView(createTestAction(new SimulateBadgeScannedAction()));
        tests.addView(createTestAction(new ShowFeedbackNotificationAction()));
        tests.addView(createTestAction(new ShowSessionNotificationDebugAction()
        ));
        return rootView;
    }

    protected View createTestAction(final DebugAction test) {
        Button testButton = new Button(this.getActivity());
        testButton.setText(test.getLabel());
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final long start = System.currentTimeMillis();
                mLogArea.setText("");
                test.run(view.getContext(), new DebugAction.Callback() {
                    @Override
                    public void done(boolean success, String message) {
                        logTimed((System.currentTimeMillis() - start),
                                (success ? "[OK] " : "[FAIL] ") + message);
                    }
                });
            }
        });
        return testButton;
    }

    protected void logTimed(long time, String message) {
        message = "["+time+"ms] "+message;
        Log.d(TAG, message);
        mLogArea.append(message + "\n");
    }

}
