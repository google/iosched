/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.ui;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Feedback;
import com.google.android.apps.iosched.util.AccountUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.plus.PlusClient;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import android.view.*;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that lets the user submit feedback about a given session.
 */
public class SessionFeedbackFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String TAG = makeLogTag(SessionDetailFragment.class);

    // Set this boolean extra to true to show a variable height header
    public static final String EXTRA_VARIABLE_HEIGHT_HEADER =
            "com.google.android.iosched.extra.VARIABLE_HEIGHT_HEADER";

    private String mSessionId;
    private Uri mSessionUri;

    private String mTitleString;


    private TextView mTitle;
    private PlusClient mPlusClient;

    private boolean mVariableHeightHeader = false;

    private RatingBarHelper mSessionRatingFeedbackBar;
    private RatingBarHelper mQ1FeedbackBar;
    private RatingBarHelper mQ2FeedbackBar;
    private RatingBarHelper mQ3FeedbackBar;

    private RadioGroup mQ4RadioGroup;
    private EditText mComments;

    public SessionFeedbackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String chosenAccountName = AccountUtils.getChosenAccountName(getActivity());
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

        mSessionRatingFeedbackBar = RatingBarHelper.create(rootView.findViewById(
                R.id.session_rating_container));
        mQ1FeedbackBar = RatingBarHelper.create(rootView.findViewById(
                R.id.session_feedback_q1_container));
        mQ2FeedbackBar = RatingBarHelper.create(rootView.findViewById(
                R.id.session_feedback_q2_container));
        mQ3FeedbackBar = RatingBarHelper.create(rootView.findViewById(
                R.id.session_feedback_q3_container));

        mQ4RadioGroup = (RadioGroup) rootView.findViewById(R.id.session_feedback_q4);

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
                        EasyTracker.getTracker().sendEvent("Session", "Feedback", mTitleString, 0L);
                        LOGD("Tracker", "Feedback: " + mTitleString);
                        getActivity().finish();
                    }
                });
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

        EasyTracker.getTracker().sendView("Feedback: " + mTitleString);
        LOGD("Tracker", "Feedback: " + mTitleString);
    }

    /* ALL THE FEEDBACKS */
    void submitAllFeedback() {
        int rating = mSessionRatingFeedbackBar.getValue() + 1;
        int q1Answer = mQ1FeedbackBar.getValue() + 1;
        int q2Answer = mQ2FeedbackBar.getValue() + 1;
        int q3Answer = mQ3FeedbackBar.getValue() + 1;

        // Don't add +1, since this is effectively a boolean. index 0 = false, 1 = true,
        // -1 means no answer was given.
        int q4Answer = getCheckedRadioIndex(mQ4RadioGroup);

        String comments = mComments.getText().toString();

        String answers = mSessionId + ", "
                + rating + ", "
                + q1Answer + ", "
                + q2Answer + ", "
                + q3Answer + ", "
                + q4Answer + ", "
                + comments;
        LOGD(TAG, answers);

        ContentValues values = new ContentValues();
        values.put(Feedback.SESSION_ID, mSessionId);
        values.put(Feedback.UPDATED, System.currentTimeMillis());
        values.put(Feedback.SESSION_RATING, rating);
        values.put(Feedback.ANSWER_RELEVANCE, q1Answer);
        values.put(Feedback.ANSWER_CONTENT, q2Answer);
        values.put(Feedback.ANSWER_SPEAKER, q3Answer);
        values.put(Feedback.ANSWER_WILLUSE, q4Answer);
        values.put(Feedback.COMMENTS, comments);

        getActivity().getContentResolver()
                .insert(ScheduleContract.Feedback.buildFeedbackUri(mSessionId), values);
    }

    int getCheckedRadioIndex(RadioGroup rg) {
        int radioId = rg.getCheckedRadioButtonId();
        View rb = rg.findViewById(radioId);
        return rg.indexOfChild(rb);
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
	public void onLoaderReset(Loader<Cursor> loader) {}

    /**
     * Helper class for building a rating bar from a {@link SeekBar}.
     */
    private static class RatingBarHelper implements SeekBar.OnSeekBarChangeListener {
        private SeekBar mBar;
        private boolean mTrackingTouch;
        private TextView[] mLabels;

        public static RatingBarHelper create(View container) {
            return new RatingBarHelper(container);
        }

        private RatingBarHelper(View container) {
            // Force the seekbar to multiples of 100
            mBar = (SeekBar) container.findViewById(R.id.rating_bar);
            mLabels = new TextView[]{
                    (TextView) container.findViewById(R.id.rating_bar_label_1),
                    (TextView) container.findViewById(R.id.rating_bar_label_2),
                    (TextView) container.findViewById(R.id.rating_bar_label_3),
                    (TextView) container.findViewById(R.id.rating_bar_label_4),
                    (TextView) container.findViewById(R.id.rating_bar_label_5),
            };
            mBar.setMax(400);
            mBar.setProgress(200);
            onProgressChanged(mBar, 200, false);
            mBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int value = Math.round(progress / 100f);
            if (fromUser) {
                seekBar.setProgress(value * 100);
            }
            if (!mTrackingTouch) {
                for (int i = 0; i < mLabels.length; i++) {
                    mLabels[i].setSelected(i == value);
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mTrackingTouch = true;
            for (TextView mLabel : mLabels) {
                mLabel.setSelected(false);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int value = getValue();
            mTrackingTouch = false;
            for (int i = 0; i < mLabels.length; i++) {
                mLabels[i].setSelected(i == value);
            }
        }

        public int getValue() {
            return mBar.getProgress() / 100;
        }
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Sessions} query parameters.
     */
    private interface SessionsQuery {
        String[] PROJECTION = {
                ScheduleContract.Sessions.SESSION_TITLE,
        };

        int TITLE = 0;
    }
}
