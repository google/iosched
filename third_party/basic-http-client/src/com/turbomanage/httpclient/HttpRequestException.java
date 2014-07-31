package com.turbomanage.httpclient;

/**
 * Custom exception class that holds an {@link HttpResponse}.
 * This allows upstream code to receive an HTTP status code and 
 * any content received as well as the underlying exception.
 * 
 * @author David M. Chandler
 */
public class HttpRequestException extends Exception {

    private static final long serialVersionUID = -2413629666163901633L;
    
    private HttpResponse httpResponse;
    
    /**
     * Constructs the exception with 
     * 
     * @param e
     * @param httpResponse
     */
    public HttpRequestException(Exception e, HttpResponse httpResponse) {
        super(e);
        this.httpResponse = httpResponse;
    }

    /**
     * Access the response.
     * 
     * @return Response object
     */
    public HttpResponse getHttpResponse() {
        return httpResponse;
    }
}
