package com.google.samples.apps.iosched.util;

import android.app.Activity;
import android.widget.Toast;

import no.java.schedule.v2.BuildConfig;
import no.java.schedule.v2.io.model.JZFeedback;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(new ToStringConverterFactory())
                .build();
        restDevApi = retrofit.create(RestDevApi.class);
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
        restDevApi.postSessionFeedback(eventId, sessionId, voterId, feedbackBody).
                enqueue(retrofitCallBack);
    }

    public Callback retrofitCallBack = new Callback() {
        @Override
        public void onResponse(Call call, Response response) {
            Toast.makeText(activity,
                    "Thank you for the feedback!",
                    Toast.LENGTH_SHORT).show();
            activity.finish();
        }

        @Override
        public void onFailure(Call call, Throwable t) {
            Toast.makeText(activity,
                    "Something went wrong with the connection, please try again later!",
                    Toast.LENGTH_SHORT).show();
        }
    };
}
