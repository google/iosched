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

package com.google.samples.apps.iosched.iowear;

import static com.google.samples.apps.iosched.iowear.utils.Utils.LOGD;
import static com.google.samples.apps.iosched.iowear.utils.Utils.makeLogTag;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.iowear.utils.Utils;
import com.google.samples.apps.iosched.iowear.fragments.FeedbackFragment;
import com.google.samples.apps.iosched.iowear.fragments.RadioFragment;
import com.google.samples.apps.iosched.iowear.fragments.StarFragment;
import com.google.samples.apps.iosched.iowear.fragments.SubmitFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The main activity tht build all pages of a session feedback.
 */
public class PagerActivity extends Activity
        implements OnQuestionAnsweredListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long PAGE_FLIP_DELAY_MS = 2000; // in millis
    private ViewPager mViewPager;
    private static final String TAG = makeLogTag("PagerActivity");
    private List<FeedbackFragment> mFragments = new ArrayList<FeedbackFragment>();
    private Timer mTimer;
    private TimerTask mTimerTask;
    private Handler mHandler;
    private Map<Integer, Integer> responses = new HashMap<Integer, Integer>();
    private GoogleApiClient mGoogleApiClient;
    private String mSessionId;
    private ImageView[] indicators = null;
    private int[] mSavedResponses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler = new Handler();
        setupViews();

        if (getIntent().hasExtra(HomeListenerService.KEY_SESSION_ID)) {
            mSessionId = getIntent().getExtras().getString(HomeListenerService.KEY_SESSION_ID);
            LOGD(TAG, "Session received from service: " + mSessionId);

        }

        // if there is any prior persisted answers for this feedback, lets load them.
        mSavedResponses = Utils.getPersistedResponses(this, mSessionId);
        Utils.saveSessionId(this, mSessionId);

        for (int i = 0; i < 4; i++) {
            LOGD(TAG, "Response " + i + ": " + mSavedResponses[i]);
            if (mSavedResponses[i] > -1) {
                responses.put(i, mSavedResponses[i]);
            }
        }

        final PagerAdapter adapter = new PagerAdapter(getFragmentManager());
        StarFragment fragment0 = StarFragment.newInstance(0, mSavedResponses[0]);
        RadioFragment fragment1 = RadioFragment.newInstance(1, mSavedResponses[1]);
        RadioFragment fragment2 = RadioFragment.newInstance(2, mSavedResponses[2]);
        RadioFragment fragment3 = RadioFragment.newInstance(3, mSavedResponses[3]);

        mFragments.add(fragment0);
        mFragments.add(fragment1);
        mFragments.add(fragment2);
        mFragments.add(fragment3);

        for (FeedbackFragment f : mFragments) {
            f.setOnQuestionListener(this);
            adapter.addFragment(f);
        }

        adapter.addFragment(new SubmitFragment(this));
        mViewPager.setAdapter(adapter);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                clearTimer();
                setIndicator(i);
                if (i == 0) {
                    mFragments.get(i).reshowQuestion();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onQuestionAnswered(int questionNumber, int responseNumber) {
        LOGD(TAG, "Question Answered: " + questionNumber + " -> " + responseNumber);
        Utils.saveResponse(this, questionNumber, responseNumber);
        renewTimer(questionNumber + 1);
        responses.put(questionNumber, responseNumber);
    }

    @Override
    public void submit() {
        // do the submission
        Intent finishIntent = new Intent(this, FinishActivity.class);
        try {
            PutDataRequest dataRequest = buildDataRequest();
            if (mGoogleApiClient.isConnected()) {
                Wearable.DataApi.putDataItem(
                        mGoogleApiClient, dataRequest)
                        .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                if (!dataItemResult.getStatus().isSuccess()) {
                                    LOGD(TAG, "Failed to send back responses, status: "
                                            + dataItemResult.getStatus());
                                }
                            }
                        });
            } else {
                Log.e(TAG, "submit() Failed to send data to phone since there was no Google API "
                        + "client connectivity");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build a json from responses", e);
        }

        // remove local notification upon submission
        NotificationManagerCompat.from(this)
                .cancel(mSessionId, HomeListenerService.NOTIFICATION_ID);

        // clear persisted local data
        Utils.clearResponses(this);
        startActivity(finishIntent);
        finish();
    }

    /**
     * Builds a {@link com.google.android.gms.wearable.PutDataRequest} which holds a JSON
     * representation of the feedback collected.
     */
    private PutDataRequest buildDataRequest() throws JSONException {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest
                .create(HomeListenerService.PATH_RESPONSE);
        DataMap dataMap = putDataMapRequest.getDataMap();

        JSONArray jsonArray = new JSONArray();
        if (!responses.isEmpty()) {
            JSONObject sessionObj = new JSONObject();
            sessionObj.put("s", mSessionId);
            jsonArray.put(0, sessionObj);
            int i = 1;
            for (Integer key : responses.keySet()) {
                JSONObject obj = new JSONObject();
                obj.put("q", key);
                obj.put("a", responses.get(key));
                jsonArray.put(i++, obj);
            }
        }
        String response = jsonArray.toString();
        LOGD(TAG, "JSON representation of the response: " + response);
        dataMap.putString("response", response);
        return putDataMapRequest.asPutDataRequest();
    }

    /**
     * Renews the timer that causes pages to flip upon providing an answer.
     */
    private void renewTimer(final int targetPage) {
        clearTimer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mViewPager.setCurrentItem(targetPage);
                    }
                });
            }
        };
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, PAGE_FLIP_DELAY_MS);
    }

    /**
     * Clear the timer that flips the pages.
     */
    private void clearTimer() {
        if (null != mTimer) {
            mTimer.cancel();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LOGD(TAG, "onConnected() Connected to Google Api Service");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed(): Connection to Google Api Service failed with result: "
                + connectionResult);
    }

    private void setupViews() {
        mViewPager = (ViewPager) findViewById(R.id.pager);
        indicators = new ImageView[5];
        indicators[0] = (ImageView) findViewById(R.id.indicator_0);
        indicators[1] = (ImageView) findViewById(R.id.indicator_1);
        indicators[2] = (ImageView) findViewById(R.id.indicator_2);
        indicators[3] = (ImageView) findViewById(R.id.indicator_3);
        indicators[4] = (ImageView) findViewById(R.id.indicator_4);
    }

    /**
     * Sets the page indicator for the ViewPager
     */
    private void setIndicator(int i) {
        for (int k = 0; k < indicators.length; k++) {
            indicators[k].setImageResource(i == k
                    ? R.drawable.page_dot_full : R.drawable.page_dot_empty);
        }
    }
}
