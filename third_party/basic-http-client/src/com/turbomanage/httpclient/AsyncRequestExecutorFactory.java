package com.turbomanage.httpclient;

/**
 * Factory that provides an async wrapper for a request, that is, something
 * that can execute a request asynchronously. 
 * 
 * @author David M. Chandler
 */
public interface AsyncRequestExecutorFactory {

    AsyncRequestExecutor getAsyncRequestExecutor(AsyncHttpClient client, AsyncCallback callback);
    
}
