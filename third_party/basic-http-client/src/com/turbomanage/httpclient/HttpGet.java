package com.turbomanage.httpclient;

/**
 * An HTTP GET request.
 * 
 * @author David M. Chandler
 */
public class HttpGet extends HttpRequest {

    /**
     * Constructs an HTTP GET request.
     * 
     * @param path Partial URL
     * @param params Name-value pairs to be appended to the URL
     */
    public HttpGet(String path, ParameterMap params) {
        super(path, params);
        this.httpMethod = HttpMethod.GET;
    }

}
