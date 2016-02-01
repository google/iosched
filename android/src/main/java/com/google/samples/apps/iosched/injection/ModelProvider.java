package com.google.samples.apps.iosched.injection;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.net.Uri;

import com.google.samples.apps.iosched.feedback.FeedbackHelper;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel;
import com.google.samples.apps.iosched.session.SessionDetailModel;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.videolibrary.VideoLibraryModel;

/**
 * Provides a way to inject stub classes when running integration tests.
 */
public class ModelProvider {

    private static SessionDetailModel stubSessionDetailModel = null;

    private static MyScheduleModel stubMyScheduleModel = null;

    private static SessionFeedbackModel stubSessionFeedbackModel = null;

    private static VideoLibraryModel stubVideoLibraryModel = null;

    public static void setStubSessionDetailModel(SessionDetailModel model) {
        stubSessionDetailModel = model;
    }

    public static SessionDetailModel provideSessionDetailModel(Uri sessionUri, Context context,
            SessionsHelper sessionsHelper, LoaderManager loaderManager) {
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

    public static void setStubSessionFeedbackModel(SessionFeedbackModel model) {
        stubSessionFeedbackModel = model;
    }

    public static SessionFeedbackModel provideSessionFeedbackModel(Uri sessionUri, Context context,
            FeedbackHelper feedbackHelper, LoaderManager loaderManager) {
        if (stubSessionFeedbackModel != null) {
            return stubSessionFeedbackModel;
        } else {
            return new SessionFeedbackModel(loaderManager, sessionUri, context, feedbackHelper);
        }
    }

    public static void setStubVideoLibraryModel(VideoLibraryModel model) {
        stubVideoLibraryModel = model;
    }

    public static VideoLibraryModel provideVideoLibraryModel(Uri videoUri, Uri myVideosUri,
            Uri filterUri, Activity activity, LoaderManager loaderManager) {

        if (stubVideoLibraryModel != null) {
            return stubVideoLibraryModel;
        } else {
            return new VideoLibraryModel(activity, loaderManager, videoUri, myVideosUri, filterUri);
        }
    }

}
