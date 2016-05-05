package com.google.samples.apps.iosched.util;

import android.app.Activity;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import no.java.schedule.BuildConfig;
import no.java.schedule.io.model.JZFeedback;
import retrofit.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by kkho on 05.05.2016.
 */
public class RestServiceDevNull {
    private static RestServiceDevNull instance = null;
    private RestDevApi restDevApi = null;
    private Activity activity = null;

    private RestServiceDevNull(String mode) {
        String endPoint = BuildConfig.SESSION_FEEDBACK_WEB_URI;
        if(mode.equals("TEST")) {
            endPoint = BuildConfig.SESSION_FEEDBACK_WEB_URI_TEST;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endPoint)
                .build();
        RestDevApi service = retrofit.create(RestDevApi.class);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public static RestServiceDevNull getInstance(String mode, Activity activity) {
        if (instance == null) {
            instance = new RestServiceDevNull(mode);
        }
        instance.setActivity(activity);
        return instance;
    }

    public void submitFeedbackToDevNull(String eventId, String sessionId, String voterId, JZFeedback feedbackBody) {
        restDevApi.postSessionFeedback(eventId, sessionId, voterId, feedbackBody, retrofitCallBack);
    }

    public Callback retrofitCallBack = new Callback() {

        @Override
        public void onResponse(retrofit.Response response, retrofit.Retrofit retrofit) {
            Toast.makeText(activity,
                    "Thank you for the feedback!",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(Throwable t) {
            Toast.makeText(activity,
                    "Something went wrong with the connection, please try again!",
                    Toast.LENGTH_SHORT).show();
        }
    };
}
