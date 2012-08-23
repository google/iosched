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
import com.google.android.apps.iosched.util.ImageFetcher;
import com.google.android.apps.iosched.util.UIUtils;

import com.actionbarsherlock.app.SherlockFragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that shows detail information for a developer sandbox company, including
 * company name, description, logo, etc.
 */
public class VendorDetailFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(VendorDetailFragment.class);

    private Uri mVendorUri;

    private TextView mName;

    private ImageView mLogo;
    private TextView mUrl;
    private TextView mDesc;

    private ImageFetcher mImageFetcher;

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
        mVendorUri = intent.getData();
        if (mVendorUri == null) {
            return;
        }

        mImageFetcher = UIUtils.getImageFetcher(getActivity());
        mImageFetcher.setImageFadeIn(false);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mVendorUri == null) {
            return;
        }

        // Start background query to load vendor details
        getLoaderManager().initLoader(VendorsQuery._TOKEN, null, this);
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
    public void onPause() {
        super.onPause();
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_vendor_detail, null);
        mName = (TextView) rootView.findViewById(R.id.vendor_name);
        mLogo = (ImageView) rootView.findViewById(R.id.vendor_logo);
        mUrl = (TextView) rootView.findViewById(R.id.vendor_url);
        mDesc = (TextView) rootView.findViewById(R.id.vendor_desc);
        return rootView;
    }

    public void buildUiFromCursor(Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (!cursor.moveToFirst()) {
            return;
        }

        String nameString = cursor.getString(VendorsQuery.NAME);
        mName.setText(nameString);

        // Start background fetch to load vendor logo
        final String logoUrl = cursor.getString(VendorsQuery.LOGO_URL);

        if (!TextUtils.isEmpty(logoUrl)) {
            mImageFetcher.loadThumbnailImage(logoUrl, mLogo, R.drawable.sandbox_logo_empty);
        }

        mUrl.setText(cursor.getString(VendorsQuery.URL));
        mDesc.setText(cursor.getString(VendorsQuery.DESC));

        EasyTracker.getTracker().trackView("Sandbox Vendor: " + nameString);
        LOGD("Tracker", "Sandbox Vendor: " + nameString);

        mCallbacks.onTrackIdAvailable(cursor.getString(VendorsQuery.TRACK_ID));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(), mVendorUri, VendorsQuery.PROJECTION, null, null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        buildUiFromCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Vendors}
     * query parameters.
     */
    private interface VendorsQuery {
        int _TOKEN = 0x4;

        String[] PROJECTION = {
                ScheduleContract.Vendors.VENDOR_NAME,
                ScheduleContract.Vendors.VENDOR_DESC,
                ScheduleContract.Vendors.VENDOR_URL,
                ScheduleContract.Vendors.VENDOR_LOGO_URL,
                ScheduleContract.Vendors.TRACK_ID,
        };

        int NAME = 0;
        int DESC = 1;
        int URL = 2;
        int LOGO_URL = 3;
        int TRACK_ID = 4;
    }
}
