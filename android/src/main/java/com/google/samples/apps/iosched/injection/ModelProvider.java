package com.google.samples.apps.iosched.injection;

import android.app.LoaderManager;
import android.content.Context;
import android.net.Uri;

import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel;
import com.google.samples.apps.iosched.session.SessionDetailModel;
import com.google.samples.apps.iosched.util.SessionsHelper;

/**
 * Provides a way to inject stub classes when running integration tests.
 */
public class ModelProvider {

    private static SessionDetailModel stubSessionDetailModel = null;

    private static MyScheduleModel stubMyScheduleModel = null;

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

    public static void setStubMyScheduleModel(MyScheduleModel model) {
        stubMyScheduleModel = model;
    }

    public static MyScheduleModel provideMyScheduleModel(ScheduleHelper scheduleHelper,
            Context context) {
        if (stubMyScheduleModel != null) {
            return stubMyScheduleModel;
        } else {
            return new MyScheduleModel(scheduleHelper, context).initStaticDataAndObservers();
        }
    }

}
