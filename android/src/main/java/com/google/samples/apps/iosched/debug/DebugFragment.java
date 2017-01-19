/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.debug;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.debug.actions.DisplayUserDataDebugAction;
import com.google.samples.apps.iosched.debug.actions.ForceAppDataSyncNowAction;
import com.google.samples.apps.iosched.debug.actions.ForceSyncNowAction;
import com.google.samples.apps.iosched.debug.actions.ScheduleStarredSessionAlarmsAction;
import com.google.samples.apps.iosched.debug.actions.ShowSessionNotificationDebugAction;
import com.google.samples.apps.iosched.debug.actions.TestScheduleHelperAction;
import com.google.samples.apps.iosched.explore.ExploreSessionsActivity;
import com.google.samples.apps.iosched.service.SessionAlarmService;
import com.google.samples.apps.iosched.settings.ConfMessageCardUtils;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.util.WiFiUtils;
import com.google.samples.apps.iosched.welcome.WelcomeActivity;

import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * {@link android.app.Activity} displaying debug options so a developer can debug and test. This
 * functionality is only enabled when {@link com.google.samples.apps.iosched.BuildConfig}.DEBUG
 * is true.
 */
public class DebugFragment extends Fragment {

    private static final String TAG = makeLogTag(DebugFragment.class);

    /**
     * Area of screen used to display log log messages.
     */
    private TextView mLogArea;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.debug_frag, null);
        mLogArea = (TextView) rootView.findViewById(R.id.logArea);
        ViewGroup tests = (ViewGroup) rootView.findViewById(R.id.debug_action_list);
        tests.addView(createTestAction(new ForceSyncNowAction()));
        tests.addView(createTestAction(new DisplayUserDataDebugAction()));
        tests.addView(createTestAction(new ForceAppDataSyncNowAction()));
        tests.addView(createTestAction(new TestScheduleHelperAction()));
        tests.addView(createTestAction(new ScheduleStarredSessionAlarmsAction()));
        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(final Context context, final Callback callback) {
                final String sessionId = SessionAlarmService.DEBUG_SESSION_ID;
                final String sessionTitle = "Debugging with Placeholder Text";

                Intent intent = new Intent(
                        SessionAlarmService.ACTION_NOTIFY_SESSION_FEEDBACK,
                        null, context, SessionAlarmService.class);
                intent.putExtra(SessionAlarmService.EXTRA_SESSION_ID, sessionId);
                intent.putExtra(SessionAlarmService.EXTRA_SESSION_START, System.currentTimeMillis()
                        - 30 * 60 * 1000);
                intent.putExtra(SessionAlarmService.EXTRA_SESSION_END, System.currentTimeMillis());
                intent.putExtra(SessionAlarmService.EXTRA_SESSION_TITLE, sessionTitle);
                context.startService(intent);
                Toast.makeText(context, "Showing DEBUG session feedback notification.", Toast.LENGTH_LONG).show();
            }

            @Override
            public String getLabel() {
                return "Show session feedback notification";
            }
        }));
        tests.addView(createTestAction(new ShowSessionNotificationDebugAction()));
        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(Context context, Callback callback) {
                SettingsUtils.markTosAccepted(context, false);
                SettingsUtils.markConductAccepted(context, false);
                SettingsUtils.setAttendeeAtVenue(context, false);
                SettingsUtils.markAnsweredLocalOrRemote(context, false);
                AccountUtils.setActiveAccount(context, null);
                context.startActivity(new Intent(context, WelcomeActivity.class));
            }

            @Override
            public String getLabel() {
                return "Display Welcome Activity";
            }
        }));

        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(Context context, Callback callback) {
                SettingsUtils.markTosAccepted(context, false);
                SettingsUtils.markConductAccepted(context, false);
                SettingsUtils.setAttendeeAtVenue(context, false);
                SettingsUtils.markAnsweredLocalOrRemote(context, false);
                AccountUtils.setActiveAccount(context, null);
                ConfMessageCardUtils.unsetStateForAllCards(context);
            }

            @Override
            public String getLabel() {
                return "Reset Welcome Flags";
            }
        }));
        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(Context context, Callback callback) {
                Intent intent = new Intent(context, ExploreSessionsActivity.class);
                intent.putExtra(ExploreSessionsActivity.EXTRA_FILTER_TAG, "TOPIC_ANDROID");
                context.startActivity(intent);
            }

            @Override
            public String getLabel() {
                return "Show Explore Sessions Activity (Android Topic)";
            }
        }));
        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(Context context, Callback callback) {
                LOGW(TAG, "Unsetting all Explore I/O message card answers.");
                ConfMessageCardUtils.markAnsweredConfMessageCardsPrompt(context, null);
                ConfMessageCardUtils.setConfMessageCardsEnabled(context, null);
                ConfMessageCardUtils.unsetStateForAllCards(context);
            }

            @Override
            public String getLabel() {
                return "Unset all Explore I/O-based card answers";
            }
        }));
        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(Context context, Callback callback) {
                TimeUtils.setCurrentTimeRelativeToStartOfConference(context, -TimeUtils.HOUR * 3);
            }

            @Override
            public String getLabel() {
                return "Set time to 3 hours before Conf";
            }
        }));
        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(Context context, Callback callback) {
                TimeUtils.setCurrentTimeRelativeToStartOfConference(context, -TimeUtils.DAY);
            }

            @Override
            public String getLabel() {
                return "Set time to Day Before Conf";
            }
        }));

        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(Context context, Callback callback) {
                TimeUtils.setCurrentTimeRelativeToStartOfConference(context, TimeUtils.HOUR * 3);

                LOGW(TAG, "Unsetting all Explore I/O card answers and settings.");
                ConfMessageCardUtils.markAnsweredConfMessageCardsPrompt(context, null);
                ConfMessageCardUtils.setConfMessageCardsEnabled(context, null);
                SettingsUtils.markDeclinedWifiSetup(context, false);
                WiFiUtils.uninstallConferenceWiFi(context);
            }

            @Override
            public String getLabel() {
                return "Set time to 3 hours after Conf start";
            }
        }));
        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(Context context, Callback callback) {
                TimeUtils.setCurrentTimeRelativeToStartOfSecondDayOfConference(context,
                        TimeUtils.HOUR * 3);
            }

            @Override
            public String getLabel() {
                return "Set time to 3 hours after 2nd day start";
            }
        }));
        tests.addView(createTestAction(new DebugAction() {
            @Override
            public void run(Context context, Callback callback) {
                TimeUtils.setCurrentTimeRelativeToEndOfConference(context, TimeUtils.HOUR * 3);
            }

            @Override
            public String getLabel() {
                return "Set time to 3 hours after Conf end";
            }
        }));

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
        message = "[" + time + "ms] " + message;
        Log.d(TAG, message);
        mLogArea.append(message + "\n");
    }

    private void setContentTopClearance(int clearance) {
        if (getView() != null) {
            getView().setPadding(getView().getPaddingLeft(), clearance,
                    getView().getPaddingRight(), getView().getPaddingBottom());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // configure fragment's top clearance to take our overlaid controls (Action Bar
        // and spinner box) into account.
        int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }
        setContentTopClearance(actionBarSize
                + getResources().getDimensionPixelSize(R.dimen.explore_grid_padding));
    }
}
