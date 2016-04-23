package no.java.ems.client.android.lib.retrofit;

import android.support.annotation.NonNull;

import com.squareup.okhttp.OkHttpClient;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
public class RetrofitUtillity {
    @NonNull
    public static Retrofit createRetrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(EMSService.EMS_ROOT)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
    }
}
