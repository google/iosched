package com.google.samples.apps.iosched.session;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.testutils.StubModelHelper;

import java.util.HashMap;

/**
 * A stub {@link SessionDetailModel}, to be injected using {@link com.google.samples.apps.iosched
 * .injection.ModelProvider}. It overrides {@link #requestData(QueryEnum, DataQueryCallback)} to
 * bypass the loader manager mechanism. Use the classes in {@link com.google.samples.apps.iosched
 * .mockdata}
 * to provide the stub cursors.
 */
public class StubSessionDetailModel extends SessionDetailModel {

    private HashMap<QueryEnum, Cursor> mFakeData = new HashMap<QueryEnum, Cursor>();

    public StubSessionDetailModel(Uri sessionUri, Context context, Cursor sessionCursor,
            Cursor speakersCursor, Cursor tagMetadataCursor) {
        super(sessionUri, context, null, null);
        mFakeData.put(SessionDetailQueryEnum.SESSIONS, sessionCursor);
        mFakeData.put(SessionDetailQueryEnum.SPEAKERS, speakersCursor);
        mFakeData.put(SessionDetailQueryEnum.TAG_METADATA, tagMetadataCursor);
    }

    /**
     * Overrides the loader manager mechanism by directly calling {@link #onLoadFinished(QueryEnum,
     * Cursor)} with a stub {@link Cursor} as provided in the constructor.
     */
    @Override
    public void requestData(final @NonNull SessionDetailQueryEnum query,
            final @NonNull DataQueryCallback callback) {
        new StubModelHelper<SessionDetailModel.SessionDetailQueryEnum>()
                .overrideLoaderManager(query, callback, mFakeData, mDataQueryCallbacks, this);
    }
}
