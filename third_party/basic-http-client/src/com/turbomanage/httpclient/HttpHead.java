package com.turbomanage.httpclient;

/**
 * An HTTP HEAD request.
 * 
 * @author David M. Chandler
 */
public class HttpHead extends HttpRequest {

    /**
     * Constructs an HTTP HEAD request.
     * 
     * @param path Partial URL
     * @param params Name-value pairs to be appended to the URL
     */
    public HttpHead(String path, ParameterMap params) {
        super(path, params);
        this.httpMethod = HttpMethod.HEAD;
    }

}
