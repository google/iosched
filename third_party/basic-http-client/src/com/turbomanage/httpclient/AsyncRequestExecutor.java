package com.turbomanage.httpclient;

/**
 * Describes a class that can execute a request asynchronously. These are
 * produced by a {@link AsyncRequestExecutorFactory}.
 * 
 * @author David M. Chandler
 */
public interface AsyncRequestExecutor {
    
    void execute(HttpRequest httpRequest);
    
}
