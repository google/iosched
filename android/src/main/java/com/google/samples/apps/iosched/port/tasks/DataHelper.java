package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;
import co.touchlab.android.superbus.http.RetrofitBusErrorHandler;
import co.touchlab.droidconnyc.R;
import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.Response;
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
        return makeRequestAdapterBuilder(context, new AppBusErrorHandler());
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

    public static class AppBusErrorHandler implements ErrorHandler
    {
        @Override
        public Throwable handleError(RetrofitError cause)
        {
            if(cause.isNetworkError())
            {
                return new TransientException(cause.getCause());
            }

            return handleErrorCustom(cause);
        }

        protected Throwable handleErrorCustom(RetrofitError cause)
        {
            AppPermanentException appPermanentException = new AppPermanentException(cause.getCause());
            Response response = cause.getResponse();
            if(response != null)
            {
                appPermanentException.setStatus(response.getStatus());
                try
                {
                    String body = IOUtils.toString(response.getBody().in());
                    appPermanentException.setResponseBody(body);
                }
                catch (Exception e)
                {
                    //Uhh.... TODO: something
                }
            }
            return appPermanentException;
        }
    }

    public static class AppPermanentException extends PermanentException
    {
        private String responseBody;
        private int status;

        public AppPermanentException()
        {
        }

        public AppPermanentException(String detailMessage)
        {
            super(detailMessage);
        }

        public AppPermanentException(String detailMessage, Throwable throwable)
        {
            super(detailMessage, throwable);
        }

        public AppPermanentException(Throwable throwable)
        {
            super(throwable);
        }

        public String getResponseBody()
        {
            return responseBody;
        }

        public void setResponseBody(String responseBody)
        {
            this.responseBody = responseBody;
        }

        public int getStatus()
        {
            return status;
        }

        public void setStatus(int status)
        {
            this.status = status;
        }
    }
}
