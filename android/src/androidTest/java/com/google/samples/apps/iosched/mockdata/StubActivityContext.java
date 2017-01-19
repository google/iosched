package com.google.samples.apps.iosched.mockdata;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

/**
 * An object allowing to use a non Activity context and then to add an Activity context to be used
 * with {@link Context#startActivity(Intent)}.
 */
public class StubActivityContext extends ContextWrapper {

    private Activity mActivity;

    public StubActivityContext(Context context) {
        super(context);
    }

    public void setActivityContext(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void startActivity(Intent intent) {
        if (mActivity != null) {
            mActivity.startActivity(intent);
        }
    }
}
