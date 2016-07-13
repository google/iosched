package com.google.samples.apps.iosched.util;

import com.google.gson.JsonElement;

import no.java.schedule.io.model.JZFeedback;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RestDevApi {
    @Headers( "Content-Type: application/json" )
    @POST("/events/{eventId}/sessions/{sessionId}/feedbacks")
    Call<String> postSessionFeedback(@Path("eventId") String eventId,
                                      @Path("sessionId") String sessionId,
                                      @Header("Voter-ID") String voterId,
                                      @Body JZFeedback jzFeedbackBody);

    @Headers("Content-Type: application/json")
    @POST("/events/58b3bfaa-4981-11e5-a151-feff819cdc9f/sessions/58b3c298-4981-11e5-a151-feff819cdc9f/feedbacks")
    Call<String> postSessionFeedbackTest(@Header("Voter-ID") String voterId,
                                 @Body JZFeedback jzFeedbackBody,
                                 Callback<JsonElement> success);

}
