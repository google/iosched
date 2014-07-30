package com.turbomanage.httpclient.android;

import android.os.AsyncTask;

import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.AsyncHttpClient;
import com.turbomanage.httpclient.AsyncRequestExecutor;
import com.turbomanage.httpclient.AsyncRequestExecutorFactory;

/**
 * Android-specific factory produces an {@link AsyncTask} that can 
 * execute an HTTP request. 
 * 
 * @author David M. Chandler
 */
public class AsyncTaskFactory implements AsyncRequestExecutorFactory {

    /* (non-Javadoc)
     * @see com.turbomanage.httpclient.AsyncRequestExecutorFactory#getAsyncRequestExecutor(com.turbomanage.httpclient.AsyncHttpClient, com.turbomanage.httpclient.AsyncCallback)
     */
    @Override
    public AsyncRequestExecutor getAsyncRequestExecutor(AsyncHttpClient client,
            AsyncCallback callback) {
        return new DoHttpRequestTask(client, callback);
    }

}
