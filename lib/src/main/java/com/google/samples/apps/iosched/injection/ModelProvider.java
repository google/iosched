package com.google.samples.apps.iosched.injection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.LoaderManager;

import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.explore.ExploreIOModel;
import com.google.samples.apps.iosched.feed.FeedModel;
import com.google.samples.apps.iosched.feedback.FeedbackHelper;
import com.google.samples.apps.iosched.feedback.SessionFeedbackModel;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.myio.MyIOModel;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel;
import com.google.samples.apps.iosched.session.SessionDetailModel;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.videolibrary.VideoLibraryModel;

/**
 * Provides a way to inject stub classes when running integration tests.
 */
public class ModelProvider {

    // These are all only used for instrumented tests
    @SuppressLint("StaticFieldLeak")
    private static SessionDetailModel stubSessionDetailModel = null;

    @SuppressLint("StaticFieldLeak")
    private static MyScheduleModel stubMyScheduleModel = null;

    @SuppressLint("StaticFieldLeak")
    private static MyIOModel stubMyIOModel = null;

    @SuppressLint("StaticFieldLeak")
    private static SessionFeedbackModel stubSessionFeedbackModel = null;

    @SuppressLint("StaticFieldLeak")
    private static VideoLibraryModel stubVideoLibraryModel = null;

    @SuppressLint("StaticFieldLeak")
    private static ExploreIOModel stubExploreIOModel = null;

    public static SessionDetailModel provideSessionDetailModel(Uri sessionUri, Context context,
            SessionsHelper sessionsHelper, LoaderManager loaderManager) {
        if (stubSessionDetailModel != null) {
            return stubSessionDetailModel;
        } else {
            return new SessionDetailModel(sessionUri, context, sessionsHelper, loaderManager);
        }
    }

    public static MyScheduleModel provideMyScheduleModel(ScheduleHelper scheduleHelper,
            SessionsHelper sessionsHelper, Context context) {
        MyScheduleModel model = stubMyScheduleModel != null
                ? stubMyScheduleModel
                : new MyScheduleModel(scheduleHelper, sessionsHelper, context);
        model.initStaticDataAndObservers();
        return model;
    }

    public static FeedModel provideFeedModel(Context context) {
        FeedModel model = new FeedModel();
        return model;
    }

    public static SessionFeedbackModel provideSessionFeedbackModel(Uri sessionUri, Context context,
            FeedbackHelper feedbackHelper, LoaderManager loaderManager) {
        if (stubSessionFeedbackModel != null) {
            return stubSessionFeedbackModel;
        } else {
            return new SessionFeedbackModel(loaderManager, sessionUri, context, feedbackHelper);
        }
    }

    public static VideoLibraryModel provideVideoLibraryModel(Uri videoUri, Uri myVideosUri,
            Uri filterUri, Activity activity, LoaderManager loaderManager) {
        if (stubVideoLibraryModel != null) {
            return stubVideoLibraryModel;
        } else {
            return new VideoLibraryModel(activity, loaderManager, videoUri, myVideosUri, filterUri);
        }
    }

    public static ExploreIOModel provideExploreIOModel(Uri sessionsUri, Context context,
            LoaderManager loaderManager) {
        if (stubExploreIOModel != null) {
            return stubExploreIOModel;
        } else {
            return new ExploreIOModel(context, sessionsUri, loaderManager);
        }
    }

    public static void setStubModel(Model model) {
        if (model instanceof  ExploreIOModel) {
            stubExploreIOModel = (ExploreIOModel) model;
        } else if (model instanceof  VideoLibraryModel) {
            stubVideoLibraryModel = (VideoLibraryModel) model;
        } else if (model instanceof SessionFeedbackModel) {
            stubSessionFeedbackModel = (SessionFeedbackModel) model;
        } else if (model instanceof SessionDetailModel) {
            stubSessionDetailModel = (SessionDetailModel) model;
        } if (model instanceof MyScheduleModel) {
            stubMyScheduleModel = (MyScheduleModel) model;
        }
    }

}
