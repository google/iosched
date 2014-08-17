package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import co.touchlab.android.superbus.http.RetrofitBusErrorHandler;
import co.touchlab.droidconnyc.R;
import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.android.AndroidLog;
import retrofit.converter.GsonConverter;

/**
 * Created by kgalligan on 8/17/14.
 */
public class DataHelper
{
    public static RestAdapter makeRequestAdapter(Context context)
    {
        RestAdapter.Builder builder = makeRequestAdapterBuilder(context);
        return builder
                .build();
    }

    public static RestAdapter.Builder makeRequestAdapterBuilder(Context context)
    {
        return makeRequestAdapterBuilder(context, new RetrofitBusErrorHandler());
    }

    public static RestAdapter.Builder makeRequestAdapterBuilder(Context context, ErrorHandler errorHandler)
    {
        AppPrefs appPrefs = AppPrefs.getInstance(context);
        final String userUuid = appPrefs.getUserUuid();

        RequestInterceptor requestInterceptor = new RequestInterceptor()
        {
            @Override
            public void intercept(RequestFacade request)
            {
                request.addHeader("Accept", "application/json");
                if (!TextUtils.isEmpty(userUuid))
                    request.addHeader("uuid", userUuid);
            }
        };
        Gson gson = new GsonBuilder().create();
        GsonConverter gsonConverter = new GsonConverter(gson);

        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setRequestInterceptor(requestInterceptor)
                .setConverter(gsonConverter)
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog("DroidconApp"))
                .setEndpoint(context.getString(R.string.base_url));

        if (errorHandler != null)
            builder.setErrorHandler(errorHandler);

        return builder;
    }
}
