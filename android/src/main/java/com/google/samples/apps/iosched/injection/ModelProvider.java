package com.google.samples.apps.iosched.injection;

import android.app.LoaderManager;
import android.content.Context;
import android.net.Uri;

import com.google.samples.apps.iosched.session.SessionDetailModel;
import com.google.samples.apps.iosched.util.SessionsHelper;

/**
 * Provides a way to inject stub classes when running integration tests.
 */
public class ModelProvider {

    private static SessionDetailModel stubSessionDetailModel = null;

    public static void setStubSessionDetailModel(SessionDetailModel model) {
        stubSessionDetailModel = model;
    }

    public static SessionDetailModel provideSessionDetailModel(Uri sessionUri, Context context,
            SessionsHelper sessionsHelper,
            LoaderManager loaderManager) {
        if (stubSessionDetailModel != null) {
            return stubSessionDetailModel;
        } else {
            return new SessionDetailModel(sessionUri, context, sessionsHelper, loaderManager);
        }
    }

}
