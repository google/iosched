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

package com.google.samples.apps.iosched.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.plus.PlusClient;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.NumberRatingBar;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.FeedbackUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that lets the user submit feedback about a given session.
 */
public class SessionFeedbackFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String TAG = makeLogTag(SessionDetailActivity.class);

    // Set this boolean extra to true to show a variable height header
    public static final String EXTRA_VARIABLE_HEIGHT_HEADER =
            "com.google.android.iosched.extra.VARIABLE_HEIGHT_HEADER";

    private String mSessionId;
    private Uri mSessionUri;

    private String mTitleString;

    private TextView mTitle;
    private PlusClient mPlusClient;

    private boolean mVariableHeightHeader = false;
    private RatingBar mSessionRatingFeedbackBar;
    private NumberRatingBar mQ1FeedbackBar;
    private NumberRatingBar mQ2FeedbackBar;
    private NumberRatingBar mQ3FeedbackBar;

    private EditText mComments;

    public SessionFeedbackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String chosenAccountName = AccountUtils.getActiveAccountName(getActivity());
        mPlusClient = new PlusClient.Builder(getActivity(), this, this)
                .clearScopes()
                .setAccountName(chosenAccountName)
                .build();

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mSessionUri = intent.getData();

        if (mSessionUri == null) {
            return;
        }

        mSessionId = ScheduleContract.Sessions.getSessionId(mSessionUri);

        mVariableHeightHeader = intent.getBooleanExtra(EXTRA_VARIABLE_HEIGHT_HEADER, false);

        LoaderManager manager = getLoaderManager();
        manager.restartLoader(0, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_session_feedback, null);

        mTitle = (TextView) rootView.findViewById(R.id.session_title);
        mSessionRatingFeedbackBar = (RatingBar) rootView.findViewById(R.id.rating_bar_0);
        mQ1FeedbackBar = (NumberRatingBar) rootView.findViewById(R.id.rating_bar_1);
        mQ2FeedbackBar = (NumberRatingBar) rootView.findViewById(R.id.rating_bar_2);
        mQ3FeedbackBar = (NumberRatingBar) rootView.findViewById(R.id.rating_bar_3);

        mComments = (EditText) rootView.findViewById(R.id.session_feedback_comments);

        if (mVariableHeightHeader) {
            View headerView = rootView.findViewById(R.id.header_session);
            ViewGroup.LayoutParams layoutParams = headerView.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            headerView.setLayoutParams(layoutParams);
        }

        rootView.findViewById(R.id.submit_feedback_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        submitAllFeedback();
                        /* [ANALYTICS:EVENT]
                         * TRIGGER:   Send feedback about a session.
                         * CATEGORY:  'Session'
                         * ACTION:    'Feedback'
                         * LABEL:     session title/subtitle. Specific feedback IS NOT included.
                         * [/ANALYTICS]
                         */
                        AnalyticsManager.sendEvent("Session", "Feedback", mTitleString, 0L);
                        getActivity().finish();
                    }
                }
        );
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mPlusClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mPlusClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Don't show an error just for the +1 button. Google Play services errors
        // should be caught at a higher level in the app
    }

    /**
     * Handle {@link SessionsQuery} {@link Cursor}.
     */
    private void onSessionQueryComplete(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            return;
        }

        mTitleString = cursor.getString(SessionsQuery.TITLE);

        // Format time block this session occupies
        mTitle.setText(mTitleString);

        /* [ANALYTICS:SCREEN]
         * TRIGGER:   View the Send Session Feedback screen.
         * LABEL:     'Feedback' + session title/subtitle
         * [/ANALYTICS]
         */
        AnalyticsManager.sendScreenView("Feedback: " + mTitleString);
        LOGD("Tracker", "Feedback: " + mTitleString);
    }

    /* ALL THE FEEDBACKS */
    void submitAllFeedback() {

        int rating = (int) mSessionRatingFeedbackBar.getRating();
        int q1Answer = mQ1FeedbackBar.getProgress();
        int q2Answer = mQ2FeedbackBar.getProgress();
        int q3Answer = mQ3FeedbackBar.getProgress();

        String comments = mComments.getText().toString();
        FeedbackUtils.saveSessionFeedback(getActivity(), mSessionId, rating, q1Answer, q2Answer,
                q3Answer, comments);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(), mSessionUri, SessionsQuery.PROJECTION, null,
                null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (!isAdded()) {
            return;
        }

        onSessionQueryComplete(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * {@link com.google.samples.apps.iosched.provider.ScheduleContract.Sessions} query parameters.
     */
    private interface SessionsQuery {

        String[] PROJECTION = {
                ScheduleContract.Sessions.SESSION_TITLE,
        };

        int TITLE = 0;
    }
}
