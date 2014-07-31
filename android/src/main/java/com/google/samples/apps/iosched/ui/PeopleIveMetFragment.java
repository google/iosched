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

import android.app.AlertDialog;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.app.*;
import android.content.CursorLoader;
import android.content.Loader;
import android.widget.CursorAdapter;
import android.widget.PopupMenu;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class PeopleIveMetFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<People.LoadPeopleResult>,
        View.OnClickListener {

    private static final String TAG = makeLogTag(PeopleIveMetFragment.class);
    private final ArrayList<String> mPlusIds = new ArrayList<String>();
    private CollectionView mCollectionView;
    private GoogleApiClient mApiClient;
    private View mApiError;
    private boolean mCallingApi;

    public static PeopleIveMetFragment newInstance() {
        return new PeopleIveMetFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_people_ive_met, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mCollectionView = (CollectionView) view.findViewById(R.id.collection_view);
        mApiError = view.findViewById(R.id.api_error);
        view.findViewById(R.id.retry).setOnClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        if (!activity.isFinishing()) {
            // Start loading data
            LoaderManager loaderManager = getLoaderManager();
            loaderManager.restartLoader(PeopleIveMetQuery.TOKEN, null, this);
            loaderManager.restartLoader(PeopleIveMetSubQuery.TOKEN, null, this);
            // Set up the API client
            GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity, this, this)
                    .addApi(Plus.API)
                    .addScope(Plus.SCOPE_PLUS_LOGIN);
            if (AccountUtils.hasActiveAccount(activity)) {
                builder.setAccountName(AccountUtils.getActiveAccountName(activity));
            }
            mApiClient = builder.build();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mApiClient.isConnected()) {
            mApiClient.disconnect();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case PeopleIveMetQuery.TOKEN: {
                return new CursorLoader(getActivity(), ScheduleContract.PeopleIveMet.CONTENT_URI,
                        PeopleIveMetQuery.PROJECTION,
                        null, null, ScheduleContract.PeopleIveMet.DEFAULT_SORT);
            }
            case PeopleIveMetSubQuery.TOKEN: {
                return new CursorLoader(getActivity(), ScheduleContract.PeopleIveMet.CONTENT_URI,
                        PeopleIveMetSubQuery.PROJECTION,
                        PeopleIveMetSubQuery.SELECTION, null, null);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case PeopleIveMetQuery.TOKEN: {
                if (0 == cursor.getCount()) {
                    EmptyAdapter adapter = new EmptyAdapter(getActivity());
                    mCollectionView.setCollectionAdapter(adapter);
                    mCollectionView.updateInventory(adapter.getInventory());
                } else {
                    PeopleIveMetAdapter adapter = new PeopleIveMetAdapter(getActivity(), cursor);
                    mCollectionView.setCollectionAdapter(adapter);
                    mCollectionView.updateInventory(adapter.getInventory());
                }
                break;
            }
            case PeopleIveMetSubQuery.TOKEN: {
                if (!mCallingApi) {
                    mPlusIds.clear();
                    while (cursor.moveToNext()) {
                        mPlusIds.add(cursor.getString(PeopleIveMetSubQuery.PERSON_ID));
                    }
                    fillInMissingData();
                }
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case PeopleIveMetQuery.TOKEN: {
                mCollectionView.updateInventory(new CollectionView.Inventory());
                break;
            }
            case PeopleIveMetSubQuery.TOKEN: {
                mPlusIds.clear();
                break;
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LOGD(TAG, "onConnected");
        fillInMissingData();
        hideApiError();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // We just wait until the API is available
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (result.hasResolution()) {
            try {
                Activity activity = getActivity();
                if (null != activity) {
                    result.startResolutionForResult(activity,
                            PeopleIveMetActivity.REQUEST_RESOLUTION_FOR_RESULT);
                }
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            showApiError();
        }
    }

    private void fillInMissingData() {
        if (mPlusIds.isEmpty()) {
            return;
        }
        if (!mApiClient.isConnected()) {
            return;
        }
        LOGD(TAG, "fillInMissingData (this should appear only once)");
        mCallingApi = true;
        Plus.PeopleApi.load(mApiClient, mPlusIds).setResultCallback(this);
    }

    void showApiError() {
        mApiError.setVisibility(View.VISIBLE);
    }

    void hideApiError() {
        mApiError.setVisibility(View.GONE);
    }

    void retry() {
        mApiClient.connect();
    }

    @Override
    public void onResult(final People.LoadPeopleResult loadPeopleResult) {
        if (!loadPeopleResult.getStatus().isSuccess()) {
            showApiError();
            return;
        }
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                PersonBuffer personBuffer = loadPeopleResult.getPersonBuffer();
                ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                for (int i = 0, max = personBuffer.getCount(); i < max; ++i) {
                    Person person = personBuffer.get(i);
                    ContentValues values = new ContentValues();
                    values.put(ScheduleContract.PeopleIveMet.PERSON_NAME,
                            person.getDisplayName());
                    values.put(ScheduleContract.PeopleIveMet.PERSON_IMAGE_URL,
                            person.getImage().getUrl());
                    operations.add(ContentProviderOperation.newUpdate(
                            ScheduleContract.PeopleIveMet.buildPersonUri(mPlusIds.get(i)))
                            .withValues(values)
                            .build());
                }
                ContentResolver resolver = getActivity().getContentResolver();
                try {
                    resolver.applyBatch(ScheduleContract.CONTENT_AUTHORITY, operations);
                    return operations.size();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer result) {
                mCallingApi = false;
            }
        }.execute();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retry: {
                retry();
                break;
            }
        }
    }

    private interface PeopleIveMetQuery {

        final int TOKEN = 0x1;

        final String[] PROJECTION = {
                ScheduleContract.PeopleIveMet._ID,
                ScheduleContract.PeopleIveMet.PERSON_ID,
                ScheduleContract.PeopleIveMet.PERSON_NAME,
                ScheduleContract.PeopleIveMet.PERSON_IMAGE_URL,
                ScheduleContract.PeopleIveMet.PERSON_NOTE,
                ScheduleContract.PeopleIveMet.PERSON_TIMESTAMP,
        };

        final int PERSON_ID = 1;
        final int PERSON_NAME = 2;
        final int PERSON_IMAGE_URL = 3;
        final int PERSON_NOTE = 4;
        final int PERSON_TIMESTAMP = 5;

    }

    private interface PeopleIveMetSubQuery {

        final int TOKEN = 0x2;

        final String[] PROJECTION = {
                ScheduleContract.PeopleIveMet.PERSON_ID,
        };

        final int PERSON_ID = 0;

        final String SELECTION = ScheduleContract.PeopleIveMet.PERSON_NAME + " IS NULL";

    }

    private static class EmptyAdapter implements CollectionViewCallbacks {

        private final Context mContext;

        public EmptyAdapter(Context context) {
            mContext = context;
        }

        public CollectionView.Inventory getInventory() {
            CollectionView.Inventory inventory = new CollectionView.Inventory();
            inventory.addGroup(new CollectionView.InventoryGroup(PeopleIveMetQuery.TOKEN)
                    .setDisplayCols(1)
                    .setItemCount(1)
                    .setDataIndexStart(0)
                    .setShowHeader(true)
                    .setHeaderLabel(mContext.getString(R.string.title_people_ive_met)));
            return inventory;
        }

        @Override
        public View newCollectionHeaderView(Context context, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.header_people_ive_met,
                    parent, false);
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId,
                String headerLabel) {
        }

        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.list_item_people_ive_met_empty,
                    parent, false);
        }

        @Override
        public void bindCollectionItemView(Context context, View view, int groupId,
                int indexInGroup, int dataIndex, Object tag) {
        }

    }

    private static class PeopleIveMetAdapter extends CursorAdapter implements
            CollectionViewCallbacks, View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        private static final int OFFSET = 2;
        private final ImageLoader mImageLoader;
        private final FragmentManager mFragmentManager;
        private final Cursor mCursor;
        private final Context mContext;
        private String mCurrentMenuPersonId;
        private String mCurrentMenuPersonName;
        private String mCurrentMenuPersonNote;

        public PeopleIveMetAdapter(Activity activity, Cursor c) {
            super(activity, c, 0);
            mCursor = c;
            mContext = activity;
            mImageLoader = new ImageLoader(activity, R.drawable.person_image_empty);
            mFragmentManager = activity.getFragmentManager();
        }

        private static String getPlusUrl(String plusId) {
            return "https://plus.google.com/" + plusId;
        }

        public CollectionView.Inventory getInventory() {
            CollectionView.Inventory inventory = new CollectionView.Inventory();
            inventory.addGroup(new CollectionView.InventoryGroup(PeopleIveMetQuery.TOKEN)
                    .setDisplayCols(1)
                    .setItemCount(mCursor.getCount())
                    .setDataIndexStart(0)
                    .setShowHeader(true)
                    .setHeaderLabel(mContext.getString(R.string.title_people_ive_met)));
            return inventory;
        }

        @Override
        public View newCollectionHeaderView(Context context, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.header_people_ive_met,
                    parent, false);
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId,
                String headerLabel) {
        }

        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            return newView(context, null, parent);
        }

        @Override
        public void bindCollectionItemView(Context context, View view, int groupId,
                int indexInGroup, int dataIndex, Object tag) {
            setCursorPosition(indexInGroup);
            bindView(view, context, mCursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_item_people_ive_met,
                    parent, false);
            assert view != null;
            ViewHolder holder = new ViewHolder();
            holder.image = (ImageView) view.findViewById(R.id.image);
            holder.name = (TextView) view.findViewById(R.id.name);
            holder.timestamp = (TextView) view.findViewById(R.id.timestamp);
            holder.note = (TextView) view.findViewById(R.id.note);
            holder.actions = (ImageButton) view.findViewById(R.id.actions);
            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String personId = cursor.getString(PeopleIveMetQuery.PERSON_ID);
            String personName = cursor.getString(PeopleIveMetQuery.PERSON_NAME);
            String note = cursor.getString(PeopleIveMetQuery.PERSON_NOTE);
            ViewHolder holder = (ViewHolder) view.getTag();
            // Click listeners
            view.setOnClickListener(this);
            view.setTag(R.id.tag_person_id, personId);
            holder.actions.setOnClickListener(this);
            holder.actions.setTag(R.id.tag_person_id, personId);
            holder.actions.setTag(R.id.tag_person_name, personName);
            holder.actions.setTag(R.id.tag_person_note, note);
            // Display the image and labels
            if (!cursor.isNull(PeopleIveMetQuery.PERSON_IMAGE_URL)) {
                mImageLoader.loadImage(cursor.getString(PeopleIveMetQuery.PERSON_IMAGE_URL),
                        holder.image);
            }
            if (TextUtils.isEmpty(personName)) {
                holder.name.setText(getPlusUrl(personId));
            } else {
                holder.name.setText(personName);
            }
            holder.timestamp.setText(DateUtils.getRelativeDateTimeString(mContext,
                    cursor.getLong(PeopleIveMetQuery.PERSON_TIMESTAMP),
                    DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0));
            // note
            if (TextUtils.isEmpty(note)) {
                holder.note.setVisibility(View.GONE);
            } else {
                holder.note.setVisibility(View.VISIBLE);
                holder.note.setText(note);
            }
        }

        private void setCursorPosition(int position) {
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.container: {
                    String personId = (String) v.getTag(R.id.tag_person_id);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(getPlusUrl(personId)));
                    UIUtils.preferPackageForIntent(mContext, intent,
                            UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    mContext.startActivity(intent);
                    break;
                }
                case R.id.actions: {
                    mCurrentMenuPersonId = (String) v.getTag(R.id.tag_person_id);
                    mCurrentMenuPersonName = (String) v.getTag(R.id.tag_person_name);
                    mCurrentMenuPersonNote = (String) v.getTag(R.id.tag_person_note);
                    PopupMenu popup = new PopupMenu(mContext, v);
                    popup.getMenuInflater().inflate(R.menu.people_ive_met, popup.getMenu());
                    popup.setOnMenuItemClickListener(this);
                    popup.show();
                    break;
                }
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.note: {
                    EditNoteFragment.newInstance(mCurrentMenuPersonId, mCurrentMenuPersonName,
                            mCurrentMenuPersonNote)
                            .show(mFragmentManager, EditNoteFragment.TAG);
                    return true;
                }
                case R.id.delete: {
                    new AlertDialog.Builder(mContext)
                            .setMessage(R.string.people_ive_met_delete_confirmation)
                            .setPositiveButton(R.string.people_ive_met_delete,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            new DeletePersonTask(mContext, mCurrentMenuPersonId)
                                                    .execute();
                                        }
                                    }
                            )
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return true;
                }
            }
            return false;
        }

        private static class ViewHolder {
            ImageView image;
            TextView name;
            TextView timestamp;
            TextView note;
            ImageButton actions;
        }

    }

    private static class DeletePersonTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mPersonId;

        public DeletePersonTask(Context context, String personId) {
            mContext = context;
            mPersonId = personId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return 0 < mContext.getContentResolver()
                    .delete(ScheduleContract.PeopleIveMet.buildPersonUri(mPersonId), null, null);
        }

    }

    private static class UpdatePersonNoteTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mPersonId;
        private final String mNote;

        public UpdatePersonNoteTask(Context context, String personId, String note) {
            mContext = context;
            mPersonId = personId;
            mNote = note;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            ContentValues values = new ContentValues();
            values.put(ScheduleContract.PeopleIveMet.PERSON_NOTE, mNote);
            return 0 < mContext.getContentResolver()
                    .update(ScheduleContract.PeopleIveMet.buildPersonUri(mPersonId), values,
                            null, null);
        }

    }

    public static class EditNoteFragment extends DialogFragment implements View.OnClickListener {

        public static final String TAG =
                "com.google.samples.apps.iosched.ui.PeopleIveMetFragment.EditNoteFragment";

        private static final String ARG_PERSON_ID = "person_id";
        private static final String ARG_PERSON_NAME = "person_name";
        private static final String ARG_DEFAULT_CONTENT = "default_content";

        private EditText mEditNote;

        public EditNoteFragment() {
        }

        public static EditNoteFragment newInstance(String personId, String personName,
                String defaultContent) {
            EditNoteFragment fragment = new EditNoteFragment();
            Bundle args = new Bundle();
            args.putString(ARG_PERSON_ID, personId);
            args.putString(ARG_PERSON_NAME, personName);
            args.putString(ARG_DEFAULT_CONTENT, defaultContent);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            // Set title
            Bundle args = getArguments();
            String personName = args.getString(ARG_PERSON_NAME);
            if (TextUtils.isEmpty(personName)) {
                getDialog().setTitle(getString(R.string.people_ive_met_edit_note_title_with_name, personName));
            } else {
                getDialog().setTitle(getString(R.string.people_ive_met_edit_note_title));
            }
            // Inflate the dialog content
            return inflater.inflate(R.layout.fragment_edit_note, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            mEditNote = (EditText) view.findViewById(R.id.note);
            Bundle args = getArguments();
            mEditNote.setText(args.getString(ARG_DEFAULT_CONTENT));
            view.findViewById(R.id.ok).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ok: {
                    dismiss();
                    break;
                }
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            Bundle args = getArguments();
            String personId = args.getString(ARG_PERSON_ID);
            new UpdatePersonNoteTask(getActivity(), personId, mEditNote.getText().toString())
                    .execute();
            super.onDismiss(dialog);
        }

    }

}
