package com.google.samples.apps.iosched.session;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import com.google.samples.apps.iosched.archframework.QueryEnum;

import org.junit.runner.RunWith;

/**
 * A stub {@link SessionDetailModel}, to be injected using {@link com.google.samples.apps.iosched
 * .injection.ModelProvider}.
 * It overrides {@link #requestData(QueryEnum, DataQueryCallback)} to bypass the loader
 * manager mechanism. Use the classes in {@link com.google.samples.apps.iosched.mockdata} to provide
 * the stub cursors.
 */
@RunWith(AndroidJUnit4.class)
public class StubSessionDetailModel extends SessionDetailModel {

    private Cursor mSessionCursor;

    private Cursor mSpeakersCursor;

    private Cursor mTagMetadataCursor;

    public StubSessionDetailModel(Context context, Cursor sessionCursor, Cursor speakersCursor,
            Cursor tagMetadataCursor) {
        super(null, context, null, null);
        mSessionCursor = sessionCursor;
        mSpeakersCursor = speakersCursor;
        mTagMetadataCursor = tagMetadataCursor;
    }

    /**
     * Overrides the loader manager mechanism by directly calling {@link #onLoadFinished(QueryEnum,
     * Cursor)} with a stub {@link Cursor} as provided in the constructor.
     */
    @Override
    public void requestData(final @NonNull SessionDetailQueryEnum query,
            final @NonNull DataQueryCallback callback) {
        // Add the callback so it gets fired properly
        mDataQueryCallbacks.put(query, callback);

        Handler h = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // Call onLoadFinished with stub cursor and query
                switch (query) {
                    case SESSIONS:
                        onLoadFinished(query, mSessionCursor);
                        break;
                    case FEEDBACK:
                        break;
                    case SPEAKERS:
                        onLoadFinished(query, mSpeakersCursor);
                        break;
                    case TAG_METADATA:
                        onLoadFinished(query, mTagMetadataCursor);
                        break;
                    case MY_VIEWED_VIDEOS:
                        break;
                }
            }
        };

        // Delayed to ensure the UI is ready, because it will fire the callback to update the view
        // very quickly
        // TODO - look into possibility of using IdlingResource instead of this 5ms delay
        h.postDelayed(r, 5);
    }
}
