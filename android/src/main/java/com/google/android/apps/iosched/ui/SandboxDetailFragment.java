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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.ImageLoader;
import com.google.android.apps.iosched.util.SessionsHelper;
import com.google.android.apps.iosched.util.UIUtils;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that shows detail information for a sandbox company, including
 * company name, description, product description, logo, etc.
 */
public class SandboxDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(SandboxDetailFragment.class);

    private Uri mCompanyUri;

    private TextView mName;
    private TextView mSubtitle;

    private ImageView mLogo;
    private TextView mUrl;
    private TextView mDesc;

    private ImageLoader mImageLoader;
    private int mCompanyImageSize;
    private Drawable mCompanyPlaceHolderImage;

    private StringBuilder mBuffer = new StringBuilder();
    private String mRoomId;
    private String mCompanyName;

    public interface Callbacks {
        public void onTrackIdAvailable(String trackId);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onTrackIdAvailable(String trackId) {}
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mCompanyUri = intent.getData();
        if (mCompanyUri == null) {
            return;
        }

        mCompanyImageSize = getResources().getDimensionPixelSize(R.dimen.sandbox_company_image_size);
        mCompanyPlaceHolderImage = getResources().getDrawable(R.drawable.sandbox_logo_empty);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mCompanyUri == null) {
            return;
        }

        if (getActivity() instanceof ImageLoader.ImageLoaderProvider) {
            mImageLoader = ((ImageLoader.ImageLoaderProvider) getActivity()).getImageLoaderInstance();
        }

        // Start background query to load sandbox company details
        getLoaderManager().initLoader(SandboxQuery._TOKEN, null, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.sandbox_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SessionsHelper helper = new SessionsHelper(getActivity());
        switch (item.getItemId()) {
            case R.id.menu_map:
                if (mRoomId != null && mCompanyName != null) {
                    EasyTracker.getTracker().sendEvent(
                            "Sandbox", "Map", mCompanyName, 0L);
                    LOGD("Tracker", "Map: " + mCompanyName);

                    helper.startMapActivity(mRoomId);
                    return true;
                }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_sandbox_detail, null);
        mName = (TextView) rootView.findViewById(R.id.company_name);
        mLogo = (ImageView) rootView.findViewById(R.id.company_logo);
        mUrl = (TextView) rootView.findViewById(R.id.company_url);
        mDesc = (TextView) rootView.findViewById(R.id.company_desc);
        mSubtitle = (TextView) rootView.findViewById(R.id.company_subtitle);
        return rootView;
    }

    void buildUiFromCursor(Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (!cursor.moveToFirst()) {
            return;
        }

        mCompanyName = cursor.getString(SandboxQuery.NAME);
        mName.setText(mCompanyName);

        // Start background fetch to load company logo
        final String logoUrl = cursor.getString(SandboxQuery.LOGO_URL);

        if (!TextUtils.isEmpty(logoUrl) && mImageLoader != null) {
            mImageLoader.get(UIUtils.getConferenceImageUrl(logoUrl), mLogo,
                    mCompanyPlaceHolderImage, mCompanyImageSize, mCompanyImageSize);
            mLogo.setVisibility(View.VISIBLE);
        } else {
            mLogo.setVisibility(View.GONE);
        }

        mRoomId = cursor.getString(SandboxQuery.ROOM_ID);

        // Set subtitle: time and room
        long blockStart = cursor.getLong(SandboxQuery.BLOCK_START);
        long blockEnd = cursor.getLong(SandboxQuery.BLOCK_END);
        String roomName = cursor.getString(SandboxQuery.ROOM_NAME);
        final String subtitle = UIUtils.formatSessionSubtitle(
                "Sandbox", blockStart, blockEnd, roomName, mBuffer,
                getActivity());

        mSubtitle.setText(subtitle);

        mUrl.setText(cursor.getString(SandboxQuery.URL));
        mDesc.setText(cursor.getString(SandboxQuery.DESC));

        String trackId = cursor.getString(SandboxQuery.TRACK_ID);
        EasyTracker.getTracker().sendView("Sandbox Company: " + mCompanyName);
        LOGD("Tracker", "Sandbox Company: " + mCompanyName);

        mCallbacks.onTrackIdAvailable(trackId);
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Sandbox}
     * query parameters.
     */
    private interface SandboxQuery {
        int _TOKEN = 0x4;

        String[] PROJECTION = {
                ScheduleContract.Sandbox.COMPANY_NAME,
                ScheduleContract.Sandbox.COMPANY_DESC,
                ScheduleContract.Sandbox.COMPANY_URL,
                ScheduleContract.Sandbox.COMPANY_LOGO_URL,
                ScheduleContract.Sandbox.TRACK_ID,
                ScheduleContract.Sandbox.BLOCK_START,
                ScheduleContract.Sandbox.BLOCK_END,
                ScheduleContract.Sandbox.ROOM_NAME,
                ScheduleContract.Sandbox.ROOM_ID
        };

        int NAME = 0;
        int DESC = 1;
        int URL = 2;
        int LOGO_URL = 3;
        int TRACK_ID = 4;
        int BLOCK_START = 5;
        int BLOCK_END = 6;
        int ROOM_NAME = 7;
        int ROOM_ID = 8;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(), mCompanyUri, SandboxQuery.PROJECTION, null, null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        buildUiFromCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
