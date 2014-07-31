package com.turbomanage.httpclient;

/**
 * Callback passed to an {@link AsyncRequestExecutor} so that the
 * calling code can be notified when a request is complete or
 * has thrown an exception.
 * 
 * @author David M. Chandler
 */
public abstract class AsyncCallback {

    /**
     * Called when response is available or max retries exhausted. 
     * 
     * @param httpResponse may be null!
     */
    public abstract void onComplete(HttpResponse httpResponse);
    
    /**
     * Called when a non-recoverable exception has occurred.
     * Timeout exceptions are considered recoverable and won't
     * trigger this call.
     * 
     * @param e
     */
    public void onError(Exception e) {
        e.printStackTrace();
    }

}
