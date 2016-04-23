package no.java.ems.client.android.model;

import android.support.annotation.NonNull;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import java.io.File;
import java.io.IOException;

import no.java.ems.client.android.lib.retrofit.RetrofitUtillity;


public class RetrofitTestUtilities extends RetrofitUtillity {
    @NonNull
    public static OkHttpClient createDebugOkHttpClient() {
        OkHttpClient client = new OkHttpClient();
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client.interceptors().add(interceptor);
        return client;
    }

    public static void enableCache(OkHttpClient client) {
        int cacheSize = 100 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(new File("/Users/oyvlokli/Projects/JavaBin/EMSClientLibrary/cache/"), cacheSize);
        client.setCache(cache);
    }


    public static void enableForceCache(OkHttpClient okHttpClient) {

        okHttpClient.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {

                Request forceCacheRequest = chain.request().newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();

                return chain.proceed(forceCacheRequest);
            }
        });
    }
}
