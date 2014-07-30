
package com.turbomanage.httpclient;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * HTTP request logger used by {@link BasicHttpClient}.
 * 
 * @author David M. Chandler
 */
public interface RequestLogger {
    
    /**
     * Determine whether requests should be logged.
     * 
     * @return true if enabled
     */
    boolean isLoggingEnabled();
    
    /**
     * Writes a log message.
     * 
     * @param msg
     */
    void log(String msg);

    /**
     * Log the HTTP request and content to be sent with the request.
     * 
     * @param urlConnection
     * @param content
     * @throws IOException
     */
    void logRequest(HttpURLConnection urlConnection, Object content) throws IOException;

    /**
     * Logs the HTTP response.
     * 
     * @param httpResponse
     * @throws IOException
     */
    void logResponse(HttpResponse httpResponse);
    
}
