package com.google.samples.apps.iosched.util;

import com.google.gson.JsonElement;

import no.java.schedule.io.model.JZFeedback;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;

public interface RestDevApi {
    @Headers( "Content-Type: application/json" )
    @POST("/events/{eventId}/sessions/{sessionId}/feedbacks")
    void postSessionFeedback(@Path("eventId") String eventId,
                             @Path("sessionId") String sessionId,
                             @Header("Voter-ID") String voterId,
                             @Body JZFeedback jzFeedbackBody,
                             Callback<JsonElement> success);

    @Headers("Content-Type: application/json")
    @POST("/events/58b3bfaa-4981-11e5-a151-feff819cdc9f/sessions/58b3c298-4981-11e5-a151-feff819cdc9f/feedbacks")
    void postSessionFeedbackTest(@Header("Voter-ID") String voterId,
                                 @Body JZFeedback jzFeedbackBody,
                                 Callback<JsonElement> success);

}
